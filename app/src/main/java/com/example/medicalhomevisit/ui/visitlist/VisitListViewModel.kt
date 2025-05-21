package com.example.medicalhomevisit.ui.visitlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.data.model.Visit
import com.example.medicalhomevisit.data.model.VisitStatus
import com.example.medicalhomevisit.domain.repository.VisitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class VisitListViewModel(
    private val visitRepository: VisitRepository
) : ViewModel() {

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

    init {
        loadVisits()
    }

    fun loadVisits() {
        viewModelScope.launch {
            _uiState.value = VisitListUiState.Loading

            try {
                val visits = visitRepository.getVisitsForToday()

                // Сохраняем все визиты
                _allVisits.value = visits

                // Применяем текущие фильтры
                applyFilters()
            } catch (e: Exception) {
                Log.e("VisitList", "Error loading visits: ${e.message}", e)
                _uiState.value = VisitListUiState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    // Обновляем дату фильтра
    fun updateSelectedDate(date: Date) {
        _filterParams.update { it.copy(selectedDate = date) }
        applyFilters()
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
    }

    fun updateVisitStatus(visitId: String, newStatus: VisitStatus) {
        viewModelScope.launch {
            try {
                visitRepository.updateVisitStatus(visitId, newStatus)
                loadVisits() // Перезагружаем список визитов
            } catch (e: Exception) {
                Log.e("VisitList", "Error updating visit status: ${e.message}", e)
                // Обработка ошибки
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