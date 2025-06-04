package com.example.medicalhomevisit.data.repository

import android.util.Log
import com.example.medicalhomevisit.data.local.dao.VisitDao
import com.example.medicalhomevisit.data.local.entity.VisitEntity
import com.example.medicalhomevisit.data.remote.api.VisitApiService
import com.example.medicalhomevisit.data.remote.dto.VisitDto
import com.example.medicalhomevisit.data.remote.dto.VisitStatusUpdateRequest
import com.example.medicalhomevisit.domain.model.Visit
import com.example.medicalhomevisit.domain.model.VisitStatus
import com.example.medicalhomevisit.domain.repository.AuthRepository
import com.example.medicalhomevisit.domain.repository.VisitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleOfflineVisitRepository @Inject constructor(
    private val visitDao: VisitDao,
    private val apiService: VisitApiService,
    private val authRepository: AuthRepository
) : VisitRepository {

    companion object {
        private const val TAG = "SimpleOfflineVisitRepo"
    }

    override suspend fun getVisitsForStaff(staffId: String): List<Visit> {
        Log.d(TAG, "🔍 Getting visits for staff: $staffId")

        // 1. СНАЧАЛА проверяем есть ли данные в Room
        val localVisits = visitDao.getVisitsForStaffSync(staffId)
        Log.d(TAG, "📱 Found ${localVisits.size} visits in Room")

        // 2. Если данных нет - загружаем с сервера
        if (localVisits.isEmpty()) {
            Log.d(TAG, "📡 No local data, loading from server...")
            try {
                val response = apiService.getMyVisits()
                if (response.isSuccessful) {
                    val visitDtos = response.body() ?: emptyList()
                    Log.d(TAG, "✅ Server returned ${visitDtos.size} visits")

                    // Сохраняем в Room
                    val entities = visitDtos.map { dto -> convertDtoToEntity(dto) }
                    visitDao.insertVisits(entities)
                    Log.d(TAG, "💾 Saved ${entities.size} visits to Room")

                    return entities.map { convertEntityToDomain(it) }
                } else {
                    Log.e(TAG, "❌ Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Network error: ${e.message}")
            }
        }

        // 3. Возвращаем данные из Room
        val finalVisits = visitDao.getVisitsForStaffSync(staffId)
        Log.d(TAG, "📋 Returning ${finalVisits.size} visits from Room")
        return finalVisits.map { convertEntityToDomain(it) }
    }

    override suspend fun updateVisitStatus(visitId: String, newStatus: VisitStatus) {
        Log.d(TAG, "🔄 Updating visit status: $visitId -> $newStatus")

        // 1. СНАЧАЛА сохраняем в Room
        visitDao.updateVisitStatus(visitId, newStatus.name)
        Log.d(TAG, "✅ Updated status in Room")

        // 2. ПОТОМ пытаемся отправить на сервер
        try {
            val request = VisitStatusUpdateRequest(newStatus.name)
            val response = apiService.updateVisitStatus(visitId, request)

            if (response.isSuccessful) {
                Log.d(TAG, "✅ Synced status to server")
                // TODO: пометить как синхронизированный
            } else {
                Log.w(TAG, "❌ Server sync failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "❌ Network sync failed: ${e.message}")
            // Данные остаются в Room, попробуем позже
        }
    }

    override fun observeVisits(): Flow<List<Visit>> {
        return authRepository.currentUser.flatMapLatest { user ->
            val userId = user?.id ?: ""
            if (userId.isNotEmpty()) {
                visitDao.getVisitsForStaff(userId)
                    .map { entities -> entities.map { convertEntityToDomain(it) } }
            } else {
                flowOf(emptyList())
            }
        }
    }

    // 🔄 МАППЕРЫ
    private fun convertDtoToEntity(dto: VisitDto): VisitEntity {
        return VisitEntity(
            id = dto.id,
            patientId = dto.patientId,
            scheduledTime = dto.scheduledTime,
            status = dto.status,
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
            originalRequestId = null,
            isSynced = true, // Данные с сервера считаются синхронизированными
            syncAction = null
        )
    }

    private fun convertEntityToDomain(entity: VisitEntity): Visit {
        return Visit(
            id = entity.id,
            patientId = entity.patientId,
            scheduledTime = entity.scheduledTime,
            status = try {
                VisitStatus.valueOf(entity.status)
            } catch (e: Exception) {
                VisitStatus.PLANNED
            },
            address = entity.address,
            reasonForVisit = entity.reasonForVisit,
            notes = entity.notes,
            assignedStaffId = entity.assignedStaffId,
            assignedStaffName = entity.assignedStaffName,
            actualStartTime = entity.actualStartTime,
            actualEndTime = entity.actualEndTime,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isFromRequest = entity.isFromRequest,
            originalRequestId = entity.originalRequestId
        )
    }

    private fun getCurrentUserId(): String {
        // Flow не имеет .value, поэтому используем другой подход
        // Можно получить из savedStateHandle или передавать параметром
        return "current-user-id" // Временная заглушка для тестирования
    }

    // Альтернативный вариант - получать userId как параметр
    private suspend fun getCurrentUserIdSafe(): String {
        return try {
            // Если нужно получить из Flow - используем first()
            // authRepository.currentUser.first()?.id ?: ""
            "current-user-id" // Пока заглушка
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get current user ID: ${e.message}")
            ""
        }
    }

    // Остальные методы интерфейса - пока оставляем как есть
    override suspend fun getVisitsForToday(): List<Visit> {
        return getVisitsForStaff(getCurrentUserId())
    }

    override suspend fun getVisitsForDate(date: Date): List<Visit> {
        return getVisitsForStaff(getCurrentUserId()) // Упрощенно
    }

    override suspend fun getVisitById(visitId: String): Visit {
        val entity = visitDao.getVisitById(visitId)
        return entity?.let { convertEntityToDomain(it) }
            ?: throw Exception("Visit not found")
    }

    override suspend fun updateVisitNotes(visitId: String, notes: String) {
        // TODO: реализуем позже
    }

    override suspend fun updateScheduledTime(visitId: String, scheduledTime: Date) {
        // TODO: реализуем позже
    }

    override suspend fun updateVisit(visit: Visit): Visit {
        // TODO: реализуем позже
        return visit
    }

    override suspend fun addUnplannedVisit(visit: Visit): Visit {
        // TODO: реализуем позже
        return visit
    }

    override suspend fun getVisitHistoryForPatient(patientId: String): List<Visit> {
        return emptyList() // TODO: реализуем позже
    }

    override suspend fun cacheVisits(visits: List<Visit>) {
        // TODO: реализуем позже
    }

    override suspend fun getCachedVisits(): List<Visit> {
        return emptyList() // TODO: реализуем позже
    }

    override suspend fun syncVisits(): Result<List<Visit>> {
        return Result.success(emptyList()) // TODO: реализуем позже
    }
}