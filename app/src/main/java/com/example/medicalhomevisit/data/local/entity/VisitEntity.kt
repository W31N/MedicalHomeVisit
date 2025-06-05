package com.example.medicalhomevisit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "visits")
data class VisitEntity(
    @PrimaryKey
    val id: String,
    val patientId: String,
    val scheduledTime: Date,
    val status: String,
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
    val originalRequestId: String? = null,

    val isSynced: Boolean = true,
    val syncAction: String? = null,
    val lastSyncAttempt: Date? = null
)