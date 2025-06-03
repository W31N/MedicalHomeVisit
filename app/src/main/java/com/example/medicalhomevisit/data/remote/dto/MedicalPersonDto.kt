package com.example.medicalhomevisit.data.remote.dto

data class MedicalPersonDto(
    val medicalPersonId: String,
    val userId: String,
    val fullName: String,
    val specialization: String?
)
