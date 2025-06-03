package com.example.medicalhomevisit.data.remote.api

import com.example.medicalhomevisit.data.remote.dto.ApplyTemplateRequest
import com.example.medicalhomevisit.data.remote.dto.ProtocolFieldUpdateRequest
import com.example.medicalhomevisit.data.remote.dto.ProtocolTemplateDto
import com.example.medicalhomevisit.data.remote.dto.ProtocolVitalsUpdateRequest
import com.example.medicalhomevisit.data.remote.dto.VisitProtocolDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProtocolApiService {

    /**
     * Получить протокол для визита
     */
    @GET("api/protocols/visit/{visitId}")
    suspend fun getProtocolForVisit(@Path("visitId") visitId: String): Response<VisitProtocolDto>

    /**
     * Создать протокол
     */
    @POST("api/protocols")
    suspend fun createProtocol(@Body protocol: VisitProtocolDto): Response<VisitProtocolDto>

    /**
     * Обновить протокол
     */
    @PUT("api/protocols")
    suspend fun updateProtocol(@Body protocol: VisitProtocolDto): Response<VisitProtocolDto>

    /**
     * Создать протокол для конкретного визита
     */
    @POST("api/protocols/visit/{visitId}")
    suspend fun createProtocolForVisit(
        @Path("visitId") visitId: String,
        @Body protocol: VisitProtocolDto
    ): Response<VisitProtocolDto>

    /**
     * Обновить протокол для конкретного визита
     */
    @PUT("api/protocols/visit/{visitId}")
    suspend fun updateProtocolForVisit(
        @Path("visitId") visitId: String,
        @Body protocol: VisitProtocolDto
    ): Response<VisitProtocolDto>

    /**
     * Применить шаблон к протоколу
     */
    @POST("api/protocols/visit/{visitId}/apply-template")
    suspend fun applyTemplate(
        @Path("visitId") visitId: String,
        @Body request: ApplyTemplateRequest
    ): Response<VisitProtocolDto>

    /**
     * Удалить протокол
     */
    @DELETE("api/protocols/visit/{visitId}")
    suspend fun deleteProtocol(@Path("visitId") visitId: String): Response<Unit>

    /**
     * Частичное обновление поля протокола
     */
    @PATCH("api/protocols/visit/{visitId}/field")
    suspend fun updateProtocolField(
        @Path("visitId") visitId: String,
        @Body fieldUpdate: ProtocolFieldUpdateRequest
    ): Response<VisitProtocolDto>

    /**
     * Обновление витальных показателей
     */
    @PATCH("api/protocols/visit/{visitId}/vitals")
    suspend fun updateVitals(
        @Path("visitId") visitId: String,
        @Body vitalsUpdate: ProtocolVitalsUpdateRequest
    ): Response<VisitProtocolDto>

    /**
     * Получить все шаблоны протоколов
     * ИСПРАВЛЕНО: используем правильный endpoint
     */
    @GET("api/protocols/templates")
    suspend fun getProtocolTemplates(): Response<List<ProtocolTemplateDto>>

    /**
     * Получить шаблон по ID
     * ИСПРАВЛЕНО: используем правильный endpoint
     */
    @GET("api/protocols/templates/{templateId}")
    suspend fun getProtocolTemplateById(@Path("templateId") templateId: String): Response<ProtocolTemplateDto>
}