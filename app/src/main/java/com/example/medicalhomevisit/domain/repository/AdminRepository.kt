// com/example/medicalhomevisit/domain/repository/AdminRepository.kt
package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.data.model.User
import java.util.Date

interface AdminRepository {
    suspend fun getAllStaff(): List<User>
    suspend fun registerNewPatient(
        email: String,
        password: String,
        displayName: String,
        phoneNumber: String,
        address: String,
        dateOfBirth: Date,
        gender: String,
        medicalCardNumber: String?,
        additionalInfo: String?
    ): User

    suspend fun getActiveStaff(): List<User>
}