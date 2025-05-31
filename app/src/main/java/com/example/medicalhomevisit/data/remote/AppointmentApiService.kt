package com.example.medicalhomevisit.data.remote

import com.example.medicalhomevisit.data.remote.dtos.*
import retrofit2.Response
import retrofit2.http.*

interface AppointmentApiService {

    @POST("api/appointment-requests")
    suspend fun createRequest(@Body request: CreateAppointmentRequestDto): Response<AppointmentRequestDto>

    @GET("api/appointment-requests/my")
    suspend fun getMyRequests(): Response<List<AppointmentRequestDto>>

    @GET("api/appointment-requests/{requestId}")
    suspend fun getRequestById(@Path("requestId") requestId: String): Response<AppointmentRequestDto>

    @GET("api/appointment-requests/patient/{patientId}")
    suspend fun getRequestsForPatient(@Path("patientId") patientId: String): Response<List<AppointmentRequestDto>>

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