package com.example.medicalhomevisit.data.remote.api

import com.example.medicalhomevisit.data.remote.dto.VisitDto
import com.example.medicalhomevisit.data.remote.dto.VisitNotesUpdateRequest
import com.example.medicalhomevisit.data.remote.dto.VisitStatusUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface VisitApiService {
    @GET("api/visits/my")
    suspend fun getMyVisits(): Response<List<VisitDto>>

    @GET("api/visits/my/today")
    suspend fun getMyVisitsForToday(): Response<List<VisitDto>>

    @GET("api/visits/my/date/{date}")
    suspend fun getMyVisitsForDate(@Path("date") date: String): Response<List<VisitDto>>

    @GET("api/visits/{visitId}")
    suspend fun getVisitById(@Path("visitId") visitId: String): Response<VisitDto>

    @PUT("api/visits/{visitId}/status")
    suspend fun updateVisitStatus(
        @Path("visitId") visitId: String,
        @Body statusUpdate: VisitStatusUpdateRequest
    ): Response<VisitDto>

    @PUT("api/visits/{visitId}/notes")
    suspend fun updateVisitNotes(
        @Path("visitId") visitId: String,
        @Body notesUpdate: VisitNotesUpdateRequest
    ): Response<VisitDto>
}