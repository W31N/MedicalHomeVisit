package com.example.medicalhomevisit.data.remote.dto

import java.util.Date

data class PatientProfileUpdateDto(
    val dateOfBirth: Date?,
    val gender: String?,
    val address: String?,
    val phoneNumber: String?,
    val policyNumber: String?,
    val allergies: List<String>?,
    val chronicConditions: List<String>?
)