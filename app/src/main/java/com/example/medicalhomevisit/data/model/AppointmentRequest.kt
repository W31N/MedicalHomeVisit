package com.example.medicalhomevisit.data.model

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
    val preferredDate: Date? = null,
    val preferredTimeRange: String = "", // Добавляем это поле
    val status: RequestStatus = RequestStatus.NEW,
    val assignedStaffId: String? = null,
    val assignedStaffName: String? = null,
    val assignedBy: String? = null,
    val assignedAt: Date? = null,
    val assignmentNote: String? = null,
    val responseMessage: String = "", // Добавляем это поле
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),

    // Добавляем поле urgencyLevel
    val urgencyLevel: UrgencyLevel? = null,
    val priority: Int = 0 // 0 - обычный, 1 - высокий, 2 - критический
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

enum class UrgencyLevel {
    LOW,      // Низкая
    NORMAL,   // Обычная
    HIGH,     // Высокая
    CRITICAL  // Критическая
}