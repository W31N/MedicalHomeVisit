package com.example.medicalhomevisit.data.remote.dto

data class PatientSelfRegisterRequestDto(
    val fullName: String,
    val email: String,
    val password: String,
    val confirmPassword: String
)