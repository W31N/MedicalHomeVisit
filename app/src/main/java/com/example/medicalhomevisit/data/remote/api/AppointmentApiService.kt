package com.example.medicalhomevisit.data.remote.api

import com.example.medicalhomevisit.data.remote.dto.AppointmentRequestDto
import com.example.medicalhomevisit.data.remote.dto.AssignStaffToRequestDto
import com.example.medicalhomevisit.data.remote.dto.CreateAppointmentRequestDto
import com.example.medicalhomevisit.data.remote.dto.UpdateRequestStatusDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AppointmentApiService {
    @POST("api/appointment-requests")
    suspend fun createRequest(@Body request: CreateAppointmentRequestDto): Response<AppointmentRequestDto>

    @GET("api/appointment-requests/my")
    suspend fun getMyRequests(): Response<List<AppointmentRequestDto>>

    @GET("api/appointment-requests/{requestId}")
    suspend fun getRequestById(@Path("requestId") requestId: String): Response<AppointmentRequestDto>

    @GET("api/appointment-requests/active")
    suspend fun getAllActiveRequests(): Response<List<AppointmentRequestDto>>

    @PUT("api/appointment-requests/{requestId}/assign")
    suspend fun assignRequest(
        @Path("requestId") requestId: String,
        @Body request: AssignStaffToRequestDto
    ): Response<AppointmentRequestDto>

    @PUT("api/appointment-requests/{requestId}/status")
    suspend fun updateRequestStatus(
        @Path("requestId") requestId: String,
        @Body request: UpdateRequestStatusDto
    ): Response<AppointmentRequestDto>

    @PUT("api/appointment-requests/{requestId}/cancel")
    suspend fun cancelRequest(
        @Path("requestId") requestId: String,
        @Query("reason") reason: String
    ): Response<AppointmentRequestDto>
}