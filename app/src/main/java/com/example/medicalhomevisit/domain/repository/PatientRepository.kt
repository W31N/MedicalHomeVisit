package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.domain.model.Patient
import com.example.medicalhomevisit.domain.model.PatientProfileUpdate
import kotlinx.coroutines.flow.Flow

interface PatientRepository {
    suspend fun getPatientById(patientId: String): Patient
    fun observePatient(patientId: String): Flow<Patient>
    suspend fun getCachedPatientById(patientId: String): Patient?
    suspend fun getMyProfile(): Patient
    suspend fun updateMyProfile(profileUpdate: PatientProfileUpdate): Patient
}