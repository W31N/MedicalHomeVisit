package com.example.medicalhomevisit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey
    val id: String,

    val fullName: String,
    val dateOfBirth: Date? = null,
    val age: Int? = null,
    val gender: String? = null, // "MALE", "FEMALE", "UNKNOWN"
    val address: String = "",
    val phoneNumber: String = "",
    val policyNumber: String = "",
    val allergies: List<String>? = null, // Требует TypeConverter
    val chronicConditions: List<String>? = null, // Требует TypeConverter

    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),

    // Поля для синхронизации
    val isSynced: Boolean = true,
    val syncAction: String? = null, // "CREATE", "UPDATE", "DELETE"
    val lastSyncAttempt: Date? = null
)