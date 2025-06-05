package com.example.medicalhomevisit.data.repository

import android.util.Log
import com.example.medicalhomevisit.data.local.dao.ProtocolTemplateDao
import com.example.medicalhomevisit.data.local.dao.VisitProtocolDao
import com.example.medicalhomevisit.data.local.entity.ProtocolTemplateEntity
import com.example.medicalhomevisit.data.local.entity.VisitProtocolEntity
import com.example.medicalhomevisit.data.remote.api.ProtocolApiService // Тебе нужно будет создать/обновить этот сервис
import com.example.medicalhomevisit.data.remote.dto.ProtocolTemplateDto
import com.example.medicalhomevisit.data.remote.dto.VisitProtocolDto
import com.example.medicalhomevisit.data.sync.SyncManager
import com.example.medicalhomevisit.domain.model.ProtocolTemplate
import com.example.medicalhomevisit.domain.model.VisitProtocol // Предполагаемая доменная модель
import com.example.medicalhomevisit.domain.repository.AuthRepository
import com.example.medicalhomevisit.domain.repository.ProtocolRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SimpleOfflineProtocolRepository @Inject constructor(
    private val protocolApiService: ProtocolApiService,
    private val visitProtocolDao: VisitProtocolDao,
    private val protocolTemplateDao: ProtocolTemplateDao,
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager
) : ProtocolRepository {

    companion object {
        private const val TAG = "OfflineProtocolRepo"
    }

    fun observeProtocolForVisit(visitId: String): Flow<VisitProtocol?> { // Сделаем публичным и не override, если нет в интерфейсе
        return visitProtocolDao.getProtocolByVisitId(visitId).map { entity ->
            entity?.toDomainModel()
        }
    }

    // Этот метод не в интерфейсе, поэтому без override
    suspend fun refreshProtocolForVisit(visitId: String): Result<Unit> {
        return try {
            Log.d(TAG, "📡 Refreshing protocol for visit $visitId from server...")
            val response = protocolApiService.getProtocolForVisit(visitId)
            if (response.isSuccessful && response.body() != null) {
                val protocolDto = response.body()!!
                visitProtocolDao.insertProtocol(protocolDto.toEntity(isSynced = true)) // Сохраняем как синхронизированный
                Log.d(TAG, "✅ Protocol for visit $visitId refreshed and saved to Room.")
                Result.success(Unit)
            } else {
                val errorMsg = "Error fetching protocol: ${response.code()} - ${response.message()}"
                Log.w(TAG, "❌ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Network error refreshing protocol for visit $visitId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getProtocolForVisit(visitId: String): VisitProtocol? {
        val localProtocol = visitProtocolDao.getProtocolByVisitIdOnce(visitId)?.toDomainModel()
        try {
            Log.d(TAG, "📡 Attempting to fetch/update protocol for visit $visitId from server...")
            val response = protocolApiService.getProtocolForVisit(visitId)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                visitProtocolDao.insertProtocol(dto.toEntity(isSynced = true))
                Log.d(TAG, "✅ Protocol for visit $visitId fetched/updated and saved.")
                return dto.toDomainModel()
            } else if (localProtocol != null) {
                Log.w(TAG, "⚠️ Failed to fetch protocol for $visitId from server (${response.code()}), returning local version.")
                return localProtocol
            } else {
                Log.w(TAG, "⚠️ Failed to fetch protocol for $visitId from server (${response.code()}) and no local version.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Network error fetching protocol for $visitId: ${e.message}")
            if (localProtocol != null) {
                Log.w(TAG, "Returning local version of protocol for $visitId due to network error.")
                return localProtocol
            }
        }
        return null
    }

    override suspend fun saveProtocol(protocol: VisitProtocol): VisitProtocol {
        Log.d(TAG, "💾 Saving protocol for visit ${protocol.visitId}, protocol ID: ${protocol.id}")
        val existingLocalProtocolById = protocol.id?.takeIf { it.isNotBlank() && !it.startsWith("local_") }?.let {
            visitProtocolDao.getProtocolById(it)
        }
        // Если ID есть и он не локальный, пытаемся найти по нему. Если нет, ищем по visitId, чтобы определить, новый ли это протокол для данного визита.
        val existingLocalProtocolByVisitId = visitProtocolDao.getProtocolByVisitIdOnce(protocol.visitId)

        // Считаем новым для сервера, если:
        // 1. ID протокола пустой или локальный.
        // 2. ID протокола есть, но такой записи нет в локальной базе (значит, это ID с сервера, но мы ее еще не сохраняли локально как "несинхронизированную").
        // 3. Или если протокола для данного visitId вообще не было локально.
        val isNewToServer = protocol.id.isNullOrBlank() ||
                protocol.id.startsWith("local_") ||
                (existingLocalProtocolById == null && existingLocalProtocolByVisitId == null) ||
                (existingLocalProtocolByVisitId != null && existingLocalProtocolByVisitId.syncAction == "CREATE")


        val entityToSave = protocol.toEntity(
            isSynced = false,
            syncAction = if (isNewToServer) "CREATE" else "UPDATE",
            updatedAt = Date(),
            // Генерируем ID, если он отсутствует или является "временным" локальным
            idForEntity = if (protocol.id.isNullOrBlank() || protocol.id.startsWith("local_")) {
                "local_proto_${UUID.randomUUID()}"
            } else {
                protocol.id
            }
        )

        visitProtocolDao.insertProtocol(entityToSave)
        Log.d(TAG, "📝 Protocol for visit ${entityToSave.visitId} (ID: ${entityToSave.id}) saved to Room, marked for sync: ${entityToSave.syncAction}")
        syncManager.syncNow()
        return entityToSave.toDomainModel()
    }

    // Имя и возвращаемый тип изменены, чтобы соответствовать интерфейсу
    override suspend fun applyTemplate(visitId: String, templateId: String): VisitProtocol {
        Log.d(TAG, "🔧 Applying template $templateId to protocol for visit $visitId")
        val template = protocolTemplateDao.getTemplateById(templateId)?.toDomainModel()
            ?: throw NoSuchElementException("Template $templateId not found locally")

        var currentProtocol = getProtocolForVisit(visitId) // Используем уже существующий метод, который может загрузить с сервера
        val now = Date()

        if (currentProtocol == null) {
            currentProtocol = VisitProtocol(
                id = "local_${UUID.randomUUID()}",
                visitId = visitId,
                templateId = templateId,
                complaints = template.complaints,
                anamnesis = template.anamnesis,
                objectiveStatus = template.objectiveStatus,
                recommendations = template.recommendations,
                createdAt = now,
                updatedAt = now
            )
        } else {
            currentProtocol = currentProtocol.copy(
                templateId = templateId,
                complaints = template.complaints.takeIf { !it.isNullOrEmpty() } ?: currentProtocol.complaints,
                anamnesis = template.anamnesis.takeIf { !it.isNullOrEmpty() } ?: currentProtocol.anamnesis,
                objectiveStatus = template.objectiveStatus.takeIf { !it.isNullOrEmpty() } ?: currentProtocol.objectiveStatus,
                recommendations = template.recommendations.takeIf { !it.isNullOrEmpty() } ?: currentProtocol.recommendations,
                updatedAt = now
            )
        }
        return saveProtocol(currentProtocol) // saveProtocol пометит для синхронизации и вернет обновленную доменную модель
    }

    override suspend fun updateProtocolField(visitId: String, field: String, value: String): VisitProtocol {
        var protocol = getProtocolForVisit(visitId)
            ?: throw NoSuchElementException("Protocol for visit $visitId not found to update field $field")

        protocol = when (field.lowercase()) {
            "complaints" -> protocol.copy(complaints = value)
            "anamnesis" -> protocol.copy(anamnesis = value)
            "objectivestatus" -> protocol.copy(objectiveStatus = value) // Убедись в правильности имен полей
            "diagnosis" -> protocol.copy(diagnosis = value)
            "diagnosiscode" -> protocol.copy(diagnosisCode = value)
            "recommendations" -> protocol.copy(recommendations = value)
            else -> throw IllegalArgumentException("Unknown field for protocol: $field")
        }
        return saveProtocol(protocol.copy(updatedAt = Date()))
    }

    override suspend fun updateVitals(
        visitId: String,
        temperature: Float?,
        systolicBP: Int?,
        diastolicBP: Int?,
        pulse: Int?
    ): VisitProtocol {
        var protocol = getProtocolForVisit(visitId)
            ?: throw NoSuchElementException("Protocol for visit $visitId not found to update vitals")

        protocol = protocol.copy(
            temperature = temperature ?: protocol.temperature,
            systolicBP = systolicBP ?: protocol.systolicBP,
            diastolicBP = diastolicBP ?: protocol.diastolicBP,
            pulse = pulse ?: protocol.pulse,
            updatedAt = Date()
        )
        return saveProtocol(protocol)
    }

    override suspend fun deleteProtocol(visitId: String) {
        Log.d(TAG, "🗑️ Request to delete protocol for visit $visitId")
        val protocolEntity = visitProtocolDao.getProtocolByVisitIdOnce(visitId)
        if (protocolEntity != null) {
            if (protocolEntity.syncAction == "CREATE") {
                visitProtocolDao.deleteProtocolById(protocolEntity.id)
                Log.d(TAG, "🗑️ Locally created protocol ${protocolEntity.id} deleted from Room.")
            } else {
                val updatedEntity = protocolEntity.copy(
                    isSynced = false,
                    syncAction = "DELETE",
                    updatedAt = Date()
                )
                visitProtocolDao.insertProtocol(updatedEntity)
                syncManager.syncNow()
                Log.d(TAG, "📝 Protocol ${protocolEntity.id} marked for DELETE sync.")
            }
        } else {
            Log.w(TAG, "No protocol found for visit $visitId to delete.")
        }
    }

    override suspend fun syncProtocols(): Result<Unit> {
        Log.d(TAG, "🔄 Manual sync requested for protocols.")
        syncManager.syncNow()
        return Result.success(Unit)
    }

    override suspend fun cacheProtocols(protocols: List<VisitProtocol>) {
        Log.d(TAG, "💾 Caching ${protocols.size} protocols.")
        val entities = protocols.map {
            it.toEntity(isSynced = true, syncAction = null, updatedAt = it.updatedAt ?: Date(), idForEntity = it.id)
        }
        visitProtocolDao.insertProtocols(entities)
    }

    override suspend fun getCachedProtocols(): List<VisitProtocol> {
        // Предполагаем, что нужен метод в DAO для получения ВСЕХ протоколов
        // Если такого нет, эту функцию нужно будет адаптировать или удалить.
        // @Query("SELECT * FROM visit_protocols") fun getAllProtocols(): List<VisitProtocolEntity>
        // val allEntities = visitProtocolDao.getAllProtocols()
        // return allEntities.map { it.toDomainModel() }
        Log.w(TAG, "getCachedProtocols: Needs DAO.getAllProtocols() or similar. Returning empty for now.")
        return emptyList()
    }

    override suspend fun getCachedProtocolForVisit(visitId: String): VisitProtocol? {
        return visitProtocolDao.getProtocolByVisitIdOnce(visitId)?.toDomainModel()
    }

    // --- МЕТОДЫ ДЛЯ ProtocolTemplate ---
    // Реализуем их здесь, так как интерфейс ProtocolRepository их требует

    override suspend fun getProtocolTemplates(): List<ProtocolTemplate> {
        var templates = protocolTemplateDao.getAllTemplates().firstOrNull()?.map { it.toDomainModel() } ?: emptyList()
        if (templates.isEmpty()) { // Простое условие для попытки обновления, если кэш пуст
            Log.d(TAG, "No local templates, attempting to refresh from server...")
            refreshProtocolTemplatesInternal().fold(
                onSuccess = {
                    templates = protocolTemplateDao.getAllTemplates().firstOrNull()?.map { it.toDomainModel() } ?: emptyList()
                },
                onFailure = { Log.e(TAG, "Failed to refresh templates: ${it.message}") }
            )
        }
        return templates
    }

    private suspend fun refreshProtocolTemplatesInternal(): Result<Unit> { // Вспомогательный приватный метод
        Log.d(TAG, "📡 Refreshing protocol templates from server (internal)...")
        return try {
            val response = protocolApiService.getProtocolTemplates()
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                if (dtos.isNotEmpty()){
                    protocolTemplateDao.insertTemplates(dtos.map { it.toEntity() })
                    Log.d(TAG, "✅ Refreshed and saved ${dtos.size} templates to Room.")
                } else {
                    Log.d(TAG, "✅ Server returned 0 templates. No changes to local cache.")
                    // Реши, нужно ли очищать кэш, если сервер вернул 0. Пока не очищаем.
                    // protocolTemplateDao.deleteAllTemplates() // Если нужно очистить
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch templates from API: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProtocolTemplateById(templateId: String): ProtocolTemplate? {
        return protocolTemplateDao.getTemplateById(templateId)?.toDomainModel()
        // Можно добавить логику "если не найдено локально, загрузить с сервера и сохранить"
    }

    // --- НЕОБХОДИМЫЕ МАППЕРЫ (помести их в этот файл ниже или в отдельный Mappers.kt) ---

    // Мапперы для VisitProtocol
    private fun VisitProtocolEntity.toDomainModel(): VisitProtocol {
        return VisitProtocol(
            id = this.id,
            visitId = this.visitId,
            templateId = this.templateId,
            complaints = this.complaints ?: "",
            anamnesis = this.anamnesis ?: "",
            objectiveStatus = this.objectiveStatus ?: "",
            diagnosis = this.diagnosis ?: "",
            diagnosisCode = this.diagnosisCode ?: "",
            recommendations = this.recommendations ?: "",
            temperature = this.temperature,
            systolicBP = this.systolicBP,
            diastolicBP = this.diastolicBP,
            pulse = this.pulse,
            additionalVitals = this.additionalVitals,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
            // Поля isSynced, syncAction и т.д. обычно не нужны в доменной модели
        )
    }

    // idForEntity - чтобы можно было передать сгенерированный локальный ID
    private fun VisitProtocol.toEntity(isSynced: Boolean, syncAction: String?, updatedAt: Date, idForEntity: String? = this.id): VisitProtocolEntity {
        val finalId = idForEntity ?: "error_id_${UUID.randomUUID()}" // Должен быть ID
        return VisitProtocolEntity(
            id = finalId,
            visitId = this.visitId,
            templateId = this.templateId,
            complaints = this.complaints,
            anamnesis = this.anamnesis,
            objectiveStatus = this.objectiveStatus,
            diagnosis = this.diagnosis,
            diagnosisCode = this.diagnosisCode,
            recommendations = this.recommendations,
            temperature = this.temperature,
            systolicBP = this.systolicBP,
            diastolicBP = this.diastolicBP,
            pulse = this.pulse,
            additionalVitals = this.additionalVitals ?: emptyMap(),
            createdAt = this.createdAt ?: updatedAt, // createdAt не должен меняться после первого сохранения
            updatedAt = updatedAt,
            isSynced = isSynced,
            syncAction = syncAction,
            lastSyncAttempt = if (!isSynced) Date() else null, // Пример
            failCount = if (!isSynced) 0 else (this as? VisitProtocolEntity)?.failCount ?: 0 // Не очень хорошо, лучше не смешивать
        )
    }

    // Маппер из DTO в Entity для сохранения ответа сервера
    private fun VisitProtocolDto.toEntity(isSynced: Boolean = true): VisitProtocolEntity {
        val currentDate = Date()
        return VisitProtocolEntity(
            id = this.id ?: "dto_err_${UUID.randomUUID()}", // Сервер должен всегда возвращать ID
            visitId = this.visitId,
            templateId = this.templateId,
            complaints = this.complaints,
            anamnesis = this.anamnesis,
            objectiveStatus = this.objectiveStatus,
            diagnosis = this.diagnosis,
            diagnosisCode = this.diagnosisCode,
            recommendations = this.recommendations,
            temperature = this.temperature,
            systolicBP = this.systolicBP,
            diastolicBP = this.diastolicBP,
            pulse = this.pulse,
            additionalVitals = this.additionalVitals ?: emptyMap(),
            createdAt = this.createdAt ?: currentDate,
            updatedAt = this.updatedAt ?: currentDate,
            isSynced = isSynced,
            syncAction = null // С сервера приходит уже синхронизированное
        )
    }

    // Маппер из DTO в Domain (если API возвращает DTO, а ты хочешь сразу Domain)
    private fun VisitProtocolDto.toDomainModel(): VisitProtocol {
        val currentDate = Date()
        return VisitProtocol(
            id = this.id ?: "dto_err_${UUID.randomUUID()}",
            visitId = this.visitId,
            templateId = this.templateId,
            complaints = this.complaints ?: "",
            anamnesis = this.anamnesis ?: "",
            objectiveStatus = this.objectiveStatus ?: "",
            diagnosis = this.diagnosis ?: "",
            diagnosisCode = this.diagnosisCode ?: "",
            recommendations = this.recommendations ?: "",
            temperature = this.temperature,
            systolicBP = this.systolicBP,
            diastolicBP = this.diastolicBP,
            pulse = this.pulse,
            additionalVitals = this.additionalVitals,
            createdAt = this.createdAt ?: currentDate,
            updatedAt = this.updatedAt ?: currentDate
        )
    }

    // Мапперы для ProtocolTemplate (ты их уже частично сделал)
    private fun ProtocolTemplateEntity.toDomainModel(): ProtocolTemplate {
        return ProtocolTemplate(
            id = this.id,
            name = this.name,
            description = this.description ?: "",
            complaints = this.complaintsTemplate ?: "",
            anamnesis = this.anamnesisTemplate ?: "",
            objectiveStatus = this.objectiveStatusTemplate ?: "",
            recommendations = this.recommendationsTemplate ?: "",
            requiredVitals = this.requiredVitals,
            // category = null, // Если в Domain модели есть, а в Entity нет
            // createdAt = this.createdAt, // Если есть в Domain
            // updatedAt = this.updatedAt  // Если есть в Domain
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