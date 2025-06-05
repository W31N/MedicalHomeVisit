package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.domain.model.ProtocolTemplate
import com.example.medicalhomevisit.domain.model.VisitProtocol

interface ProtocolRepository {
    suspend fun getProtocolForVisit(visitId: String): VisitProtocol?
    suspend fun saveProtocol(protocol: VisitProtocol): VisitProtocol
    suspend fun getProtocolTemplates(): List<ProtocolTemplate>
    suspend fun getProtocolTemplateById(templateId: String): ProtocolTemplate?
    suspend fun applyTemplate(visitId: String, templateId: String): VisitProtocol
    suspend fun updateProtocolField(visitId: String, field: String, value: String): VisitProtocol
    suspend fun updateVitals(
        visitId: String,
        temperature: Float? = null,
        systolicBP: Int? = null,
        diastolicBP: Int? = null,
        pulse: Int? = null
    ): VisitProtocol
    suspend fun syncProtocols(): Result<Unit>
    suspend fun getCachedProtocolForVisit(visitId: String): VisitProtocol?
}