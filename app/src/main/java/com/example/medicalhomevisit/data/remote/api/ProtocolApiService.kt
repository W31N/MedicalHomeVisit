package com.example.medicalhomevisit.data.remote.api

import com.example.medicalhomevisit.data.remote.dto.ApplyTemplateRequest
import com.example.medicalhomevisit.data.remote.dto.ProtocolTemplateDto
import com.example.medicalhomevisit.data.remote.dto.VisitProtocolDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProtocolApiService {
    @GET("api/protocols/visit/{visitId}")
    suspend fun getProtocolForVisit(@Path("visitId") visitId: String): Response<VisitProtocolDto>

    @POST("api/protocols/visit/{visitId}")
    suspend fun createProtocolForVisit(
        @Path("visitId") visitId: String,
        @Body protocol: VisitProtocolDto
    ): Response<VisitProtocolDto>

    @PUT("api/protocols/visit/{visitId}")
    suspend fun updateProtocolForVisit(
        @Path("visitId") visitId: String,
        @Body protocol: VisitProtocolDto
    ): Response<VisitProtocolDto>

    @POST("api/protocols/visit/{visitId}/apply-template")
    suspend fun applyTemplate(
        @Path("visitId") visitId: String,
        @Body request: ApplyTemplateRequest
    ): Response<VisitProtocolDto>

    @DELETE("api/protocols/visit/{visitId}")
    suspend fun deleteProtocol(@Path("visitId") visitId: String): Response<Unit>

    @GET("api/protocols/templates")
    suspend fun getProtocolTemplates(): Response<List<ProtocolTemplateDto>>
}