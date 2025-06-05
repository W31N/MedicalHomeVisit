package com.example.medicalhomevisit.data.remote.dto

data class UserDto(
    val id: String, // ID пользователя (UserEntity.id)
    val email: String,
    val displayName: String,
    val role: String,
    val emailVerified: Boolean, // Убедись, что сервер это поле присылает
    val medicalPersonId: String? // <--- НОВОЕ ПОЛЕ (MedicalPerson.id)
)