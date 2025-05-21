// com/example/medicalhomevisit/data/repository/FirebaseAppointmentRequestRepository.kt
package com.example.medicalhomevisit.data.repository

import android.util.Log
import com.example.medicalhomevisit.data.model.AppointmentRequest
import com.example.medicalhomevisit.data.model.RequestStatus
import com.example.medicalhomevisit.data.model.RequestType
import com.example.medicalhomevisit.domain.repository.AppointmentRequestRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class FirebaseAppointmentRequestRepository : AppointmentRequestRepository {
    private val db = Firebase.firestore
    private val requestsCollection = db.collection("appointment_requests")
    private val patientRequestsCache = mutableMapOf<String, MutableStateFlow<List<AppointmentRequest>>>()

    override suspend fun createRequest(request: AppointmentRequest): AppointmentRequest {
        try {
            val requestId = request.id.ifEmpty { UUID.randomUUID().toString() }
            val newRequest = request.copy(
                id = requestId,
                createdAt = Date(),
                updatedAt = Date()
            )

            val requestData = mapOf(
                "patientId" to newRequest.patientId,
                "requestType" to newRequest.requestType.name,
                "symptoms" to newRequest.symptoms,
                "preferredDate" to newRequest.preferredDate?.let { Timestamp(it) },
                "preferredTimeRange" to newRequest.preferredTimeRange,
                "address" to newRequest.address,
                "additionalNotes" to newRequest.additionalNotes,
                "status" to newRequest.status.name,
                "assignedStaffId" to newRequest.assignedStaffId,
                "responseMessage" to newRequest.responseMessage,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            requestsCollection.document(requestId).set(requestData).await()

            // Обновляем кэш, если он существует для этого пациента
            patientRequestsCache[newRequest.patientId]?.let { cache ->
                val currentRequests = cache.value.toMutableList()
                currentRequests.add(newRequest)
                cache.value = currentRequests
            }

            return newRequest
        } catch (e: Exception) {
            Log.e(TAG, "Error creating appointment request", e)
            throw e
        }
    }

    override suspend fun getRequestById(requestId: String): AppointmentRequest {
        try {
            val document = requestsCollection.document(requestId).get().await()
            if (document.exists()) {
                val data = document.data ?: throw IllegalArgumentException("Документ не содержит данных")

                return AppointmentRequest(
                    id = document.id,
                    patientId = data["patientId"] as String,
                    requestType = RequestType.valueOf(data["requestType"] as String),
                    symptoms = data["symptoms"] as String,
                    preferredDate = (data["preferredDate"] as? Timestamp)?.toDate(),
                    preferredTimeRange = data["preferredTimeRange"] as? String,
                    address = data["address"] as String,
                    additionalNotes = data["additionalNotes"] as? String,
                    status = RequestStatus.valueOf(data["status"] as String),
                    assignedStaffId = data["assignedStaffId"] as? String,
                    responseMessage = data["responseMessage"] as? String,
                    createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                    updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date()
                )
            } else {
                throw IllegalArgumentException("Запрос не найден")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting appointment request by ID", e)
            throw e
        }
    }

    override suspend fun getRequestsForPatient(patientId: String): List<AppointmentRequest> {
        try {
            val snapshot = requestsCollection
                .whereEqualTo("patientId", patientId)
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val requests = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null

                    AppointmentRequest(
                        id = doc.id,
                        patientId = data["patientId"] as String,
                        requestType = RequestType.valueOf(data["requestType"] as String),
                        symptoms = data["symptoms"] as String,
                        preferredDate = (data["preferredDate"] as? Timestamp)?.toDate(),
                        preferredTimeRange = data["preferredTimeRange"] as? String,
                        address = data["address"] as String,
                        additionalNotes = data["additionalNotes"] as? String,
                        status = RequestStatus.valueOf(data["status"] as String),
                        assignedStaffId = data["assignedStaffId"] as? String,
                        responseMessage = data["responseMessage"] as? String,
                        createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                        updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document", e)
                    null
                }
            }

            // Обновляем кэш
            if (patientId !in patientRequestsCache) {
                patientRequestsCache[patientId] = MutableStateFlow(requests)
            } else {
                patientRequestsCache[patientId]?.value = requests
            }

            return requests
        } catch (e: Exception) {
            Log.e(TAG, "Error getting requests for patient", e)
            throw e
        }
    }

    override fun observeRequestsForPatient(patientId: String): Flow<List<AppointmentRequest>> {
        // Инициализируем кэш, если его еще нет
        if (patientId !in patientRequestsCache) {
            patientRequestsCache[patientId] = MutableStateFlow(emptyList())

            // Настраиваем слушатель для реального времени
            requestsCollection
                .whereEqualTo("patientId", patientId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening for patient requests", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val requests = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data ?: return@mapNotNull null

                                AppointmentRequest(
                                    id = doc.id,
                                    patientId = data["patientId"] as String,
                                    requestType = RequestType.valueOf(data["requestType"] as String),
                                    symptoms = data["symptoms"] as String,
                                    preferredDate = (data["preferredDate"] as? Timestamp)?.toDate(),
                                    preferredTimeRange = data["preferredTimeRange"] as? String,
                                    address = data["address"] as String,
                                    additionalNotes = data["additionalNotes"] as? String,
                                    status = RequestStatus.valueOf(data["status"] as String),
                                    assignedStaffId = data["assignedStaffId"] as? String,
                                    responseMessage = data["responseMessage"] as? String,
                                    createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                                    updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date()
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error converting document", e)
                                null
                            }
                        }

                        patientRequestsCache[patientId]?.value = requests
                    }
                }
        }

        return patientRequestsCache[patientId]!!.asStateFlow()
    }

    override suspend fun updateRequestStatus(
        requestId: String,
        status: RequestStatus,
        responseMessage: String?
    ): AppointmentRequest {
        try {
            val updates = mutableMapOf<String, Any>(
                "status" to status.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            responseMessage?.let { updates["responseMessage"] = it }

            requestsCollection.document(requestId)
                .update(updates)
                .await()

            return getRequestById(requestId)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating request status", e)
            throw e
        }
    }

    override suspend fun cancelRequest(requestId: String, reason: String): AppointmentRequest {
        return updateRequestStatus(requestId, RequestStatus.CANCELLED, reason)
    }

    override suspend fun getActiveRequestsForPatient(patientId: String): List<AppointmentRequest> {
        try {
            val snapshot = requestsCollection
                .whereEqualTo("patientId", patientId)
                .whereIn("status", listOf(
                    RequestStatus.NEW.name,
                    RequestStatus.PENDING.name,
                    RequestStatus.ASSIGNED.name,
                    RequestStatus.SCHEDULED.name
                ))
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null

                    AppointmentRequest(
                        id = doc.id,
                        patientId = data["patientId"] as String,
                        requestType = RequestType.valueOf(data["requestType"] as String),
                        symptoms = data["symptoms"] as String,
                        preferredDate = (data["preferredDate"] as? Timestamp)?.toDate(),
                        preferredTimeRange = data["preferredTimeRange"] as? String,
                        address = data["address"] as String,
                        additionalNotes = data["additionalNotes"] as? String,
                        status = RequestStatus.valueOf(data["status"] as String),
                        assignedStaffId = data["assignedStaffId"] as? String,
                        responseMessage = data["responseMessage"] as? String,
                        createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                        updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active requests for patient", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "FirebaseAppointmentRepo"
    }
}