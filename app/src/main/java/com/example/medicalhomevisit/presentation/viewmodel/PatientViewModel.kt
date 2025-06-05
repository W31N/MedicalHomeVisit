package com.example.medicalhomevisit.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.domain.model.AppointmentRequest
import com.example.medicalhomevisit.domain.model.RequestStatus
import com.example.medicalhomevisit.domain.model.RequestType
import com.example.medicalhomevisit.domain.model.User
import com.example.medicalhomevisit.domain.repository.AppointmentRequestRepository
import com.example.medicalhomevisit.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PatientViewModel @Inject constructor(
    private val appointmentRequestRepository: AppointmentRequestRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PatientViewModel"
    }

    private val _uiState = MutableStateFlow<PatientUiState>(PatientUiState.Initial)
    val uiState: StateFlow<PatientUiState> = _uiState.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _requests = MutableStateFlow<List<AppointmentRequest>>(emptyList())
    val requests: StateFlow<List<AppointmentRequest>> = _requests.asStateFlow()

    init {
        Log.d(TAG, "Initializing PatientViewModel")
        viewModelScope.launch {
            authRepository.currentUser.collect { userValue ->
                Log.d(TAG, "currentUser collected in PatientViewModel: ${userValue?.email}")
                _user.value = userValue
                if (userValue != null) {
                    loadMyRequestsInternal()
                } else {
                    _requests.value = emptyList()
                    _uiState.value = PatientUiState.NotLoggedIn
                    Log.d(TAG, "User logged out, cleared requests.")
                }
            }
        }
    }


    private fun loadMyRequestsInternal() {
        Log.d(TAG, "loadMyRequestsInternal called for current patient")
        _uiState.value = PatientUiState.Loading
        viewModelScope.launch {
            try {
                val result = appointmentRequestRepository.getMyRequests()
                if (result.isSuccess) {
                    val patientRequests = result.getOrNull() ?: emptyList()
                    _requests.value = patientRequests
                    _uiState.value = if (patientRequests.isEmpty()) {
                        PatientUiState.Empty
                    } else {
                        PatientUiState.Success(patientRequests)
                    }
                    Log.d(TAG, "My Requests loaded successfully: ${patientRequests.size} items")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Ошибка загрузки моих заявок"
                    Log.e(TAG, "Error loading my requests: $errorMsg", result.exceptionOrNull())
                    _uiState.value =
                        PatientUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadMyRequestsInternal", e)
                _uiState.value =
                    PatientUiState.Error(e.message ?: "Непредвиденная ошибка загрузки моих заявок")
            }
        }
    }


    fun createNewRequest(
        requestType: RequestType,
        symptoms: String,
        preferredDate: Date?,
        @Suppress("UNUSED_PARAMETER") preferredTimeRange: String?,
        address: String,
        additionalNotes: String? = ""
    ) {
        val currentUser = _user.value
        if (currentUser == null) {
            _uiState.value = PatientUiState.Error("Пользователь не авторизован. Войдите в систему.")
            Log.w(TAG, "createNewRequest: currentUser is null.")
            return
        }

        _uiState.value = PatientUiState.Loading
        Log.d(TAG, "Creating new request. Type: $requestType, Symptoms: $symptoms, Address: $address, PreferredDate: $preferredDate")

        viewModelScope.launch {
            try {
                val newRequest = AppointmentRequest(
                    id = "",
                    patientId = "",
                    patientName = currentUser.displayName.ifEmpty { "Пациент" },
                    patientPhone = "Не указан",
                    address = address,
                    requestType = requestType,
                    symptoms = symptoms,
                    additionalNotes = additionalNotes ?: "",
                    preferredDateTime = preferredDate,
                    status = RequestStatus.NEW
                )
                Log.d(TAG, "Constructed AppointmentRequest model: $newRequest")

                val result = appointmentRequestRepository.createRequest(newRequest)

                if (result.isSuccess) {
                    val createdRequest = result.getOrNull()
                    Log.d(TAG, "Request created successfully on backend: ${createdRequest?.id}")
                    loadMyRequestsInternal()
                    _uiState.value = PatientUiState.RequestCreated(createdRequest)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Ошибка при создании запроса"
                    Log.e(TAG, "Error creating new request: $errorMsg", result.exceptionOrNull())
                    _uiState.value = PatientUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in createNewRequest", e)
                _uiState.value =
                    PatientUiState.Error(e.message ?: "Непредвиденная ошибка при создании запроса")
            }
        }
    }

    fun cancelRequest(requestId: String, reason: String) {
        val currentUserId = _user.value?.id
        if (currentUserId == null) {
            _uiState.value = PatientUiState.Error("Пользователь не авторизован.")
            return
        }
        _uiState.value = PatientUiState.Loading
        Log.d(TAG, "Cancelling request: $requestId with reason: $reason")
        viewModelScope.launch {
            try {
                val result = appointmentRequestRepository.cancelRequest(requestId, reason)
                if (result.isSuccess) {
                    Log.d(TAG, "Request $requestId cancelled successfully.")
                    _uiState.value = PatientUiState.RequestCancelled
                    loadMyRequestsInternal()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Ошибка при отмене запроса"
                    Log.e(TAG, "Error cancelling request $requestId: $errorMsg", result.exceptionOrNull())
                    _uiState.value = PatientUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in cancelRequest for $requestId", e)
                _uiState.value =
                    PatientUiState.Error(e.message ?: "Непредвиденная ошибка при отмене запроса")
            }
        }
    }

    fun refreshRequests() {
        val currentUserId = _user.value?.id
        if (currentUserId != null) {
            Log.d(TAG, "Refreshing requests for userId: $currentUserId")
            loadMyRequestsInternal()
        } else {
            Log.w(TAG, "Cannot refresh requests, user is null.")
            _requests.value = emptyList()
            _uiState.value = PatientUiState.NotLoggedIn
        }
    }
}

sealed class PatientUiState {
    object Initial : PatientUiState()
    object Loading : PatientUiState()
    data class Success(val requests: List<AppointmentRequest>) : PatientUiState()
    object Empty : PatientUiState()
    data class RequestCreated(val createdRequest: AppointmentRequest?) : PatientUiState()
    object RequestCancelled : PatientUiState()
    object NotLoggedIn : PatientUiState()
    data class Error(val message: String) : PatientUiState()
}