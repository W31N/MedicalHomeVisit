package com.example.medicalhomevisit.domain.model

data class MedicalStaffDisplay(
    val medicalPersonId: String,
    val userId: String,
    val displayName: String,
    val role: UserRole,
    val specialization: String?
)
