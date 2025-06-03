    package com.example.medicalhomevisit.data.remote.dto

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
        val patientId: String,
        val patientName: String?,
        val patientPhone: String?,
        val address: String,
        val requestType: String,
        val symptoms: String,
        val additionalNotes: String?,
        val preferredDateTime: Date?,
        val status: String,
        val assignedStaffId: String?,
        val assignedStaffName: String?,
        val assignedBy: String?, //id админа
        val assignedAt: Date?,
        val assignmentNote: String?,
        val responseMessage: String?,
        val createdAt: Date?,
        val updatedAt: Date?
    )

    data class UpdateRequestStatusDto(
        val status: String,
        val responseMessage: String? = null
    )

    data class AssignStaffToRequestDto(
        val staffId: String,
        val assignmentNote: String? = null
    )