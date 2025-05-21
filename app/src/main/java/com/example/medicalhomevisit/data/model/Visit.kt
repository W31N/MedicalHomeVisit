package com.example.medicalhomevisit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey val id: String,
    val patientId: String,
    val scheduledTime: Date,
    val actualStartTime: Date? = null,
    val actualEndTime: Date? = null,
    val status: VisitStatus = VisitStatus.PLANNED,
    val address: String,
    val reasonForVisit: String,
    val notes: String? = null,
    val createdAt: Date,
    val updatedAt: Date
)

enum class VisitStatus {
    PLANNED,     // Визит запланирован
    IN_PROGRESS, // Визит в процессе (врач в пути или у пациента)
    COMPLETED,   // Визит завершен
    CANCELLED    // Визит отменен
}