package com.example.medicalhomevisit.data.repository

import android.util.Log
import com.example.medicalhomevisit.data.model.Gender
import com.example.medicalhomevisit.data.model.Patient
import com.example.medicalhomevisit.domain.repository.PatientRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class FirebasePatientRepository : PatientRepository {
    private val db = Firebase.firestore
    private val patientsCollection = db.collection("patients")
    private val patientsCache = mutableMapOf<String, MutableStateFlow<Patient?>>()

    override suspend fun getPatientById(patientId: String): Patient {
        try {
            val document = patientsCollection.document(patientId).get().await()
            if (document.exists()) {
                val data = document.data ?: throw IllegalArgumentException("Документ не содержит данных")
                val dateOfBirth = (data["dateOfBirth"] as? Timestamp)?.toDate() ?: Date()

                val patient = Patient(
                    id = document.id,
                    fullName = data["fullName"] as String,
                    dateOfBirth = dateOfBirth,
                    age = calculateAge(dateOfBirth),
                    gender = Gender.valueOf(data["gender"] as String),
                    address = data["address"] as String,
                    phoneNumber = data["phoneNumber"] as String,
                    policyNumber = data["policyNumber"] as? String ?: "",
                    allergies = (data["allergies"] as? List<*>)?.filterIsInstance<String>(),
                    chronicConditions = (data["chronicConditions"] as? List<*>)?.filterIsInstance<String>()
                )

                // Кэшируем пациента
                if (patientId !in patientsCache) {
                    patientsCache[patientId] = MutableStateFlow(patient)
                } else {
                    patientsCache[patientId]?.value = patient
                }

                return patient
            } else {
                throw IllegalArgumentException("Пациент не найден")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting patient by ID", e)
            throw e
        }
    }

    override suspend fun searchPatients(query: String): List<Patient> {
        if (query.isEmpty()) {
            return emptyList()
        }

        try {
            // Поиск по имени
            val nameQuery = query.lowercase()
            val snapshot = patientsCollection
                .orderBy("fullName")
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val fullName = (data["fullName"] as String).lowercase()

                    // Фильтруем только пациентов, чьи имена содержат запрос
                    if (fullName.contains(nameQuery)) {
                        val dateOfBirth = (data["dateOfBirth"] as? Timestamp)?.toDate() ?: Date()

                        Patient(
                            id = doc.id,
                            fullName = data["fullName"] as String,
                            dateOfBirth = dateOfBirth,
                            age = calculateAge(dateOfBirth),
                            gender = Gender.valueOf(data["gender"] as String),
                            address = data["address"] as String,
                            phoneNumber = data["phoneNumber"] as String,
                            policyNumber = data["policyNumber"] as? String ?: "",
                            allergies = (data["allergies"] as? List<*>)?.filterIsInstance<String>(),
                            chronicConditions = (data["chronicConditions"] as? List<*>)?.filterIsInstance<String>()
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting patient document", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching patients", e)
            return emptyList()
        }
    }

    override fun observePatient(patientId: String): Flow<Patient> {
        // Создаем поток, если он еще не существует
        if (patientId !in patientsCache) {
            patientsCache[patientId] = MutableStateFlow(null)

            // Настраиваем слушатель изменений
            patientsCollection.document(patientId).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for patient data", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val data = snapshot.data
                        if (data != null) {
                            val dateOfBirth = (data["dateOfBirth"] as? Timestamp)?.toDate() ?: Date()

                            val patient = Patient(
                                id = snapshot.id,
                                fullName = data["fullName"] as String,
                                dateOfBirth = dateOfBirth,
                                age = calculateAge(dateOfBirth),
                                gender = Gender.valueOf(data["gender"] as String),
                                address = data["address"] as String,
                                phoneNumber = data["phoneNumber"] as String,
                                policyNumber = data["policyNumber"] as? String ?: "",
                                allergies = (data["allergies"] as? List<*>)?.filterIsInstance<String>(),
                                chronicConditions = (data["chronicConditions"] as? List<*>)?.filterIsInstance<String>()
                            )

                            patientsCache[patientId]?.value = patient
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting patient data", e)
                    }
                }
            }
        }

        // Возвращаем Flow, отфильтрованный от null-значений
        return patientsCache[patientId]!!.map { it ?: throw IllegalStateException("Patient not available") }
    }

    override suspend fun cachePatients(patients: List<Patient>) {
        // Firebase Firestore автоматически кэширует данные, но мы можем
        // добавить и в наш локальный кэш для быстрого доступа
        for (patient in patients) {
            if (patient.id !in patientsCache) {
                patientsCache[patient.id] = MutableStateFlow(patient)
            } else {
                patientsCache[patient.id]?.value = patient
            }
        }

        Log.d(TAG, "Patients are cached (${patients.size} patients)")
    }

    override suspend fun getCachedPatients(): List<Patient> {
        // Возвращаем все пациенты из нашего локального кэша
        return patientsCache.values.mapNotNull { it.value }
    }

    // Вспомогательная функция для расчета возраста
    private fun calculateAge(dateOfBirth: Date): Int {
        val birthCalendar = Calendar.getInstance().apply {
            time = dateOfBirth
        }

        val currentCalendar = Calendar.getInstance()

        var age = currentCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)

        if (currentCalendar.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age
    }

    companion object {
        private const val TAG = "FirebasePatientRepo"
    }
}