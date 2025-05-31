package com.example.medicalhomevisit.data.remote

import android.util.Log
import com.example.medicalhomevisit.data.model.*
import com.example.medicalhomevisit.data.remote.dtos.*
import com.example.medicalhomevisit.domain.repository.AppointmentRequestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class BackendAppointmentRequestRepository @Inject constructor(
    private val apiService: AppointmentApiService
) : AppointmentRequestRepository {

    private val _requestsFlow = MutableStateFlow<List<AppointmentRequest>>(emptyList())
    private val requestsCache = mutableMapOf<String, MutableStateFlow<List<AppointmentRequest>>>()

    companion object {
        private const val TAG = "BackendAppointmentRepo"
    }

    override suspend fun createRequest(request: AppointmentRequest): AppointmentRequest {
        return withContext(Dispatchers.IO) {
            try {
                val createDto = CreateAppointmentRequestDto(
                    requestType = request.requestType.name,
                    symptoms = request.symptoms,
                    additionalNotes = request.additionalNotes,
                    preferredDateTime = request.preferredDate,
                    address = request.address
                )

                val response = apiService.createRequest(createDto)
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!
                    val createdRequest = convertDtoToModel(dto)

                    updateCache(request.patientId, createdRequest)

                    createdRequest
                } else {
                    throw Exception("Ошибка создания заявки: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating request", e)
                throw e
            }
        }
    }

    override suspend fun getRequestById(requestId: String): AppointmentRequest {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRequestById(requestId)
                if (response.isSuccessful && response.body() != null) {
                    convertDtoToModel(response.body()!!)
                } else {
                    throw Exception("Заявка не найдена")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting request by ID", e)
                throw e
            }
        }
    }

    override suspend fun getRequestsForPatient(patientId: String): List<AppointmentRequest> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMyRequests() // Используем /my endpoint для пациента
                if (response.isSuccessful && response.body() != null) {
                    val requests = response.body()!!.map { convertDtoToModel(it) }
                    
                    if (patientId !in requestsCache) {
                        requestsCache[patientId] = MutableStateFlow(requests)
                    } else {
                        requestsCache[patientId]?.value = requests
                    }

                    requests
                } else {
                    throw Exception("Ошибка загрузки заявок")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting patient requests", e)
                throw e
            }
        }
    }

    override suspend fun getAllActiveRequests(): List<AppointmentRequest> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAllActiveRequests()
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.map { convertDtoToModel(it) }
                } else {
                    throw Exception("Ошибка загрузки активных заявок")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting active requests", e)
                throw e
            }
        }
    }

    override suspend fun assignRequestToStaff(
        requestId: String,
        staffId: String,
        staffName: String,
        assignedBy: String,
        note: String?
    ): AppointmentRequest {
        return withContext(Dispatchers.IO) {
            try {
                val assignDto = AssignStaffToRequestDto(staffId, note)
                val response = apiService.assignRequest(requestId, assignDto)

                if (response.isSuccessful && response.body() != null) {
                    convertDtoToModel(response.body()!!)
                } else {
                    throw Exception("Ошибка назначения врача")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error assigning staff", e)
                throw e
            }
        }
    }

    override suspend fun updateRequestStatus(requestId: String, status: RequestStatus) {
        withContext(Dispatchers.IO) {
            try {
                val updateDto = UpdateRequestStatusDto(status.name)
                apiService.updateRequestStatus(requestId, updateDto)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating status", e)
                throw e
            }
        }
    }

    override suspend fun updateRequestStatus(
        requestId: String,
        status: RequestStatus,
        responseMessage: String?
    ): AppointmentRequest {
        return withContext(Dispatchers.IO) {
            try {
                val updateDto = UpdateRequestStatusDto(status.name, responseMessage)
                val response = apiService.updateRequestStatus(requestId, updateDto)

                if (response.isSuccessful && response.body() != null) {
                    convertDtoToModel(response.body()!!)
                } else {
                    throw Exception("Ошибка обновления статуса")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating status", e)
                throw e
            }
        }
    }

    override suspend fun cancelRequest(requestId: String, reason: String): AppointmentRequest {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.cancelRequest(requestId, reason)
                if (response.isSuccessful && response.body() != null) {
                    convertDtoToModel(response.body()!!)
                } else {
                    throw Exception("Ошибка отмены заявки")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling request", e)
                throw e
            }
        }
    }

    override suspend fun getPendingRequests(): List<AppointmentRequest> {
        return getAllActiveRequests().filter {
            it.status == RequestStatus.NEW || it.status == RequestStatus.PENDING
        }
    }

    override suspend fun getRequestsForStaff(staffId: String): List<AppointmentRequest> {
        // Этот метод потребует отдельного endpoint на бэкенде
        // Пока возвращаем пустой список
        return emptyList()
    }

    override suspend fun getActiveRequestsForPatient(patientId: String): List<AppointmentRequest> {
        return getRequestsForPatient(patientId).filter { request ->
            request.status in listOf(
                RequestStatus.NEW,
                RequestStatus.PENDING,
                RequestStatus.ASSIGNED,
                RequestStatus.SCHEDULED
            )
        }
    }

    override fun observeRequests(): Flow<List<AppointmentRequest>> {
        return _requestsFlow.asStateFlow()
    }

    override fun observeRequestsForPatient(patientId: String): Flow<List<AppointmentRequest>> {
        if (patientId !in requestsCache) {
            requestsCache[patientId] = MutableStateFlow(emptyList())
        }
        return requestsCache[patientId]!!.asStateFlow()
    }

    override suspend fun syncRequests(): Result<List<AppointmentRequest>> {
        return try {
            val requests = getRequestsForPatient("") // Нужно получить ID текущего пользователя
            _requestsFlow.value = requests
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cacheRequests(requests: List<AppointmentRequest>) {
        _requestsFlow.value = requests
    }

    override suspend fun getCachedRequests(): List<AppointmentRequest> {
        return _requestsFlow.value
    }

    // Вспомогательные методы
    private fun convertDtoToModel(dto: AppointmentRequestDto): AppointmentRequest {
        return AppointmentRequest(
            id = dto.id,
            patientId = "", // Нужно будет получать из контекста или добавить в DTO
            patientName = dto.emailPatient, // Временно используем email
            patientPhone = "Не указано", // Добавить в DTO
            address = dto.address,
            requestType = try {
                RequestType.valueOf(dto.requestType)
            } catch (e: Exception) {
                RequestType.REGULAR
            },
            symptoms = dto.symptoms,
            additionalNotes = dto.additionalNotes ?: "",
            preferredDate = dto.preferredDateTime,
            status = try {
                RequestStatus.valueOf(dto.status)
            } catch (e: Exception) {
                RequestStatus.NEW
            },
            assignedStaffId = null, // Нужно добавить в DTO
            assignedStaffName = dto.assignedStaffEmail,
            assignedBy = dto.assignedByUserEmail,
            assignedAt = dto.assignedAt,
            assignmentNote = dto.assignmentNote,
            responseMessage = dto.responseMessage ?: "",
            createdAt = Date(), // Добавить в DTO
            updatedAt = Date()  // Добавить в DTO
        )
    }

    private fun updateCache(patientId: String, request: AppointmentRequest) {
        if (patientId in requestsCache) {
            val currentRequests = requestsCache[patientId]?.value?.toMutableList() ?: mutableListOf()
            currentRequests.add(request)
            requestsCache[patientId]?.value = currentRequests
        }
    }
}