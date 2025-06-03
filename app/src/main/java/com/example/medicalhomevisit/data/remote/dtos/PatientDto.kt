package com.example.medicalhomevisit.data.remote.dtos

import java.util.Date

data class PatientDto(
    val id: String,
    val fullName: String,
    val dateOfBirth: Date?,
    val age: Int?,
    val gender: String?,
    val address: String?,
    val phoneNumber: String?,
    val policyNumber: String?,
    val allergies: List<String>?,
    val chronicConditions: List<String>?,
    val createdAt: Date,
    val updatedAt: Date
)
