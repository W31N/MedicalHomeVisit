// com/example/medicalhomevisit/data/repository/FirebaseAuthRepository.kt
package com.example.medicalhomevisit.data.repository

import android.util.Log
import com.example.medicalhomevisit.data.model.User
import com.example.medicalhomevisit.data.model.UserRole
import com.example.medicalhomevisit.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository : AuthRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val usersCollection = Firebase.firestore.collection("users")

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.let { firebaseUser ->
                User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    isEmailVerified = firebaseUser.isEmailVerified
                )
            })
        }

        auth.addAuthStateListener(listener)

        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Unknown error occurred during sign in"))

            // Попытка получить дополнительные данные о пользователе из Firestore
            try {
                val userDoc = usersCollection.document(firebaseUser.uid).get().await()
                val role = if (userDoc.exists()) {
                    val roleStr = userDoc.getString("role") ?: "MEDICAL_STAFF"
                    UserRole.valueOf(roleStr)
                } else {
                    // Создаем запись пользователя, если его нет в базе
                    val defaultUserData = hashMapOf(
                        "email" to (firebaseUser.email ?: ""),
                        "displayName" to (firebaseUser.displayName ?: ""),
                        "role" to UserRole.MEDICAL_STAFF.name
                    )
                    usersCollection.document(firebaseUser.uid).set(defaultUserData).await()
                    UserRole.MEDICAL_STAFF
                }

                Result.success(User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    role = role,
                    isEmailVerified = firebaseUser.isEmailVerified
                ))
            } catch (e: Exception) {
                // Если не удалось получить дополнительные данные, возвращаем базовые
                Log.e(TAG, "Error fetching user data from Firestore", e)
                Result.success(User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    isEmailVerified = firebaseUser.isEmailVerified
                ))
            }
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Sign in error: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String, displayName: String, role: UserRole): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Unknown error occurred during sign up"))

            // Установка отображаемого имени
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()

            firebaseUser.updateProfile(profileUpdates).await()

            // Сохранение дополнительных данных в Firestore
            val userData = hashMapOf(
                "email" to email,
                "displayName" to displayName,
                "role" to role.name // Используем предоставленную роль (по умолчанию PATIENT)
            )

            usersCollection.document(firebaseUser.uid).set(userData).await()

            Result.success(User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = displayName,
                role = role,
                isEmailVerified = firebaseUser.isEmailVerified
            ))
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Sign up error: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Password reset error: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(displayName: String): Result<Unit> {
        val firebaseUser = auth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        return try {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()

            firebaseUser.updateProfile(profileUpdates).await()

            // Обновление в Firestore
            usersCollection.document(firebaseUser.uid)
                .update("displayName", displayName)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Profile update error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getUser(): User? {
        val firebaseUser = auth.currentUser ?: return null

        return try {
            val userDoc = usersCollection.document(firebaseUser.uid).get().await()
            val role = if (userDoc.exists()) {
                val roleStr = userDoc.getString("role") ?: "MEDICAL_STAFF"
                UserRole.valueOf(roleStr)
            } else {
                UserRole.MEDICAL_STAFF
            }

            User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                role = role,
                isEmailVerified = firebaseUser.isEmailVerified
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user: ${e.message}", e)
            null
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    companion object {
        private const val TAG = "FirebaseAuthRepo"
    }
}