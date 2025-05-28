package com.example.medicalhomevisit.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.data.model.AppointmentRequest
import com.example.medicalhomevisit.data.model.User
import com.example.medicalhomevisit.domain.repository.AdminRepository
import com.example.medicalhomevisit.domain.repository.AppointmentRequestRepository
import com.example.medicalhomevisit.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val appointmentRequestRepository: AppointmentRequestRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Loading)
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _activeRequests = MutableStateFlow<List<AppointmentRequest>>(emptyList())
    val activeRequests: StateFlow<List<AppointmentRequest>> = _activeRequests.asStateFlow()

    private val _medicalStaff = MutableStateFlow<List<User>>(emptyList())
    val medicalStaff: StateFlow<List<User>> = _medicalStaff.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _user.value = user
                if (user != null) {
                    loadActiveRequests()
                    loadMedicalStaff()
                }
            }
        }
    }

    private fun loadActiveRequests() {
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            try {
                val requests = appointmentRequestRepository.getAllActiveRequests()
                _activeRequests.value = requests
                _uiState.value = if (requests.isEmpty()) {
                    AdminUiState.Empty
                } else {
                    AdminUiState.Success
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading active requests", e)
                _uiState.value = AdminUiState.Error(e.message ?: "Ошибка загрузки заявок")
            }
        }
    }

    private fun loadMedicalStaff() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading medical staff...")
                val staff = adminRepository.getActiveStaff()
                Log.d(TAG, "Loaded ${staff.size} medical staff members")
                _medicalStaff.value = staff
            } catch (e: Exception) {
                Log.e(TAG, "Error loading medical staff", e)
            }
        }
    }

    fun assignRequestToStaff(requestId: String, staffId: String, staffName: String, note: String?) {
        viewModelScope.launch {
            try {
                val adminId = _user.value?.id ?: throw Exception("Администратор не авторизован")

                _uiState.value = AdminUiState.Loading
                appointmentRequestRepository.assignRequestToStaff(
                    requestId = requestId,
                    staffId = staffId,
                    staffName = staffName,
                    assignedBy = adminId,
                    note = note
                )

                loadActiveRequests()

                _uiState.value = AdminUiState.RequestAssigned
            } catch (e: Exception) {
                Log.e(TAG, "Error assigning request to staff", e)
                _uiState.value = AdminUiState.Error(e.message ?: "Ошибка при назначении заявки")
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
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            try {
                adminRepository.registerNewPatient(
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
                _uiState.value = AdminUiState.PatientCreated
            } catch (e: Exception) {
                Log.e(TAG, "Error registering new patient", e)
                _uiState.value = AdminUiState.Error(e.message ?: "Ошибка при регистрации пациента")
            }
        }
    }

    fun refreshData() {
        loadActiveRequests()
        loadMedicalStaff()
    }

    companion object {
        private const val TAG = "AdminViewModel"
    }
}

sealed class AdminUiState {
    object Loading : AdminUiState()
    object Success : AdminUiState()
    object Empty : AdminUiState()
    object RequestAssigned : AdminUiState()
    object PatientCreated : AdminUiState()
    data class Error(val message: String) : AdminUiState()
}