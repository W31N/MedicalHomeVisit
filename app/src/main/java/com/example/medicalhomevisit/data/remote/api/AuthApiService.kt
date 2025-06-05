package com.example.medicalhomevisit.data.remote.api

import com.example.medicalhomevisit.data.remote.dto.LoginRequestDto
import com.example.medicalhomevisit.data.remote.dto.LoginResponseDto
import com.example.medicalhomevisit.data.remote.dto.PatientSelfRegisterRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @POST("api/auth/register")
    suspend fun register(@Body request: PatientSelfRegisterRequestDto): Response<LoginResponseDto>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>
}