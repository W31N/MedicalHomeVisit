package com.example.medicalhomevisit.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.medicalhomevisit.data.local.entity.VisitEntity
import java.util.Date

@Dao
interface VisitDao {

    // 📱 ОСНОВНЫЕ ОПЕРАЦИИ
    @Query("SELECT * FROM visits WHERE assignedStaffId = :staffId ORDER BY scheduledTime ASC")
    fun getVisitsForStaff(staffId: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE assignedStaffId = :staffId ORDER BY scheduledTime ASC")
    suspend fun getVisitsForStaffSync(staffId: String): List<VisitEntity>

    @Query("SELECT * FROM visits WHERE id = :visitId")
    suspend fun getVisitById(visitId: String): VisitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisits(visits: List<VisitEntity>)

    @Update
    suspend fun updateVisit(visit: VisitEntity)

    @Delete
    suspend fun deleteVisit(visit: VisitEntity)

    // 🔄 МЕТОДЫ ДЛЯ СИНХРОНИЗАЦИИ (добавляем недостающие!)
    @Query("SELECT * FROM visits WHERE isSynced = 0 ORDER BY updatedAt ASC")
    suspend fun getUnsyncedVisits(): List<VisitEntity>

    @Query("UPDATE visits SET isSynced = 1, syncAction = null WHERE id = :visitId")
    suspend fun markAsSynced(visitId: String)

    @Query("UPDATE visits SET lastSyncAttempt = :timestamp WHERE id = :visitId")
    suspend fun updateLastSyncAttempt(visitId: String, timestamp: Date)

    @Query("UPDATE visits SET status = :status, isSynced = 0, syncAction = 'UPDATE', updatedAt = :now WHERE id = :visitId")
    suspend fun updateVisitStatus(visitId: String, status: String, now: Date = Date())

    @Query("UPDATE visits SET notes = :notes, isSynced = 0, syncAction = 'UPDATE', updatedAt = :now WHERE id = :visitId")
    suspend fun updateVisitNotes(visitId: String, notes: String, now: Date = Date())

    // 🗑️ УДОБНЫЕ МЕТОДЫ
    @Query("DELETE FROM visits WHERE assignedStaffId = :staffId")
    suspend fun deleteVisitsForStaff(staffId: String)

    @Query("SELECT COUNT(*) FROM visits WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int

    // 🔍 ОТЛАДОЧНЫЕ МЕТОДЫ
    @Query("SELECT COUNT(*) FROM visits")
    suspend fun getTotalVisitsCount(): Int

    @Query("SELECT DISTINCT assignedStaffId FROM visits")
    suspend fun getAllAssignedStaffIds(): List<String>

    @Query("SELECT * FROM visits ORDER BY createdAt DESC LIMIT 5")
    suspend fun getRecentVisits(): List<VisitEntity>

    @Query("""
        SELECT v.*, 
               CASE WHEN v.assignedStaffId = :currentUserId THEN 'ASSIGNED_TO_USER' 
                    ELSE 'NOT_ASSIGNED' END as assignment_status
        FROM visits v 
        ORDER BY scheduledTime ASC
    """)
    suspend fun getVisitsWithAssignmentStatus(currentUserId: String): List<VisitEntity>
}