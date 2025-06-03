package com.example.medicalhomevisit.data.remote.api

import com.example.medicalhomevisit.data.remote.dto.AdminPatientRegistrationDto
import com.example.medicalhomevisit.data.remote.dto.MedicalPersonDto
import com.example.medicalhomevisit.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AdminApiService {

    @GET("api/medical-person/active") // Или /api/medical-person/medical-staff
    suspend fun getActiveMedicalStaff(): Response<List<MedicalPersonDto>>

    @POST("api/admin/register-patient") // Пример эндпоинта для регистрации пациента админом
    suspend fun registerPatientByAdmin(@Body registrationDto: AdminPatientRegistrationDto): Response<UserDto>
    // Убедись, что бэкенд реализует этот эндпоинт и принимает AdminPatientRegistrationDto
}