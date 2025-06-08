package com.example.medicalhomevisit.domain.model

import java.util.Date

data class Visit(
    val id: String? = null,
    val patientId: String,
    val scheduledTime: Date,
    val status: VisitStatus,
    val address: String,
    val reasonForVisit: String,
    val notes: String = "",
    val assignedStaffId: String? = null,
    val assignedStaffName: String? = null,
    val actualStartTime: Date? = null,
    val actualEndTime: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val isFromRequest: Boolean = false,
    val originalRequestId: String? = null
)

enum class VisitStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}