package com.example.medicalhomevisit.data.remote.dto

import java.util.Date

data class AdminPatientRegistrationDto(
    val email: String,
    val password: String, // Пароль в открытом виде до бэкенда, бэкенд хеширует
    val fullName: String,
    val phoneNumber: String,
    val address: String,
    val dateOfBirth: Date, // Или String в формате ISO "yyyy-MM-dd"
    val gender: String, // "MALE", "FEMALE", "UNKNOWN"
    val medicalCardNumber: String?,
    val additionalInfo: String?
)
