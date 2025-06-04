package com.example.medicalhomevisit.domain.model

import java.util.Date

data class PatientProfileUpdate(
    val dateOfBirth: Date?,
    val gender: Gender?,
    val address: String?,
    val phoneNumber: String?,
    val policyNumber: String?,
    val allergies: List<String>?,
    val chronicConditions: List<String>?
)