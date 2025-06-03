package com.example.medicalhomevisit.data.remote

import com.example.medicalhomevisit.data.model.Visit
import com.example.medicalhomevisit.data.model.VisitStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface VisitRepository {

    /**
     * Получить все визиты для конкретного медработника
     */
    suspend fun getVisitsForStaff(staffId: String): List<Visit>

    /**
     * Получить визиты на сегодня
     */
    suspend fun getVisitsForToday(): List<Visit>

    /**
     * Получить визиты на конкретную дату
     */
    suspend fun getVisitsForDate(date: Date): List<Visit>

    /**
     * Получить визит по ID
     */
    suspend fun getVisitById(visitId: String): Visit

    /**
     * Получить историю визитов для пациента
     */
    suspend fun getVisitHistoryForPatient(patientId: String): List<Visit>

    /**
     * Обновить статус визита
     */
    suspend fun updateVisitStatus(visitId: String, newStatus: VisitStatus)

    /**
     * Обновить заметки к визиту
     */
    suspend fun updateVisitNotes(visitId: String, notes: String)

    /**
     * Обновить запланированное время визита
     */
    suspend fun updateScheduledTime(visitId: String, scheduledTime: Date)

    /**
     * Обновить весь визит
     */
    suspend fun updateVisit(visit: Visit): Visit

    /**
     * Добавить внеплановый визит
     */
    suspend fun addUnplannedVisit(visit: Visit): Visit

    /**
     * Наблюдение за изменениями визитов (для real-time обновлений)
     */
    fun observeVisits(): Flow<List<Visit>>

    /**
     * Кэширование для офлайн режима
     */
    suspend fun cacheVisits(visits: List<Visit>)
    suspend fun getCachedVisits(): List<Visit>
    suspend fun syncVisits(): Result<List<Visit>>
}
