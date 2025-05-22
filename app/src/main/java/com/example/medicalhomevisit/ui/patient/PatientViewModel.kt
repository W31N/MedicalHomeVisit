package com.example.medicalhomevisit.ui.patient

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.data.model.AppointmentRequest
import com.example.medicalhomevisit.data.model.RequestStatus
import com.example.medicalhomevisit.data.model.RequestType
import com.example.medicalhomevisit.data.model.User
import com.example.medicalhomevisit.domain.repository.AppointmentRequestRepository
import com.example.medicalhomevisit.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class PatientViewModel(
    private val appointmentRequestRepository: AppointmentRequestRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PatientViewModel"
    }

    private val _uiState = MutableStateFlow<PatientUiState>(PatientUiState.Loading)
    val uiState: StateFlow<PatientUiState> = _uiState.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _requests = MutableStateFlow<List<AppointmentRequest>>(emptyList())
    val requests: StateFlow<List<AppointmentRequest>> = _requests.asStateFlow()

    init {
        viewModelScope.launch {
            // Получаем текущего пользователя
            authRepository.currentUser.collect { user ->
                _user.value = user

                if (user != null) {
                    // Загружаем запросы, если пользователь авторизован
                    loadRequests(user.id)

                    // Наблюдаем за изменениями запросов в реальном времени
                    observeRequests(user.id)
                }
            }
        }
    }

    private fun loadRequests(patientId: String) {
        _uiState.value = PatientUiState.Loading

        viewModelScope.launch {
            try {
                val patientRequests = appointmentRequestRepository.getRequestsForPatient(patientId)

                // Данные уже отсортированы в репозитории
                _requests.value = patientRequests

                _uiState.value = if (patientRequests.isEmpty()) {
                    PatientUiState.Empty
                } else {
                    PatientUiState.Success
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading patient requests", e)
                _uiState.value = PatientUiState.Error(e.message ?: "Ошибка загрузки запросов")
            }
        }
    }

    private fun observeRequests(patientId: String) {
        viewModelScope.launch {
            try {
                appointmentRequestRepository.observeRequestsForPatient(patientId)
                    .collectLatest { requests ->
                        _requests.value = requests

                        // Обновляем состояние UI
                        if (_uiState.value !is PatientUiState.Error) {
                            _uiState.value = if (requests.isEmpty()) {
                                PatientUiState.Empty
                            } else {
                                PatientUiState.Success
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing patient requests", e)
            }
        }
    }

    fun createNewRequest(
        requestType: RequestType,
        symptoms: String,
        preferredDate: Date?,
        preferredTimeRange: String? = "",
        address: String,
        additionalNotes: String? = "",
        patientPhone: String = "Не указано" // Добавляем как параметр функции
    ) {
        val currentUser = _user.value ?: return

        _uiState.value = PatientUiState.Loading

        viewModelScope.launch {
            try {
                val newRequest = AppointmentRequest(
                    patientId = currentUser.id,
                    patientName = currentUser.displayName.ifEmpty { "Не указано" },
                    patientPhone = patientPhone, // Используем переданный параметр
                    address = address,
                    requestType = requestType,
                    symptoms = symptoms,
                    additionalNotes = additionalNotes ?: "",
                    preferredDate = preferredDate,
                    preferredTimeRange = preferredTimeRange ?: "",
                    status = RequestStatus.NEW
                )

                appointmentRequestRepository.createRequest(newRequest)
                _uiState.value = PatientUiState.RequestCreated
            } catch (e: Exception) {
                Log.e(TAG, "Error creating new request", e)
                _uiState.value = PatientUiState.Error(e.message ?: "Ошибка при создании запроса")
            }
        }
    }

    fun cancelRequest(requestId: String, reason: String) {
        _uiState.value = PatientUiState.Loading

        viewModelScope.launch {
            try {
                appointmentRequestRepository.cancelRequest(requestId, reason)
                _uiState.value = PatientUiState.RequestCancelled
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling request", e)
                _uiState.value = PatientUiState.Error(e.message ?: "Ошибка при отмене запроса")
            }
        }
    }

    fun resetUiState() {
        if (_uiState.value !is PatientUiState.Loading) {
            _uiState.value = PatientUiState.Success
        }
    }
}

sealed class PatientUiState {
    object Loading : PatientUiState()
    object Success : PatientUiState()
    object Empty : PatientUiState()
    object RequestCreated : PatientUiState()
    object RequestCancelled : PatientUiState()
    data class Error(val message: String) : PatientUiState()
}