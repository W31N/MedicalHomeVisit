package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(fullName: String, email: String, password: String, confirmPassword: String): Result<User>
    suspend fun signOut()
    fun isLoggedIn(): Boolean
}