package com.example.medicalhomevisit.domain.model

import java.util.Date

data class Patient(
    val id: String,
    val fullName: String,
    val dateOfBirth: Date? = null,
    val age: Int? = null,
    val gender: Gender = Gender.UNKNOWN,
    val address: String,
    val phoneNumber: String,
    val policyNumber: String,
    val allergies: List<String>? = null,
    val chronicConditions: List<String>? = null
)

enum class Gender {
    MALE,
    FEMALE,
    UNKNOWN
}