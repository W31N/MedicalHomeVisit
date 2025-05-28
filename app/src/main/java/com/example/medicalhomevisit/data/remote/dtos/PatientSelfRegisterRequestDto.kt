package com.example.medicalhomevisit.data.remote.dtos

data class PatientSelfRegisterRequestDto(
    val fullName: String,
    val email: String,
    val password: String,
    val confirmPassword: String
)