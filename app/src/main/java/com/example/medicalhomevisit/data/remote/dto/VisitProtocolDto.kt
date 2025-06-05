package com.example.medicalhomevisit.data.remote.dto

import java.util.Date

data class VisitProtocolDto(
    val id: String? = null,
    val visitId: String,
    val templateId: String? = null,
    val complaints: String? = null,
    val anamnesis: String? = null,
    val objectiveStatus: String? = null,
    val diagnosis: String? = null,
    val diagnosisCode: String? = null,
    val recommendations: String? = null,
    val temperature: Float? = null,
    val systolicBP: Int? = null,
    val diastolicBP: Int? = null,
    val pulse: Int? = null,
    val additionalVitals: Map<String, String>? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

data class ProtocolTemplateDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val complaintsTemplate: String? = null,
    val anamnesisTemplate: String? = null,
    val objectiveStatusTemplate: String? = null,
    val recommendationsTemplate: String? = null,
    val requiredVitals: List<String>? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

data class ApplyTemplateRequest(
    val templateId: String
)
