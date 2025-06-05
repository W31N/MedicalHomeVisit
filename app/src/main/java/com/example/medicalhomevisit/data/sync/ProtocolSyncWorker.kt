package com.example.medicalhomevisit.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.medicalhomevisit.data.local.dao.VisitProtocolDao
import com.example.medicalhomevisit.data.remote.api.ProtocolApiService
import com.example.medicalhomevisit.data.remote.dto.VisitProtocolDto
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
        Log.d(TAG, "Starting protocol synchronization...")

        return try {
            val unsyncedProtocols = visitProtocolDao.getUnsyncedProtocols()
            Log.d(TAG, "Found ${unsyncedProtocols.size} unsynced protocols")

            var successCount = 0
            var failCount = 0

            for (protocolEntity in unsyncedProtocols) {
                try {
                    val isCurrentIdLocal = isLocalId(protocolEntity.id)
                    val visitIdString = protocolEntity.visitId

                    val templateIdString = protocolEntity.templateId

                    if (protocolEntity.syncAction == "CREATE" || (protocolEntity.syncAction == "UPDATE" && isCurrentIdLocal)) {
                        Log.d(TAG, "Attempting to CREATE protocol (local ID: ${protocolEntity.id}, original action: ${protocolEntity.syncAction}) on server for visit $visitIdString")

                        try {
                            UUID.fromString(visitIdString)
                        } catch (e: IllegalArgumentException) {
                            Log.e(TAG, "Invalid visitId format in entity: $visitIdString for local protocol ${protocolEntity.id}")
                            failCount++
                            visitProtocolDao.incrementFailCount(protocolEntity.id, Date())
                            continue
                        }
                        templateIdString?.let {
                            try {
                                UUID.fromString(it)
                            } catch (e: IllegalArgumentException) {
                                Log.w(TAG, "Invalid templateId format in entity: $it for local protocol ${protocolEntity.id}. Will be sent as is or you might want to nullify it.")
                            }
                        }

                        val createDto = VisitProtocolDto(
                            id = null,
                            visitId = visitIdString,
                            templateId = templateIdString,
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

                        val response = apiService.createProtocolForVisit(visitIdString, createDto)

                        if (response.isSuccessful && response.body() != null) {
                            val serverProtocolDto = response.body()!!
                            val oldLocalId = protocolEntity.id

                            if (isCurrentIdLocal) {
                                visitProtocolDao.deleteProtocolById(oldLocalId)
                            }

                            val syncedEntity = protocolEntity.copy(
                                id = serverProtocolDto.id!!,
                                visitId = serverProtocolDto.visitId,
                                templateId = serverProtocolDto.templateId,
                                isSynced = true,
                                syncAction = null,
                                failCount = 0,
                                updatedAt = serverProtocolDto.updatedAt ?: Date()
                            )
                            visitProtocolDao.insertProtocol(syncedEntity)

                            successCount++
                            Log.d(TAG, "Successfully CREATED protocol (local ${oldLocalId} -> server ${serverProtocolDto.id})")
                        } else {
                            visitProtocolDao.incrementFailCount(protocolEntity.id, Date())
                            failCount++
                            Log.w(TAG, "Failed to CREATE protocol ${protocolEntity.id} (tried as POST): ${response.code()} - ${response.errorBody()?.string()}")
                        }

                    } else if (protocolEntity.syncAction == "UPDATE" && !isCurrentIdLocal) {
                        Log.d(TAG, "Updating protocol ${protocolEntity.id} (server ID) on server for visit $visitIdString")

                        val protocolIdString = protocolEntity.id

                        try { UUID.fromString(visitIdString) } catch (e: IllegalArgumentException) { Log.e(TAG,"..."); failCount++; continue }
                        try { UUID.fromString(protocolIdString) } catch (e: IllegalArgumentException) { Log.e(TAG,"..."); failCount++; continue }
                        templateIdString?.let { try { UUID.fromString(it) } catch (e: IllegalArgumentException) {}
                        }


                        val updateDto = VisitProtocolDto(
                            id = protocolIdString,
                            visitId = visitIdString,
                            templateId = templateIdString,
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

                        val response = apiService.updateProtocolForVisit(visitIdString, updateDto)

                        if (response.isSuccessful) {
                            visitProtocolDao.markAsSynced(protocolEntity.id)

                            successCount++
                            Log.d(TAG, "Updated protocol ${protocolEntity.id}")
                        } else {
                            visitProtocolDao.incrementFailCount(protocolEntity.id, Date())
                            failCount++
                            Log.w(TAG, "Failed to UPDATE protocol ${protocolEntity.id}: ${response.code()} - ${response.errorBody()?.string()}")
                        }
                    } else if (protocolEntity.syncAction == "DELETE") {
                        if (isCurrentIdLocal) {
                            Log.w(TAG, "Attempting to DELETE protocol ${protocolEntity.id} with local ID. Deleting locally.")
                            visitProtocolDao.deleteProtocolById(protocolEntity.id)
                            successCount++
                            continue
                        }
                        Log.d(TAG, "Deleting protocol ${protocolEntity.id} on server")
                        val visitIdStringToUseForDelete = visitIdString
                        try { UUID.fromString(visitIdStringToUseForDelete) } catch (e: IllegalArgumentException) { Log.e(TAG,"..."); failCount++; continue }

                        val response = apiService.deleteProtocol(visitIdStringToUseForDelete)

                        if (response.isSuccessful) {
                            visitProtocolDao.deleteProtocolById(protocolEntity.id)
                            successCount++
                            Log.d(TAG, "Deleted protocol ${protocolEntity.id}")
                        } else {
                            visitProtocolDao.incrementFailCount(protocolEntity.id, Date())
                            failCount++
                            Log.w(TAG, "Failed to delete protocol ${protocolEntity.id}: ${response.code()} - ${response.errorBody()?.string()}")
                        }
                    } else {
                        Log.w(TAG, "Unknown sync action: ${protocolEntity.syncAction} for protocol ${protocolEntity.id} or inconsistent ID state (isLocalId: $isCurrentIdLocal). Skipping.")
                        visitProtocolDao.incrementFailCount(protocolEntity.id, Date())
                        failCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during sync of protocol ${protocolEntity.id}: ${e.message}", e)
                    visitProtocolDao.incrementFailCount(protocolEntity.id, Date())
                    failCount++
                }
            }

            Log.d(TAG, "Protocol sync completed: $successCount success, $failCount failed")

            return if (failCount == 0) Result.success() else Result.retry()

        } catch (e: Exception) {
            Log.e(TAG, "Protocol sync worker failed catastrophically: ${e.message}", e)
            Result.failure()
        }
    }
}