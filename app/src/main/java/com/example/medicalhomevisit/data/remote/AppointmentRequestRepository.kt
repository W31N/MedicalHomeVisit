package com.example.medicalhomevisit.data.remote

import com.example.medicalhomevisit.data.model.AppointmentRequest // Ваша доменная модель
import com.example.medicalhomevisit.data.model.RequestStatus // Ваш доменный enum
import kotlinx.coroutines.flow.Flow

interface AppointmentRequestRepository {
    // Метод для создания заявки пациентом
    suspend fun createRequest(request: AppointmentRequest): Result<AppointmentRequest>

    // Метод для получения заявки по ID
    suspend fun getRequestById(requestId: String): Result<AppointmentRequest>

    // Метод для получения заявок текущего аутентифицированного пациента
    suspend fun getMyRequests(): Result<List<AppointmentRequest>>

    // Метод для получения заявок конкретного пациента (например, для админа)
    suspend fun getRequestsForPatient(patientId: String): Result<List<AppointmentRequest>>

    // Метод для получения всех активных заявок (для админа/диспетчера)
    suspend fun getAllActiveRequests(): Result<List<    AppointmentRequest>>

    // Метод для назначения медработника на заявку
    suspend fun assignRequestToStaff(requestId: String, staffId: String, assignmentNote: String?): Result<AppointmentRequest>

    // Метод для обновления статуса заявки
    suspend fun updateRequestStatus(requestId: String, newStatus: RequestStatus, responseMessage: String?): Result<AppointmentRequest>

    // Метод для отмены заявки
    suspend fun cancelRequest(requestId: String, reason: String): Result<AppointmentRequest>

    // Метод для получения назначенных заявок для ТЕКУЩЕГО медработника
    suspend fun getMyAssignedRequests(): Result<List<AppointmentRequest>>

    // Поток для наблюдения за "моими" заявками (заявками текущего пациента)
    fun observeMyRequests(): Flow<List<AppointmentRequest>>

}