package com.example.medicalhomevisit.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.medicalhomevisit.domain.repository.ProtocolTemplateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val protocolTemplateRepository: ProtocolTemplateRepository
) {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val TAG = "SyncManager"
    }

    fun syncNow() {
        Log.d(TAG, "Starting immediate sync for all data...")
        syncVisitsNow()
        syncProtocolsNow()
        syncPatientsNow()
        syncTemplatesNow()
    }

    private fun syncTemplatesNow() {
        Log.d(TAG, "Starting immediate template sync...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = protocolTemplateRepository.refreshTemplates()
                if (result.isSuccess) {
                    Log.d(TAG, "Templates synced successfully")
                } else {
                    Log.w(TAG, "Templates sync failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Templates sync error: ${e.message}", e)
            }
        }
    }

    fun setupPeriodicSync() {
        Log.d(TAG, "Setting up periodic sync for all data...")
        setupPeriodicVisitSync()
        setupPeriodicProtocolSync()
        setupPeriodicPatientSync()
    }

    private fun syncVisitsNow() {
        Log.d(TAG, "Starting immediate visit sync...")

        val syncRequest = OneTimeWorkRequestBuilder<VisitSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            VisitSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    private fun setupPeriodicVisitSync() {
        Log.d(TAG, "Setting up periodic visit sync...")

        val periodicSyncRequest = PeriodicWorkRequestBuilder<VisitSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "${VisitSyncWorker.WORK_NAME}_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }

    fun syncProtocolsNow() {
        Log.d(TAG, "Starting immediate protocol sync...")

        val syncRequest = OneTimeWorkRequestBuilder<ProtocolSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            ProtocolSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    private fun setupPeriodicProtocolSync() {
        Log.d(TAG, "Setting up periodic protocol sync...")

        val periodicSyncRequest = PeriodicWorkRequestBuilder<ProtocolSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "${ProtocolSyncWorker.WORK_NAME}_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }

    private fun syncPatientsNow() {
        Log.d(TAG, "Starting immediate patient sync...")

        val syncRequest = OneTimeWorkRequestBuilder<PatientSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            PatientSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    private fun setupPeriodicPatientSync() {
        Log.d(TAG, "Setting up periodic patient sync...")

        val periodicSyncRequest = PeriodicWorkRequestBuilder<PatientSyncWorker>(
            30, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "${PatientSyncWorker.WORK_NAME}_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }
}