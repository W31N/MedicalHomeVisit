package com.example.medicalhomevisit.data.repository

import android.util.Log
import com.example.medicalhomevisit.data.local.dao.ProtocolTemplateDao
import com.example.medicalhomevisit.data.local.dao.VisitProtocolDao
import com.example.medicalhomevisit.data.local.entity.ProtocolTemplateEntity
import com.example.medicalhomevisit.data.local.entity.VisitProtocolEntity
import com.example.medicalhomevisit.data.remote.api.ProtocolApiService
import com.example.medicalhomevisit.data.remote.dto.ProtocolTemplateDto
import com.example.medicalhomevisit.data.remote.dto.VisitProtocolDto
import com.example.medicalhomevisit.data.sync.SyncManager
import com.example.medicalhomevisit.domain.model.ProtocolTemplate
import com.example.medicalhomevisit.domain.model.VisitProtocol
import com.example.medicalhomevisit.domain.repository.ProtocolRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleOfflineProtocolRepository @Inject constructor(
    private val protocolApiService: ProtocolApiService,
    private val visitProtocolDao: VisitProtocolDao,
    private val protocolTemplateDao: ProtocolTemplateDao,
    private val syncManager: SyncManager
) : ProtocolRepository {

    companion object {
        private const val TAG = "OfflineProtocolRepo"
    }

    fun observeProtocolForVisit(visitId: String): Flow<VisitProtocol?> {
        return visitProtocolDao.getProtocolByVisitId(visitId).map { entity ->
            entity?.toDomainModel()
        }
    }

    override suspend fun getProtocolForVisit(visitId: String): VisitProtocol? {
        Log.d(TAG, "Getting protocol for visit: $visitId")

        val localProtocol = visitProtocolDao.getProtocolByVisitIdOnce(visitId)?.toDomainModel()

        tryRefreshProtocolFromServer(visitId)

        return localProtocol
    }

    override suspend fun saveProtocol(protocol: VisitProtocol): VisitProtocol {
        Log.d(TAG, "Saving protocol for visit ${protocol.visitId}")

        val now = Date()
        val existingEntity = visitProtocolDao.getProtocolByVisitIdOnce(protocol.visitId)

        val isNewProtocol = existingEntity == null
        val protocolId = if (isNewProtocol || protocol.id.isBlank()) {
            "local_proto_${UUID.randomUUID()}"
        } else {
            protocol.id
        }

        val entityToSave = protocol.toEntity(
            id = protocolId,
            isSynced = false,
            syncAction = if (isNewProtocol) "CREATE" else "UPDATE",
            updatedAt = now
        )

        visitProtocolDao.insertProtocol(entityToSave)
        Log.d(TAG, "Protocol saved locally with action: ${entityToSave.syncAction}")

        syncManager.syncProtocolsNow()

        return entityToSave.toDomainModel()
    }

    override suspend fun updateProtocolField(visitId: String, field: String, value: String): VisitProtocol {
        Log.d(TAG, "Updating field '$field' for visit $visitId")

        var protocol = getProtocolForVisit(visitId)
        if (protocol == null) {
            protocol = VisitProtocol(
                id = "local_proto_${UUID.randomUUID()}",
                visitId = visitId,
                createdAt = Date(),
                updatedAt = Date()
            )
        }

        val updatedProtocol = when (field.lowercase()) {
            "complaints" -> protocol.copy(complaints = value)
            "anamnesis" -> protocol.copy(anamnesis = value)
            "objectivestatus" -> protocol.copy(objectiveStatus = value)
            "diagnosis" -> protocol.copy(diagnosis = value)
            "diagnosiscode" -> protocol.copy(diagnosisCode = value)
            "recommendations" -> protocol.copy(recommendations = value)
            else -> throw IllegalArgumentException("Unknown field: $field")
        }.copy(updatedAt = Date())

        return saveProtocol(updatedProtocol)
    }

    override suspend fun updateVitals(
        visitId: String,
        temperature: Float?,
        systolicBP: Int?,
        diastolicBP: Int?,
        pulse: Int?
    ): VisitProtocol {
        Log.d(TAG, "Updating vitals for visit $visitId")

        var protocol = getProtocolForVisit(visitId)
        if (protocol == null) {
            protocol = VisitProtocol(
                id = "local_proto_${UUID.randomUUID()}",
                visitId = visitId,
                createdAt = Date(),
                updatedAt = Date()
            )
        }

        val updatedProtocol = protocol.copy(
            temperature = temperature ?: protocol.temperature,
            systolicBP = systolicBP ?: protocol.systolicBP,
            diastolicBP = diastolicBP ?: protocol.diastolicBP,
            pulse = pulse ?: protocol.pulse,
            updatedAt = Date()
        )

        return saveProtocol(updatedProtocol)
    }

    override suspend fun applyTemplate(visitId: String, templateId: String): VisitProtocol {
        Log.d(TAG, "Applying template $templateId to visit $visitId")

        val apiResult = tryApplyTemplateOnServer(visitId, templateId)
        if (apiResult != null) {
            return apiResult
        }

        val template = getProtocolTemplateById(templateId)
            ?: throw IllegalArgumentException("Template $templateId not found")

        var protocol = getProtocolForVisit(visitId)
        if (protocol == null) {
            protocol = VisitProtocol(
                id = "local_proto_${UUID.randomUUID()}",
                visitId = visitId,
                createdAt = Date(),
                updatedAt = Date()
            )
        }

        val updatedProtocol = protocol.copy(
            templateId = templateId,
            complaints = template.complaints.ifBlank { protocol.complaints },
            anamnesis = template.anamnesis.ifBlank { protocol.anamnesis },
            objectiveStatus = template.objectiveStatus.ifBlank { protocol.objectiveStatus },
            recommendations = template.recommendations.ifBlank { protocol.recommendations },
            updatedAt = Date()
        )

        return saveProtocol(updatedProtocol)
    }

    override suspend fun getProtocolTemplates(): List<ProtocolTemplate> {
        Log.d(TAG, "Getting protocol templates")

        var templates = protocolTemplateDao.getAllTemplates().firstOrNull()?.map { it.toDomainModel() } ?: emptyList()

        if (templates.isEmpty()) {
            tryRefreshTemplatesFromServer()
            templates = protocolTemplateDao.getAllTemplates().firstOrNull()?.map { it.toDomainModel() } ?: emptyList()
        }

        return templates
    }

    override suspend fun getProtocolTemplateById(templateId: String): ProtocolTemplate? {
        return protocolTemplateDao.getTemplateById(templateId)?.toDomainModel()
    }

    override suspend fun syncProtocols(): Result<Unit> {
        Log.d(TAG, "Manual protocol sync requested")
        syncManager.syncProtocolsNow()
        return Result.success(Unit)
    }

    override suspend fun getCachedProtocolForVisit(visitId: String): VisitProtocol? {
        return visitProtocolDao.getProtocolByVisitIdOnce(visitId)?.toDomainModel()
    }

    private suspend fun tryRefreshProtocolFromServer(visitId: String) {
        try {
            Log.d(TAG, "Refreshing protocol for visit $visitId from server")
            val response = protocolApiService.getProtocolForVisit(visitId)

            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val entity = dto.toEntity(isSynced = true)
                visitProtocolDao.insertProtocol(entity)
                Log.d(TAG, "Protocol refreshed from server")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh protocol from server: ${e.message}")
        }
    }

    private suspend fun tryApplyTemplateOnServer(visitId: String, templateId: String): VisitProtocol? {
        return try {
            Log.d(TAG, "Applying template on server")
            val response = protocolApiService.applyTemplate(visitId,
                com.example.medicalhomevisit.data.remote.dto.ApplyTemplateRequest(templateId))

            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val entity = dto.toEntity(isSynced = true)
                visitProtocolDao.insertProtocol(entity)
                Log.d(TAG, "Template applied on server")
                entity.toDomainModel()
            } else {
                Log.w(TAG, "Failed to apply template on server: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying template on server: ${e.message}")
            null
        }
    }

    private suspend fun tryRefreshTemplatesFromServer() {
        try {
            Log.d(TAG, "Refreshing templates from server")
            val response = protocolApiService.getProtocolTemplates()

            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                val entities = dtos.map { it.toEntity() }
                protocolTemplateDao.insertTemplates(entities)
                Log.d(TAG, "${entities.size} templates refreshed from server")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh templates: ${e.message}")
        }
    }

    suspend fun getUnsyncedCount(): Int {
        return try {
            visitProtocolDao.getUnsyncedProtocols().size
        } catch (e: Exception) {
            Log.w(TAG, "Error getting unsynced count: ${e.message}")
            0
        }
    }

    suspend fun getUnsyncedProtocols(): List<VisitProtocol> {
        return try {
            visitProtocolDao.getUnsyncedProtocols().map { it.toDomainModel() }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting unsynced protocols: ${e.message}")
            emptyList()
        }
    }

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
        )
    }

    private fun VisitProtocol.toEntity(
        id: String = this.id,
        isSynced: Boolean = true,
        syncAction: String? = null,
        updatedAt: Date = this.updatedAt
    ): VisitProtocolEntity {
        return VisitProtocolEntity(
            id = id,
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
            createdAt = this.createdAt,
            updatedAt = updatedAt,
            isSynced = isSynced,
            syncAction = syncAction,
            lastSyncAttempt = if (!isSynced) Date() else null,
            failCount = 0
        )
    }

    private fun VisitProtocolDto.toEntity(isSynced: Boolean = true): VisitProtocolEntity {
        val now = Date()
        return VisitProtocolEntity(
            id = this.id ?: "dto_${UUID.randomUUID()}",
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
            createdAt = this.createdAt ?: now,
            updatedAt = this.updatedAt ?: now,
            isSynced = isSynced,
            syncAction = null
        )
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
            requiredVitals = this.requiredVitals,
            category = null
        )
    }

    private fun ProtocolTemplateDto.toEntity(): ProtocolTemplateEntity {
        val now = Date()
        return ProtocolTemplateEntity(
            id = this.id,
            name = this.name,
            description = this.description,
            complaintsTemplate = this.complaintsTemplate,
            anamnesisTemplate = this.anamnesisTemplate,
            objectiveStatusTemplate = this.objectiveStatusTemplate,
            recommendationsTemplate = this.recommendationsTemplate,
            requiredVitals = this.requiredVitals ?: emptyList(),
            createdAt = this.createdAt ?: now,
            updatedAt = this.updatedAt ?: now
        )
    }
}