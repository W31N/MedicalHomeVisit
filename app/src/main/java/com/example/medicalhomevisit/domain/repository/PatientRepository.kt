package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.data.model.Patient
import kotlinx.coroutines.flow.Flow

interface PatientRepository {
    suspend fun getPatientById(patientId: String): Patient
    suspend fun searchPatients(query: String): List<Patient>
    fun observePatient(patientId: String): Flow<Patient>
    suspend fun cachePatients(patients: List<Patient>)
    suspend fun getCachedPatients(): List<Patient>
}