package com.example.medicalhomevisit.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.data.di.OnlinePatientRepository
import com.example.medicalhomevisit.domain.model.Gender
import com.example.medicalhomevisit.domain.model.PatientProfileUpdate
import com.example.medicalhomevisit.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PatientProfileViewModel @Inject constructor(
    @OnlinePatientRepository private val patientRepository: PatientRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PatientProfileViewModel"
    }

    private val _uiState = MutableStateFlow<PatientProfileUiState>(PatientProfileUiState.Loading)
    val uiState: StateFlow<PatientProfileUiState> = _uiState.asStateFlow()

    private val _profileData = MutableStateFlow(PatientProfileData())
    val profileData: StateFlow<PatientProfileData> = _profileData.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = PatientProfileUiState.Loading
            try {
                Log.d(TAG, "Loading patient profile")
                val patient = patientRepository.getMyProfile()

                _profileData.value = PatientProfileData(
                    fullName = patient.fullName,
                    dateOfBirth = patient.dateOfBirth,
                    gender = patient.gender,
                    address = patient.address,
                    phoneNumber = patient.phoneNumber,
                    policyNumber = patient.policyNumber,
                    allergies = patient.allergies ?: emptyList(),
                    chronicConditions = patient.chronicConditions ?: emptyList()
                )

                _uiState.value = PatientProfileUiState.Success
                Log.d(TAG, "Patient profile loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading patient profile", e)
                _uiState.value = PatientProfileUiState.Error(e.message ?: "Ошибка загрузки профиля")
            }
        }
    }

    fun updateProfile(
        dateOfBirth: Date?,
        gender: Gender?,
        address: String?,
        phoneNumber: String?,
        policyNumber: String?,
        allergies: List<String>,
        chronicConditions: List<String>
    ) {
        viewModelScope.launch {
            _uiState.value = PatientProfileUiState.Loading
            try {
                Log.d(TAG, "Updating patient profile")

                val profileUpdate = PatientProfileUpdate(
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                    address = address?.takeIf { it.isNotBlank() },
                    phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
                    policyNumber = policyNumber?.takeIf { it.isNotBlank() },
                    allergies = allergies.takeIf { it.isNotEmpty() },
                    chronicConditions = chronicConditions.takeIf { it.isNotEmpty() }
                )

                val updatedPatient = patientRepository.updateMyProfile(profileUpdate)

                // Обновляем локальные данные
                _profileData.value = PatientProfileData(
                    fullName = updatedPatient.fullName,
                    dateOfBirth = updatedPatient.dateOfBirth,
                    gender = updatedPatient.gender,
                    address = updatedPatient.address,
                    phoneNumber = updatedPatient.phoneNumber,
                    policyNumber = updatedPatient.policyNumber,
                    allergies = updatedPatient.allergies ?: emptyList(),
                    chronicConditions = updatedPatient.chronicConditions ?: emptyList()
                )

                _uiState.value = PatientProfileUiState.Updated
                Log.d(TAG, "Patient profile updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating patient profile", e)
                _uiState.value = PatientProfileUiState.Error(e.message ?: "Ошибка обновления профиля")
            }
        }
    }

    fun retry() {
        loadProfile()
    }

    fun resetToSuccess() {
        if (_uiState.value is PatientProfileUiState.Updated) {
            _uiState.value = PatientProfileUiState.Success
        }
    }
}

sealed class PatientProfileUiState {
    object Loading : PatientProfileUiState()
    object Success : PatientProfileUiState()
    object Updated : PatientProfileUiState()
    data class Error(val message: String) : PatientProfileUiState()
}

data class PatientProfileData(
    val fullName: String = "",
    val dateOfBirth: Date? = null,
    val gender: Gender = Gender.UNKNOWN,
    val address: String = "",
    val phoneNumber: String = "",
    val policyNumber: String = "",
    val allergies: List<String> = emptyList(),
    val chronicConditions: List<String> = emptyList()
)