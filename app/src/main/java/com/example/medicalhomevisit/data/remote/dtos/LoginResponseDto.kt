package com.example.medicalhomevisit.data.remote.dtos // или ваш пакет

data class LoginResponseDto(
    val token: String,
    val tokenType: String = "Bearer",
    val user: UserDto
)