package com.example.medicalhomevisit.data.remote.repository

import android.util.Log
import com.example.medicalhomevisit.data.remote.api.VisitApiService
import com.example.medicalhomevisit.domain.model.Visit
import com.example.medicalhomevisit.domain.model.VisitStatus
import com.example.medicalhomevisit.data.remote.dto.VisitDto
import com.example.medicalhomevisit.data.remote.dto.VisitStatusUpdateRequest
import com.example.medicalhomevisit.domain.repository.VisitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisitRepositoryImpl @Inject constructor(
    private val apiService: VisitApiService
) : VisitRepository {

    companion object {
        private const val TAG = "HttpVisitRepository"
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    private val _cachedVisits = MutableStateFlow<List<Visit>>(emptyList())
    private val _visitsFlow = MutableStateFlow<List<Visit>>(emptyList())

    override suspend fun getVisitsForStaff(staffId: String): List<Visit> {
        return try {
            Log.d(TAG, "Getting visits for staff: $staffId")
            val response = apiService.getMyVisits()

            if (response.isSuccessful) {
                val visitDtos = response.body() ?: emptyList()
                val visits = visitDtos.map { convertDtoToVisit(it) }

                _cachedVisits.value = visits
                _visitsFlow.value = visits

                Log.d(TAG, "Successfully loaded ${visits.size} visits")
                visits
            } else {
                Log.e(TAG, "Failed to load visits: ${response.code()} ${response.message()}")
                throw Exception("Ошибка загрузки визитов: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading visits for staff", e)
            throw e
        }
    }

    override suspend fun getVisitsForDate(date: Date): List<Visit> {
        return try {
            val dateString = dateFormatter.format(date)
            Log.d(TAG, "Getting visits for date: $dateString")

            val response = apiService.getMyVisitsForDate(dateString)

            if (response.isSuccessful) {
                val visitDtos = response.body() ?: emptyList()
                val visits = visitDtos.map { convertDtoToVisit(it) }

                Log.d(TAG, "Successfully loaded ${visits.size} visits for date $dateString")
                visits
            } else {
                Log.e(TAG, "Failed to load visits for date: ${response.code()} ${response.message()}")
                throw Exception("Ошибка загрузки визитов на дату: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading visits for date", e)
            throw e
        }
    }

    override suspend fun getVisitById(visitId: String): Visit {
        return try {
            Log.d(TAG, "Getting visit by ID: $visitId")
            val response = apiService.getVisitById(visitId)

            if (response.isSuccessful) {
                val visitDto = response.body() ?: throw Exception("Визит не найден")
                val visit = convertDtoToVisit(visitDto)

                Log.d(TAG, "Successfully loaded visit: $visitId")
                visit
            } else {
                Log.e(TAG, "Failed to load visit: ${response.code()} ${response.message()}")
                throw Exception("Ошибка загрузки визита: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading visit by ID", e)
            throw e
        }
    }

    override suspend fun updateVisitStatus(visitId: String, newStatus: VisitStatus) {
        try {
            Log.d(TAG, "Updating visit status: $visitId -> $newStatus")
            val request = VisitStatusUpdateRequest(newStatus.name)
            val response = apiService.updateVisitStatus(visitId, request)

            if (response.isSuccessful) {
                Log.d(TAG, "Successfully updated visit status")

                updateVisitInCache(visitId) { visit ->
                    visit.copy(status = newStatus)
                }
            } else {
                Log.e(TAG, "Failed to update visit status: ${response.code()} ${response.message()}")
                throw Exception("Ошибка обновления статуса визита: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating visit status", e)
            throw e
        }
    }

    override fun observeVisits(): Flow<List<Visit>> {
        return _visitsFlow.asStateFlow()
    }

    override suspend fun getCachedVisits(): List<Visit> {
        return _cachedVisits.value
    }

    private fun updateVisitInCache(visitId: String, updateFn: (Visit) -> Visit) {
        val currentVisits = _cachedVisits.value.toMutableList()
        val index = currentVisits.indexOfFirst { it.id == visitId }

        if (index != -1) {
            currentVisits[index] = updateFn(currentVisits[index])
            _cachedVisits.value = currentVisits
            _visitsFlow.value = currentVisits
        }
    }

    private fun convertDtoToVisit(dto: VisitDto): Visit {
        return Visit(
            id = dto.id,
            patientId = dto.patientId,
            scheduledTime = dto.scheduledTime,
            status = try {
                VisitStatus.valueOf(dto.status)
            } catch (e: Exception) {
                VisitStatus.PLANNED
            },
            address = dto.address,
            reasonForVisit = dto.reasonForVisit,
            notes = dto.notes ?: "",
            assignedStaffId = dto.assignedStaffId,
            assignedStaffName = dto.assignedStaffName,
            actualStartTime = dto.actualStartTime,
            actualEndTime = dto.actualEndTime,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            isFromRequest = true,
            originalRequestId = null
        )
    }
}