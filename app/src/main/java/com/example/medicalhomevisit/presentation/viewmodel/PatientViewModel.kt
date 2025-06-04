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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
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

    private val _uiState = MutableStateFlow<PatientUiState>(PatientUiState.Initial) // Начальное состояние
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
//                    observeRequests(userValue.id)
                } else {
                    // Если пользователь вышел, очищаем заявки и состояние
                    _requests.value = emptyList()
                    _uiState.value = PatientUiState.NotLoggedIn // или другое подходящее состояние
                    Log.d(TAG, "User logged out, cleared requests.")
                }
            }
        }
    }


    private fun loadMyRequestsInternal() { // patientId больше не нужен как параметр
        Log.d(TAG, "loadMyRequestsInternal called for current patient")
        _uiState.value = PatientUiState.Loading
        viewModelScope.launch {
            try {
                val result = appointmentRequestRepository.getMyRequests() // <--- ИЗМЕНЕНИЕ ЗДЕСЬ
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
                        PatientUiState.Error(errorMsg) // Сообщение об ошибке может измениться
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadMyRequestsInternal", e)
                _uiState.value =
                    PatientUiState.Error(e.message ?: "Непредвиденная ошибка загрузки моих заявок")
            }
        }
    }





    private fun loadRequests(patientId: String) {
        Log.d(TAG, "loadRequests called for patientId: $patientId")
        _uiState.value = PatientUiState.Loading
        viewModelScope.launch {
            try {
                val result = appointmentRequestRepository.getRequestsForPatient(patientId)
                if (result.isSuccess) {
                    val patientRequests = result.getOrNull() ?: emptyList()
                    _requests.value = patientRequests // Предполагается, что репозиторий возвращает отсортированный список
                    _uiState.value = if (patientRequests.isEmpty()) {
                        PatientUiState.Empty
                    } else {
                        PatientUiState.Success(patientRequests)
                    }
                    Log.d(TAG, "Requests loaded successfully: ${patientRequests.size} items")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Ошибка загрузки заявок"
                    Log.e(TAG, "Error loading patient requests: $errorMsg", result.exceptionOrNull())
                    _uiState.value = PatientUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadRequests", e)
                _uiState.value =
                    PatientUiState.Error(e.message ?: "Непредвиденная ошибка загрузки запросов")
            }
        }
    }

     fun observeRequestsForPatient(patientId: String): Flow<List<AppointmentRequest>> {
        Log.w(TAG, "observeRequestsForPatient for patientId $patientId - Using STUB implementation (returns empty flow). Needs proper implementation if used or ViewModel logic adjustment.")
        // Эта заглушка позволит коду компилироваться и работать, но не будет предоставлять реальных данных
        // для заявок произвольного пациента в реальном времени.
        return flowOf(emptyList()) // Возвращаем пустой поток данных
    }


    fun createNewRequest(
        requestType: RequestType,
        symptoms: String,
        // Имя параметра preferredDate соответствует вызову из CreateRequestScreen
        preferredDate: Date?,
        // preferredTimeRange принимаем, но не используем, т.к. нет в доменной модели AppointmentRequest
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
                    id = "", // Будет присвоен на бэкенде
                    patientId = "",
                    patientName = currentUser.displayName.ifEmpty { "Пациент" },
                    patientPhone = "Не указан",
                    address = address,
                    requestType = requestType,
                    symptoms = symptoms,
                    additionalNotes = additionalNotes ?: "",
                    preferredDateTime = preferredDate, // Присваиваем параметр preferredDate полю preferredDateTime модели
                    status = RequestStatus.NEW
                    // Поле preferredTimeRange отсутствует в модели AppointmentRequest, поэтому не устанавливаем его
                )
                Log.d(TAG, "Constructed AppointmentRequest model: $newRequest")

                val result = appointmentRequestRepository.createRequest(newRequest)

                if (result.isSuccess) {
                    val createdRequest = result.getOrNull()
                    Log.d(TAG, "Request created successfully on backend: ${createdRequest?.id}")
                    // Репозиторий createRequest уже обновляет _myRequestsFlow,
                    // loadRequests() перезагрузит все заявки, включая новую.
                    // Если createRequest возвращает созданную заявку, можно ее добавить в _requests вручную,
                    // чтобы избежать лишнего запроса getRequestsForPatient, но loadRequests проще.
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

    fun resetUiStateToDefault() {
        Log.d(TAG, "resetUiStateToDefault called. Current requests count: ${_requests.value.size}")
        if (_requests.value.isNotEmpty()) {
            _uiState.value = PatientUiState.Success(_requests.value)
        } else {
            _uiState.value = PatientUiState.Empty
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
            _uiState.value = PatientUiState.NotLoggedIn // или Initial
        }
    }
}

// Обновляем PatientUiState для большей информативности
sealed class PatientUiState {
    object Initial : PatientUiState() // Добавим начальное состояние
    object Loading : PatientUiState()
    data class Success(val requests: List<AppointmentRequest>) : PatientUiState() // Теперь Success хранит данные
    object Empty : PatientUiState()
    data class RequestCreated(val createdRequest: AppointmentRequest?) : PatientUiState() // Может содержать созданную заявку
    object RequestCancelled : PatientUiState()
    object NotLoggedIn : PatientUiState() // Для случая, когда пользователь не вошел
    data class Error(val message: String) : PatientUiState()
}
