package com.example.medicalhomevisit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey val id: String,
    val fullName: String,
    val dateOfBirth: Date,
    val age: Int,
    val gender: Gender,
    val address: String,
    val phoneNumber: String,
    val policyNumber: String,
    val allergies: List<String>? = null,
    val chronicConditions: List<String>? = null
)

enum class Gender {
    MALE, FEMALE
}