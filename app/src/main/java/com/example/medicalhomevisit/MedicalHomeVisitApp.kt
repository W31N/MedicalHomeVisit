package com.example.medicalhomevisit

import android.app.Application
import android.util.Log
import com.example.medicalhomevisit.data.remote.NetworkModule
import com.example.medicalhomevisit.data.repository.FirebaseAuthRepository
import com.example.medicalhomevisit.domain.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@HiltAndroidApp
class MedicalHomeVisitApp : Application() {
    private lateinit var authRepository: AuthRepository

    override fun onCreate() {
        super.onCreate()



//        NetworkModule.initialize(applicationContext)



        // Инициализация Firebase
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        Firebase.firestore.firestoreSettings = settings

        // Инициализация репозитория аутентификации
        authRepository = FirebaseAuthRepository()

        // Отслеживание состояния аутентификации
        CoroutineScope(Dispatchers.Main).launch {
            authRepository.currentUser.collect { user ->
                // Можно добавить логику для обработки изменений состояния аутентификации
                Log.d("MedicalHomeVisitApp", "Auth state changed: ${user != null}")
            }
        }
    }
}