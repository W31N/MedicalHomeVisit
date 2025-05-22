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

    companion object {
        private const val TAG = "FirebaseVisitRepo"
    }

    init {
        // Слушаем изменения в коллекции визитов
        visitsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for visits", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val visits = snapshot.documents.mapNotNull { doc ->
                    convertDocumentToVisit(doc.id, doc.data)
                }
                _visitsFlow.value = visits
            }
        }
    }

    private fun convertDocumentToVisit(docId: String, data: Map<String, Any>?): Visit? {
        return try {
            if (data == null) return null

            Visit(
                id = docId,
                patientId = data["patientId"] as? String ?: "",
                scheduledTime = (data["scheduledTime"] as? Timestamp)?.toDate() ?: Date(),
                status = try {
                    VisitStatus.valueOf(data["status"] as? String ?: "PLANNED")
                } catch (e: Exception) {
                    VisitStatus.PLANNED
                },
                address = data["address"] as? String ?: "",
                reasonForVisit = data["reasonForVisit"] as? String ?: "",
                notes = data["notes"] as? String ?: "", // Теперь корректно обрабатываем null
                assignedStaffId = data["assignedStaffId"] as? String,
                assignedStaffName = data["assignedStaffName"] as? String,
                actualStartTime = (data["actualStartTime"] as? Timestamp)?.toDate(),
                actualEndTime = (data["actualEndTime"] as? Timestamp)?.toDate(),
                createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date(),
                isFromRequest = data["isFromRequest"] as? Boolean ?: false,
                originalRequestId = data["originalRequestId"] as? String
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting document to Visit", e)
            null
        }
    }

    override suspend fun updateVisitNotes(visitId: String, notes: String) {
        try {
            val updates = mapOf(
                "notes" to notes,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            visitsCollection.document(visitId).update(updates).await()

            // Обновляем локальный кэш
            val cachedVisits = getCachedVisits().toMutableList()
            val index = cachedVisits.indexOfFirst { it.id == visitId }
            if (index != -1) {
                val updatedVisit = cachedVisits[index].copy(notes = notes)
                cachedVisits[index] = updatedVisit
                // Обновляем flow
                _visitsFlow.value = cachedVisits
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating visit notes", e)
            throw e
        }
    }

    override suspend fun updateScheduledTime(visitId: String, scheduledTime: Date) {
        try {
            val updates = mapOf(
                "scheduledTime" to Timestamp(scheduledTime),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            visitsCollection.document(visitId).update(updates).await()

            // Обновляем локальный кэш
            val cachedVisits = getCachedVisits().toMutableList()
            val index = cachedVisits.indexOfFirst { it.id == visitId }
            if (index != -1) {
                val updatedVisit = cachedVisits[index].copy(scheduledTime = scheduledTime)
                cachedVisits[index] = updatedVisit
                _visitsFlow.value = cachedVisits
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating scheduled time", e)
            throw e
        }
    }

    override suspend fun updateVisit(visit: Visit): Visit {
        try {
            val visitData = mapOf(
                "patientId" to visit.patientId,
                "scheduledTime" to Timestamp(visit.scheduledTime),
                "status" to visit.status.name,
                "address" to visit.address,
                "reasonForVisit" to visit.reasonForVisit,
                "notes" to visit.notes,
                "assignedStaffId" to visit.assignedStaffId,
                "assignedStaffName" to visit.assignedStaffName,
                "actualStartTime" to visit.actualStartTime?.let { Timestamp(it) },
                "actualEndTime" to visit.actualEndTime?.let { Timestamp(it) },
                "isFromRequest" to visit.isFromRequest,
                "originalRequestId" to visit.originalRequestId,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            visitsCollection.document(visit.id).update(visitData).await()

            // Обновляем кэш
            val cachedVisits = getCachedVisits().toMutableList()
            val index = cachedVisits.indexOfFirst { it.id == visit.id }
            if (index != -1) {
                cachedVisits[index] = visit
                _visitsFlow.value = cachedVisits
            }

            return visit
        } catch (e: Exception) {
            Log.e(TAG, "Error updating visit", e)
            throw e
        }
    }

    override suspend fun getVisitsForToday(): List<Visit> {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        try {
            val snapshot = visitsCollection
                .whereGreaterThanOrEqualTo("scheduledTime", Timestamp(today))
                .whereLessThan("scheduledTime", Timestamp(tomorrow))
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                convertDocumentToVisit(doc.id, doc.data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting visits for today", e)
            throw e
        }
    }

    override suspend fun getVisitsForStaff(staffId: String): List<Visit> {
        try {
            val snapshot = visitsCollection
                .whereEqualTo("assignedStaffId", staffId)
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                convertDocumentToVisit(doc.id, doc.data)
            }.sortedBy { it.scheduledTime }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting visits for staff: $staffId", e)
            throw e
        }
    }

    override suspend fun getVisitsForDate(date: Date): List<Visit> {
        val startOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val endOfDay = Calendar.getInstance().apply {
            time = date
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        try {
            val snapshot = visitsCollection
                .whereGreaterThanOrEqualTo("scheduledTime", Timestamp(startOfDay))
                .whereLessThan("scheduledTime", Timestamp(endOfDay))
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                convertDocumentToVisit(doc.id, doc.data)
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
                return convertDocumentToVisit(document.id, document.data)
                    ?: throw IllegalArgumentException("Не удалось преобразовать данные визита")
            } else {
                throw IllegalArgumentException("Визит с ID $visitId не найден")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting visit by ID: $visitId", e)
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
                convertDocumentToVisit(doc.id, doc.data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting visit history for patient: $patientId", e)
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

            Log.d(TAG, "Visit status updated successfully: $visitId -> $newStatus")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating visit status for: $visitId", e)
            throw e
        }
    }

    override suspend fun addUnplannedVisit(visit: Visit): Visit {
        try {
            val visitData = hashMapOf(
                "patientId" to visit.patientId,
                "scheduledTime" to Timestamp(visit.scheduledTime),
                "status" to visit.status.name,
                "address" to visit.address,
                "reasonForVisit" to visit.reasonForVisit,
                "notes" to visit.notes,
                "assignedStaffId" to visit.assignedStaffId,
                "assignedStaffName" to visit.assignedStaffName,
                "actualStartTime" to visit.actualStartTime?.let { Timestamp(it) },
                "actualEndTime" to visit.actualEndTime?.let { Timestamp(it) },
                "isFromRequest" to visit.isFromRequest,
                "originalRequestId" to visit.originalRequestId,
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
        // Но мы можем обновить наш локальный flow для согласованности
        _visitsFlow.value = visits
        Log.d(TAG, "Visits cached locally: ${visits.size} items")
    }

    override suspend fun getCachedVisits(): List<Visit> {
        return _visitsFlow.value
    }

    override suspend fun syncVisits(): Result<List<Visit>> {
        return try {
            val visits = getVisitsForToday()
            _visitsFlow.value = visits
            Result.success(visits)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing visits", e)
            Result.failure(e)
        }
    }
}