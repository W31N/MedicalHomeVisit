package com.example.medicalhomevisit.data.remote

import com.example.medicalhomevisit.data.remote.dtos.PatientDto
import retrofit2.Response
import retrofit2.http.*

interface PatientApiService {

    /**
     * Получить пациента по ID
     */
    @GET("api/patients/{patientId}")
    suspend fun getPatientById(@Path("patientId") patientId: String): Response<PatientDto>

    /**
     * Поиск пациентов по имени
     */
    @GET("api/patients/search")
    suspend fun searchPatients(@Query("query") query: String): Response<List<PatientDto>>

    /**
     * Получить всех пациентов (только для админов)
     */
    @GET("api/patients")
    suspend fun getAllPatients(): Response<List<PatientDto>>
}