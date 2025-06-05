package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.domain.model.Visit
import com.example.medicalhomevisit.domain.model.VisitStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface VisitRepository {
    suspend fun getVisitsForStaff(staffId: String): List<Visit>
    suspend fun getVisitsForToday(): List<Visit>
    suspend fun getVisitsForDate(date: Date): List<Visit>
    suspend fun getVisitById(visitId: String): Visit
    suspend fun getVisitHistoryForPatient(patientId: String): List<Visit>
    suspend fun updateVisitStatus(visitId: String, newStatus: VisitStatus)
    suspend fun updateVisitNotes(visitId: String, notes: String)
    suspend fun updateScheduledTime(visitId: String, scheduledTime: Date)
    suspend fun updateVisit(visit: Visit): Visit
    suspend fun addUnplannedVisit(visit: Visit): Visit
    fun observeVisits(): Flow<List<Visit>>
    suspend fun cacheVisits(visits: List<Visit>)
    suspend fun getCachedVisits(): List<Visit>
    suspend fun syncVisits(): Result<List<Visit>>
}