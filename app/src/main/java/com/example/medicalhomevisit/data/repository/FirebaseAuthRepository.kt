// com/example/medicalhomevisit/data/repository/FirebaseAuthRepository.kt
package com.example.medicalhomevisit.data.repository

import android.util.Log
import com.example.medicalhomevisit.data.model.Gender
import com.example.medicalhomevisit.data.model.User
import com.example.medicalhomevisit.data.model.UserRole
import com.example.medicalhomevisit.domain.repository.AuthRepository
import com.google.firebase.Timestamp
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
import java.util.Date

class FirebaseAuthRepository : AuthRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val usersCollection = Firebase.firestore.collection("users")
    private val patientsCollection = Firebase.firestore.collection("patients") // Добавьте ссылку на коллекцию пациентов

    // В FirebaseAuthRepository.kt
    override val currentUser: Flow<User?> = callbackFlow {
        Log.d(TAG, "currentUser Flow: Listener attached.")
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                Log.d(TAG, "currentUser Flow: AuthState changed, firebaseUser is NOT null. UID: ${firebaseUser.uid}")
                usersCollection.document(firebaseUser.uid).get()
                    .addOnSuccessListener { documentSnapshot ->
                        val role = if (documentSnapshot.exists()) {
                            val roleStr = documentSnapshot.getString("role")
                            Log.d(TAG, "currentUser Flow: User doc exists. Role string from Firestore: $roleStr")
                            try { UserRole.valueOf(roleStr ?: UserRole.PATIENT.name) }
                            catch (e: IllegalArgumentException) {
                                Log.w(TAG, "currentUser Flow: Invalid role string '$roleStr', defaulting to PATIENT.")
                                UserRole.PATIENT
                            }
                        } else {
                            Log.w(TAG, "currentUser Flow: User document NOT found in Firestore for UID: ${firebaseUser.uid}. Defaulting role to PATIENT.")
                            UserRole.PATIENT
                        }
                        val userToSend = User(
                            id = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            displayName = firebaseUser.displayName ?: "",
                            role = role,
                            isEmailVerified = firebaseUser.isEmailVerified
                        )
                        Log.d(TAG, "currentUser Flow: Trying to send user: $userToSend")
                        val offerResult = trySend(userToSend)
                        Log.d(TAG, "currentUser Flow: trySend(user) result: ${offerResult.isSuccess}")

                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "currentUser Flow: Error fetching user role from Firestore for UID: ${firebaseUser.uid}", exception)
                        val errorUser = User(
                            id = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            displayName = firebaseUser.displayName ?: "",
                            role = UserRole.PATIENT, // Дефолтная роль при ошибке
                            isEmailVerified = firebaseUser.isEmailVerified
                        )
                        Log.d(TAG, "currentUser Flow: Trying to send user (on failure): $errorUser")
                        val offerResult = trySend(errorUser)
                        Log.d(TAG, "currentUser Flow: trySend(user on failure) result: ${offerResult.isSuccess}")
                    }
            } else {
                Log.d(TAG, "currentUser Flow: AuthState changed, firebaseUser is NULL.")
                val offerResult = trySend(null)
                Log.d(TAG, "currentUser Flow: trySend(null) result: ${offerResult.isSuccess}")
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            Log.d(TAG, "currentUser Flow: Listener removed.")
            auth.removeAuthStateListener(listener)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Неизвестная ошибка при входе"))

            val userDoc = usersCollection.document(firebaseUser.uid).get().await()
            val role = if (userDoc.exists()) {
                UserRole.valueOf(userDoc.getString("role") ?: UserRole.MEDICAL_STAFF.name)
            } else {
                // Этого не должно происходить при входе, т.к. пользователь уже должен быть в users
                Log.w(TAG, "User document not found in Firestore for UID: ${firebaseUser.uid} during sign in. Defaulting role.")
                UserRole.MEDICAL_STAFF // Или другая логика по умолчанию
            }

            Result.success(User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                role = role,
                isEmailVerified = firebaseUser.isEmailVerified
            ))
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
                ?: return Result.failure(Exception("Произошла неизвестная ошибка при регистрации"))

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val userData = hashMapOf(
                "email" to email,
                "displayName" to displayName,
                "role" to role.name
            )
            usersCollection.document(firebaseUser.uid).set(userData).await()

            if (role == UserRole.PATIENT) {
                Log.d(TAG, "User role is PATIENT, creating patient record in 'patients' collection.")
                val patientData = hashMapOf(
                    "fullName" to displayName,
                    // dateOfBirth и gender НЕ устанавливаем здесь, если они не приходят как параметры
                    // Они будут null или UNKNOWN по умолчанию в модели Patient,
                    // а в Firestore эти поля просто не будут созданы или будут null
                    "dateOfBirth" to null, // Явно null, если не передается
                    "gender" to Gender.UNKNOWN.name, // Явно UNKNOWN, если не передается
                    "address" to "Адрес не указан", // Или пустая строка, если это предпочтительнее
                    "phoneNumber" to "Телефон не указан", // Или пустая строка
                    "policyNumber" to "",
                    "allergies" to emptyList<String>(),
                    "chronicConditions" to emptyList<String>()
                )
                try {
                    patientsCollection.document(firebaseUser.uid).set(patientData).await()
                    Log.d(TAG, "Patient record created successfully for UID: ${firebaseUser.uid}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating patient record in 'patients' collection: ${e.message}", e)
                }
            }

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