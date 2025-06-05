package com.example.medicalhomevisit.data.remote.api

import com.example.medicalhomevisit.data.remote.dto.PatientDto
import com.example.medicalhomevisit.data.remote.dto.PatientProfileUpdateDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PatientApiService {
    @GET("api/patients/{patientId}")
    suspend fun getPatientById(@Path("patientId") patientId: String): Response<PatientDto>

    @GET("api/patients/search")
    suspend fun searchPatients(@Query("query") query: String): Response<List<PatientDto>>

    @GET("api/patients")
    suspend fun getAllPatients(): Response<List<PatientDto>>

    @GET("api/patients/profile")
    suspend fun getMyProfile(): Response<PatientDto>

    @PUT("api/patients/profile")
    suspend fun updateMyProfile(@Body profileUpdate: PatientProfileUpdateDto): Response<PatientDto>
}