package com.example.medicalhomevisit.domain.repository

import com.example.medicalhomevisit.data.model.User
import com.example.medicalhomevisit.data.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>

    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String, displayName: String, role: UserRole = UserRole.PATIENT): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun updateProfile(displayName: String): Result<Unit>
    suspend fun getUser(): User?
    suspend fun isLoggedIn(): Boolean
}