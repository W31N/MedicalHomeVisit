package com.example.medicalhomevisit.data.remote

import com.example.medicalhomevisit.data.model.Patient
import kotlinx.coroutines.flow.Flow

interface PatientRepository {

    /**
     * Получить пациента по ID
     */
    suspend fun getPatientById(patientId: String): Patient

    /**
     * Поиск пациентов по имени (для медработников и админов)
     */
    suspend fun searchPatients(query: String): List<Patient>

    /**
     * Получить всех пациентов (только для админов)
     */
    suspend fun getAllPatients(): List<Patient>

    /**
     * Наблюдение за изменениями пациентов (для real-time обновлений)
     */
    fun observePatients(): Flow<List<Patient>>

    fun observePatient(patientId: String): Flow<Patient>

    /**
     * Кэширование для офлайн режима
     */
    suspend fun cachePatients(patients: List<Patient>)
    suspend fun getCachedPatients(): List<Patient>
    suspend fun getCachedPatientById(patientId: String): Patient?
    suspend fun syncPatients(): Result<List<Patient>>
}