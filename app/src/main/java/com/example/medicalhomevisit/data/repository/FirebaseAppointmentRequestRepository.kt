//package com.example.medicalhomevisit.data.repository
//
//import android.util.Log
//import com.example.medicalhomevisit.data.model.AppointmentRequest
//import com.example.medicalhomevisit.data.model.RequestStatus
//import com.example.medicalhomevisit.data.model.RequestType
//import com.example.medicalhomevisit.data.model.UrgencyLevel
//import com.example.medicalhomevisit.domain.repository.AppointmentRequestRepository
//import com.google.firebase.Timestamp
//import com.google.firebase.firestore.FieldValue
//import com.google.firebase.firestore.ktx.firestore
//import com.google.firebase.ktx.Firebase
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.tasks.await
//import java.util.Date
//import java.util.UUID
//
//class FirebaseAppointmentRequestRepository : AppointmentRequestRepository {
//    private val db = Firebase.firestore
//    private val requestsCollection = db.collection("appointment_requests")
//    private val patientRequestsCache = mutableMapOf<String, MutableStateFlow<List<AppointmentRequest>>>()
//    private val _requestsFlow = MutableStateFlow<List<AppointmentRequest>>(emptyList())
//
//    companion object {
//        private const val TAG = "FirebaseAppointmentRepo"
//    }
//
//    init {
//        requestsCollection.addSnapshotListener { snapshot, error ->
//            if (error != null) {
//                Log.e(TAG, "Error listening for requests", error)
//                return@addSnapshotListener
//            }
//
//            if (snapshot != null) {
//                val requests = snapshot.documents.mapNotNull { doc ->
//                    convertDocumentToRequest(doc.id, doc.data)
//                }
//                _requestsFlow.value = requests
//            }
//        }
//    }
//
//    private fun convertDocumentToRequest(docId: String, data: Map<String, Any>?): AppointmentRequest? {
//        return try {
//            if (data == null) return null
//
//            AppointmentRequest(
//                id = docId,
//                patientId = data["patientId"] as? String ?: "",
//                patientName = data["patientName"] as? String ?: "",
//                patientPhone = data["patientPhone"] as? String ?: "",
//                address = data["address"] as? String ?: "",
//                requestType = try {
//                    RequestType.valueOf(data["requestType"] as? String ?: "REGULAR")
//                } catch (e: Exception) {
//                    RequestType.REGULAR
//                },
//                symptoms = data["symptoms"] as? String ?: "",
//                additionalNotes = data["additionalNotes"] as? String ?: "",
//                preferredDate = (data["preferredDate"] as? Timestamp)?.toDate(),
//                preferredTimeRange = data["preferredTimeRange"] as? String ?: "",
//                status = try {
//                    RequestStatus.valueOf(data["status"] as? String ?: "NEW")
//                } catch (e: Exception) {
//                    RequestStatus.NEW
//                },
//                assignedStaffId = data["assignedStaffId"] as? String,
//                assignedStaffName = data["assignedStaffName"] as? String,
//                assignedBy = data["assignedBy"] as? String,
//                assignedAt = (data["assignedAt"] as? Timestamp)?.toDate(),
//                assignmentNote = data["assignmentNote"] as? String,
//                responseMessage = data["responseMessage"] as? String ?: "",
//                createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
//                updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date(),
//                urgencyLevel = (data["urgencyLevel"] as? String)?.let {
//                    try { UrgencyLevel.valueOf(it) } catch (e: Exception) { null }
//                },
//                priority = (data["priority"] as? Long)?.toInt() ?: 0
//            )
//        } catch (e: Exception) {
//            Log.e(TAG, "Error converting document to AppointmentRequest", e)
//            null
//        }
//    }
//
//    override suspend fun createRequest(request: AppointmentRequest): AppointmentRequest {
//        try {
//            val requestId = request.id.ifEmpty { UUID.randomUUID().toString() }
//            val newRequest = request.copy(
//                id = requestId,
//                createdAt = Date(),
//                updatedAt = Date()
//            )
//
//            val requestData = mapOf(
//                "patientId" to newRequest.patientId,
//                "patientName" to newRequest.patientName,
//                "patientPhone" to newRequest.patientPhone,
//                "address" to newRequest.address,
//                "requestType" to newRequest.requestType.name,
//                "symptoms" to newRequest.symptoms,
//                "additionalNotes" to newRequest.additionalNotes,
//                "preferredDate" to newRequest.preferredDate?.let { Timestamp(it) },
//                "preferredTimeRange" to newRequest.preferredTimeRange,
//                "status" to newRequest.status.name,
//                "assignedStaffId" to newRequest.assignedStaffId,
//                "assignedStaffName" to newRequest.assignedStaffName,
//                "assignedBy" to newRequest.assignedBy,
//                "assignedAt" to newRequest.assignedAt?.let { Timestamp(it) },
//                "assignmentNote" to newRequest.assignmentNote,
//                "responseMessage" to newRequest.responseMessage,
//                "urgencyLevel" to newRequest.urgencyLevel?.name,
//                "priority" to newRequest.priority,
//                "createdAt" to FieldValue.serverTimestamp(),
//                "updatedAt" to FieldValue.serverTimestamp()
//            )
//
//            requestsCollection.document(requestId).set(requestData).await()
//
//            patientRequestsCache[newRequest.patientId]?.let { cache ->
//                val currentRequests = cache.value.toMutableList()
//                currentRequests.add(newRequest)
//                cache.value = currentRequests
//            }
//
//            return newRequest
//        } catch (e: Exception) {
//            Log.e(TAG, "Error creating appointment request", e)
//            throw e
//        }
//    }
//
//    override suspend fun getRequestById(requestId: String): AppointmentRequest {
//        try {
//            val document = requestsCollection.document(requestId).get().await()
//            if (document.exists()) {
//                return convertDocumentToRequest(document.id, document.data)
//                    ?: throw IllegalArgumentException("Не удалось преобразовать данные заявки")
//            } else {
//                throw IllegalArgumentException("Заявка с ID $requestId не найдена")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error getting appointment request by ID: $requestId", e)
//            throw e
//        }
//    }
//
//    override suspend fun getPendingRequests(): List<AppointmentRequest> {
//        try {
//            val snapshot = requestsCollection
//                .whereIn("status", listOf(RequestStatus.NEW.name, RequestStatus.PENDING.name))
//                .get()
//                .await()
//
//            return snapshot.documents.mapNotNull { doc ->
//                convertDocumentToRequest(doc.id, doc.data)
//            }.sortedByDescending { it.createdAt }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error getting pending requests", e)
//            throw e
//        }
//    }
//
//    override suspend fun getAllActiveRequests(): List<AppointmentRequest> {
//        try {
//            val activeStatuses = listOf(
//                RequestStatus.NEW.name,
//                RequestStatus.PENDING.name,
//                RequestStatus.ASSIGNED.name,
//                RequestStatus.SCHEDULED.name
//            )
//
//            val snapshot = requestsCollection
//                .whereIn("status", activeStatuses)
//                .get()
//                .await()
//
//            return snapshot.documents.mapNotNull { doc ->
//                convertDocumentToRequest(doc.id, doc.data)
//            }.sortedByDescending { it.createdAt }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error getting all active requests", e)
//            throw e
//        }
//    }
//
//    override suspend fun assignRequestToStaff(
//        requestId: String,
//        staffId: String,
//        staffName: String,
//        assignedBy: String,
//        note: String?
//    ): AppointmentRequest {
//        try {
//            // 1. Обновляем заявку
//            val updates = mapOf(
//                "status" to RequestStatus.ASSIGNED.name,
//                "assignedStaffId" to staffId,
//                "assignedStaffName" to staffName,
//                "assignedBy" to assignedBy,
//                "assignedAt" to FieldValue.serverTimestamp(),
//                "assignmentNote" to note,
//                "updatedAt" to FieldValue.serverTimestamp()
//            )
//
//            requestsCollection.document(requestId).update(updates).await()
//
//            val updatedRequest = getRequestById(requestId)
//
//            val visitData = mapOf(
//                "patientId" to updatedRequest.patientId,
//                "scheduledTime" to (updatedRequest.preferredDate?.let { Timestamp(it) }
//                    ?: FieldValue.serverTimestamp()),
//                "status" to "PLANNED", // VisitStatus.PLANNED
//                "address" to updatedRequest.address,
//                "reasonForVisit" to "${getRequestTypeText(updatedRequest.requestType)}: ${updatedRequest.symptoms}",
//                "notes" to (updatedRequest.additionalNotes),
//                "assignedStaffId" to staffId,
//                "assignedStaffName" to staffName,
//                "originalRequestId" to requestId,
//                "isFromRequest" to true,
//                "createdAt" to FieldValue.serverTimestamp(),
//                "updatedAt" to FieldValue.serverTimestamp()
//            )
//
//
//            db.collection("visits").document(requestId).set(visitData).await()
//
//            requestsCollection.document(requestId)
//                .update("status", RequestStatus.SCHEDULED.name)
//                .await()
//
//            return getRequestById(requestId)
//        } catch (e: Exception) {
//            Log.e(TAG, "Error assigning request to staff", e)
//            throw e
//        }
//    }
//
//    private fun getRequestTypeText(requestType: RequestType): String {
//        return when (requestType) {
//            RequestType.EMERGENCY -> "Неотложная"
//            RequestType.REGULAR -> "Плановая"
//            RequestType.CONSULTATION -> "Консультация"
//        }
//    }
//
//    override suspend fun updateRequestStatus(requestId: String, status: RequestStatus) {
//        try {
//            val updates = mapOf(
//                "status" to status.name,
//                "updatedAt" to FieldValue.serverTimestamp()
//            )
//            requestsCollection.document(requestId).update(updates).await()
//        } catch (e: Exception) {
//            Log.e(TAG, "Error updating request status", e)
//            throw e
//        }
//    }
//
//    override suspend fun updateRequestStatus(
//        requestId: String,
//        status: RequestStatus,
//        responseMessage: String?
//    ): AppointmentRequest {
//        try {
//            val updates = mutableMapOf<String, Any>(
//                "status" to status.name,
//                "updatedAt" to FieldValue.serverTimestamp()
//            )
//
//            responseMessage?.let { updates["responseMessage"] = it }
//
//            requestsCollection.document(requestId).update(updates).await()
//            return getRequestById(requestId)
//        } catch (e: Exception) {
//            Log.e(TAG, "Error updating request status with message", e)
//            throw e
//        }
//    }
//
//    override suspend fun getRequestsForStaff(staffId: String): List<AppointmentRequest> {
//        try {
//            val snapshot = requestsCollection
//                .whereEqualTo("assignedStaffId", staffId)
//                .get()
//                .await()
//
//            return snapshot.documents.mapNotNull { doc ->
//                convertDocumentToRequest(doc.id, doc.data)
//            }.sortedByDescending { it.updatedAt }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error getting requests for staff: $staffId", e)
//            throw e
//        }
//    }
//
//    override suspend fun getRequestsForPatient(patientId: String): List<AppointmentRequest> {
//        try {
//            val snapshot = requestsCollection
//                .whereEqualTo("patientId", patientId)
//                .get()
//                .await()
//
//            val requests = snapshot.documents.mapNotNull { doc ->
//                convertDocumentToRequest(doc.id, doc.data)
//            }.sortedByDescending { it.updatedAt }
//
//            // Обновляем кэш
//            if (patientId !in patientRequestsCache) {
//                patientRequestsCache[patientId] = MutableStateFlow(requests)
//            } else {
//                patientRequestsCache[patientId]?.value = requests
//            }
//
//            return requests
//        } catch (e: Exception) {
//            Log.e(TAG, "Error getting requests for patient: $patientId", e)
//            throw e
//        }
//    }
//
//    override suspend fun getActiveRequestsForPatient(patientId: String): List<AppointmentRequest> {
//        try {
//            val snapshot = requestsCollection
//                .whereEqualTo("patientId", patientId)
//                .get()
//                .await()
//
//            return snapshot.documents.mapNotNull { doc ->
//                convertDocumentToRequest(doc.id, doc.data)
//            }.filter { request ->
//                request.status in listOf(
//                    RequestStatus.NEW,
//                    RequestStatus.PENDING,
//                    RequestStatus.ASSIGNED,
//                    RequestStatus.SCHEDULED
//                )
//            }.sortedByDescending { it.updatedAt }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error getting active requests for patient: $patientId", e)
//            throw e
//        }
//    }
//
//    override suspend fun cancelRequest(requestId: String, reason: String): AppointmentRequest {
//        return updateRequestStatus(requestId, RequestStatus.CANCELLED, reason)
//    }
//
//    override fun observeRequests(): Flow<List<AppointmentRequest>> {
//        return _requestsFlow.asStateFlow()
//    }
//
//    override fun observeRequestsForPatient(patientId: String): Flow<List<AppointmentRequest>> {
//        if (patientId !in patientRequestsCache) {
//            patientRequestsCache[patientId] = MutableStateFlow(emptyList())
//
//            requestsCollection
//                .whereEqualTo("patientId", patientId)
//                .addSnapshotListener { snapshot, error ->
//                    if (error != null) {
//                        Log.e(TAG, "Error listening for patient requests", error)
//                        return@addSnapshotListener
//                    }
//
//                    if (snapshot != null) {
//                        val requests = snapshot.documents.mapNotNull { doc ->
//                            convertDocumentToRequest(doc.id, doc.data)
//                        }.sortedByDescending { it.updatedAt }
//
//                        patientRequestsCache[patientId]?.value = requests
//                    }
//                }
//        }
//
//        return patientRequestsCache[patientId]!!.asStateFlow()
//    }
//
//    override suspend fun syncRequests(): Result<List<AppointmentRequest>> {
//        return try {
//            val requests = requestsCollection.get().await().documents.mapNotNull { doc ->
//                convertDocumentToRequest(doc.id, doc.data)
//            }
//
//            cacheRequests(requests)
//            Result.success(requests)
//        } catch (e: Exception) {
//            Log.e(TAG, "Error syncing requests", e)
//            Result.failure(e)
//        }
//    }
//
//    override suspend fun cacheRequests(requests: List<AppointmentRequest>) {
//        _requestsFlow.value = requests
//        Log.d(TAG, "Requests cached locally: ${requests.size} items")
//    }
//
//    override suspend fun getCachedRequests(): List<AppointmentRequest> {
//        return _requestsFlow.value
//    }
//}