package com.example.medicalhomevisit.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val authRepository: AuthRepository
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
        // Получаем текущего пользователя и загружаем визиты
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                currentUserId = user?.id
                if (currentUserId != null) {
                    loadVisits()
                    // В HTTP режиме нет постоянного наблюдения,
                    // поэтому убираем observeVisits()
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
                    if (allVisits.isNotEmpty()) {
                        // Фильтруем только визиты текущего пользователя
                        val userVisits = allVisits.filter { visit ->
                            visit.assignedStaffId == currentUserId
                        }
                        _allVisits.value = userVisits
                        applyFilters()
                        _isOffline.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing visits: ${e.message}", e)
            }
        }
    }

    fun loadVisits() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _uiState.value = VisitListUiState.Loading

            try {
                val visits = visitRepository.getVisitsForStaff(userId)

                _allVisits.value = visits

                applyFilters()


                _isOffline.value = false

                Log.d(TAG, "Loaded ${visits.size} visits for staff $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading visits: ${e.message}", e)

                try {
                    val cachedVisits = visitRepository.getCachedVisits()

                    if (cachedVisits.isNotEmpty()) {
                        _allVisits.value = cachedVisits
                        applyFilters()
                        _isOffline.value = true
                    } else {
                        _uiState.value = VisitListUiState.Error(e.message ?: "Неизвестная ошибка")
                    }
                } catch (cacheEx: Exception) {
                    _uiState.value = VisitListUiState.Error(e.message ?: "Неизвестная ошибка")
                }
            }
        }
    }

    // Синхронизация данных (для использования при восстановлении подключения)
    fun syncVisits() {
        viewModelScope.launch {
            try {
                val result = visitRepository.syncVisits()
                if (result.isSuccess) {
                    val allVisits = result.getOrNull() ?: emptyList()
                    _allVisits.value = allVisits
                    applyFilters()
                    _isOffline.value = false
                    Log.d(TAG, "Visits synced successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing visits: ${e.message}", e)
            }
        }
    }

    // Обновляем дату фильтра
    fun updateSelectedDate(date: Date) {
        _filterParams.update { it.copy(selectedDate = date) }

        // Загружаем визиты для выбранной даты
        viewModelScope.launch {
            try {
                val visits = visitRepository.getVisitsForDate(date)
                _allVisits.value = visits
                applyFilters()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading visits for date: ${e.message}", e)
                // В случае ошибки оставляем текущие данные и просто применяем фильтры
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
    private fun applyFilters() {
        val filteredVisits = _allVisits.value.filter { visit ->
            // Фильтр по дате
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

        // Обновляем UI-состояние
        _uiState.value = if (filteredVisits.isEmpty()) {
            VisitListUiState.Empty
        } else {
            VisitListUiState.Success(filteredVisits)
        }

        Log.d(TAG, "Applied filters: ${filteredVisits.size} visits shown")
    }

    fun updateVisitStatus(visitId: String, newStatus: VisitStatus) {
        viewModelScope.launch {
            try {
                // Оптимистично обновляем UI
                val currentVisits = _allVisits.value.toMutableList()
                val index = currentVisits.indexOfFirst { it.id == visitId }

                if (index != -1) {
                    val updatedVisit = currentVisits[index].copy(status = newStatus)
                    currentVisits[index] = updatedVisit
                    _allVisits.value = currentVisits
                    applyFilters()
                }

                // Выполняем обновление на сервере
                visitRepository.updateVisitStatus(visitId, newStatus)

                Log.d(TAG, "Visit status updated: $visitId -> $newStatus")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating visit status: ${e.message}", e)
                // В случае ошибки перезагружаем данные
                loadVisits()
            }
        }
    }

    // Добавим новые методы для работы с визитами
    fun updateVisitNotes(visitId: String, notes: String) {
        viewModelScope.launch {
            try {
                visitRepository.updateVisitNotes(visitId, notes)

                // Обновляем локальный кэш
                val currentVisits = _allVisits.value.toMutableList()
                val index = currentVisits.indexOfFirst { it.id == visitId }
                if (index != -1) {
                    currentVisits[index] = currentVisits[index].copy(notes = notes)
                    _allVisits.value = currentVisits
                    applyFilters()
                }

                Log.d(TAG, "Visit notes updated: $visitId")
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