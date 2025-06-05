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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class SimpleOfflineVisitRepository @Inject constructor(
    private val visitDao: VisitDao,
    private val apiService: VisitApiService,
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager,
) : VisitRepository {

    companion object {
        private const val TAG = "OfflineVisitRepo"
    }

    private suspend fun getCurrentMedicalPersonId(): String? {
        return try {
            authRepository.currentUser.first()?.medicalPersonId
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get current user's medicalPersonId: ${e.message}")
            null
        }
    }

    override fun observeVisits(): Flow<List<Visit>> {
        return authRepository.currentUser.flatMapLatest { user ->
            val medicalId = user?.medicalPersonId
            if (!medicalId.isNullOrEmpty()) {
                Log.d(TAG, "Observing visits for medicalPersonId: $medicalId (User ID: ${user.id})")
                visitDao.getVisitsForStaff(medicalId)
                    .map { entities -> entities.map { convertEntityToDomain(it) } }
            } else {
                Log.d(TAG, "No medicalPersonId for user ${user?.id}, returning empty flow for observeVisits")
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

    suspend fun getUnsyncedVisits(): List<Visit> {
        return try {
            val entities = visitDao.getUnsyncedVisits()
            entities.map { convertEntityToDomain(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting unsynced visits: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getVisitsForStaff(staffId: String): List<Visit> {
        val medicalIdToQuery = getCurrentMedicalPersonId()

        if (medicalIdToQuery.isNullOrEmpty()) {
            Log.w(TAG, "No medicalPersonId for current user (called with staffId: $staffId). Returning empty list.")
            tryLoadFromServer()
            return emptyList()
        }

        Log.d(TAG, "Getting visits for medicalPersonId: $medicalIdToQuery (requested by user with UserEntity.id: $staffId)")

        val localVisits = visitDao.getVisitsForStaffSync(medicalIdToQuery)
        Log.d(TAG, "Found ${localVisits.size} visits in Room for medicalPersonId: $medicalIdToQuery")

        tryLoadFromServer()

        return localVisits.map { convertEntityToDomain(it) }
    }

    override suspend fun getVisitsForDate(date: Date): List<Visit> {
        val medicalIdToQuery = getCurrentMedicalPersonId()

        if (medicalIdToQuery.isNullOrEmpty()) {
            Log.w(TAG, "No medicalPersonId for current user. Cannot effectively filter visits for date by staff from Room.")
            tryLoadFromServerForDate(date)
            return emptyList()
        }
        Log.d(TAG, "Getting visits for date $date for medicalPersonId: $medicalIdToQuery")

        tryLoadFromServerForDate(date)

        val localVisits = visitDao.getVisitsForStaffSync(medicalIdToQuery)
        Log.d(TAG, "Returning ${localVisits.size} visits from Room for medicalPersonId: $medicalIdToQuery (date filtering in ViewModel)")

        return localVisits.map { convertEntityToDomain(it) }
    }

    override suspend fun updateVisitStatus(visitId: String, newStatus: VisitStatus) {
        Log.d(TAG, "Updating visit status: $visitId -> $newStatus")
        visitDao.updateVisitStatus(visitId, newStatus.name)
        Log.d(TAG, "Updated status in Room, marked as unsynced")
        syncManager.syncNow()
    }

    override suspend fun updateVisitNotes(visitId: String, notes: String) {
        Log.d(TAG, "Updating visit notes: $visitId")
        visitDao.updateVisitNotes(visitId, notes)
        Log.d(TAG, "Updated notes in Room, marked as unsynced")
        syncManager.syncNow()
    }

    override suspend fun getVisitById(visitId: String): Visit {
        val entity = visitDao.getVisitById(visitId)
        return entity?.let { convertEntityToDomain(it) }
            ?: throw Exception("Visit not found for id: $visitId")
    }

    private suspend fun tryLoadFromServer() {
        try {
            Log.d(TAG, "Trying to load from server (getMyVisits)...")
            val response = apiService.getMyVisits()

            if (response.isSuccessful) {
                val visitDtos = response.body() ?: emptyList()
                Log.d(TAG, "Server returned ${visitDtos.size} visits")
                val entities = visitDtos.map { dto ->
                    convertDtoToEntity(dto, isSynced = true)
                }
                visitDao.insertVisits(entities)
                Log.d(TAG, "Saved ${entities.size} visits to Room")
            } else {
                Log.w(TAG, "Server error on getMyVisits: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network error on getMyVisits: ${e.message}")
        }
    }

    private suspend fun tryLoadFromServerForDate(date: Date) {
        try {
            Log.d(TAG, "Trying to load from server for date $date...")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateString = dateFormat.format(date)
            val response = apiService.getMyVisitsForDate(dateString)

            if (response.isSuccessful) {
                val visitDtos = response.body() ?: emptyList()
                Log.d(TAG, "Server returned ${visitDtos.size} visits for date $dateString")
                val entities = visitDtos.map { dto ->
                    convertDtoToEntity(dto, isSynced = true)
                }
                visitDao.insertVisits(entities)
                Log.d(TAG, "Saved ${entities.size} visits to Room for date $dateString")
            } else {
                Log.w(TAG, "Server error for date $dateString: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network error for date $date: ${e.message}")
        }
    }

    override suspend fun syncVisits(): Result<List<Visit>> {
        return try {
            Log.d(TAG, "Manual sync requested by user")
            syncManager.syncNow()

            val medicalId = getCurrentMedicalPersonId()
            if (!medicalId.isNullOrEmpty()) {
                val localVisits = visitDao.getVisitsForStaffSync(medicalId)
                Result.success(localVisits.map { convertEntityToDomain(it) })
            } else {
                Log.w(TAG, "Cannot fetch visits for sync: medicalPersonId is null")
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Manual sync trigger failed: ${e.message}")
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
        val medicalId = getCurrentMedicalPersonId()
        if (!medicalId.isNullOrEmpty()) {
            val entities = visitDao.getVisitsForStaffSync(medicalId)
            return entities.map { convertEntityToDomain(it) }
        }
        return emptyList()
    }

    override suspend fun getVisitsForToday(): List<Visit> {
        return getVisitsForDate(Date())
    }

    override suspend fun updateScheduledTime(visitId: String, scheduledTime: Date) {
        Log.w(TAG, "updateScheduledTime not implemented yet")
    }

    override suspend fun updateVisit(visit: Visit): Visit {
        Log.w(TAG, "updateVisit not implemented yet")
        return visit
    }

    override suspend fun addUnplannedVisit(visit: Visit): Visit {
        Log.w(TAG, "addUnplannedVisit not implemented yet")
        return visit
    }

    override suspend fun getVisitHistoryForPatient(patientId: String): List<Visit> {
        Log.w(TAG, "getVisitHistoryForPatient not implemented yet")
        return emptyList()
    }

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