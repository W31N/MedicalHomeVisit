package com.example.medicalhomevisit.data.repository

import android.util.Log
import com.example.medicalhomevisit.data.local.dao.ProtocolTemplateDao
import com.example.medicalhomevisit.data.local.entity.ProtocolTemplateEntity
import com.example.medicalhomevisit.data.remote.api.ProtocolApiService
import com.example.medicalhomevisit.data.remote.dto.ProtocolTemplateDto
import com.example.medicalhomevisit.domain.model.ProtocolTemplate
import com.example.medicalhomevisit.domain.repository.ProtocolTemplateRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SimpleOfflineProtocolTemplateRepository @Inject constructor(
    private val protocolTemplateDao: ProtocolTemplateDao,
    private val protocolApiService: ProtocolApiService
) : ProtocolTemplateRepository {

    companion object {
        private const val TAG = "OfflinePTemplateRepo"
    }

    override suspend fun getAllTemplates(): List<ProtocolTemplate> {
        refreshTemplates()
        return protocolTemplateDao.getAllTemplates().map { entities ->
            entities.map { it.toDomainModel() }
        }.firstOrNull() ?: emptyList()
    }

    override suspend fun refreshTemplates(): Result<Unit> {
        Log.d(TAG, "Refreshing protocol templates from server...")
        return try {
            val response = protocolApiService.getProtocolTemplates()
            if (response.isSuccessful && response.body() != null) {
                val templateDtos = response.body()!!
                if (templateDtos.isNotEmpty()) {
                    val templateEntities = templateDtos.map { it.toEntity() }
                    protocolTemplateDao.insertTemplates(templateEntities)
                    Log.d(TAG, "Refreshed and saved ${templateEntities.size} templates to Room.")
                } else {
                    Log.d(TAG, "Server returned 0 templates. Local cache might be cleared if 'insertTemplates' handles empty list by deleting.")
                    protocolTemplateDao.insertTemplates(emptyList())

                }
                Result.success(Unit)
            } else {
                val errorMsg = "Error fetching templates: ${response.code()} - ${response.message()}"
                Log.w(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error refreshing templates: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun ProtocolTemplateEntity.toDomainModel(): ProtocolTemplate {
        return ProtocolTemplate(
            id = this.id,
            name = this.name,
            description = this.description ?: "",
            complaints = this.complaintsTemplate ?: "",
            anamnesis = this.anamnesisTemplate ?: "",
            objectiveStatus = this.objectiveStatusTemplate ?: "",
            recommendations = this.recommendationsTemplate ?: "",
            requiredVitals = this.requiredVitals
        )
    }

    private fun ProtocolTemplateDto.toEntity(): ProtocolTemplateEntity {
        val currentDate = Date()
        return ProtocolTemplateEntity(
            id = this.id,
            name = this.name,
            description = this.description,
            complaintsTemplate = this.complaintsTemplate,
            anamnesisTemplate = this.anamnesisTemplate,
            objectiveStatusTemplate = this.objectiveStatusTemplate,
            recommendationsTemplate = this.recommendationsTemplate,
            requiredVitals = this.requiredVitals ?: emptyList(),
            createdAt = this.createdAt ?: currentDate,
            updatedAt = this.updatedAt ?: currentDate
        )
    }
}