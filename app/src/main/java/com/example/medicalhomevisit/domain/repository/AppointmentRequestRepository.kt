package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.data.model.AppointmentRequest
import com.example.medicalhomevisit.data.model.RequestStatus
import kotlinx.coroutines.flow.Flow

interface AppointmentRequestRepository {
    suspend fun createRequest(request: AppointmentRequest): AppointmentRequest
    suspend fun getRequestById(requestId: String): AppointmentRequest
    suspend fun getPendingRequests(): List<AppointmentRequest>
    suspend fun getAllActiveRequests(): List<AppointmentRequest> // Добавляем этот метод
    suspend fun assignRequestToStaff(
        requestId: String,
        staffId: String,
        staffName: String,
        assignedBy: String,
        note: String? = null
    ): AppointmentRequest
    suspend fun updateRequestStatus(requestId: String, status: RequestStatus)
    suspend fun updateRequestStatus(
        requestId: String,
        status: RequestStatus,
        responseMessage: String?
    ): AppointmentRequest
    suspend fun getRequestsForStaff(staffId: String): List<AppointmentRequest>
    suspend fun getRequestsForPatient(patientId: String): List<AppointmentRequest>
    suspend fun getActiveRequestsForPatient(patientId: String): List<AppointmentRequest>
    suspend fun cancelRequest(requestId: String, reason: String): AppointmentRequest
    fun observeRequests(): Flow<List<AppointmentRequest>>
    fun observeRequestsForPatient(patientId: String): Flow<List<AppointmentRequest>>

    // Добавляем недостающий метод синхронизации
    suspend fun syncRequests(): Result<List<AppointmentRequest>>
    suspend fun cacheRequests(requests: List<AppointmentRequest>)
    suspend fun getCachedRequests(): List<AppointmentRequest>
}