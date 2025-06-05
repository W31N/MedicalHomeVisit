package com.example.medicalhomevisit.data.remote.repository

import android.util.Log
import com.example.medicalhomevisit.data.remote.api.AppointmentApiService
import com.example.medicalhomevisit.data.remote.dto.*
import com.example.medicalhomevisit.domain.model.AppointmentRequest
import com.example.medicalhomevisit.domain.model.RequestStatus
import com.example.medicalhomevisit.domain.model.RequestType
import com.example.medicalhomevisit.domain.repository.AppointmentRequestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class   AppointmentRequestRepositoryImpl @Inject constructor(
    private val apiService: AppointmentApiService
) : AppointmentRequestRepository {

    private val _myRequestsFlow = MutableStateFlow<List<AppointmentRequest>>(emptyList())

    companion object {
        private const val TAG = "BackendAppointmentRepo"
    }

    override suspend fun createRequest(request: AppointmentRequest): Result<AppointmentRequest> {
        Log.d(TAG, "createRequest called with request for patientId: ${request.patientId}")
        return withContext(Dispatchers.IO) {
            try {
                val createDto = CreateAppointmentRequestDto(
                    requestType = request.requestType.name,
                    symptoms = request.symptoms,
                    additionalNotes = request.additionalNotes,
                    preferredDateTime = request.preferredDateTime,
                    address = request.address
                )
                Log.d(TAG, "Attempting to create request on backend with DTO: $createDto")
                val response = apiService.createRequest(createDto)
                Log.d(TAG, "Create request API response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!
                    Log.d(TAG, "Create request successful. Response DTO: $dto")
                    val createdRequest = convertDtoToModel(dto)
                    val currentRequests = _myRequestsFlow.value.toMutableList()
                    currentRequests.add(0, createdRequest)
                    _myRequestsFlow.value = currentRequests
                    Log.d(TAG, "Request created and mapped to domain model. ID: ${createdRequest.id}. Updated _myRequestsFlow.")
                    Result.success(createdRequest)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Error creating request: Code=${response.code()}, Body='$errorBody'")
                    Result.failure(Exception("Ошибка создания заявки: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in createRequest", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getRequestById(requestId: String): Result<AppointmentRequest> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRequestById(requestId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(convertDtoToModel(response.body()!!))
                } else {
                    Result.failure(Exception("Заявка не найдена"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting request by ID", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getMyRequests(): Result<List<AppointmentRequest>> {
        Log.d(TAG, "getMyRequests() called.")
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Calling apiService.getMyRequests()")
                val response = apiService.getMyRequests()
                Log.d(TAG, "getMyRequests API response: code=${response.code()}, isSuccessful=${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    val dtoList = response.body()!!
                    Log.d(TAG, "getMyRequests successful. Received ${dtoList.size} DTOs from backend.")
                    val requests = dtoList.map { convertDtoToModel(it) }
                        .sortedByDescending { it.createdAt }
                    Log.d(TAG, "Mapped to ${requests.size} domain models.")

                    _myRequestsFlow.value = requests
                    Log.d(TAG, "_myRequestsFlow updated with ${requests.size} items.")
                    Result.success(requests)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error from backend"
                    Log.e(TAG, "Error getting my requests: Code=${response.code()}, Body='$errorBody'")
                    Result.failure(Exception("Ошибка загрузки заявок (код: ${response.code()})"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getMyRequests", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getRequestsForPatient(patientId: String): Result<List<AppointmentRequest>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRequestsForPatient(patientId)
                if (response.isSuccessful && response.body() != null) {
                    val requests = response.body()!!.map { convertDtoToModel(it) }
                    Result.success(requests)
                } else {
                    Result.failure(Exception("Ошибка загрузки заявок пациента"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting patient requests", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getAllActiveRequests(): Result<List<AppointmentRequest>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAllActiveRequests()
                if (response.isSuccessful && response.body() != null) {
                    val requests = response.body()!!.map { convertDtoToModel(it) }
                    Result.success(requests)
                } else {
                    Result.failure(Exception("Ошибка загрузки активных заявок"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting active requests", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun assignRequestToStaff(
        requestId: String,
        staffId: String,
        assignmentNote: String?
    ): Result<AppointmentRequest> {
        return withContext(Dispatchers.IO) {
            try {
                val assignDto = AssignStaffToRequestDto(staffId, assignmentNote)
                val response = apiService.assignRequest(requestId, assignDto)

                if (response.isSuccessful && response.body() != null) {
                    val updatedRequest = convertDtoToModel(response.body()!!)
                    Result.success(updatedRequest)
                } else {
                    Result.failure(Exception("Ошибка назначения врача"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error assigning staff", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateRequestStatus(
        requestId: String,
        newStatus: RequestStatus,
        responseMessage: String?
    ): Result<AppointmentRequest> {
        return withContext(Dispatchers.IO) {
            try {
                val updateDto = UpdateRequestStatusDto(newStatus.name, responseMessage)
                val response = apiService.updateRequestStatus(requestId, updateDto)

                if (response.isSuccessful && response.body() != null) {
                    val updatedRequest = convertDtoToModel(response.body()!!)

                    val currentRequests = _myRequestsFlow.value.toMutableList()
                    val index = currentRequests.indexOfFirst { it.id == requestId }
                    if (index >= 0) {
                        currentRequests[index] = updatedRequest
                        _myRequestsFlow.value = currentRequests
                    }

                    Result.success(updatedRequest)
                } else {
                    Result.failure(Exception("Ошибка обновления статуса"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating status", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun cancelRequest(requestId: String, reason: String): Result<AppointmentRequest> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.cancelRequest(requestId, reason)
                if (response.isSuccessful && response.body() != null) {
                    val cancelledRequest = convertDtoToModel(response.body()!!)

                    val currentRequests = _myRequestsFlow.value.toMutableList()
                    val index = currentRequests.indexOfFirst { it.id == requestId }
                    if (index >= 0) {
                        currentRequests[index] = cancelledRequest
                        _myRequestsFlow.value = currentRequests
                    }

                    Result.success(cancelledRequest)
                } else {
                    Result.failure(Exception("Ошибка отмены заявки"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling request", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getMyAssignedRequests(): Result<List<AppointmentRequest>> {
        // TODO: Реализовать, когда будет готов endpoint на бэкенде
        return Result.failure(NotImplementedError("Метод еще не реализован на бэкенде"))
    }

    override fun observeMyRequests(): Flow<List<AppointmentRequest>> {
        return _myRequestsFlow.asStateFlow()
    }

    private fun convertDtoToModel(dto: AppointmentRequestDto): AppointmentRequest {
        return AppointmentRequest(
            id = dto.id,
            patientId = dto.patientId,
            patientName = dto.patientName ?: "",
            patientPhone = dto.patientPhone ?: "",
            address = dto.address,
            requestType = try {
                RequestType.valueOf(dto.requestType)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown request type: ${dto.requestType}, defaulting to REGULAR")
                RequestType.REGULAR
            },
            symptoms = dto.symptoms,
            additionalNotes = dto.additionalNotes ?: "",
            preferredDateTime = dto.preferredDateTime,
            status = try {
                RequestStatus.valueOf(dto.status)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown status: ${dto.status}, defaulting to NEW")
                RequestStatus.NEW
            },
            assignedStaffId = dto.assignedStaffId,
            assignedStaffName = dto.assignedStaffName,
            assignedBy = null,
            assignedAt = dto.assignedAt,
            assignmentNote = dto.assignmentNote,
            responseMessage = dto.responseMessage ?: "",
            createdAt = dto.createdAt ?: Date(),
            updatedAt = dto.updatedAt ?: Date()
        )
    }
}