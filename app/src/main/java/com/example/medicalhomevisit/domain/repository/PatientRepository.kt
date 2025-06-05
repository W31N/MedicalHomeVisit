package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.domain.model.Patient
import com.example.medicalhomevisit.domain.model.PatientProfileUpdate
import kotlinx.coroutines.flow.Flow

interface PatientRepository {
    suspend fun getPatientById(patientId: String): Patient
    suspend fun searchPatients(query: String): List<Patient>
    suspend fun getAllPatients(): List<Patient>
    fun observePatients(): Flow<List<Patient>>
    fun observePatient(patientId: String): Flow<Patient>
    suspend fun cachePatients(patients: List<Patient>)
    suspend fun getCachedPatients(): List<Patient>
    suspend fun getCachedPatientById(patientId: String): Patient?
    suspend fun syncPatients(): Result<List<Patient>>
    suspend fun getMyProfile(): Patient
    suspend fun updateMyProfile(profileUpdate: PatientProfileUpdate): Patient
}