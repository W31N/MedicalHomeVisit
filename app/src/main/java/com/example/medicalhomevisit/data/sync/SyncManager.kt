package com.example.medicalhomevisit.data.sync

import android.content.Context
import android.util.Log
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

    // Запускаем единоразовую синхронизацию
    fun syncNow() {
        Log.d(TAG, "🚀 Starting immediate sync...")

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

    // Настраиваем периодическую синхронизацию
    fun setupPeriodicSync() {
        Log.d(TAG, "⚙️ Setting up periodic sync...")

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

    // Отменяем все задачи синхронизации
    fun cancelSync() {
        Log.d(TAG, "❌ Cancelling all sync work...")
        workManager.cancelUniqueWork(VisitSyncWorker.WORK_NAME)
        workManager.cancelUniqueWork("${VisitSyncWorker.WORK_NAME}_periodic")
    }
}