package com.example.medicalhomevisit.data.repository

import com.example.medicalhomevisit.data.TestDataManager
import com.example.medicalhomevisit.data.model.VisitProtocol
import com.example.medicalhomevisit.domain.model.ProtocolTemplate
import com.example.medicalhomevisit.domain.repository.ProtocolRepository

class FakeProtocolRepository : ProtocolRepository {
    override suspend fun getProtocolForVisit(visitId: String): VisitProtocol? {
        return TestDataManager.getProtocolForVisit(visitId)
    }

    override suspend fun saveProtocol(protocol: VisitProtocol): VisitProtocol {
        return TestDataManager.saveProtocol(protocol)
    }

    override suspend fun getProtocolTemplates(): List<ProtocolTemplate> {
        return TestDataManager.getProtocolTemplates()
    }

    override suspend fun getProtocolTemplateById(templateId: String): ProtocolTemplate? {
        return TestDataManager.getProtocolTemplateById(templateId)
    }
}