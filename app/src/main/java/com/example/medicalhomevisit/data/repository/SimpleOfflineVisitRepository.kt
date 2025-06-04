package com.example.medicalhomevisit.data.repository

import android.util.Log
import com.example.medicalhomevisit.data.local.dao.VisitDao
import com.example.medicalhomevisit.data.local.entity.VisitEntity
import com.example.medicalhomevisit.data.remote.api.VisitApiService
import com.example.medicalhomevisit.data.remote.dto.VisitDto
import com.example.medicalhomevisit.data.sync.SyncManager
import com.example.medicalhomevisit.domain.model.Visit
import com.example.medicalhomevisit.domain.model.VisitStatus
import com.example.medicalhomevisit.domain.repository.AuthRepository
import com.example.medicalhomevisit.domain.repository.VisitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager
) : VisitRepository {

    companion object {
        private const val TAG = "OfflineVisitRepo"
    }

    // 🔄 OFFLINE-FIRST: Всегда возвращаем данные из Room
    override fun observeVisits(): Flow<List<Visit>> {
        return authRepository.currentUser.flatMapLatest { user ->
            val userId = user?.id ?: ""
            if (userId.isNotEmpty()) {
                Log.d(TAG, "🔄 Observing visits for user: $userId")
                visitDao.getVisitsForStaff(userId)
                    .map { entities -> entities.map { convertEntityToDomain(it) } }
            } else {
                Log.d(TAG, "🔄 No user, returning empty flow")
                flowOf(emptyList())
            }
        }
    }

    suspend fun getUnsyncedCount(): Int {
        return try {
            visitDao.getUnsyncedCount()
        } catch (e: Exception) {
            Log.w(TAG, "Error getting unsynced count: ${e.message}")
            0
        }
    }

    // 🔄 Получение всех несинхронизированных записей (для отладки)
    suspend fun getUnsyncedVisits(): List<Visit> {
        return try {
            val entities = visitDao.getUnsyncedVisits()
            entities.map { convertEntityToDomain(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting unsynced visits: ${e.message}")
            emptyList()
        }
    }

    // 🔄 Принудительная синхронизация конкретного визита
    suspend fun syncVisit(visitId: String): Result<Unit> {
        return try {
            val entity = visitDao.getVisitById(visitId)
            if (entity != null && !entity.isSynced) {
                when (entity.syncAction) {
                    "UPDATE" -> {
                        // Пытаемся синхронизировать статус
                        val request = com.example.medicalhomevisit.data.remote.dto.VisitStatusUpdateRequest(entity.status)
                        val response = apiService.updateVisitStatus(entity.id, request)

                        if (response.isSuccessful) {
                            visitDao.markAsSynced(entity.id)
                            Log.d(TAG, "✅ Visit $visitId synced successfully")
                            Result.success(Unit)
                        } else {
                            visitDao.updateLastSyncAttempt(entity.id, Date())
                            Result.failure(Exception("Server error: ${response.code()}"))
                        }
                    }
                    else -> {
                        Result.failure(Exception("Unsupported sync action: ${entity.syncAction}"))
                    }
                }
            } else {
                Result.success(Unit) // Уже синхронизировано
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing visit $visitId: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 🔄 OFFLINE-FIRST: Загружаем из Room, обновляем с сервера в фоне
    override suspend fun getVisitsForStaff(staffId: String): List<Visit> {
        val actualStaffId = staffId.ifEmpty { getCurrentUserId() }
        Log.d(TAG, "🔍 Getting visits for staff: $actualStaffId")

        // 1. ВСЕГДА сначала возвращаем данные из Room
        val localVisits = visitDao.getVisitsForStaffSync(actualStaffId)
        Log.d(TAG, "📱 Found ${localVisits.size} visits in Room")

        // 2. В фоне пытаемся обновить с сервера
        tryLoadFromServer(actualStaffId)

        // 3. Возвращаем локальные данные (могут быть пустые, но это OK)
        return localVisits.map { convertEntityToDomain(it) }
    }

    // 🔄 OFFLINE-FIRST: Загружаем для даты
    override suspend fun getVisitsForDate(date: Date): List<Visit> {
        val currentUserId = getCurrentUserId()
        Log.d(TAG, "🔍 Getting visits for date for user: $currentUserId")

        // 1. Сначала пытаемся загрузить с сервера для конкретной даты
        tryLoadFromServerForDate(date, currentUserId)

        // 2. Возвращаем все локальные данные (фильтрация по дате будет в ViewModel)
        val localVisits = visitDao.getVisitsForStaffSync(currentUserId)
        Log.d(TAG, "📱 Returning ${localVisits.size} visits from Room")

        return localVisits.map { convertEntityToDomain(it) }
    }

    // 🔄 OFFLINE-FIRST: Обновление статуса
    override suspend fun updateVisitStatus(visitId: String, newStatus: VisitStatus) {
        Log.d(TAG, "🔄 Updating visit status: $visitId -> $newStatus")

        // 1. СНАЧАЛА сохраняем в Room (всегда работает)
        visitDao.updateVisitStatus(visitId, newStatus.name)
        Log.d(TAG, "✅ Updated status in Room, marked as unsynced")

        // 2. Запускаем синхронизацию в фоне
        syncManager.syncNow()
    }

    // 🔄 OFFLINE-FIRST: Обновление заметок
    override suspend fun updateVisitNotes(visitId: String, notes: String) {
        Log.d(TAG, "🔄 Updating visit notes: $visitId")

        // 1. СНАЧАЛА сохраняем в Room
        visitDao.updateVisitNotes(visitId, notes)
        Log.d(TAG, "✅ Updated notes in Room, marked as unsynced")

        // 2. Запускаем синхронизацию в фоне
        syncManager.syncNow()
    }

    // 🔄 OFFLINE-FIRST: Получение визита по ID
    override suspend fun getVisitById(visitId: String): Visit {
        val entity = visitDao.getVisitById(visitId)
        return entity?.let { convertEntityToDomain(it) }
            ?: throw Exception("Visit not found")
    }

    // 📡 ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ РАБОТЫ С СЕРВЕРОМ

    private suspend fun tryLoadFromServer(staffId: String) {
        try {
            Log.d(TAG, "📡 Trying to load from server...")
            val response = apiService.getMyVisits()

            if (response.isSuccessful) {
                val visitDtos = response.body() ?: emptyList()
                Log.d(TAG, "✅ Server returned ${visitDtos.size} visits")

                // Конвертируем и сохраняем в Room
                val entities = visitDtos.map { dto ->
                    convertDtoToEntity(dto, isSynced = true)
                }
                visitDao.insertVisits(entities)
                Log.d(TAG, "💾 Saved ${entities.size} visits to Room")
            } else {
                Log.w(TAG, "❌ Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "❌ Network error: ${e.message}")
            // В офлайн режиме это нормально - просто продолжаем работу с Room
        }
    }

    private suspend fun tryLoadFromServerForDate(date: Date, userId: String) {
        try {
            Log.d(TAG, "📡 Trying to load from server for date...")
            val dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(date)
            val response = apiService.getMyVisitsForDate(dateString)

            if (response.isSuccessful) {
                val visitDtos = response.body() ?: emptyList()
                Log.d(TAG, "✅ Server returned ${visitDtos.size} visits for date")

                val entities = visitDtos.map { dto ->
                    convertDtoToEntity(dto, isSynced = true)
                }
                visitDao.insertVisits(entities)
                Log.d(TAG, "💾 Saved ${entities.size} visits to Room")
            } else {
                Log.w(TAG, "❌ Server error for date: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "❌ Network error for date: ${e.message}")
        }
    }

    private suspend fun getCurrentUserId(): String {
        return try {
            authRepository.currentUser.first()?.id ?: ""
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get current user ID: ${e.message}")
            ""
        }
    }

    // 🔄 МЕТОДЫ ДЛЯ СИНХРОНИЗАЦИИ И КЕШИРОВАНИЯ

    override suspend fun syncVisits(): Result<List<Visit>> {
        return try {
            Log.d(TAG, "🔄 Manual sync requested")
            syncManager.syncNow()

            // Возвращаем текущие данные из Room
            val currentUserId = getCurrentUserId()
            val localVisits = visitDao.getVisitsForStaffSync(currentUserId)
            Result.success(localVisits.map { convertEntityToDomain(it) })
        } catch (e: Exception) {
            Log.e(TAG, "❌ Manual sync failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun cacheVisits(visits: List<Visit>) {
        val entities = visits.map { visit ->
            convertDomainToEntity(visit, isSynced = true)
        }
        visitDao.insertVisits(entities)
        Log.d(TAG, "💾 Cached ${entities.size} visits")
    }

    override suspend fun getCachedVisits(): List<Visit> {
        val currentUserId = getCurrentUserId()
        val entities = visitDao.getVisitsForStaffSync(currentUserId)
        return entities.map { convertEntityToDomain(it) }
    }

    // 📊 МЕТОДЫ НЕ ТРЕБУЮЩИЕ ОФЛАЙН РЕЖИМА (пока заглушки)

    override suspend fun getVisitsForToday(): List<Visit> {
        return getVisitsForDate(Date())
    }

    override suspend fun updateScheduledTime(visitId: String, scheduledTime: Date) {
        // TODO: Реализовать с offline поддержкой
        Log.w(TAG, "updateScheduledTime not implemented yet")
    }

    override suspend fun updateVisit(visit: Visit): Visit {
        // TODO: Реализовать с offline поддержкой
        Log.w(TAG, "updateVisit not implemented yet")
        return visit
    }

    override suspend fun addUnplannedVisit(visit: Visit): Visit {
        // TODO: Реализовать с offline поддержкой
        Log.w(TAG, "addUnplannedVisit not implemented yet")
        return visit
    }

    override suspend fun getVisitHistoryForPatient(patientId: String): List<Visit> {
        // TODO: Реализовать с offline поддержкой
        Log.w(TAG, "getVisitHistoryForPatient not implemented yet")
        return emptyList()
    }

    // 🔄 КОНВЕРТЕРЫ

    private fun convertDtoToEntity(dto: VisitDto, isSynced: Boolean = false): VisitEntity {
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
            isSynced = isSynced,
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

    private fun convertDomainToEntity(visit: Visit, isSynced: Boolean): VisitEntity {
        return VisitEntity(
            id = visit.id,
            patientId = visit.patientId,
            scheduledTime = visit.scheduledTime,
            status = visit.status.name,
            address = visit.address,
            reasonForVisit = visit.reasonForVisit,
            notes = visit.notes,
            assignedStaffId = visit.assignedStaffId,
            assignedStaffName = visit.assignedStaffName,
            actualStartTime = visit.actualStartTime,
            actualEndTime = visit.actualEndTime,
            createdAt = visit.createdAt,
            updatedAt = visit.updatedAt,
            isFromRequest = visit.isFromRequest,
            originalRequestId = visit.originalRequestId,
            isSynced = isSynced,
            syncAction = null
        )
    }
}