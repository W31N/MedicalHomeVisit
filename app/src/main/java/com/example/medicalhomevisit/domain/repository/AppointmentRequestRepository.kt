// com/example/medicalhomevisit/domain/repository/AppointmentRequestRepository.kt
package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.data.model.AppointmentRequest
import com.example.medicalhomevisit.data.model.RequestStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface AppointmentRequestRepository {
    suspend fun createRequest(request: AppointmentRequest): AppointmentRequest
    suspend fun getRequestById(requestId: String): AppointmentRequest
    suspend fun getRequestsForPatient(patientId: String): List<AppointmentRequest>
    fun observeRequestsForPatient(patientId: String): Flow<List<AppointmentRequest>>
    suspend fun updateRequestStatus(requestId: String, status: RequestStatus, responseMessage: String? = null): AppointmentRequest
    suspend fun cancelRequest(requestId: String, reason: String): AppointmentRequest
    suspend fun getActiveRequestsForPatient(patientId: String): List<AppointmentRequest>
}