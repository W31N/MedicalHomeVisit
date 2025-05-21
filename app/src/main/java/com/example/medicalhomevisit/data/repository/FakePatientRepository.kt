package com.example.medicalhomevisit.data.repository

import com.example.medicalhomevisit.data.TestDataManager
import com.example.medicalhomevisit.data.model.Patient
import com.example.medicalhomevisit.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakePatientRepository : PatientRepository {
    override suspend fun getPatientById(patientId: String): Patient {
        return TestDataManager.getPatient(patientId)
    }

    override suspend fun searchPatients(query: String): List<Patient> {
        return emptyList()
    }

    override fun observePatient(patientId: String): Flow<Patient> {
        return flowOf(TestDataManager.getPatient(patientId))
    }

    override suspend fun cachePatients(patients: List<Patient>) {
        // Ничего не делаем в фейковом репозитории
    }

    override suspend fun getCachedPatients(): List<Patient> {
        return emptyList()
    }
}