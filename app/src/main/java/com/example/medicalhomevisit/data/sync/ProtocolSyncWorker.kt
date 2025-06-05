package com.example.medicalhomevisit.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.medicalhomevisit.data.local.dao.VisitProtocolDao
import com.example.medicalhomevisit.data.remote.api.ProtocolApiService
import com.example.medicalhomevisit.data.remote.dto.VisitProtocolDto // Клиентский DTO
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date
import java.util.UUID

@HiltWorker
class ProtocolSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val visitProtocolDao: VisitProtocolDao,
    private val apiService: ProtocolApiService
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ProtocolSyncWorker"
        const val WORK_NAME = "protocol_sync_work"

        fun isLocalId(id: String): Boolean {
            return id.startsWith("local_proto_")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🔄 Starting protocol synchronization...")

        return try {
            val unsyncedProtocols = visitProtocolDao.getUnsyncedProtocols()
            Log.d(TAG, "📝 Found ${unsyncedProtocols.size} unsynced protocols")

            var successCount = 0
            var failCount = 0

            for (protocolEntity in unsyncedProtocols) {
                try {
                    val isCurrentIdLocal = isLocalId(protocolEntity.id)
                    val visitIdString = protocolEntity.visitId // Уже String из VisitProtocolEntity

                    // Конвертируем templateId в String?, если он есть
                    val templateIdString = protocolEntity.templateId // Уже String? из VisitProtocolEntity

                    if (protocolEntity.syncAction == "CREATE" || (protocolEntity.syncAction == "UPDATE" && isCurrentIdLocal)) {
                        Log.d(TAG, "🆕 Attempting to CREATE protocol (local ID: ${protocolEntity.id}, original action: ${protocolEntity.syncAction}) on server for visit $visitIdString")

                        // Проверка visitIdString на валидность UUID необязательна, если вы уверены, что в БД он всегда корректен.
                        // Если нет, то можно добавить:
                        try {
                            UUID.fromString(visitIdString)
                        } catch (e: IllegalArgumentException) {
                            Log.e(TAG, "Invalid visitId format in entity: $visitIdString for local protocol ${protocolEntity.id}")
                            failCount++
                            visitProtocolDao.incrementFailCount(protocolEntity.id, Date())
                            continue
                        }
                        // Аналогично для templateIdString, если он не null
                        templateIdString?.let {
                            try {
                                UUID.fromString(it)
                            } catch (e: IllegalArgumentException) {
                                Log.w(TAG, "Invalid templateId format in entity: $it for local protocol ${protocolEntity.id}. Will be sent as is or you might want to nullify it.")
                                // templateIdString = null // Раскомментировать, если невалидный templateId лучше не слать
                            }
                        }


                        val createDto = VisitProtocolDto(
                            id = null,
                            visitId = visitIdString, // Передаем String
                            templateId = templateIdString, // Передаем String?
                            complaints = protocolEntity.complaints,
                            anamnesis = protocolEntity.anamnesis,
                            objectiveStatus = protocolEntity.objectiveStatus,
                            diagnosis = protocolEntity.diagnosis,
                            diagnosisCode = protocolEntity.diagnosisCode,
                            recommendations = protocolEntity.recommendations,
                            temperature = protocolEntity.temperature,
                            systolicBP = protocolEntity.systolicBP,
                            diastolicBP = protocolEntity.diastolicBP,
                            pulse = protocolEntity.pulse,
                            additionalVitals = protocolEntity.additionalVitals.takeIf { it.isNotEmpty() }
                        )

                        // Передаем visitIdString (String) в метод API
                        val response = apiService.createProtocolForVisit(visitIdString, createDto)

                        if (response.isSuccessful && response.body() != null) {
                            val serverProtocolDto = response.body()!! // Это DTO с бэкенда (поля id, visitId, templateId типа String? или String)
                            val oldLocalId = protocolEntity.id

                            if (isCurrentIdLocal) {
                                visitProtocolDao.deleteProtocolById(oldLocalId)
                            }

                            val syncedEntity = protocolEntity.copy(
                                id = serverProtocolDto.id!!, // ID от сервера точно будет не null
                                visitId = serverProtocolDto.visitId, // visitId от сервера
                                templateId = serverProtocolDto.templateId, // templateId от сервера
                                isSynced = true,
                                syncAction = null,
                                failCount = 0,
                                updatedAt = serverProtocolDto.updatedAt ?: Date() // Обновляем с серверной датой
                            )
                            visitProtocolDao.insertProtocol(syncedEntity)

                            successCount++
                            Log.d(TAG, "✅ Successfully CREATED protocol (local ${oldLocalId} -> server ${serverProtocolDto.id})")
                        } else {
                            visitProtocolDao.incrementFailCount(protocolEntity.id, Date())
                            failCount++
                            Log.w(TAG, "❌ Failed to CREATE protocol ${protocolEntity.id} (tried as POST): ${response.code()} - ${response.errorBody()?.string()}")
                        }

                    } else if (protocolEntity.syncAction == "UPDATE" && !isCurrentIdLocal) {
                        Log.d(TAG, "📝 Updating protocol ${protocolEntity.id} (server ID) on server for visit $visitIdString")

                        val protocolIdString = protocolEntity.id // Уже String (серверный UUID)

                        // Опциональные проверки, что это валидные UUID строки
                        try { UUID.fromString(visitIdString) } catch (e: IllegalArgumentException) { Log.e(TAG,"..."); failCount++; continue }
                        try { UUID.fromString(protocolIdString) } catch (e: IllegalArgumentException) { Log.e(TAG,"..."); failCount++; continue }
                        templateIdString?.let { try { UUID.fromString(it) } catch (e: IllegalArgumentException) { /* Log warning */ } }


                        val updateDto = VisitProtocolDto(
                            id = protocolIdString, // Серверный ID (String)
                            visitId = visitIdString, // (String)
                            templateId = templateIdString, // (String?)
                            complaints = protocolEntity.complaints,
                            anamnesis = protocolEntity.anamnesis,
                            objectiveStatus = protocolEntity.objectiveStatus,
                            diagnosis = protocolEntity.diagnosis,
                            diagnosisCode = protocolEntity.diagnosisCode,
                            recommendations = protocolEntity.recommendations,
                            temperature = protocolEntity.temperature,
                            systolicBP = protocolEntity.systolicBP,
                            diastolicBP = protocolEntity.diastolicBP,
                            pulse = protocolEntity.pulse,
                            additionalVitals = protocolEntity.additionalVitals.takeIf { it.isNotEmpty() }
                        )
                        // Передаем visitIdString (String) в метод API
                        val response = apiService.updateProtocolForVisit(visitIdString, updateDto)

                        if (response.isSuccessful) {
                            val serverResponseDto = response.body()
                            // Если markAsSynced принимает только protocolId:
                            visitProtocolDao.markAsSynced(protocolEntity.id)

                            // Если markAsSynced принимает protocolId, isSynced, updatedAt:
                            // visitProtocolDao.markAsSynced(
                            //     protocolId = protocolEntity.id,
                            //     isSynced = true, // Вы явно передаете true
                            //     updatedAt = serverResponseDto?.updatedAt ?: Date()
                            // )
                            successCount++
                            Log.d(TAG, "✅ Updated protocol ${protocolEntity.id}")
                        } else {
                            visitProtocolDao.incrementFailCount(protocolEntity.id, Date())
                            failCount++
                            Log.w(TAG, "❌ Failed to UPDATE protocol ${protocolEntity.id}: ${response.code()} - ${response.errorBody()?.string()}")
                        }
                    } else if (protocolEntity.syncAction == "DELETE") {
                        if (isCurrentIdLocal) {
                            Log.w(TAG, "⚠️ Attempting to DELETE protocol ${protocolEntity.id} with local ID. Deleting locally.")
                            visitProtocolDao.deleteProtocolById(protocolEntity.id)
                            successCount++ // Считаем успешным, т.к. локально удалили "мусор"
                            continue
                        }
                        Log.d(TAG, "🗑️ Deleting protocol ${protocolEntity.id} on server")
                        val visitIdStringToUseForDelete = visitIdString
                        try { UUID.fromString(visitIdStringToUseForDelete) } catch (e: IllegalArgumentException) { Log.e(TAG,"..."); failCount++; continue }

                        val response = apiService.deleteProtocol(visitIdStringToUseForDelete)

                        if (response.isSuccessful) {
                            visitProtocolDao.deleteProtocolById(protocolEntity.id)
                            successCount++
                            Log.d(TAG, "✅ Deleted protocol ${protocolEntity.id}")
                        } else {
                            visitProtocolDao.incrementFailCount(protocolEntity.id, Date())
                            failCount++
                            Log.w(TAG, "❌ Failed to delete protocol ${protocolEntity.id}: ${response.code()} - ${response.errorBody()?.string()}")
                        }
                    } else {
                        Log.w(TAG, "🤷 Unknown sync action: ${protocolEntity.syncAction} for protocol ${protocolEntity.id} or inconsistent ID state (isLocalId: $isCurrentIdLocal). Skipping.")
                        visitProtocolDao.incrementFailCount(protocolEntity.id, Date())
                        failCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "💥 Exception during sync of protocol ${protocolEntity.id}: ${e.message}", e)
                    visitProtocolDao.incrementFailCount(protocolEntity.id, Date())
                    failCount++
                }
            }

            Log.d(TAG, "🏁 Protocol sync completed: $successCount success, $failCount failed")

            return if (failCount == 0) Result.success() else Result.retry()

        } catch (e: Exception) {
            Log.e(TAG, "🔥 Protocol sync worker failed catastrophically: ${e.message}", e)
            Result.failure()
        }
    }
}