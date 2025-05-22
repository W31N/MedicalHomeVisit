package com.example.medicalhomevisit.ui.visitdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.data.model.AppointmentRequest
import com.example.medicalhomevisit.data.model.Patient
import com.example.medicalhomevisit.data.model.RequestStatus
import com.example.medicalhomevisit.data.model.Visit
import com.example.medicalhomevisit.data.model.VisitStatus
import com.example.medicalhomevisit.domain.repository.AppointmentRequestRepository
import com.example.medicalhomevisit.domain.repository.PatientRepository
import com.example.medicalhomevisit.domain.repository.ProtocolRepository
import com.example.medicalhomevisit.domain.repository.VisitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VisitDetailViewModel(
    private val visitId: String,
    private val visitRepository: VisitRepository,
    private val appointmentRequestRepository: AppointmentRequestRepository,
    private val protocolRepository: ProtocolRepository,
    private val patientRepository: PatientRepository
) : ViewModel() {

    companion object {
        private const val TAG = "VisitDetailViewModel"
    }

    private val _uiState = MutableStateFlow<VisitDetailUiState>(VisitDetailUiState.Loading)
    val uiState: StateFlow<VisitDetailUiState> = _uiState.asStateFlow()

    private val _patientState = MutableStateFlow<PatientState>(PatientState.Loading)
    val patientState: StateFlow<PatientState> = _patientState.asStateFlow()

    private val _originalRequest = MutableStateFlow<AppointmentRequest?>(null)
    val originalRequest: StateFlow<AppointmentRequest?> = _originalRequest.asStateFlow()

    // Флаг офлайн режима
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    // Хранение текущего визита для удобства
    private val _visit = MutableStateFlow<Visit?>(null)
    private val visit: StateFlow<Visit?> = _visit.asStateFlow()

    init {
        Log.d(TAG, "VisitDetailViewModel initialized with visitId: $visitId")
        loadVisitDetails()
        observeVisitChanges()
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
                // Получаем визит из репозитория
                val visit = visitRepository.getVisitById(visitId)
                _visit.value = visit
                _uiState.value = VisitDetailUiState.Success(visit)
                Log.d(TAG, "Visit loaded: ${visit.id}, Patient ID for this visit: ${visit.patientId}") // <--- ЛОГ

                // Если визит создан из заявки, загружаем дополнительную информацию
                if (visit.isFromRequest && visit.originalRequestId != null) {
                    loadOriginalRequest(visit.originalRequestId)
                }

                // Загружаем информацию о пациенте
                loadPatientDetails(visit.patientId)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading visit: ${e.message}", e)

                // Проверяем, есть ли кэшированные данные
                try {
                    val cachedVisits = visitRepository.getCachedVisits()
                    val cachedVisit = cachedVisits.find { it.id == visitId }
                    if (cachedVisit != null) {
                        _visit.value = cachedVisit
                        _uiState.value = VisitDetailUiState.Success(cachedVisit)
                        _isOffline.value = true
                        loadPatientDetails(cachedVisit.patientId)

                        // Попытка загрузить оригинальную заявку из кэша
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
                val originalRequest = appointmentRequestRepository.getRequestById(requestId)
                _originalRequest.value = originalRequest
                Log.d(TAG, "Original request loaded: ${originalRequest.id}")
            } catch (e: Exception) {
                Log.w(TAG, "Could not load original request: ${e.message}", e)
                // Не критичная ошибка, продолжаем работу без оригинальной заявки
            }
        }
    }

    private fun loadPatientDetails(patientId: String) {
        viewModelScope.launch {
            _patientState.value = PatientState.Loading
            Log.d(TAG, "Attempting to load patient details for patientId: '$patientId'") // <--- ЛОГ
            if (patientId.isBlank()) {
                Log.e(TAG, "Patient ID is blank, cannot load details.")
                _patientState.value = PatientState.Error("ID пациента не указан для визита")
                return@launch
            }
            try {
                val patient = patientRepository.getPatientById(patientId)
                _patientState.value = PatientState.Success(patient)
                Log.d(TAG, "Patient loaded successfully from repository: ${patient.fullName}") // <--- ЛОГ
                observePatientChanges(patientId)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading patient from repository for ID '$patientId': ${e.message}", e) // <--- ЛОГ
                // Проверяем кэш
                try {
                    val cachedPatients = patientRepository.getCachedPatients()
                    val cachedPatient = cachedPatients.find { it.id == patientId }
                    if (cachedPatient != null) {
                        _patientState.value = PatientState.Success(cachedPatient)
                        _isOffline.value = true
                        Log.d(TAG, "Patient loaded successfully from cache: ${cachedPatient.fullName}") // <--- ЛОГ
                    } else {
                        _patientState.value = PatientState.Error(e.message ?: "Ошибка загрузки данных пациента")
                        Log.e(TAG, "Patient not found in cache for ID '$patientId'. Previous error: ${e.message}") // <--- ЛОГ
                    }
                } catch (cacheEx: Exception) {
                    _patientState.value = PatientState.Error(e.message ?: "Ошибка загрузки данных пациента")
                    Log.e(TAG, "Error loading patient from cache for ID '$patientId': ${cacheEx.message}", cacheEx) // <--- ЛОГ
                }
            }
        }
    }

    private fun observePatientChanges(patientId: String) {
        viewModelScope.launch {
            try {
                patientRepository.observePatient(patientId).collectLatest { patient ->
                    _patientState.value = PatientState.Success(patient)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing patient: ${e.message}", e)
            }
        }
    }

    fun updateVisitStatus(newStatus: VisitStatus) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating visit status to: $newStatus")
                val currentVisit = _visit.value ?: return@launch

                // Для мгновенного обновления UI
                val updatedVisit = currentVisit.copy(status = newStatus)
                _visit.value = updatedVisit
                _uiState.value = VisitDetailUiState.Success(updatedVisit)

                // Обновляем статус визита в репозитории
                visitRepository.updateVisitStatus(visitId, newStatus)

                // Если визит связан с заявкой, обновляем и её статус
                if (currentVisit.isFromRequest && currentVisit.originalRequestId != null) {
                    val requestStatus = mapVisitStatusToRequestStatus(newStatus)
                    Log.d(TAG, "Updating original request status to: $requestStatus")

                    appointmentRequestRepository.updateRequestStatus(
                        currentVisit.originalRequestId,
                        requestStatus
                    )

                    // Обновляем кэшированную заявку
                    _originalRequest.value?.let { currentRequest ->
                        _originalRequest.value = currentRequest.copy(status = requestStatus)
                    }
                }

                Log.d(TAG, "Status updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating status: ${e.message}", e)
                _isOffline.value = true
                // В случае ошибки возвращаем предыдущее состояние
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
            else -> RequestStatus.ASSIGNED
        }
    }

    fun addVisitNote(note: String) {
        viewModelScope.launch {
            try {
                val currentVisit = _visit.value ?: return@launch
                val updatedNotes = if (currentVisit.notes.isBlank()) {
                    note
                } else {
                    "${currentVisit.notes}\n\n$note"
                }

                visitRepository.updateVisitNotes(visitId, updatedNotes)
                Log.d(TAG, "Visit note added successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding visit note: ${e.message}", e)
                _isOffline.value = true
            }
        }
    }

    fun updateScheduledTime(newTime: java.util.Date) {
        viewModelScope.launch {
            try {
                visitRepository.updateScheduledTime(visitId, newTime)
                Log.d(TAG, "Scheduled time updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating scheduled time: ${e.message}", e)
                _isOffline.value = true
            }
        }
    }

    // Метод для проверки, можно ли создать протокол для данного визита
    fun canCreateProtocol(): Boolean {
        val currentVisit = _visit.value
        return currentVisit != null &&
                (currentVisit.status == VisitStatus.IN_PROGRESS ||
                        currentVisit.status == VisitStatus.COMPLETED)
    }

    // Метод для получения дополнительной информации из оригинальной заявки
    fun getAdditionalRequestInfo(): String? {
        val request = _originalRequest.value
        return if (request != null) {
            buildString {
                appendLine("Информация из заявки:")
                appendLine("Тип: ${getRequestTypeText(request.requestType)}")
                appendLine("Симптомы: ${request.symptoms}")
                if (request.additionalNotes.isNotBlank()) {
                    appendLine("Дополнительные заметки: ${request.additionalNotes}")
                }
                request.urgencyLevel?.let { urgency ->
                    appendLine("Уровень срочности: ${getUrgencyLevelText(urgency)}")
                }
            }
        } else null
    }

    private fun getRequestTypeText(requestType: com.example.medicalhomevisit.data.model.RequestType): String {
        return when (requestType) {
            com.example.medicalhomevisit.data.model.RequestType.EMERGENCY -> "Неотложная"
            com.example.medicalhomevisit.data.model.RequestType.REGULAR -> "Плановая"
            com.example.medicalhomevisit.data.model.RequestType.CONSULTATION -> "Консультация"
        }
    }

    private fun getUrgencyLevelText(urgencyLevel: com.example.medicalhomevisit.data.model.UrgencyLevel): String {
        return when (urgencyLevel) {
            com.example.medicalhomevisit.data.model.UrgencyLevel.LOW -> "Низкая"
            com.example.medicalhomevisit.data.model.UrgencyLevel.NORMAL -> "Обычная"
            com.example.medicalhomevisit.data.model.UrgencyLevel.HIGH -> "Высокая"
            com.example.medicalhomevisit.data.model.UrgencyLevel.CRITICAL -> "Критическая"
        }
    }

    // Метод для повторной синхронизации при восстановлении соединения
    fun syncData() {
        viewModelScope.launch {
            try {
                visitRepository.syncVisits()
                appointmentRequestRepository.syncRequests()
                _isOffline.value = false
                loadVisitDetails()
                Log.d(TAG, "Data synced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing data: ${e.message}", e)
                // Даже если синхронизация не удалась, попробуем загрузить локальные данные
                loadVisitDetails()
            }
        }
    }

    // Метод для проверки, является ли визит созданным из заявки
    fun isVisitFromRequest(): Boolean {
        return _visit.value?.isFromRequest == true
    }

    // Метод для получения ID оригинальной заявки
    fun getOriginalRequestId(): String? {
        return _visit.value?.originalRequestId
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