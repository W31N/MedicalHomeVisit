package com.example.medicalhomevisit.data.remote.dto

import java.util.Date

data class AdminPatientRegistrationDto(
    val email: String,
    val password: String,
    val fullName: String,
    val phoneNumber: String,
    val address: String,
    val dateOfBirth: Date,
    val gender: String,
    val medicalCardNumber: String?,
    val additionalInfo: String?
)
