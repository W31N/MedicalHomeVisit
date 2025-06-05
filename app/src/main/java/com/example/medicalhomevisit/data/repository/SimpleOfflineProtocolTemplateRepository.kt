package com.example.medicalhomevisit.data.repository

import android.util.Log
import com.example.medicalhomevisit.data.local.dao.ProtocolTemplateDao
import com.example.medicalhomevisit.data.local.entity.ProtocolTemplateEntity
import com.example.medicalhomevisit.data.remote.api.ProtocolApiService // или отдельный TemplateApiService
import com.example.medicalhomevisit.data.remote.dto.ProtocolTemplateDto
import com.example.medicalhomevisit.domain.model.ProtocolTemplate // Доменная модель
import com.example.medicalhomevisit.domain.repository.ProtocolTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SimpleOfflineProtocolTemplateRepository @Inject constructor(
    private val protocolTemplateDao: ProtocolTemplateDao,
    private val protocolApiService: ProtocolApiService // Используем тот же или создаем новый
) : ProtocolTemplateRepository { // Реализуем твой новый интерфейс

    companion object {
        private const val TAG = "OfflinePTemplateRepo"
    }

    override fun observeAllTemplates(): Flow<List<ProtocolTemplate>> {
        return protocolTemplateDao.getAllTemplates().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getAllTemplates(): List<ProtocolTemplate> {
        // Можно добавить логику сначала из DAO, потом попытка с сервера, если DAO пусто
        // Для простоты пока всегда пробуем обновить и возвращаем из DAO
        refreshTemplates() // Попытка обновить перед возвратом
        return protocolTemplateDao.getAllTemplates().map { entities -> // map здесь нужен если getAllTemplates возвращает Flow
            entities.map { it.toDomainModel() }
        }.firstOrNull() ?: emptyList() // Если getAllTemplates возвращает Flow<List>, иначе просто .map
    }

    override suspend fun refreshTemplates(): Result<Unit> {
        Log.d(TAG, "📡 Refreshing protocol templates from server...")
        return try {
            val response = protocolApiService.getProtocolTemplates() // Используем метод из твоего ProtocolApiService
            if (response.isSuccessful && response.body() != null) {
                val templateDtos = response.body()!!
                if (templateDtos.isNotEmpty()) {
                    val templateEntities = templateDtos.map { it.toEntity() }
                    protocolTemplateDao.insertTemplates(templateEntities)
                    Log.d(TAG, "✅ Refreshed and saved ${templateEntities.size} templates to Room.")
                } else {
                    Log.d(TAG, "✅ Server returned 0 templates. Local cache might be cleared if 'insertTemplates' handles empty list by deleting.")
                    // Если insertTemplates не очищает старые при пустом списке, то нужно это сделать отдельно, если требуется.
                    // Например, protocolTemplateDao.clearAllTemplates() перед insertTemplates(emptyList())
                    // Но обычно OnConflictStrategy.REPLACE при пустом списке ничего не делает.
                    // Если нужно очистить, если сервер вернул пусто:
                    // protocolTemplateDao.clearAllAndInsert(templateEntities) // Потребуется такой метод в DAO
                    protocolTemplateDao.insertTemplates(emptyList()) // Если нет логики очистки, а просто перезаписываем (если бы был один метод insertAllOrReplace)
                    // Либо просто ничего не делаем, если сервер вернул пусто, а локально что-то есть
                }
                Result.success(Unit)
            } else {
                val errorMsg = "Error fetching templates: ${response.code()} - ${response.message()}"
                Log.w(TAG, "❌ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Network error refreshing templates: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getTemplateById(templateId: String): ProtocolTemplate? {
        return protocolTemplateDao.getTemplateById(templateId)?.toDomainModel()
        // Опционально: если не найдено локально, можно попытаться загрузить с сервера и сохранить
    }

    override fun searchTemplates(query: String): Flow<List<ProtocolTemplate>> {
        return protocolTemplateDao.searchTemplates(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    // --- Необходимые мапперы ---
    private fun ProtocolTemplateEntity.toDomainModel(): ProtocolTemplate {
        return ProtocolTemplate(
            id = this.id,
            name = this.name,
            description = this.description ?: "", // Если description в Entity null, используем ""
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
            createdAt = this.createdAt ?: currentDate, // Если с сервера может прийти null
            updatedAt = this.updatedAt ?: currentDate  // Если с сервера может прийти null
        )
    }
}