package com.example.medicalhomevisit.data.remote.repository

import android.util.Log
import com.example.medicalhomevisit.data.remote.api.PatientApiService
import com.example.medicalhomevisit.domain.model.Patient
import com.example.medicalhomevisit.domain.model.Gender
import com.example.medicalhomevisit.data.remote.dto.PatientDto
import com.example.medicalhomevisit.data.remote.dto.PatientProfileUpdateDto
import com.example.medicalhomevisit.domain.model.PatientProfileUpdate
import com.example.medicalhomevisit.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientRepositoryImpl @Inject constructor(
    private val patientApiService: PatientApiService
) : PatientRepository {

    companion object {
        private const val TAG = "HttpPatientRepository"
    }

    private val _cachedPatients = MutableStateFlow<List<Patient>>(emptyList())
    private val _patientsFlow = MutableStateFlow<List<Patient>>(emptyList())

    override suspend fun getPatientById(patientId: String): Patient {
        return try {
            Log.d(TAG, "Getting patient by ID: $patientId")

            val cachedPatient = getCachedPatientById(patientId)
            if (cachedPatient != null) {
                Log.d(TAG, "Found patient in cache: $patientId")
                return cachedPatient
            }

            val response = patientApiService.getPatientById(patientId)

            if (response.isSuccessful) {
                val patientDto = response.body() ?: throw Exception("Пациент не найден")
                val patient = convertDtoToPatient(patientDto)

                updatePatientInCache(patient)

                Log.d(TAG, "Successfully loaded patient: $patientId")
                patient
            } else {
                Log.e(TAG, "Failed to load patient: ${response.code()} ${response.message()}")
                throw Exception("Ошибка загрузки пациента: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading patient by ID", e)
            throw e
        }
    }

    override suspend fun getMyProfile(): Patient {
        return try {
            Log.d(TAG, "Getting my patient profile")
            val response = patientApiService.getMyProfile()

            if (response.isSuccessful) {
                val patientDto = response.body() ?: throw Exception("Профиль пациента не найден")
                val patient = convertDtoToPatient(patientDto)

                Log.d(TAG, "Successfully loaded my patient profile")
                patient
            } else {
                Log.e(TAG, "Failed to load my profile: ${response.code()} ${response.message()}")
                throw Exception("Ошибка загрузки профиля: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading my patient profile", e)
            throw e
        }
    }

    override suspend fun updateMyProfile(profileUpdate: PatientProfileUpdate): Patient {
        return try {
            Log.d(TAG, "Updating my patient profile")

            val dto = PatientProfileUpdateDto(
                dateOfBirth = profileUpdate.dateOfBirth,
                gender = profileUpdate.gender?.name,
                address = profileUpdate.address,
                phoneNumber = profileUpdate.phoneNumber,
                policyNumber = profileUpdate.policyNumber,
                allergies = profileUpdate.allergies,
                chronicConditions = profileUpdate.chronicConditions
            )

            val response = patientApiService.updateMyProfile(dto)

            if (response.isSuccessful) {
                val updatedPatientDto = response.body() ?: throw Exception("Ошибка обновления профиля")
                val updatedPatient = convertDtoToPatient(updatedPatientDto)

                updatePatientInCache(updatedPatient)

                Log.d(TAG, "Successfully updated my patient profile")
                updatedPatient
            } else {
                Log.e(TAG, "Failed to update profile: ${response.code()} ${response.message()}")
                throw Exception("Ошибка обновления профиля: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating my patient profile", e)
            throw e
        }
    }

    override fun observePatient(patientId: String): Flow<Patient> {
        return _patientsFlow.map { patients ->
            patients.find { it.id == patientId }
                ?: throw Exception("Пациент с ID $patientId не найден")
        }
    }

    override suspend fun getCachedPatientById(patientId: String): Patient? {
        return _cachedPatients.value.find { it.id == patientId }
    }

    private fun updatePatientInCache(patient: Patient) {
        val currentPatients = _cachedPatients.value.toMutableList()
        val index = currentPatients.indexOfFirst { it.id == patient.id }

        if (index != -1) {
            currentPatients[index] = patient
        } else {
            currentPatients.add(patient)
        }

        _cachedPatients.value = currentPatients
        _patientsFlow.value = currentPatients
    }

    private fun convertDtoToPatient(dto: PatientDto): Patient {
        return Patient(
            id = dto.id,
            fullName = dto.fullName,
            dateOfBirth = dto.dateOfBirth,
            age = dto.age,
            gender = dto.gender?.let {
                try {
                    Gender.valueOf(it)
                } catch (e: Exception) {
                    Gender.UNKNOWN
                }
            } ?: Gender.UNKNOWN,
            address = dto.address ?: "",
            phoneNumber = dto.phoneNumber ?: "",
            policyNumber = dto.policyNumber ?: "",
            allergies = dto.allergies,
            chronicConditions = dto.chronicConditions
        )
    }
}