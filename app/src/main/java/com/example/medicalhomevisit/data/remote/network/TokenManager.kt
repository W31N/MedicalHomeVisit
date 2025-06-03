package com.example.medicalhomevisit.data.remote.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class TokenManager(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "auth_prefs",
        masterKeyAlias,
        context.applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val JWT_TOKEN_KEY = "jwt_token"
        // Можно добавить ключ для хранения UserDto или его частей, если нужно
        // private const val USER_EMAIL_KEY = "user_email"
        // private const val USER_DISPLAY_NAME_KEY = "user_display_name"
        // private const val USER_ROLE_KEY = "user_role"
        // private const val USER_ID_KEY = "user_id"
    }

    fun saveToken(token: String) {
        sharedPreferences.edit().putString(JWT_TOKEN_KEY, token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(JWT_TOKEN_KEY, null)
    }

    fun clearToken() {
        sharedPreferences.edit().remove(JWT_TOKEN_KEY).apply()
        // Также очистите сохраненные данные пользователя, если они есть
        // sharedPreferences.edit().remove(USER_ID_KEY).apply()
        // sharedPreferences.edit().remove(USER_EMAIL_KEY).apply()
        // ...
    }

    // Опционально: методы для сохранения/получения базовой информации о пользователе
    // fun saveUserInfo(user: com.example.medicalhomevisit.data.model.User) {
    //     sharedPreferences.edit()
    //         .putString(USER_ID_KEY, user.id)
    //         .putString(USER_EMAIL_KEY, user.email)
    //         .putString(USER_DISPLAY_NAME_KEY, user.displayName)
    //         .putString(USER_ROLE_KEY, user.role.name)
    //         .apply()
    // }

    // fun getUserInfo(): com.example.medicalhomevisit.data.model.User? {
    //     val id = sharedPreferences.getString(USER_ID_KEY, null) ?: return null
    //     val email = sharedPreferences.getString(USER_EMAIL_KEY, null) ?: return null
    //     val displayName = sharedPreferences.getString(USER_DISPLAY_NAME_KEY, null) ?: ""
    //     val roleString = sharedPreferences.getString(USER_ROLE_KEY, null) ?: return null
    //     return try {
    //         com.example.medicalhomevisit.data.model.User(
    //             id = id,
    //             email = email,
    //             displayName = displayName,
    //             role = com.example.medicalhomevisit.data.model.UserRole.valueOf(roleString)
    //         )
    //     } catch (e: IllegalArgumentException) {
    //         null // Если роль не может быть преобразована
    //     }
    // }
}