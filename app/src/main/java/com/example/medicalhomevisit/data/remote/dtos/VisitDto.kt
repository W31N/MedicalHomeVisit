package com.example.medicalhomevisit.data.remote.dtos

import java.util.Date

data class VisitDto(
    val id: String,
    val patientId: String,
    val patientName: String,
    val scheduledTime: Date,
    val status: String,
    val address: String,
    val reasonForVisit: String,
    val notes: String?,
    val assignedStaffId: String,
    val assignedStaffName: String?,
    val actualStartTime: Date?,
    val actualEndTime: Date?,
    val createdAt: Date,
    val updatedAt: Date
)

data class VisitStatusUpdateRequest(
    val status: String
)

data class VisitNotesUpdateRequest(
    val notes: String
)