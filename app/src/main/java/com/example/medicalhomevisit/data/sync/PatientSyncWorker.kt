package com.example.medicalhomevisit.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.medicalhomevisit.data.local.dao.PatientDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date

@HiltWorker
class PatientSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val patientDao: PatientDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "PatientSyncWorker"
        const val WORK_NAME = "patient_sync_work"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting patient synchronization...")

        return try {
            val unsyncedPatients = patientDao.getUnsyncedPatients()
            Log.d(TAG, "Found ${unsyncedPatients.size} unsynced patients")

            var successCount = 0
            var failCount = 0

            for (patient in unsyncedPatients) {
                try {
                    when (patient.syncAction) {
                        "UPDATE" -> {
                            Log.d(TAG, "Patient UPDATE sync not fully implemented yet for ${patient.id}")

                            patientDao.markAsSynced(patient.id)
                            successCount++
                        }
                        "CREATE" -> {
                            Log.d(TAG, "Patient CREATE sync not implemented yet for ${patient.id}")
                            successCount++
                        }
                        "DELETE" -> {
                            Log.d(TAG, "Patient DELETE sync not implemented yet for ${patient.id}")
                            successCount++
                        }
                        else -> {
                            Log.w(TAG, "Unknown sync action: ${patient.syncAction} for ${patient.id}")
                            patientDao.updateLastSyncAttempt(patient.id, Date())
                            failCount++
                        }
                    }
                } catch (e: Exception) {
                    patientDao.updateLastSyncAttempt(patient.id, Date())
                    failCount++
                    Log.e(TAG, "Error syncing patient ${patient.id}: ${e.message}")
                }
            }

            Log.d(TAG, "Patient sync completed: $successCount success, $failCount failed")

            if (failCount == 0) {
                Result.success()
            } else if (successCount > 0) {
                Result.retry()
            } else {
                Result.failure()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Patient sync worker failed: ${e.message}", e)
            Result.failure()
        }
    }
}