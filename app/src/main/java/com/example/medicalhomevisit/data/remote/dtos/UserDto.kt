package com.example.medicalhomevisit.data.remote.dtos

import java.util.UUID


data class UserDto(
    val id: String,// // Или UUID, если на бэкенде UUID и вы хотите его так получать
    val email: String,
    val displayName: String,
    val role: String, // Бэкенд, скорее всего, вернет роль как строку (имя enum'а)
    val emailVerified: Boolean
)