package com.example.medicalhomevisit.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.data.repository.SimpleOfflineVisitRepository
import com.example.medicalhomevisit.data.sync.SyncManager
import com.example.medicalhomevisit.domain.model.Visit
import com.example.medicalhomevisit.domain.model.VisitStatus
import com.example.medicalhomevisit.domain.repository.AuthRepository
import com.example.medicalhomevisit.domain.repository.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class VisitListViewModel @Inject constructor(
    private val visitRepository: VisitRepository,
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    companion object {
        private const val TAG = "VisitListViewModel"
    }

    // UI состояние
    private val _uiState = MutableStateFlow<VisitListUiState>(VisitListUiState.Loading)
    val uiState: StateFlow<VisitListUiState> = _uiState.asStateFlow()

    // Флаг офлайн режима
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    // Параметры фильтрации и группировки
    private val _filterParams = MutableStateFlow(FilterParams())
    val filterParams: StateFlow<FilterParams> = _filterParams.asStateFlow()

    // Все загруженные визиты (без фильтрации)
    private val _allVisits = MutableStateFlow<List<Visit>>(emptyList())

    // Текущий поисковый запрос
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // ID текущего пользователя
    private var currentUserId: String? = null

    // ✅ ИСПРАВЛЕНО: Добавляем флаг для отслеживания первоначальной загрузки
    private var hasTriedInitialLoad = false

    init {
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                currentUserId = user?.id
                if (currentUserId != null) {
                    loadVisits()
                    observeVisits()
                    syncManager.setupPeriodicSync()
                } else {
                    _allVisits.value = emptyList()
                    _uiState.value = VisitListUiState.Empty
                }
            }
        }
    }

    private fun observeVisits() {
        viewModelScope.launch {
            try {
                visitRepository.observeVisits().collectLatest { allVisits ->
                    Log.d(TAG, "📱 Received ${allVisits.size} visits from Room")
                    _allVisits.value = allVisits
                    applyFilters()

                    // ✅ ИСПРАВЛЕНО: Правильная логика определения офлайн режима
                    // Если мы пытались загрузить данные и получили их - значит не офлайн
                    if (hasTriedInitialLoad && allVisits.isNotEmpty()) {
                        _isOffline.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing visits: ${e.message}", e)
                _uiState.value = VisitListUiState.Error(e.message ?: "Ошибка загрузки данных")
            }
        }
    }

    fun loadVisits() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _uiState.value = VisitListUiState.Loading
            hasTriedInitialLoad = true

            try {
                // Пытаемся загрузить с сервера
                val visits = visitRepository.getVisitsForStaff(userId)
                Log.d(TAG, "📱 Initial load: ${visits.size} visits")

                _allVisits.value = visits
                applyFilters()

                // ✅ ИСПРАВЛЕНО: Если загрузка прошла успешно - мы онлайн
                _isOffline.value = false

            } catch (e: Exception) {
                Log.e(TAG, "Error in loadVisits: ${e.message}", e)

                // ✅ ИСПРАВЛЕНО: Ошибка загрузки = офлайн режим
                _isOffline.value = true

                // Если в Room есть данные, показываем их
                if (_allVisits.value.isEmpty()) {
                    _uiState.value = VisitListUiState.Error("Нет подключения к интернету")
                } else {
                    // У нас есть кэшированные данные
                    applyFilters()
                }
            }
        }
    }

    // 🔄 ОФЛАЙН РЕЖИМ: Принудительная синхронизация
    fun syncVisits() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Manual sync requested")

                // Показываем индикатор синхронизации
                _isOffline.value = false

                syncManager.syncNow()

                // Пытаемся загрузить свежие данные
                loadVisits()

            } catch (e: Exception) {
                Log.e(TAG, "Error syncing visits: ${e.message}", e)
                _isOffline.value = true
            }
        }
    }

    // 🔄 ОФЛАЙН РЕЖИМ: Обновление даты (работает офлайн)
    fun updateSelectedDate(date: Date) {
        _filterParams.update { it.copy(selectedDate = date) }
        applyFilters() // <--- ДОБАВЬ ЭТОТ ВЫЗОВ ЗДЕСЬ

        // Загружаем данные для даты (если есть интернет)
        // Эта корутина по-прежнему полезна для попытки обновить данные с сервера
        viewModelScope.launch {
            try {
                visitRepository.getVisitsForDate(date) // Пытаемся загрузить с сервера для новой даты
                _isOffline.value = false
                // Если observeVisits() среагирует на изменения в Room после getVisitsForDate,
                // он снова вызовет applyFilters(), что не страшно и обеспечит актуальность.
            } catch (e: Exception) {
                Log.w(TAG, "Error loading visits for date: ${e.message}")
                _isOffline.value = true
                // В офлайн режиме applyFilters() был вызван выше, так что здесь он уже не так критичен.
                // Можно оставить, если есть специфичная логика для UI при ошибке,
                // но основной эффект перефильтрации уже достигнут.
                // Для чистоты, если он уже вызван безусловно выше, здесь можно убрать.
                // applyFilters() // Например, убрать отсюда, если он есть выше.
            }
        }
    }

    // Обновляем статус фильтра
    fun updateSelectedStatus(status: VisitStatus?) {
        _filterParams.update { it.copy(selectedStatus = status) }
        applyFilters()
    }

    // Обновляем тип группировки
    fun updateGroupingType(type: GroupingType) {
        _filterParams.update { it.copy(groupingType = type) }
        applyFilters()
    }

    // Обновляем поисковый запрос
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }


    private fun applyFilters() {
        val filteredVisits = _allVisits.value.filter { visit ->
            val visitDate = Calendar.getInstance().apply { time = visit.scheduledTime }
            val selectedDate = Calendar.getInstance().apply { time = _filterParams.value.selectedDate }
            val sameDay = visitDate.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    visitDate.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)

            // Фильтр по статусу
            val statusMatches = _filterParams.value.selectedStatus == null ||
                    visit.status == _filterParams.value.selectedStatus

            // Фильтр по поисковому запросу
            val searchMatches = _searchQuery.value.isEmpty() ||
                    visit.address.contains(_searchQuery.value, ignoreCase = true) ||
                    visit.reasonForVisit.contains(_searchQuery.value, ignoreCase = true)

            sameDay && statusMatches && searchMatches
        }

        Log.d(TAG, "🔍 Filtered ${filteredVisits.size} visits from ${_allVisits.value.size} total")

        // Обновляем UI-состояние
        _uiState.value = if (filteredVisits.isEmpty()) {
            if (_allVisits.value.isEmpty() && _isOffline.value) {
                VisitListUiState.Error("Нет данных. Проверьте подключение к интернету.")
            } else {
                VisitListUiState.Empty
            }
        } else {
            VisitListUiState.Success(filteredVisits)
        }
    }

    // 🔄 ОФЛАЙН РЕЖИМ: Обновление статуса работает офлайн
    fun updateVisitStatus(visitId: String, newStatus: VisitStatus) {
        viewModelScope.launch {
            try {
                // В офлайн режиме сразу обновляем Room, синхронизация будет позже
                visitRepository.updateVisitStatus(visitId, newStatus)
                Log.d(TAG, "✅ Visit status updated: $visitId -> $newStatus")

                // UI обновится автоматически через observeVisits()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating visit status: ${e.message}", e)
                // В офлайн режиме показываем ошибку, но не перезагружаем данные
            }
        }
    }

    // 🔄 ОФЛАЙН РЕЖИМ: Обновление заметок работает офлайн
    fun updateVisitNotes(visitId: String, notes: String) {
        viewModelScope.launch {
            try {
                visitRepository.updateVisitNotes(visitId, notes)
                Log.d(TAG, "✅ Visit notes updated: $visitId")

                // UI обновится автоматически через observeVisits()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating visit notes: ${e.message}", e)
            }
        }
    }

    fun startVisit(visitId: String) {
        updateVisitStatus(visitId, VisitStatus.IN_PROGRESS)
    }

    fun completeVisit(visitId: String) {
        updateVisitStatus(visitId, VisitStatus.COMPLETED)
    }

    fun checkSyncStatus() {
        viewModelScope.launch {
            try {
                // ✅ ИСПРАВЛЕНО: Проверяем количество несинхронизированных записей
                val unsyncedCount = (visitRepository as? SimpleOfflineVisitRepository)?.getUnsyncedCount() ?: 0

                // Если есть несинхронизированные записи - показываем индикатор
                if (unsyncedCount > 0 && !_isOffline.value) {
                    Log.d(TAG, "📊 Found $unsyncedCount unsynced visits")
                    // Можно добавить отдельный индикатор "есть изменения для синхронизации"
                }

            } catch (e: Exception) {
                Log.w(TAG, "Error checking sync status: ${e.message}")
            }
        }
    }

    // 🔄 Получение статистики синхронизации (для отладки)
    fun getSyncStats() {
        viewModelScope.launch {
            try {
                val repo = visitRepository as? SimpleOfflineVisitRepository
                if (repo != null) {
                    val unsyncedCount = repo.getUnsyncedCount()
                    val unsyncedVisits = repo.getUnsyncedVisits()

                    Log.d(TAG, "📊 SYNC STATS:")
                    Log.d(TAG, "   - Total visits: ${_allVisits.value.size}")
                    Log.d(TAG, "   - Unsynced count: $unsyncedCount")
                    Log.d(TAG, "   - Unsynced visits: ${unsyncedVisits.map { it.id }}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting sync stats: ${e.message}")
            }
        }
    }
}

// Параметры фильтрации
data class FilterParams(
    val selectedDate: Date = Calendar.getInstance().time,
    val selectedStatus: VisitStatus? = null,
    val groupingType: GroupingType = GroupingType.TIME
)

// Типы группировки визитов
enum class GroupingType {
    NONE,      // Без группировки
    TIME,      // По времени (утро, день, вечер)
    STATUS,    // По статусу
    ADDRESS    // По району/адресу
}

sealed class VisitListUiState {
    object Loading : VisitListUiState()
    object Empty : VisitListUiState()
    data class Success(val visits: List<Visit>) : VisitListUiState()
    data class Error(val message: String) : VisitListUiState()
}