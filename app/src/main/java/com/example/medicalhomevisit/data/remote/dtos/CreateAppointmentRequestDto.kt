package com.example.medicalhomevisit.data.remote.dtos

import java.util.Date

data class CreateAppointmentRequestDto(
    val requestType: String,
    val symptoms: String,
    val additionalNotes: String? = null,
    val preferredDateTime: Date? = null,
    val address: String
)

data class AppointmentRequestDto(
    val id: String,
    val emailPatient: String,
    val address: String,
    val requestType: String,
    val symptoms: String,
    val additionalNotes: String?,
    val preferredDateTime: Date?,
    val status: String,
    val assignedStaffEmail: String?,
    val assignedByUserEmail: String?,
    val assignedAt: Date?,
    val assignmentNote: String?,
    val responseMessage: String?
)

data class UpdateRequestStatusDto(
    val status: String,
    val responseMessage: String? = null
)

data class AssignStaffToRequestDto(
    val staffId: String,
    val assignmentNote: String? = null
)