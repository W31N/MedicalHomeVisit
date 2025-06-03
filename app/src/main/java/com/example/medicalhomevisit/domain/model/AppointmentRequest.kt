package com.example.medicalhomevisit.domain.model

import java.util.Date

data class AppointmentRequest(
    val id: String = "",
    val patientId: String,
    val patientName: String,
    val patientPhone: String,
    val address: String,
    val requestType: RequestType,
    val symptoms: String,
    val additionalNotes: String = "",
    val preferredDateTime: Date? = null,
    val status: RequestStatus = RequestStatus.NEW,
    val assignedStaffId: String? = null,
    val assignedStaffName: String? = null,
    val assignedBy: String? = null,
    val assignedAt: Date? = null,
    val assignmentNote: String? = null,
    val responseMessage: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
)

enum class RequestType {
    EMERGENCY,
    REGULAR,
    CONSULTATION
}

enum class RequestStatus {
    NEW,
    PENDING,
    ASSIGNED,
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}