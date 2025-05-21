package com.example.medicalhomevisit.data.repository

import com.example.medicalhomevisit.data.TestDataManager
import com.example.medicalhomevisit.data.model.Visit
import com.example.medicalhomevisit.data.model.VisitStatus
import com.example.medicalhomevisit.domain.repository.VisitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Date

// Обновленная реализация, использующая TestDataManager
class FakeVisitRepository : VisitRepository {
    override suspend fun getVisitsForToday(): List<Visit> = TestDataManager.getAllVisits()

    override suspend fun getVisitsForDate(date: Date): List<Visit> = TestDataManager.getAllVisits()

    override fun observeVisits(): Flow<List<Visit>> = flowOf(TestDataManager.getAllVisits())

    override suspend fun getVisitById(visitId: String): Visit = TestDataManager.getVisit(visitId)

    override suspend fun getVisitHistoryForPatient(patientId: String): List<Visit> =
        TestDataManager.getAllVisits().filter { it.patientId == patientId }

    override suspend fun updateVisitStatus(visitId: String, newStatus: VisitStatus) {
        TestDataManager.updateVisitStatus(visitId, newStatus)
    }

    override suspend fun addUnplannedVisit(visit: Visit): Visit = visit

    override suspend fun cacheVisits(visits: List<Visit>) {}

    override suspend fun getCachedVisits(): List<Visit> = emptyList()

    override suspend fun syncVisits(): Result<List<Visit>> = Result.success(emptyList())
}