package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.domain.model.ProtocolTemplate

interface ProtocolTemplateRepository {
    suspend fun getAllTemplates(): List<ProtocolTemplate>
    suspend fun refreshTemplates(): Result<Unit>
}