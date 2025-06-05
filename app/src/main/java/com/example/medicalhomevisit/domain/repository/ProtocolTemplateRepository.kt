package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.domain.model.ProtocolTemplate
import kotlinx.coroutines.flow.Flow

interface ProtocolTemplateRepository {
    fun observeAllTemplates(): Flow<List<ProtocolTemplate>> // Реактивное получение всех
    suspend fun getAllTemplates(): List<ProtocolTemplate>    // Однократное получение всех
    suspend fun refreshTemplates(): Result<Unit>             // Принудительное обновление с сервера
    suspend fun getTemplateById(templateId: String): ProtocolTemplate?
    fun searchTemplates(query: String): Flow<List<ProtocolTemplate>>
}