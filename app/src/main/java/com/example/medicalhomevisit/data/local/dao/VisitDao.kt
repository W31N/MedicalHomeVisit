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

    // 🗑️ УДОБНЫЕ МЕТОДЫ
    @Query("DELETE FROM visits WHERE assignedStaffId = :staffId")
    suspend fun deleteVisitsForStaff(staffId: String)

    @Query("UPDATE visits SET status = :status, isSynced = 0, syncAction = 'UPDATE', updatedAt = :now WHERE id = :visitId")
    suspend fun updateVisitStatus(visitId: String, status: String, now: Date = Date())
}