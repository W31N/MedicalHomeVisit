package com.example.medicalhomevisit.data.model

import java.util.Date


data class VisitProtocol(
    val id: String,
    val visitId: String,
    val templateId: String? = null,
    val complaints: String = "",
    val anamnesis: String = "",
    val objectiveStatus: String = "",
    val diagnosis: String = "",
    val diagnosisCode: String = "",
    val recommendations: String = "",
    val temperature: Float? = null,
    val systolicBP: Int? = null,
    val diastolicBP: Int? = null,
    val pulse: Int? = null,
    val additionalVitals: Map<String, String>? = null,
    val createdAt: Date,
    val updatedAt: Date
)