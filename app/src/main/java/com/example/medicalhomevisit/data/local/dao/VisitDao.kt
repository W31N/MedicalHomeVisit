package com.example.medicalhomevisit.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.medicalhomevisit.data.local.entity.VisitEntity
import java.util.Date

@Dao
interface VisitDao {

    @Query("SELECT * FROM visits WHERE assignedStaffId = :staffId ORDER BY scheduledTime ASC")
    fun getVisitsForStaff(staffId: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE assignedStaffId = :staffId ORDER BY scheduledTime ASC")
    suspend fun getVisitsForStaffSync(staffId: String): List<VisitEntity>

    @Query("SELECT * FROM visits WHERE id = :visitId")
    suspend fun getVisitById(visitId: String): VisitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisits(visits: List<VisitEntity>)

    @Query("SELECT * FROM visits WHERE isSynced = 0 ORDER BY updatedAt ASC")
    suspend fun getUnsyncedVisits(): List<VisitEntity>

    @Query("UPDATE visits SET isSynced = 1, syncAction = null WHERE id = :visitId")
    suspend fun markAsSynced(visitId: String)

    @Query("UPDATE visits SET lastSyncAttempt = :timestamp WHERE id = :visitId")
    suspend fun updateLastSyncAttempt(visitId: String, timestamp: Date)

    suspend fun updateVisitStatus(visitId: String, status: String) {
        val now = Date()
        updateVisitStatusInternal(visitId, status, now)
    }

    @Query("UPDATE visits SET status = :status, isSynced = 0, syncAction = 'UPDATE', updatedAt = :now WHERE id = :visitId")
    suspend fun updateVisitStatusInternal(visitId: String, status: String, now: Date)

    suspend fun updateVisitNotes(visitId: String, notes: String) {
        val now = Date()
        updateVisitNotesInternal(visitId, notes, now)
    }

    @Query("UPDATE visits SET notes = :notes, isSynced = 0, syncAction = 'UPDATE', updatedAt = :now WHERE id = :visitId")
    suspend fun updateVisitNotesInternal(visitId: String, notes: String, now: Date)

    @Query("SELECT COUNT(*) FROM visits WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int
}