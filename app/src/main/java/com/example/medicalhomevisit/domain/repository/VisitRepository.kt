package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.domain.model.Visit
import com.example.medicalhomevisit.domain.model.VisitStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface VisitRepository {
    suspend fun getVisitsForStaff(staffId: String): List<Visit>
    suspend fun getVisitsForDate(date: Date): List<Visit>
    suspend fun getVisitById(visitId: String): Visit
    suspend fun updateVisitStatus(visitId: String, newStatus: VisitStatus)
    fun observeVisits(): Flow<List<Visit>>
    suspend fun getCachedVisits(): List<Visit>
}