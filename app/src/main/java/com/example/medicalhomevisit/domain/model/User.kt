package com.example.medicalhomevisit.domain.model

data class User(
    val id: String = "", // ID пользователя (UserEntity.id)
    val email: String = "",
    val displayName: String = "",
    val role: UserRole = UserRole.PATIENT, // Изменил дефолтное значение на PATIENT для большей безопасности
    val isEmailVerified: Boolean = false,
    val medicalPersonId: String? = null // <--- НОВОЕ ПОЛЕ (MedicalPerson.id)
)

enum class UserRole {
    ADMIN,
    MEDICAL_STAFF,
    DISPATCHER,
    PATIENT
}