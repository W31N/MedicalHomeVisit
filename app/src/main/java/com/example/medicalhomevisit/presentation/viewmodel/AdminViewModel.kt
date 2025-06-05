package com.example.medicalhomevisit.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.domain.model.AppointmentRequest
import com.example.medicalhomevisit.domain.model.MedicalStaffDisplay
import com.example.medicalhomevisit.domain.model.User
import com.example.medicalhomevisit.domain.model.UserRole
import com.example.medicalhomevisit.domain.repository.AdminRepository
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
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val appointmentRequestRepository: AppointmentRequestRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Initial)
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _activeRequests = MutableStateFlow<List<AppointmentRequest>>(emptyList())
    val activeRequests: StateFlow<List<AppointmentRequest>> = _activeRequests.asStateFlow()

    private val _medicalStaff = MutableStateFlow<List<MedicalStaffDisplay>>(emptyList())
    val medicalStaff: StateFlow<List<MedicalStaffDisplay>> = _medicalStaff.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { userValue ->
                _user.value = userValue
                if (userValue != null && (userValue.role == UserRole.ADMIN || userValue.role == UserRole.DISPATCHER) ) {
                    Log.d(TAG, "Admin/Dispatcher logged in. Loading initial data.")
                    refreshData()
                } else if (userValue == null) {
                    _uiState.value =
                        AdminUiState.Error("Пользователь не авторизован")
                    Log.w(TAG, "User is null, cannot load admin data.")
                } else {
                    _uiState.value = AdminUiState.Error("Недостаточно прав доступа")
                    Log.w(TAG, "User ${userValue.email} with role ${userValue.role} attempted to access admin panel.")
                }
            }
        }
    }

    private fun loadActiveRequests() {
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            Log.d(TAG, "Loading active requests...")
            try {
                val result = appointmentRequestRepository.getAllActiveRequests()
                if (result.isSuccess) {
                    val requests = result.getOrNull() ?: emptyList()
                    _activeRequests.value = requests
                    _uiState.value = if (requests.isEmpty()) {
                        AdminUiState.Empty
                    } else {
                        AdminUiState.Success
                    }
                    Log.d(TAG, "Active requests loaded: ${requests.size}")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Ошибка загрузки активных заявок"
                    Log.e(TAG, "Error loading active requests: $errorMsg", result.exceptionOrNull())
                    _uiState.value = AdminUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading active requests", e)
                _uiState.value =
                    AdminUiState.Error(e.message ?: "Непредвиденная ошибка загрузки заявок")
            }
        }
    }

    private fun loadMedicalStaff() {
        viewModelScope.launch {
            Log.d(TAG, "Loading medical staff...")
            try {
                val result = adminRepository.getActiveStaff()
                if (result.isSuccess) {
                    val staff = result.getOrNull() ?: emptyList()
                    _medicalStaff.value = staff
                    Log.d(TAG, "Loaded ${staff.size} medical staff members.")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Ошибка загрузки мед. персонала"
                    Log.e(TAG, "Error loading medical staff: $errorMsg", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading medical staff", e)
            }
        }
    }

    fun assignRequestToStaff(requestId: String, staffId: String, assignmentNote: String?) {
        Log.d(TAG, "Attempting to assign request $requestId to staff $staffId with note: $assignmentNote")
        val adminUser = _user.value
        if (adminUser == null || (adminUser.role != UserRole.ADMIN && adminUser.role != UserRole.DISPATCHER)) {
            _uiState.value =
                AdminUiState.Error("Только администратор или диспетчер могут назначать заявки.")
            Log.w(TAG, "User ${adminUser?.email} without ADMIN/DISPATCHER role tried to assign request.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            try {
                val result = appointmentRequestRepository.assignRequestToStaff(
                    requestId = requestId,
                    staffId = staffId,
                    assignmentNote = assignmentNote
                )
                if (result.isSuccess) {
                    Log.d(TAG, "Request $requestId assigned successfully to staff $staffId.")
                    _uiState.value = AdminUiState.RequestAssigned

                    refreshActiveRequestsAfterAssignment()

                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Ошибка назначения заявки"
                    Log.e(TAG, "Error assigning request: $errorMsg", result.exceptionOrNull())
                    _uiState.value = AdminUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception assigning request", e)
                _uiState.value =
                    AdminUiState.Error(e.message ?: "Непредвиденная ошибка назначения заявки")
            }
        }
    }

    private fun refreshActiveRequestsAfterAssignment() {
        viewModelScope.launch {
            try {
                val result = appointmentRequestRepository.getAllActiveRequests()
                if (result.isSuccess) {
                    val requests = result.getOrNull() ?: emptyList()
                    _activeRequests.value = requests
                    Log.d(TAG, "Active requests refreshed after assignment: ${requests.size}")
                } else {
                    Log.e(TAG, "Error refreshing active requests after assignment")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception refreshing active requests after assignment", e)
            }
        }
    }

    fun registerNewPatient(
        email: String,
        password: String,
        displayName: String,
        phoneNumber: String,
        address: String,
        dateOfBirth: Date,
        gender: String,
        medicalCardNumber: String?,
        additionalInfo: String?
    ) {
        Log.d(TAG, "Attempting to register new patient: $email")
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            try {
                val result = adminRepository.registerNewPatient(
                    email = email,
                    password = password,
                    displayName = displayName,
                    phoneNumber = phoneNumber,
                    address = address,
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                    medicalCardNumber = medicalCardNumber,
                    additionalInfo = additionalInfo
                )
                if (result.isSuccess) {
                    Log.d(TAG, "Patient $email registered successfully.")
                    _uiState.value = AdminUiState.PatientCreated
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Ошибка при регистрации пациента"
                    Log.e(TAG, "Error registering new patient: $errorMsg", result.exceptionOrNull())
                    _uiState.value = AdminUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception registering new patient", e)
                _uiState.value = AdminUiState.Error(
                    e.message ?: "Непредвиденная ошибка при регистрации пациента"
                )
            }
        }
    }

    fun refreshData() {
        Log.d(TAG, "Refreshing data...")
        loadActiveRequests()
        loadMedicalStaff()
    }

    fun consumeUiEvent() {
        if (_uiState.value is AdminUiState.RequestAssigned || _uiState.value is AdminUiState.PatientCreated) {
            _uiState.value = if (_activeRequests.value.isNotEmpty()) AdminUiState.Success else AdminUiState.Empty
        }
    }

    companion object {
        private const val TAG = "AdminViewModel"
    }
}



sealed class AdminUiState {
    object Initial : AdminUiState()
    object Loading : AdminUiState()
    object Success : AdminUiState()
    object Empty : AdminUiState()
    object RequestAssigned : AdminUiState()
    object PatientCreated : AdminUiState()
    data class Error(val message: String) : AdminUiState()
}