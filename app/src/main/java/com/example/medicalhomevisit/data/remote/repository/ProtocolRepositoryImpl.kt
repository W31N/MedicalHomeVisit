//package com.example.medicalhomevisit.data.remote.repository
//
//import android.util.Log
//import com.example.medicalhomevisit.data.remote.api.ProtocolApiService
//import com.example.medicalhomevisit.domain.model.VisitProtocol
//import com.example.medicalhomevisit.data.remote.dto.*
//import com.example.medicalhomevisit.domain.model.ProtocolTemplate
//import com.example.medicalhomevisit.domain.repository.AuthRepository
//import com.example.medicalhomevisit.domain.repository.ProtocolRepository
//import java.util.Date
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class ProtocolRepositoryImpl @Inject constructor(
//    private val apiService: ProtocolApiService,
//    private val authRepository: AuthRepository
//) : ProtocolRepository {
//
//    companion object {
//        private const val TAG = "HttpProtocolRepository"
//    }
//
//    // Кэш для офлайн режима
//    private val cachedProtocols = mutableMapOf<String, VisitProtocol>()
//
//    override suspend fun getProtocolForVisit(visitId: String): VisitProtocol? {
//        return try {
//            Log.d(TAG, "Getting protocol for visit: $visitId")
//            val response = apiService.getProtocolForVisit(visitId)
//
//            if (response.isSuccessful) {
//                val dto = response.body()
//                if (dto != null) {
//                    val protocol = convertDtoToProtocol(dto)
//
//                    // Обновляем кэш
//                    cachedProtocols[visitId] = protocol
//
//                    Log.d(TAG, "Successfully loaded protocol for visit $visitId")
//                    protocol
//                } else {
//                    Log.d(TAG, "No protocol found for visit $visitId")
//                    null
//                }
//            } else if (response.code() == 404) {
//                // 404 означает что протокол не найден - это нормально
//                Log.d(TAG, "No protocol found for visit $visitId (404)")
//                null
//            } else {
//                Log.e(TAG, "Failed to load protocol: ${response.code()} ${response.message()}")
//                throw Exception("Ошибка загрузки протокола: ${response.code()}")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error loading protocol for visit", e)
//            throw e
//        }
//    }
//
//    override suspend fun saveProtocol(protocol: VisitProtocol): VisitProtocol {
//        return try {
//            Log.d(TAG, "Saving protocol for visit: ${protocol.visitId}")
//            val dto = convertProtocolToDto(protocol)
//
//            val response = if (protocol.id.isBlank() || protocol.id == "0") {
//                // Создаем новый протокол
//                apiService.createProtocolForVisit(protocol.visitId, dto)
//            } else {
//                // Обновляем существующий
//                apiService.updateProtocolForVisit(protocol.visitId, dto)
//            }
//
//            if (response.isSuccessful) {
//                val savedDto = response.body() ?: throw Exception("Пустой ответ от сервера")
//                val savedProtocol = convertDtoToProtocol(savedDto)
//
//                // Обновляем кэш
//                cachedProtocols[protocol.visitId] = savedProtocol
//
//                Log.d(TAG, "Successfully saved protocol for visit ${protocol.visitId}")
//                savedProtocol
//            } else {
//                Log.e(TAG, "Failed to save protocol: ${response.code()} ${response.message()}")
//                throw Exception("Ошибка сохранения протокола: ${response.code()}")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error saving protocol", e)
//            throw e
//        }
//    }
//
//    override suspend fun getProtocolTemplates(): List<ProtocolTemplate> {
//        return try {
//            Log.d(TAG, "Getting protocol templates")
//            // ИСПРАВЛЕНО: используем правильный endpoint
//            val response = apiService.getProtocolTemplates()
//
//            if (response.isSuccessful) {
//                val templateDtos = response.body() ?: emptyList()
//                val templates = templateDtos.map { convertDtoToTemplate(it) }
//                Log.d(TAG, "Successfully loaded ${templates.size} templates")
//                templates
//            } else {
//                Log.e(TAG, "Failed to load templates: ${response.code()} ${response.message()}")
//                throw Exception("Ошибка загрузки шаблонов: ${response.code()}")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error loading protocol templates", e)
//            throw e
//        }
//    }
//
//    override suspend fun getProtocolTemplateById(templateId: String): ProtocolTemplate? {
//        return try {
//            Log.d(TAG, "Getting protocol template by ID: $templateId")
//            // ИСПРАВЛЕНО: используем правильный endpoint
//            val response = apiService.getProtocolTemplateById(templateId)
//
//            if (response.isSuccessful) {
//                val dto = response.body()
//                if (dto != null) {
//                    val template = convertDtoToTemplate(dto)
//                    Log.d(TAG, "Successfully loaded template: $templateId")
//                    template
//                } else {
//                    Log.d(TAG, "Template not found: $templateId")
//                    null
//                }
//            } else if (response.code() == 404) {
//                Log.d(TAG, "Template not found: $templateId (404)")
//                null
//            } else {
//                Log.e(TAG, "Failed to load template: ${response.code()} ${response.message()}")
//                throw Exception("Ошибка загрузки шаблона: ${response.code()}")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error loading protocol template by ID", e)
//            throw e
//        }
//    }
//
//    override suspend fun applyTemplate(visitId: String, templateId: String): VisitProtocol {
//        return try {
//            Log.d(TAG, "Applying template $templateId to visit $visitId")
//            val request = ApplyTemplateRequest(templateId)
//            val response = apiService.applyTemplate(visitId, request)
//
//            if (response.isSuccessful) {
//                val dto = response.body() ?: throw Exception("Пустой ответ от сервера")
//                val protocol = convertDtoToProtocol(dto)
//
//                // Обновляем кэш
//                cachedProtocols[visitId] = protocol
//
//                Log.d(TAG, "Successfully applied template to visit $visitId")
//                protocol
//            } else {
//                Log.e(TAG, "Failed to apply template: ${response.code()} ${response.message()}")
//                throw Exception("Ошибка применения шаблона: ${response.code()}")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error applying template", e)
//            throw e
//        }
//    }
//
//    override suspend fun updateProtocolField(visitId: String, field: String, value: String): VisitProtocol {
//        return try {
//            Log.d(TAG, "Updating protocol field $field for visit $visitId")
//            val request = ProtocolFieldUpdateRequest(field, value)
//            val response = apiService.updateProtocolField(visitId, request)
//
//            if (response.isSuccessful) {
//                val dto = response.body() ?: throw Exception("Пустой ответ от сервера")
//                val protocol = convertDtoToProtocol(dto)
//
//                // Обновляем кэш
//                cachedProtocols[visitId] = protocol
//
//                Log.d(TAG, "Successfully updated field $field for visit $visitId")
//                protocol
//            } else {
//                Log.e(TAG, "Failed to update field: ${response.code()} ${response.message()}")
//                throw Exception("Ошибка обновления поля: ${response.code()}")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error updating protocol field", e)
//            throw e
//        }
//    }
//
//    override suspend fun updateVitals(
//        visitId: String,
//        temperature: Float?,
//        systolicBP: Int?,
//        diastolicBP: Int?,
//        pulse: Int?
//    ): VisitProtocol {
//        return try {
//            Log.d(TAG, "Updating vitals for visit $visitId")
//            val request = ProtocolVitalsUpdateRequest(temperature, systolicBP, diastolicBP, pulse)
//            val response = apiService.updateVitals(visitId, request)
//
//            if (response.isSuccessful) {
//                val dto = response.body() ?: throw Exception("Пустой ответ от сервера")
//                val protocol = convertDtoToProtocol(dto)
//
//                // Обновляем кэш
//                cachedProtocols[visitId] = protocol
//
//                Log.d(TAG, "Successfully updated vitals for visit $visitId")
//                protocol
//            } else {
//                Log.e(TAG, "Failed to update vitals: ${response.code()} ${response.message()}")
//                throw Exception("Ошибка обновления витальных показателей: ${response.code()}")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error updating vitals", e)
//            throw e
//        }
//    }
//
//    override suspend fun deleteProtocol(visitId: String) {
//        try {
//            Log.d(TAG, "Deleting protocol for visit: $visitId")
//            val response = apiService.deleteProtocol(visitId)
//
//            if (response.isSuccessful) {
//                // Удаляем из кэша
//                cachedProtocols.remove(visitId)
//
//                Log.d(TAG, "Successfully deleted protocol for visit $visitId")
//            } else {
//                Log.e(TAG, "Failed to delete protocol: ${response.code()} ${response.message()}")
//                throw Exception("Ошибка удаления протокола: ${response.code()}")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error deleting protocol", e)
//            throw e
//        }
//    }
//
//    override suspend fun syncProtocols(): Result<Unit> {
//        return try {
//            // Здесь можно реализовать синхронизацию кэшированных изменений
//            // Пока что просто возвращаем успех
//            Log.d(TAG, "Protocols synced successfully")
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Log.e(TAG, "Error syncing protocols", e)
//            Result.failure(e)
//        }
//    }
//
//    override suspend fun cacheProtocols(protocols: List<VisitProtocol>) {
//        protocols.forEach { protocol ->
//            cachedProtocols[protocol.visitId] = protocol
//        }
//        Log.d(TAG, "Cached ${protocols.size} protocols")
//    }
//
//    override suspend fun getCachedProtocols(): List<VisitProtocol> {
//        return cachedProtocols.values.toList()
//    }
//
//    override suspend fun getCachedProtocolForVisit(visitId: String): VisitProtocol? {
//        return cachedProtocols[visitId]
//    }
//
//    /**
//     * Конвертация DTO с сервера в доменную модель
//     */
//    private fun convertDtoToProtocol(dto: VisitProtocolDto): VisitProtocol {
//        return VisitProtocol(
//            id = dto.id ?: "",
//            visitId = dto.visitId,
//            templateId = dto.templateId,
//            complaints = dto.complaints ?: "",
//            anamnesis = dto.anamnesis ?: "",
//            objectiveStatus = dto.objectiveStatus ?: "",
//            diagnosis = dto.diagnosis ?: "",
//            diagnosisCode = dto.diagnosisCode ?: "",
//            recommendations = dto.recommendations ?: "",
//            temperature = dto.temperature,
//            systolicBP = dto.systolicBP,
//            diastolicBP = dto.diastolicBP,
//            pulse = dto.pulse,
//            additionalVitals = dto.additionalVitals,
//            createdAt = dto.createdAt ?: Date(),
//            updatedAt = dto.updatedAt ?: Date()
//        )
//    }
//
//    /**
//     * Конвертация доменной модели в DTO для отправки на сервер
//     */
//    private fun convertProtocolToDto(protocol: VisitProtocol): VisitProtocolDto {
//        return VisitProtocolDto(
//            id = if (protocol.id.isBlank() || protocol.id == "0") null else protocol.id,
//            visitId = protocol.visitId,
//            templateId = protocol.templateId,
//            complaints = protocol.complaints,
//            anamnesis = protocol.anamnesis,
//            objectiveStatus = protocol.objectiveStatus,
//            diagnosis = protocol.diagnosis,
//            diagnosisCode = protocol.diagnosisCode,
//            recommendations = protocol.recommendations,
//            temperature = protocol.temperature,
//            systolicBP = protocol.systolicBP,
//            diastolicBP = protocol.diastolicBP,
//            pulse = protocol.pulse,
//            additionalVitals = protocol.additionalVitals
//        )
//    }
//
//    /**
//     * Конвертация DTO шаблона в доменную модель
//     */
//    private fun convertDtoToTemplate(dto: ProtocolTemplateDto): ProtocolTemplate {
//        return ProtocolTemplate(
//            id = dto.id,
//            name = dto.name,
//            description = dto.description ?: "",
//            complaints = dto.complaintsTemplate ?: "", // ИСПРАВЛЕНО: используем правильное поле
//            anamnesis = dto.anamnesisTemplate ?: "", // ИСПРАВЛЕНО: используем правильное поле
//            objectiveStatus = dto.objectiveStatusTemplate ?: "", // ИСПРАВЛЕНО: используем правильное поле
//            recommendations = dto.recommendationsTemplate ?: "", // ИСПРАВЛЕНО: используем правильное поле
//            requiredVitals = dto.requiredVitals ?: emptyList(),
//            category = null // На бэкенде пока нет поля category
//        )
//    }
//}