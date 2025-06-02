package com.example.medicalhomevisit.data.remote

import com.example.medicalhomevisit.data.model.MedicalStaffDisplay
import com.example.medicalhomevisit.data.model.User
import java.util.Date

interface AdminRepository {

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
    ): Result<User>

    suspend fun getActiveStaff(): Result<List<MedicalStaffDisplay>>
}