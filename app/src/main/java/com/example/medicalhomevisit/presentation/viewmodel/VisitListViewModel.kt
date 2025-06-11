package com.example.medicalhomevisit.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.data.di.OfflinePatientRepository
import com.example.medicalhomevisit.data.repository.SimpleOfflineVisitRepository
import com.example.medicalhomevisit.data.sync.SyncManager
import com.example.medicalhomevisit.domain.model.Visit
import com.example.medicalhomevisit.domain.model.VisitStatus
import com.example.medicalhomevisit.domain.repository.AuthRepository
import com.example.medicalhomevisit.domain.repository.PatientRepository
import com.example.medicalhomevisit.domain.repository.ProtocolTemplateRepository
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
    private val syncManager: SyncManager,
    @OfflinePatientRepository private val patientRepository: PatientRepository,
    private val protocolTemplateRepository: ProtocolTemplateRepository
) : ViewModel() {

    companion object {
        private const val TAG = "VisitListViewModel"
    }

    private val _uiState = MutableStateFlow<VisitListUiState>(VisitListUiState.Loading)
    val uiState: StateFlow<VisitListUiState> = _uiState.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _filterParams = MutableStateFlow(FilterParams())
    val filterParams: StateFlow<FilterParams> = _filterParams.asStateFlow()

    private val _allVisits = MutableStateFlow<List<Visit>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var currentUserId: String? = null

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
                    Log.d(TAG, "Received ${allVisits.size} visits from Room")
                    _allVisits.value = allVisits
                    applyFilters()

                    preloadPatientsForVisits(allVisits)
                    preloadProtocolTemplates()

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

    private fun preloadProtocolTemplates() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Preloading protocol templates...")

                launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val result = protocolTemplateRepository.refreshTemplates()
                        if (result.isSuccess) {
                            Log.d(TAG, "Protocol templates cached successfully")
                        } else {
                            Log.w(TAG, "Failed to cache protocol templates: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Exception caching protocol templates: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error preloading protocol templates: ${e.message}")
            }
        }
    }

    private fun preloadPatientsForVisits(visits: List<Visit>) {
        if (visits.isEmpty()) return
        viewModelScope.launch {
            try {
                val patientIds = visits.map { it.patientId }.distinct()
                Log.d(TAG, "Preloading ${patientIds.size} patients...")
                patientIds.forEach { patientId ->
                    launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            patientRepository.getPatientById(patientId)
                            Log.d(TAG, "Cached patient: $patientId")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to cache patient $patientId: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error preloading patients: ${e.message}")
            }
        }
    }

    fun loadVisits() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _uiState.value = VisitListUiState.Loading
            hasTriedInitialLoad = true
            try {
                val visits = visitRepository.getVisitsForStaff(userId)
                Log.d(TAG, "Initial load: ${visits.size} visits")
                _allVisits.value = visits
                applyFilters()
                _isOffline.value = false
                preloadProtocolTemplates()
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadVisits: ${e.message}", e)
                _isOffline.value = true
                try {
                    val localTemplates = protocolTemplateRepository.getAllTemplates()
                    Log.d(TAG, "Found ${localTemplates.size} local protocol templates")
                } catch (templateEx: Exception) {
                    Log.w(TAG, "Could not load local templates: ${templateEx.message}")
                }
                if (_allVisits.value.isEmpty()) {
                    _uiState.value = VisitListUiState.Error("Нет подключения к интернету")
                } else {
                    applyFilters()
                }
            }
        }
    }

    fun syncVisits() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Manual sync requested")
                _isOffline.value = false
                syncManager.syncNow()
                preloadProtocolTemplates()
                loadVisits()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing visits: ${e.message}", e)
                _isOffline.value = true
            }
        }
    }

    fun updateSelectedDate(date: Date) {
        _filterParams.update { it.copy(selectedDate = date) }
        applyFilters()

        viewModelScope.launch {
            try {
                visitRepository.getVisitsForDate(date)
                _isOffline.value = false
            } catch (e: Exception) {
                Log.w(TAG, "Error loading visits for date: ${e.message}")
                _isOffline.value = true
            }
        }
    }

    fun updateSelectedStatus(status: VisitStatus?) {
        _filterParams.update { it.copy(selectedStatus = status) }
        applyFilters()
    }

    fun updateGroupingType(type: GroupingType) {
        _filterParams.update { it.copy(groupingType = type) }
        applyFilters()
    }

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

            val statusMatches = _filterParams.value.selectedStatus == null ||
                    visit.status == _filterParams.value.selectedStatus

            val searchMatches = _searchQuery.value.isEmpty() ||
                    visit.address.contains(_searchQuery.value, ignoreCase = true) ||
                    visit.reasonForVisit.contains(_searchQuery.value, ignoreCase = true)

            sameDay && statusMatches && searchMatches
        }

        Log.d(TAG, "Filtered ${filteredVisits.size} visits from ${_allVisits.value.size} total")

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

    fun updateVisitStatus(visitId: String, newStatus: VisitStatus) {
        viewModelScope.launch {
            try {
                visitRepository.updateVisitStatus(visitId, newStatus)
                Log.d(TAG, "Visit status updated: $visitId -> $newStatus")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating visit status: ${e.message}", e)
            }
        }
    }

    fun checkSyncStatus() {
        viewModelScope.launch {
            try {
                val unsyncedCount = (visitRepository as? SimpleOfflineVisitRepository)?.getUnsyncedCount() ?: 0

                if (unsyncedCount > 0 && !_isOffline.value) {
                    Log.d(TAG, "Found $unsyncedCount unsynced visits")
                }

            } catch (e: Exception) {
                Log.w(TAG, "Error checking sync status: ${e.message}")
            }
        }
    }

    fun getSyncStats() {
        viewModelScope.launch {
            try {
                val repo = visitRepository as? SimpleOfflineVisitRepository
                if (repo != null) {
                    val unsyncedCount = repo.getUnsyncedCount()
                    val unsyncedVisits = repo.getUnsyncedVisits()

                    val templates = protocolTemplateRepository.getAllTemplates()

                    Log.d(TAG, "SYNC STATS:")
                    Log.d(TAG, "   - Total visits: ${_allVisits.value.size}")
                    Log.d(TAG, "   - Unsynced count: $unsyncedCount")
                    Log.d(TAG, "   - Unsynced visits: ${unsyncedVisits.map { it.id }}")
                    Log.d(TAG, "   - Protocol templates: ${templates.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting sync stats: ${e.message}")
            }
        }
    }
}

data class FilterParams(
    val selectedDate: Date = Calendar.getInstance().time,
    val selectedStatus: VisitStatus? = null,
    val groupingType: GroupingType = GroupingType.TIME
)

enum class GroupingType {
    NONE,
    TIME,
    STATUS,
    ADDRESS
}

sealed class VisitListUiState {
    object Loading : VisitListUiState()
    object Empty : VisitListUiState()
    data class Success(val visits: List<Visit>) : VisitListUiState()
    data class Error(val message: String) : VisitListUiState()
}