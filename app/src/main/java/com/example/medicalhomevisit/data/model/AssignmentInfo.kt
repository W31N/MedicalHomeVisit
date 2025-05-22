package com.example.medicalhomevisit.data.model

import java.util.Date

// Информация о назначении заявки врачу
data class AssignmentInfo(
    val staffId: String,
    val staffName: String,
    val assignedBy: String, // ID администратора, который произвел назначение
    val assignedAt: Date = Date(),
    val assignmentNote: String? = null
)