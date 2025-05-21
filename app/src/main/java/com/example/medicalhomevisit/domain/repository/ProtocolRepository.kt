package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.data.model.VisitProtocol
import com.example.medicalhomevisit.domain.model.ProtocolTemplate

interface ProtocolRepository {
    suspend fun getProtocolForVisit(visitId: String): VisitProtocol?
    suspend fun saveProtocol(protocol: VisitProtocol): VisitProtocol
    suspend fun getProtocolTemplates(): List<ProtocolTemplate>
    suspend fun getProtocolTemplateById(templateId: String): ProtocolTemplate?
}