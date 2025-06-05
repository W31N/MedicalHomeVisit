package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.domain.model.AppointmentRequest
import com.example.medicalhomevisit.domain.model.RequestStatus
import kotlinx.coroutines.flow.Flow

interface AppointmentRequestRepository {
    suspend fun createRequest(request: AppointmentRequest): Result<AppointmentRequest>
    suspend fun getRequestById(requestId: String): Result<AppointmentRequest>
    suspend fun getMyRequests(): Result<List<AppointmentRequest>>
    suspend fun getRequestsForPatient(patientId: String): Result<List<AppointmentRequest>>
    suspend fun getAllActiveRequests(): Result<List<AppointmentRequest>>
    suspend fun assignRequestToStaff(requestId: String, staffId: String, assignmentNote: String?): Result<AppointmentRequest>
    suspend fun updateRequestStatus(requestId: String, newStatus: RequestStatus, responseMessage: String?): Result<AppointmentRequest>
    suspend fun cancelRequest(requestId: String, reason: String): Result<AppointmentRequest>
    suspend fun getMyAssignedRequests(): Result<List<AppointmentRequest>>
    fun observeMyRequests(): Flow<List<AppointmentRequest>>
}