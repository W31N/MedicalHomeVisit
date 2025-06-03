package com.example.medicalhomevisit.data.remote.api

import com.example.medicalhomevisit.data.remote.dto.LoginRequestDto
import com.example.medicalhomevisit.data.remote.dto.LoginResponseDto
import com.example.medicalhomevisit.data.remote.dto.PatientSelfRegisterRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/login") // Путь к вашему эндпоинту
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto> // Оборачиваем в Response

    @POST("api/auth/register")
    suspend fun register(@Body request: PatientSelfRegisterRequestDto): Response<LoginResponseDto> // Изменили тип ответа

    // Можно добавить эндпоинт /api/auth/logout, если он что-то делает на сервере
    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit> // Или Response<String> если сервер возвращает сообщение
}