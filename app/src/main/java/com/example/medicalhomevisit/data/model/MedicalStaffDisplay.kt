package com.example.medicalhomevisit.data.model

data class MedicalStaffDisplay(
    val medicalPersonId: String, // ID для назначения (MedicalPerson.id)
    val userId: String,          // User.id (для информации, если нужно)
    val displayName: String,
    val role: UserRole,          // Будет MEDICAL_STAFF
    val specialization: String?  // Специализация из MedicalPerson
)
