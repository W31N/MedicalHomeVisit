package com.example.medicalhomevisit.data.sync

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val TAG = "SyncManager"
    }

    // ===== УНИВЕРСАЛЬНАЯ СИНХРОНИЗАЦИЯ =====

    fun syncNow() {
        Log.d(TAG, "🚀 Starting immediate sync for all data...")
        syncVisitsNow()
        syncProtocolsNow()
    }

    fun setupPeriodicSync() {
        Log.d(TAG, "⚙️ Setting up periodic sync for all data...")
        setupPeriodicVisitSync()
        setupPeriodicProtocolSync()
    }

    fun cancelAllSync() {
        Log.d(TAG, "❌ Cancelling all sync work...")
        cancelVisitSync()
        cancelProtocolSync()
    }

    // ===== СИНХРОНИЗАЦИЯ ВИЗИТОВ =====

    fun syncVisitsNow() {
        Log.d(TAG, "🚀 Starting immediate visit sync...")

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

    fun setupPeriodicVisitSync() {
        Log.d(TAG, "⚙️ Setting up periodic visit sync...")

        val periodicSyncRequest = PeriodicWorkRequestBuilder<VisitSyncWorker>(
            15, TimeUnit.MINUTES // Каждые 15 минут
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

    fun cancelVisitSync() {
        Log.d(TAG, "❌ Cancelling visit sync work...")
        workManager.cancelUniqueWork(VisitSyncWorker.WORK_NAME)
        workManager.cancelUniqueWork("${VisitSyncWorker.WORK_NAME}_periodic")
    }

    // ===== СИНХРОНИЗАЦИЯ ПРОТОКОЛОВ =====

    fun syncProtocolsNow() {
        Log.d(TAG, "🚀 Starting immediate protocol sync...")

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

    fun setupPeriodicProtocolSync() {
        Log.d(TAG, "⚙️ Setting up periodic protocol sync...")

        val periodicSyncRequest = PeriodicWorkRequestBuilder<ProtocolSyncWorker>(
            15, TimeUnit.MINUTES // Каждые 15 минут
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

    fun cancelProtocolSync() {
        Log.d(TAG, "❌ Cancelling protocol sync work...")
        workManager.cancelUniqueWork(ProtocolSyncWorker.WORK_NAME)
        workManager.cancelUniqueWork("${ProtocolSyncWorker.WORK_NAME}_periodic")
    }

    // ===== СОСТОЯНИЕ СИНХРОНИЗАЦИИ =====

    fun getSyncStatus(): LiveData<List<WorkInfo>> {
        return workManager.getWorkInfosByTagLiveData("sync")
    }

    fun isSyncing(): Boolean {
        val visitWorkInfos = workManager.getWorkInfosForUniqueWork(VisitSyncWorker.WORK_NAME)
        val protocolWorkInfos = workManager.getWorkInfosForUniqueWork(ProtocolSyncWorker.WORK_NAME)

        return try {
            val visitSyncing = visitWorkInfos.get().any { it.state == WorkInfo.State.RUNNING }
            val protocolSyncing = protocolWorkInfos.get().any { it.state == WorkInfo.State.RUNNING }
            visitSyncing || protocolSyncing
        } catch (e: Exception) {
            Log.w(TAG, "Error checking sync status: ${e.message}")
            false
        }
    }
}