package com.example.medicalhomevisit.data.remote

import com.example.medicalhomevisit.data.remote.dtos.VisitDto
import com.example.medicalhomevisit.data.remote.dtos.VisitNotesUpdateRequest
import com.example.medicalhomevisit.data.remote.dtos.VisitStatusUpdateRequest
import retrofit2.Response
import retrofit2.http.*

interface VisitApiService {

    /**
     * Получить все визиты для текущего медработника
     */
    @GET("api/visits/my")
    suspend fun getMyVisits(): Response<List<VisitDto>>

    /**
     * Получить визиты на сегодня для текущего медработника
     */
    @GET("api/visits/my/today")
    suspend fun getMyVisitsForToday(): Response<List<VisitDto>>

    /**
     * Получить визиты на конкретную дату для текущего медработника
     * Формат даты: 2024-01-15
     */
    @GET("api/visits/my/date/{date}")
    suspend fun getMyVisitsForDate(@Path("date") date: String): Response<List<VisitDto>>

    /**
     * Получить визит по ID
     */
    @GET("api/visits/{visitId}")
    suspend fun getVisitById(@Path("visitId") visitId: String): Response<VisitDto>

    /**
     * Обновить статус визита
     */
    @PUT("api/visits/{visitId}/status")
    suspend fun updateVisitStatus(
        @Path("visitId") visitId: String,
        @Body statusUpdate: VisitStatusUpdateRequest
    ): Response<VisitDto>

    /**
     * Обновить заметки к визиту
     */
    @PUT("api/visits/{visitId}/notes")
    suspend fun updateVisitNotes(
        @Path("visitId") visitId: String,
        @Body notesUpdate: VisitNotesUpdateRequest
    ): Response<VisitDto>

    /**
     * Начать визит
     */
    @POST("api/visits/{visitId}/start")
    suspend fun startVisit(@Path("visitId") visitId: String): Response<VisitDto>

    /**
     * Завершить визит
     */
    @POST("api/visits/{visitId}/complete")
    suspend fun completeVisit(@Path("visitId") visitId: String): Response<VisitDto>
}
