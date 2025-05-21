// FirebaseVisitRepository.kt
package com.example.medicalhomevisit.data.repository

import android.util.Log
import com.example.medicalhomevisit.data.model.Visit
import com.example.medicalhomevisit.data.model.VisitStatus
import com.example.medicalhomevisit.domain.repository.VisitRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class FirebaseVisitRepository : VisitRepository {
    private val db = Firebase.firestore
    private val visitsCollection = db.collection("visits")
    private val _visitsFlow = MutableStateFlow<List<Visit>>(emptyList())

    init {
        // Слушаем изменения в коллекции визитов
        visitsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for visits", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val visits = snapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        Visit(
                            id = doc.id,
                            patientId = data["patientId"] as String,
                            scheduledTime = (data["scheduledTime"] as Timestamp).toDate(),
                            status = VisitStatus.valueOf(data["status"] as String),
                            address = data["address"] as String,
                            reasonForVisit = data["reasonForVisit"] as String,
                            notes = data["notes"] as? String,
                            createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                            updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document", e)
                        null
                    }
                }
                _visitsFlow.value = visits
            }
        }
    }

    override suspend fun getVisitsForToday(): List<Visit> {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time

        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time

        try {
            val snapshot = visitsCollection
                .whereGreaterThanOrEqualTo("scheduledTime", today)
                .whereLessThan("scheduledTime", tomorrow)
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    Visit(
                        id = doc.id,
                        patientId = data["patientId"] as String,
                        scheduledTime = (data["scheduledTime"] as Timestamp).toDate(),
                        status = VisitStatus.valueOf(data["status"] as String),
                        address = data["address"] as String,
                        reasonForVisit = data["reasonForVisit"] as String,
                        notes = data["notes"] as? String,
                        createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                        updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting visits for today", e)
            throw e
        }
    }

    override suspend fun getVisitsForDate(date: Date): List<Visit> {
        val startOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time

        val endOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.time

        try {
            val snapshot = visitsCollection
                .whereGreaterThanOrEqualTo("scheduledTime", startOfDay)
                .whereLessThan("scheduledTime", endOfDay)
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    Visit(
                        id = doc.id,
                        patientId = data["patientId"] as String,
                        scheduledTime = (data["scheduledTime"] as Timestamp).toDate(),
                        status = VisitStatus.valueOf(data["status"] as String),
                        address = data["address"] as String,
                        reasonForVisit = data["reasonForVisit"] as String,
                        notes = data["notes"] as? String,
                        createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                        updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting visits for date", e)
            throw e
        }
    }

    override fun observeVisits(): Flow<List<Visit>> {
        return _visitsFlow.asStateFlow()
    }

    override suspend fun getVisitById(visitId: String): Visit {
        try {
            val document = visitsCollection.document(visitId).get().await()
            if (document.exists()) {
                val data = document.data ?: throw IllegalArgumentException("Документ не содержит данных")
                return Visit(
                    id = document.id,
                    patientId = data["patientId"] as String,
                    scheduledTime = (data["scheduledTime"] as Timestamp).toDate(),
                    status = VisitStatus.valueOf(data["status"] as String),
                    address = data["address"] as String,
                    reasonForVisit = data["reasonForVisit"] as String,
                    notes = data["notes"] as? String,
                    createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                    updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date()
                )
            } else {
                throw IllegalArgumentException("Визит не найден")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting visit by ID", e)
            throw e
        }
    }

    override suspend fun getVisitHistoryForPatient(patientId: String): List<Visit> {
        try {
            val snapshot = visitsCollection
                .whereEqualTo("patientId", patientId)
                .orderBy("scheduledTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    Visit(
                        id = doc.id,
                        patientId = data["patientId"] as String,
                        scheduledTime = (data["scheduledTime"] as Timestamp).toDate(),
                        status = VisitStatus.valueOf(data["status"] as String),
                        address = data["address"] as String,
                        reasonForVisit = data["reasonForVisit"] as String,
                        notes = data["notes"] as? String,
                        createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                        updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting visit history for patient", e)
            throw e
        }
    }

    override suspend fun updateVisitStatus(visitId: String, newStatus: VisitStatus) {
        try {
            val statusUpdate = hashMapOf<String, Any>(
                "status" to newStatus.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            visitsCollection.document(visitId)
                .update(statusUpdate)
                .await()

            Log.d(TAG, "Visit status updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating visit status", e)
            throw e
        }
    }

    override suspend fun addUnplannedVisit(visit: Visit): Visit {
        try {
            val visitData = hashMapOf(
                "patientId" to visit.patientId,
                "scheduledTime" to visit.scheduledTime,
                "status" to visit.status.name,
                "address" to visit.address,
                "reasonForVisit" to visit.reasonForVisit,
                "notes" to visit.notes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val docRef = visitsCollection.add(visitData).await()
            return visit.copy(id = docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding unplanned visit", e)
            throw e
        }
    }

    override suspend fun cacheVisits(visits: List<Visit>) {
        // В Firebase Firestore кэширование происходит автоматически
        Log.d(TAG, "Visits are automatically cached by Firebase")
    }

    override suspend fun getCachedVisits(): List<Visit> {
        return _visitsFlow.value
    }

    override suspend fun syncVisits(): Result<List<Visit>> {
        return try {
            val visits = getVisitsForToday()
            Result.success(visits)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing visits", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "FirebaseVisitRepo"
    }
}