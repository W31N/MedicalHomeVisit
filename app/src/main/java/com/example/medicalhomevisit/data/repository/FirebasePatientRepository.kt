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
import java.util.Locale

class FirebasePatientRepository : PatientRepository {
    private val db = Firebase.firestore
    private val patientsCollection = db.collection("patients")
    private val patientsCache = mutableMapOf<String, MutableStateFlow<Patient?>>()

    override suspend fun getPatientById(patientId: String): Patient {
        Log.d(TAG, "getPatientById: Attempting to fetch patient with ID '$patientId'")
        if (patientId.isBlank()) {
            Log.e(TAG, "getPatientById: patientId is blank!")
            throw IllegalArgumentException("ID пациента не может быть пустым")
        }

        try {
            val document = patientsCollection.document(patientId).get().await()

            if (document.exists()) {
                Log.d(TAG, "getPatientById: Document found for patientId: '$patientId'")
                val data = document.data ?: run {
                    Log.e(TAG, "getPatientById: Document data is null for patientId: '$patientId'")
                    throw IllegalArgumentException("Документ пациента (ID: '$patientId') не содержит данных.")
                }

                val fullName = data["fullName"] as? String
                    ?: throw IllegalArgumentException("Поле 'fullName' отсутствует или имеет неверный тип для пациента '$patientId'")

                val dateOfBirth = (data["dateOfBirth"] as? Timestamp)?.toDate() // Теперь может быть null

                val genderStr = (data["gender"] as? String)?.uppercase(Locale.getDefault())
                val gender = when (genderStr) {
                    "MALE" -> Gender.MALE
                    "FEMALE" -> Gender.FEMALE
                    else -> {
                        Log.w(TAG, "getPatientById: Invalid or missing gender value '$genderStr' for patient '$patientId'. Defaulting to UNKNOWN.")
                        Gender.UNKNOWN
                    }
                }

                val address = data["address"] as? String
                    ?: throw IllegalArgumentException("Поле 'address' отсутствует или имеет неверный тип для пациента '$patientId'")

                val phoneNumber = data["phoneNumber"] as? String
                    ?: throw IllegalArgumentException("Поле 'phoneNumber' отсутствует или имеет неверный тип для пациента '$patientId'")

                val patient = Patient(
                    id = document.id,
                    fullName = fullName,
                    dateOfBirth = dateOfBirth, // Передаем nullable Date
                    age = dateOfBirth?.let { calculateAge(it) }, // Считаем возраст, если дата есть
                    gender = gender,
                    address = address,
                    phoneNumber = phoneNumber,
                    policyNumber = data["policyNumber"] as? String ?: "",
                    allergies = (data["allergies"] as? List<*>)?.filterIsInstance<String>(),
                    chronicConditions = (data["chronicConditions"] as? List<*>)?.filterIsInstance<String>()
                )
                Log.d(TAG, "getPatientById: Patient mapped successfully: ${patient.fullName} (ID: ${patient.id})")

                if (patientId !in patientsCache) {
                    patientsCache[patientId] = MutableStateFlow(patient)
                } else {
                    patientsCache[patientId]?.value = patient
                }
                Log.d(TAG, "getPatientById: Patient '$patientId' updated in local cache.")
                return patient
            } else {
                Log.w(TAG, "getPatientById: Document NOT found for patientId: '$patientId'")
                throw IllegalArgumentException("Пациент с ID '$patientId' не найден в Firestore")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getPatientById: Unexpected error fetching patient by ID '$patientId': ${e.message}", e)
            throw e
        }
    }

    override suspend fun searchPatients(query: String): List<Patient> {
        Log.d(TAG, "FirebasePatientRepository: searchPatients called with query: '$query'")
        if (query.isEmpty()) {
            Log.d(TAG, "FirebasePatientRepository: Search query is empty, returning empty list.")
            return emptyList()
        }
        try {
            val nameQuery = query.lowercase(Locale.getDefault())
            val snapshot = patientsCollection
                .orderBy("fullName")
                .get()
                .await()
            Log.d(TAG, "FirebasePatientRepository: Fetched ${snapshot.size()} documents for search query '$query'.")

            val patients = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val fullNameInDb = (data["fullName"] as? String)?.lowercase(Locale.getDefault()) ?: ""

                    if (fullNameInDb.contains(nameQuery)) {
                        val dateOfBirth = (data["dateOfBirth"] as? Timestamp)?.toDate()
                        val genderStr = (data["gender"] as? String)?.uppercase(Locale.getDefault())
                        val gender = when (genderStr) {
                            "MALE" -> Gender.MALE
                            "FEMALE" -> Gender.FEMALE
                            else -> Gender.UNKNOWN
                        }
                        Patient(
                            id = doc.id,
                            fullName = data["fullName"] as? String ?: "Имя не указано",
                            dateOfBirth = dateOfBirth,
                            age = dateOfBirth?.let { calculateAge(it) },
                            gender = gender,
                            address = data["address"] as? String ?: "Адрес не указан",
                            phoneNumber = data["phoneNumber"] as? String ?: "Телефон не указан",
                            policyNumber = data["policyNumber"] as? String ?: "",
                            allergies = (data["allergies"] as? List<*>)?.filterIsInstance<String>(),
                            chronicConditions = (data["chronicConditions"] as? List<*>)?.filterIsInstance<String>()
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "FirebasePatientRepository: Error converting patient document during search for docId ${doc.id}", e)
                    null
                }
            }
            Log.d(TAG, "FirebasePatientRepository: Found ${patients.size} patients matching search query '$query'.")
            return patients
        } catch (e: Exception) {
            Log.e(TAG, "FirebasePatientRepository: Error searching patients for query '$query'", e)
            return emptyList()
        }
    }

    override fun observePatient(patientId: String): Flow<Patient> {
        Log.d(TAG, "FirebasePatientRepository: observePatient called for patientId: '$patientId'")
        if (patientId.isBlank()) {
            Log.e(TAG, "FirebasePatientRepository: observePatient - patientId is blank!")
        }
        if (patientId !in patientsCache) {
            patientsCache[patientId] = MutableStateFlow(null)
            Log.d(TAG, "FirebasePatientRepository: Created new MutableStateFlow in cache for patientId: '$patientId'")
        }

        patientsCollection.document(patientId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "FirebasePatientRepository: Error listening for patient data for ID '$patientId'", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG, "FirebasePatientRepository: Snapshot received for patientId: '$patientId'")
                try {
                    val data = snapshot.data
                    if (data != null) {
                        val dateOfBirth = (data["dateOfBirth"] as? Timestamp)?.toDate()
                        val genderStr = (data["gender"] as? String)?.uppercase(Locale.getDefault())
                        val gender = when (genderStr) {
                            "MALE" -> Gender.MALE
                            "FEMALE" -> Gender.FEMALE
                            else -> Gender.UNKNOWN
                        }
                        val patient = Patient(
                            id = snapshot.id,
                            fullName = data["fullName"] as? String ?: "Имя не указано",
                            dateOfBirth = dateOfBirth,
                            age = dateOfBirth?.let { calculateAge(it) },
                            gender = gender,
                            address = data["address"] as? String ?: "Адрес не указан",
                            phoneNumber = data["phoneNumber"] as? String ?: "Телефон не указан",
                            policyNumber = data["policyNumber"] as? String ?: "",
                            allergies = (data["allergies"] as? List<*>)?.filterIsInstance<String>(),
                            chronicConditions = (data["chronicConditions"] as? List<*>)?.filterIsInstance<String>()
                        )
                        Log.d(TAG, "FirebasePatientRepository: Updating cache for patientId '$patientId' with data: ${patient.fullName}")
                        patientsCache[patientId]?.value = patient
                    } else {
                        Log.w(TAG, "FirebasePatientRepository: Snapshot data is null for patientId: '$patientId'")
                        patientsCache[patientId]?.value = null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "FirebasePatientRepository: Error converting patient data from snapshot for ID '$patientId'", e)
                    patientsCache[patientId]?.value = null
                }
            } else {
                Log.w(TAG, "FirebasePatientRepository: Snapshot is null or document does not exist for patientId: '$patientId'")
                patientsCache[patientId]?.value = null
            }
        }
        return patientsCache[patientId]!!.map { patient ->
            patient ?: run {
                Log.e(TAG, "FirebasePatientRepository: observePatient - Patient data is null in flow for patientId '$patientId', throwing IllegalStateException.")
                throw IllegalStateException("Patient data for ID '$patientId' not available in observable flow")
            }
        }
    }

    override suspend fun cachePatients(patients: List<Patient>) {
        Log.d(TAG, "FirebasePatientRepository: cachePatients called with ${patients.size} patients.")
        for (patient in patients) {
            if (patient.id !in patientsCache) {
                patientsCache[patient.id] = MutableStateFlow(patient)
                Log.d(TAG, "FirebasePatientRepository: Patient '${patient.id}' added to local cache from list.")
            } else {
                patientsCache[patient.id]?.value = patient
                Log.d(TAG, "FirebasePatientRepository: Patient '${patient.id}' updated in local cache from list.")
            }
        }
    }

    override suspend fun getCachedPatients(): List<Patient> {
        val cachedList = patientsCache.values.mapNotNull { it.value }
        Log.d(TAG, "FirebasePatientRepository: getCachedPatients returning ${cachedList.size} patients.")
        return cachedList
    }

    private fun calculateAge(dateOfBirth: Date?): Int? {
        if (dateOfBirth == null) return null
        val birthCalendar = Calendar.getInstance().apply {
            time = dateOfBirth
        }
        val currentCalendar = Calendar.getInstance()
        var age = currentCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
        if (currentCalendar.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return if (age < 0) 0 else age
    }

    companion object {
        private const val TAG = "FirebasePatientRepo"
    }
}