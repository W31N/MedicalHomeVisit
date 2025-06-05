package com.example.medicalhomevisit.domain.model

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: UserRole = UserRole.PATIENT,
    val isEmailVerified: Boolean = false,
    val medicalPersonId: String? = null
)

enum class UserRole {
    ADMIN,
    MEDICAL_STAFF,
    DISPATCHER,
    PATIENT
}