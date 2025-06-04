package com.example.medicalhomevisit.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        // 🔄 ОФЛАЙН РЕЖИМ: Используем observeVisits для реактивности
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                currentUserId = user?.id
                if (currentUserId != null) {
                    // Сначала загружаем данные
                    loadVisits()

                    // Затем подписываемся на изменения в Room
                    observeVisits()

                    // Настраиваем периодическую синхронизацию
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

                    // Определяем, работаем ли мы офлайн
                    // (простая эвристика - если данные есть, значит загрузились когда-то)
                    _isOffline.value = allVisits.isEmpty()
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

            try {
                // В офлайн режиме данные придут через observeVisits()
                // Но мы все равно запускаем загрузку для обновления
                val visits = visitRepository.getVisitsForStaff(userId)
                Log.d(TAG, "📱 Initial load: ${visits.size} visits")

                _allVisits.value = visits
                applyFilters()

            } catch (e: Exception) {
                Log.e(TAG, "Error in loadVisits: ${e.message}", e)
                // В офлайн режиме ошибки не критичны - данные придут через observeVisits
                if (_allVisits.value.isEmpty()) {
                    _uiState.value = VisitListUiState.Error(e.message ?: "Неизвестная ошибка")
                }
            }
        }
    }

    // 🔄 ОФЛАЙН РЕЖИМ: Принудительная синхронизация
    fun syncVisits() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Manual sync requested")
                syncManager.syncNow()

                // Данные обновятся автоматически через observeVisits()
                _isOffline.value = false

            } catch (e: Exception) {
                Log.e(TAG, "Error syncing visits: ${e.message}", e)
                _isOffline.value = true
            }
        }
    }

    // 🔄 ОФЛАЙН РЕЖИМ: Обновление даты (работает офлайн)
    fun updateSelectedDate(date: Date) {
        _filterParams.update { it.copy(selectedDate = date) }

        // Загружаем данные для даты (если есть интернет)
        viewModelScope.launch {
            try {
                visitRepository.getVisitsForDate(date)
                // Данные обновятся через observeVisits()
            } catch (e: Exception) {
                Log.w(TAG, "Error loading visits for date: ${e.message}")
                // В офлайн режиме просто применяем фильтры к существующим данным
                applyFilters()
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

    // Применяем фильтры к списку визитов
    // Применяем фильтры к списку визитов
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
            VisitListUiState.Empty
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

    // 🔄 Проверка статуса синхронизации
    fun checkSyncStatus() {
        viewModelScope.launch {
            try {
                // Можно добавить метод в репозиторий для проверки количества несинхронизированных записей
                // val unsyncedCount = visitRepository.getUnsyncedCount()
                // _isOffline.value = unsyncedCount > 0
            } catch (e: Exception) {
                Log.w(TAG, "Error checking sync status: ${e.message}")
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