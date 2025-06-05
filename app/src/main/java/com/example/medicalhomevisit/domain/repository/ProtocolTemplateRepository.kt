package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.domain.model.ProtocolTemplate
import kotlinx.coroutines.flow.Flow

interface ProtocolTemplateRepository {
    fun observeAllTemplates(): Flow<List<ProtocolTemplate>>
    suspend fun getAllTemplates(): List<ProtocolTemplate>
    suspend fun refreshTemplates(): Result<Unit>
    suspend fun getTemplateById(templateId: String): ProtocolTemplate?
    fun searchTemplates(query: String): Flow<List<ProtocolTemplate>>
}