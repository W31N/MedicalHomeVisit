package com.example.medicalhomevisit.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.data.di.OfflinePatientRepository
import com.example.medicalhomevisit.domain.model.AppointmentRequest
import com.example.medicalhomevisit.domain.model.Patient
import com.example.medicalhomevisit.domain.model.RequestStatus
import com.example.medicalhomevisit.domain.model.Visit
import com.example.medicalhomevisit.domain.model.VisitStatus
import com.example.medicalhomevisit.domain.repository.AppointmentRequestRepository
import com.example.medicalhomevisit.domain.repository.PatientRepository
import com.example.medicalhomevisit.domain.repository.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VisitDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val visitRepository: VisitRepository,
    private val appointmentRequestRepository: AppointmentRequestRepository,
    @OfflinePatientRepository private val patientRepository: PatientRepository
) : ViewModel() {

    companion object {
        private const val TAG = "VisitDetailViewModel"
    }

    private val visitId: String = checkNotNull(savedStateHandle.get<String>("visitId")) {
        "visitId is required"
    }

    private val _uiState = MutableStateFlow<VisitDetailUiState>(VisitDetailUiState.Loading)
    val uiState: StateFlow<VisitDetailUiState> = _uiState.asStateFlow()

    private val _patientState = MutableStateFlow<PatientState>(PatientState.Loading)
    val patientState: StateFlow<PatientState> = _patientState.asStateFlow()

    private val _originalRequest = MutableStateFlow<AppointmentRequest?>(null)
    val originalRequest: StateFlow<AppointmentRequest?> = _originalRequest.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _visit = MutableStateFlow<Visit?>(null)
    private val visit: StateFlow<Visit?> = _visit.asStateFlow()

    init {
        Log.d(TAG, "VisitDetailViewModel initialized with visitId: $visitId")
        observeVisitChanges()
        loadVisitDetails()
    }

    private fun observeVisitChanges() {
        viewModelScope.launch {
            try {
                visitRepository.observeVisits().collectLatest { visits ->
                    val updatedVisit = visits.find { it.id == visitId }
                    updatedVisit?.let {
                        _visit.value = it
                        _uiState.value = VisitDetailUiState.Success(it)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing visits: ${e.message}", e)
            }
        }
    }

    private fun loadVisitDetails() {
        viewModelScope.launch {
            _uiState.value = VisitDetailUiState.Loading

            try {
                val visit = visitRepository.getVisitById(visitId)
                _visit.value = visit
                _uiState.value = VisitDetailUiState.Success(visit)
                Log.d(TAG, "Visit loaded: ${visit.id}, Patient ID for this visit: ${visit.patientId}")

                if (visit.isFromRequest && visit.originalRequestId != null) {
                    loadOriginalRequest(visit.originalRequestId)
                }

                loadPatientDetails(visit.patientId)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading visit: ${e.message}", e)

                try {
                    val cachedVisits = visitRepository.getCachedVisits()
                    val cachedVisit = cachedVisits.find { it.id == visitId }
                    if (cachedVisit != null) {
                        _visit.value = cachedVisit
                        _uiState.value = VisitDetailUiState.Success(cachedVisit)
                        _isOffline.value = true
                        loadPatientDetails(cachedVisit.patientId)

                        if (cachedVisit.isFromRequest && cachedVisit.originalRequestId != null) {
                            loadOriginalRequest(cachedVisit.originalRequestId)
                        }
                    } else {
                        _uiState.value = VisitDetailUiState.Error(e.message ?: "Неизвестная ошибка")
                    }
                } catch (cacheEx: Exception) {
                    _uiState.value = VisitDetailUiState.Error(e.message ?: "Неизвестная ошибка")
                }
            }
        }
    }

    private fun loadOriginalRequest(requestId: String) {
        viewModelScope.launch {
            try {
                val result = appointmentRequestRepository.getRequestById(requestId)
                if (result.isSuccess) {
                    val originalRequest = result.getOrNull()
                    _originalRequest.value = originalRequest
                    Log.d(TAG, "Original request loaded: ${originalRequest?.id}")
                } else {
                    Log.w(TAG, "Could not load original request: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load original request: ${e.message}", e)
            }
        }
    }

    private fun loadPatientDetails(patientId: String) {
        viewModelScope.launch {
            _patientState.value = PatientState.Loading
            Log.d(TAG, "Attempting to load patient details for patientId: '$patientId'")

            if (patientId.isBlank()) {
                Log.e(TAG, "Patient ID is blank, cannot load details.")
                _patientState.value = PatientState.Error("ID пациента не указан для визита")
                return@launch
            }

            try {
                val patient = patientRepository.getPatientById(patientId)
                _patientState.value = PatientState.Success(patient)
                Log.d(TAG, "Patient loaded successfully: ${patient.fullName}")

                observePatientChanges(patientId)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading patient: ${e.message}", e)

                try {
                    val cachedPatient = patientRepository.getCachedPatientById(patientId)
                    if (cachedPatient != null) {
                        _patientState.value = PatientState.Success(cachedPatient)
                        _isOffline.value = true
                        Log.d(TAG, "Patient loaded from cache: ${cachedPatient.fullName}")
                        observePatientChanges(patientId)
                    } else {
                        _patientState.value = PatientState.Error(
                            "Не удалось загрузить данные пациента. " +
                                    "Проверьте подключение к интернету и повторите попытку."
                        )
                        Log.e(TAG, "Patient not found anywhere for ID '$patientId'")
                    }
                } catch (cacheEx: Exception) {
                    _patientState.value = PatientState.Error(
                        "Ошибка загрузки данных пациента: ${cacheEx.message}"
                    )
                    Log.e(TAG, "Error accessing cache for patient ID '$patientId': ${cacheEx.message}", cacheEx)
                }
            }
        }
    }

    private fun observePatientChanges(patientId: String) {
        viewModelScope.launch {
            try {
                patientRepository.observePatient(patientId).collectLatest { patient ->
                    _patientState.value = PatientState.Success(patient)
                    Log.d(TAG, "Patient details updated via observation: ${patient.fullName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing patient changes: ${e.message}", e)
            }
        }
    }

    fun updateVisitStatus(newStatus: VisitStatus) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating visit status to: $newStatus")
                val currentVisit = _visit.value ?: return@launch

                val updatedVisit = currentVisit.copy(status = newStatus)
                _visit.value = updatedVisit
                _uiState.value = VisitDetailUiState.Success(updatedVisit)

                visitRepository.updateVisitStatus(visitId, newStatus)

                if (currentVisit.isFromRequest && currentVisit.originalRequestId != null) {
                    val requestStatus = mapVisitStatusToRequestStatus(newStatus)
                    Log.d(TAG, "Updating original request status to: $requestStatus")

                    val result = appointmentRequestRepository.updateRequestStatus(
                        currentVisit.originalRequestId,
                        requestStatus,
                        null
                    )

                    if (result.isSuccess) {
                        val updatedRequest = result.getOrNull()
                        _originalRequest.value = updatedRequest
                        Log.d(TAG, "Original request status updated successfully")
                    } else {
                        Log.w(TAG, "Failed to update original request status: ${result.exceptionOrNull()?.message}")
                    }
                }

                Log.d(TAG, "Status updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating status: ${e.message}", e)
                _isOffline.value = true
                loadVisitDetails()
            }
        }
    }

    private fun mapVisitStatusToRequestStatus(visitStatus: VisitStatus): RequestStatus {
        return when (visitStatus) {
            VisitStatus.PLANNED -> RequestStatus.SCHEDULED
            VisitStatus.IN_PROGRESS -> RequestStatus.IN_PROGRESS
            VisitStatus.COMPLETED -> RequestStatus.COMPLETED
            VisitStatus.CANCELLED -> RequestStatus.CANCELLED
        }
    }

    fun canCreateProtocol(): Boolean {
        val currentVisit = _visit.value
        return currentVisit != null &&
                (currentVisit.status == VisitStatus.IN_PROGRESS ||
                        currentVisit.status == VisitStatus.COMPLETED)
    }
}

sealed class VisitDetailUiState {
    object Loading : VisitDetailUiState()
    data class Success(val visit: Visit) : VisitDetailUiState()
    data class Error(val message: String) : VisitDetailUiState()
}

sealed class PatientState {
    object Loading : PatientState()
    data class Success(val patient: Patient) : PatientState()
    data class Error(val message: String) : PatientState()
}