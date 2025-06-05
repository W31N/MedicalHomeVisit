package com.example.medicalhomevisit.domain.model

import java.util.Date

data class ProtocolTemplate(
    val id: String,
    val name: String,
    val description: String,
    val complaints: String = "",
    val anamnesis: String = "",
    val objectiveStatus: String = "",
    val recommendations: String = "",
    val requiredVitals: List<String> = emptyList(),
    val category: String? = null
)