
package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.data.model.Visit
import com.example.medicalhomevisit.data.model.VisitStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface VisitRepository {
    suspend fun getVisitsForToday(): List<Visit>
    suspend fun getVisitsForStaff(staffId: String): List<Visit> // Добавляем этот метод
    suspend fun getVisitsForDate(date: Date): List<Visit>
    fun observeVisits(): Flow<List<Visit>>
    suspend fun getVisitById(visitId: String): Visit
    suspend fun getVisitHistoryForPatient(patientId: String): List<Visit>
    suspend fun updateVisitStatus(visitId: String, newStatus: VisitStatus)
    suspend fun addUnplannedVisit(visit: Visit): Visit
    suspend fun cacheVisits(visits: List<Visit>)
    suspend fun getCachedVisits(): List<Visit>
    suspend fun syncVisits(): Result<List<Visit>>

    // Добавляем недостающие методы
    suspend fun updateVisitNotes(visitId: String, notes: String)
    suspend fun updateScheduledTime(visitId: String, scheduledTime: Date)
    suspend fun updateVisit(visit: Visit): Visit
}