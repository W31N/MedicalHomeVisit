package com.example.medicalhomevisit.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: UserRole = UserRole.MEDICAL_STAFF,
    val isEmailVerified: Boolean = false
)

enum class UserRole {
    ADMIN,
    MEDICAL_STAFF,
    DISPATCHER,
    PATIENT
}