package com.example.medicalhomevisit.data.remote

import android.util.Log
import com.example.medicalhomevisit.data.model.Patient
import com.example.medicalhomevisit.data.model.Gender
import com.example.medicalhomevisit.data.remote.dtos.PatientDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpPatientRepository @Inject constructor(
    private val patientApiService: PatientApiService,
    private val authRepository: AuthRepository
) : PatientRepository {

    companion object {
        private const val TAG = "HttpPatientRepository"
    }

    // Кэш для офлайн режима
    private val _cachedPatients = MutableStateFlow<List<Patient>>(emptyList())
    private val _patientsFlow = MutableStateFlow<List<Patient>>(emptyList())

    override suspend fun getPatientById(patientId: String): Patient {
        return try {
            Log.d(TAG, "Getting patient by ID: $patientId")

            // Сначала проверяем кэш
            val cachedPatient = getCachedPatientById(patientId)
            if (cachedPatient != null) {
                Log.d(TAG, "Found patient in cache: $patientId")
                return cachedPatient
            }

            val response = patientApiService.getPatientById(patientId)

            if (response.isSuccessful) {
                val patientDto = response.body() ?: throw Exception("Пациент не найден")
                val patient = convertDtoToPatient(patientDto)

                // Обновляем кэш
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

    override suspend fun searchPatients(query: String): List<Patient> {
        return try {
            Log.d(TAG, "Searching patients with query: $query")

            if (query.isBlank()) {
                return emptyList()
            }

            val response = patientApiService.searchPatients(query)

            if (response.isSuccessful) {
                val patientDtos = response.body() ?: emptyList()
                val patients = patientDtos.map { convertDtoToPatient(it) }

                // Обновляем кэш найденными пациентами
                updatePatientsInCache(patients)

                Log.d(TAG, "Successfully found ${patients.size} patients")
                patients
            } else {
                Log.e(TAG, "Failed to search patients: ${response.code()} ${response.message()}")
                throw Exception("Ошибка поиска пациентов: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching patients", e)
            throw e
        }
    }

    override suspend fun getAllPatients(): List<Patient> {
        return try {
            Log.d(TAG, "Getting all patients")
            val response = patientApiService.getAllPatients()

            if (response.isSuccessful) {
                val patientDtos = response.body() ?: emptyList()
                val patients = patientDtos.map { convertDtoToPatient(it) }

                // Обновляем кэш
                _cachedPatients.value = patients
                _patientsFlow.value = patients

                Log.d(TAG, "Successfully loaded ${patients.size} patients")
                patients
            } else {
                Log.e(TAG, "Failed to load all patients: ${response.code()} ${response.message()}")
                throw Exception("Ошибка загрузки всех пациентов: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading all patients", e)
            throw e
        }
    }



    override fun observePatients(): Flow<List<Patient>> {
        return _patientsFlow.asStateFlow()
    }

    override fun observePatient(patientId: String): Flow<Patient> {
        return _patientsFlow.map { patients ->
            patients.find { it.id == patientId }
                ?: throw Exception("Пациент с ID $patientId не найден")
        }
    }

    override suspend fun cachePatients(patients: List<Patient>) {
        _cachedPatients.value = patients
        Log.d(TAG, "Cached ${patients.size} patients")
    }

    override suspend fun getCachedPatients(): List<Patient> {
        return _cachedPatients.value
    }

    override suspend fun getCachedPatientById(patientId: String): Patient? {
        return _cachedPatients.value.find { it.id == patientId }
    }

    override suspend fun syncPatients(): Result<List<Patient>> {
        return try {
            val patients = getAllPatients()
            _cachedPatients.value = patients
            _patientsFlow.value = patients
            Result.success(patients)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing patients", e)
            Result.failure(e)
        }
    }

    /**
     * Вспомогательная функция для обновления пациента в кэше
     */
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

    /**
     * Вспомогательная функция для обновления нескольких пациентов в кэше
     */
    private fun updatePatientsInCache(newPatients: List<Patient>) {
        val currentPatients = _cachedPatients.value.associateBy { it.id }.toMutableMap()

        newPatients.forEach { patient ->
            currentPatients[patient.id] = patient
        }

        val updatedList = currentPatients.values.toList()
        _cachedPatients.value = updatedList
        _patientsFlow.value = updatedList
    }

    /**
     * Конвертация DTO с сервера в доменную модель
     */
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