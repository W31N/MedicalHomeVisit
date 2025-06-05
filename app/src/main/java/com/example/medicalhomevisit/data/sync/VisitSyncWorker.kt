package com.example.medicalhomevisit.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.medicalhomevisit.data.local.dao.VisitDao
import com.example.medicalhomevisit.data.remote.api.VisitApiService
import com.example.medicalhomevisit.data.remote.dto.VisitStatusUpdateRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date

@HiltWorker
class VisitSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val visitDao: VisitDao,
    private val apiService: VisitApiService
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "VisitSyncWorker"
        const val WORK_NAME = "visit_sync_work"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting visit synchronization...")

        return try {
            val unsyncedVisits = visitDao.getUnsyncedVisits()
            Log.d(TAG, "📝 Found ${unsyncedVisits.size} unsynced visits")

            var successCount = 0
            var failCount = 0

            for (visit in unsyncedVisits) {
                try {
                    when (visit.syncAction) {
                        "UPDATE" -> {
                            val request = VisitStatusUpdateRequest(visit.status)
                            val response = apiService.updateVisitStatus(visit.id, request)

                            if (response.isSuccessful) {
                                visitDao.markAsSynced(visit.id)
                                successCount++
                                Log.d(TAG, "Synced visit ${visit.id}")
                            } else {
                                visitDao.updateLastSyncAttempt(visit.id, Date())
                                failCount++
                                Log.w(TAG, "Failed to sync visit ${visit.id}: ${response.code()}")
                            }
                        }
                        "CREATE" -> {
                            Log.d(TAG, "CREATE sync not implemented yet for ${visit.id}")
                        }
                        "DELETE" -> {
                            Log.d(TAG, "DELETE sync not implemented yet for ${visit.id}")
                        }
                        else -> {
                            Log.w(TAG, "Unknown sync action: ${visit.syncAction} for ${visit.id}")
                        }
                    }
                } catch (e: Exception) {
                    visitDao.updateLastSyncAttempt(visit.id, Date())
                    failCount++
                    Log.e(TAG, "Error syncing visit ${visit.id}: ${e.message}")
                }
            }

            Log.d(TAG, "Sync completed: $successCount success, $failCount failed")

            if (failCount == 0) {
                Result.success()
            } else if (successCount > 0) {
                Result.retry()
            } else {
                Result.failure()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Sync worker failed: ${e.message}", e)
            Result.failure()
        }
    }
}