package com.example.medicalhomevisit.data.repository

import android.util.Log
import com.example.medicalhomevisit.data.local.dao.PatientDao
import com.example.medicalhomevisit.data.local.entity.PatientEntity
import com.example.medicalhomevisit.data.remote.api.PatientApiService
import com.example.medicalhomevisit.data.remote.dto.PatientDto
import com.example.medicalhomevisit.data.remote.dto.PatientProfileUpdateDto
import com.example.medicalhomevisit.domain.model.Gender
import com.example.medicalhomevisit.domain.model.Patient
import com.example.medicalhomevisit.domain.model.PatientProfileUpdate
import com.example.medicalhomevisit.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleOfflinePatientRepository @Inject constructor(
    private val patientDao: PatientDao,
    private val patientApiService: PatientApiService
) : PatientRepository {

    companion object {
        private const val TAG = "OfflinePatientRepo"
    }

    override suspend fun getPatientById(patientId: String): Patient {
        Log.d(TAG, "🔍 Getting patient by ID: $patientId")

        val localPatient = patientDao.getPatientById(patientId)?.toDomainModel()

        if (localPatient != null) {
            Log.d(TAG, "✅ Patient found in local database: ${localPatient.fullName}")
            tryRefreshPatientFromServer(patientId)
            return localPatient
        }

        Log.d(TAG, "📱 Patient not found locally, trying to load from server...")

        try {
            val response = patientApiService.getPatientById(patientId)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val patient = dto.toDomainModel()

                val entity = dto.toEntity(isSynced = true)
                patientDao.insertPatient(entity)

                Log.d(TAG, "✅ Patient loaded from server and cached: ${patient.fullName}")
                return patient
            } else {
                Log.e(TAG, "❌ Server error: ${response.code()} ${response.message()}")
                throw Exception("Ошибка загрузки пациента с сервера: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Network error loading patient: ${e.message}", e)
            throw Exception("Пациент с ID $patientId не найден")
        }
    }

    override fun observePatient(patientId: String): Flow<Patient> {
        Log.d(TAG, "👁️ Observing patient: $patientId")

        tryRefreshPatientFromServer(patientId)

        return patientDao.observePatient(patientId).map { entity ->
            entity?.toDomainModel() ?: throw Exception("Пациент с ID $patientId не найден")
        }
    }

    override suspend fun getMyProfile(): Patient {
        Log.d(TAG, "👤 Getting my patient profile")

        try {
            val response = patientApiService.getMyProfile()
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val patient = dto.toDomainModel()

                val entity = patient.toEntity(isSynced = true)
                patientDao.insertPatient(entity)

                Log.d(TAG, "✅ My profile loaded from server")
                return patient
            } else {
                throw Exception("Ошибка загрузки профиля: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading my profile from server: ${e.message}", e)
            throw e
        }
    }

    override suspend fun updateMyProfile(profileUpdate: PatientProfileUpdate): Patient {
        Log.d(TAG, "✏️ Updating my patient profile")

        try {
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
            if (response.isSuccessful && response.body() != null) {
                val updatedDto = response.body()!!
                val updatedPatient = updatedDto.toDomainModel()

                val entity = updatedPatient.toEntity(isSynced = true)
                patientDao.insertPatient(entity)

                Log.d(TAG, "✅ My profile updated successfully")
                return updatedPatient
            } else {
                throw Exception("Ошибка обновления профиля: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating my profile: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getCachedPatientById(patientId: String): Patient? {
        return patientDao.getPatientById(patientId)?.toDomainModel()
    }

    private fun tryRefreshPatientFromServer(patientId: String) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                Log.d(TAG, "📡 Refreshing patient $patientId from server")
                val response = patientApiService.getPatientById(patientId)

                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!
                    val entity = dto.toEntity(isSynced = true)
                    patientDao.insertPatient(entity)
                    Log.d(TAG, "✅ Patient $patientId refreshed from server")
                }
            } catch (e: Exception) {
                Log.w(TAG, "❌ Failed to refresh patient $patientId: ${e.message}")
            }
        }
    }

    private fun trySearchPatientsOnServer(query: String) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                Log.d(TAG, "📡 Searching patients on server: '$query'")
                val response = patientApiService.searchPatients(query)

                if (response.isSuccessful && response.body() != null) {
                    val dtos = response.body()!!
                    val entities = dtos.map { it.toEntity(isSynced = true) }
                    patientDao.insertPatients(entities)
                    Log.d(TAG, "✅ Found ${entities.size} patients on server")
                }
            } catch (e: Exception) {
                Log.w(TAG, "❌ Failed to search patients on server: ${e.message}")
            }
        }
    }

    private fun tryRefreshAllPatientsFromServer() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                Log.d(TAG, "📡 Refreshing all patients from server")
                val response = patientApiService.getAllPatients()

                if (response.isSuccessful && response.body() != null) {
                    val dtos = response.body()!!
                    val entities = dtos.map { it.toEntity(isSynced = true) }
                    patientDao.insertPatients(entities)
                    Log.d(TAG, "✅ Refreshed ${entities.size} patients from server")
                }
            } catch (e: Exception) {
                Log.w(TAG, "❌ Failed to refresh all patients: ${e.message}")
            }
        }
    }

    private fun PatientEntity.toDomainModel(): Patient {
        return Patient(
            id = this.id,
            fullName = this.fullName,
            dateOfBirth = this.dateOfBirth,
            age = this.age,
            gender = try {
                if (this.gender != null) Gender.valueOf(this.gender) else Gender.UNKNOWN
            } catch (e: Exception) {
                Gender.UNKNOWN
            },
            address = this.address,
            phoneNumber = this.phoneNumber,
            policyNumber = this.policyNumber,
            allergies = this.allergies,
            chronicConditions = this.chronicConditions
        )
    }

    private fun Patient.toEntity(isSynced: Boolean = true, syncAction: String? = null): PatientEntity {
        return PatientEntity(
            id = this.id,
            fullName = this.fullName,
            dateOfBirth = this.dateOfBirth,
            age = this.age,
            gender = this.gender.name,
            address = this.address,
            phoneNumber = this.phoneNumber,
            policyNumber = this.policyNumber,
            allergies = this.allergies,
            chronicConditions = this.chronicConditions,
            createdAt = Date(),
            updatedAt = Date(),
            isSynced = isSynced,
            syncAction = syncAction
        )
    }

    private fun PatientDto.toDomainModel(): Patient {
        return Patient(
            id = this.id,
            fullName = this.fullName,
            dateOfBirth = this.dateOfBirth,
            age = this.age,
            gender = try {
                if (this.gender != null) Gender.valueOf(this.gender) else Gender.UNKNOWN
            } catch (e: Exception) {
                Gender.UNKNOWN
            },
            address = this.address ?: "",
            phoneNumber = this.phoneNumber ?: "",
            policyNumber = this.policyNumber ?: "",
            allergies = this.allergies,
            chronicConditions = this.chronicConditions
        )
    }

    private fun PatientDto.toEntity(isSynced: Boolean = true): PatientEntity {
        return PatientEntity(
            id = this.id,
            fullName = this.fullName,
            dateOfBirth = this.dateOfBirth,
            age = this.age,
            gender = this.gender,
            address = this.address ?: "",
            phoneNumber = this.phoneNumber ?: "",
            policyNumber = this.policyNumber ?: "",
            allergies = this.allergies,
            chronicConditions = this.chronicConditions,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            isSynced = isSynced,
            syncAction = null
        )
    }
}