package com.example.medicalhomevisit

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.medicalhomevisit.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MedicalApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory  // ← ЭТО НУЖНО ДЛЯ СИНХРОНИЗАЦИИ!

    @Inject
    lateinit var syncManager: SyncManager

    companion object {
        private const val TAG = "MedicalApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Medical Home Visit App starting...")

        // Инициализируем WorkManager для фоновой синхронизации
        initializeWorkManager()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)  // ← БЕЗ ЭТОГО HILT НЕ РАБОТАЕТ В WORKER!
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
    }

    private fun initializeWorkManager() {
        try {
            syncManager.setupPeriodicSync()
            Log.d(TAG, "✅ WorkManager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize WorkManager: ${e.message}", e)
        }
    }
}