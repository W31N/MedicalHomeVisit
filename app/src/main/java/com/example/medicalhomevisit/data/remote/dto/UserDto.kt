package com.example.medicalhomevisit.data.remote.dto

data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String,
    val emailVerified: Boolean,
    val medicalPersonId: String?
)