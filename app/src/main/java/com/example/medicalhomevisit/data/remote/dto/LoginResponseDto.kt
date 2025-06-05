package com.example.medicalhomevisit.data.remote.dto

data class LoginResponseDto(
    val token: String,
    val tokenType: String = "Bearer",
    val user: UserDto
)