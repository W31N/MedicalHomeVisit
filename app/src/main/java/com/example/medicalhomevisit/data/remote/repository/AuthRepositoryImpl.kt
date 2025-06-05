package com.example.medicalhomevisit.data.remote.repository

import android.util.Log
import com.example.medicalhomevisit.data.remote.api.AuthApiService
import com.example.medicalhomevisit.domain.model.User
import com.example.medicalhomevisit.domain.model.UserRole
import com.example.medicalhomevisit.data.remote.dto.LoginRequestDto
import com.example.medicalhomevisit.data.remote.dto.PatientSelfRegisterRequestDto
import com.example.medicalhomevisit.data.remote.dto.UserDto
import com.example.medicalhomevisit.data.remote.network.TokenManager
import com.example.medicalhomevisit.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    private val _currentUserFlow = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUserFlow.asStateFlow()

    init {
        if (isLoggedIn()) {
            Log.d("BackendAuthRepo", "User might be logged in (token exists). ViewModel should verify.")
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApiService.login(LoginRequestDto(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    tokenManager.saveToken(loginResponse.token)
                    val user = loginResponse.user.toDomainUser()
                    _currentUserFlow.value = user
                    Result.success(user)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Ошибка входа: ${response.code()}"
                    Log.e("BackendAuthRepo", "SignIn error: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("BackendAuthRepo", "SignIn exception", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun signUp(fullName: String, email: String, password: String, confirmPassword: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val request = PatientSelfRegisterRequestDto(fullName, email, password, confirmPassword)
                Log.d("BackendAuthRepo", "Sending registration request for email: $email")
                val response = authApiService.register(request)
                Log.d("BackendAuthRepo", "Registration response code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    Log.d("BackendAuthRepo", "Registration successful, received token: ${loginResponse.token.take(20)}...")
                    tokenManager.saveToken(loginResponse.token)
                    Log.d("BackendAuthRepo", "Token saved to TokenManager")
                    val user = loginResponse.user.toDomainUser()
                    Log.d("BackendAuthRepo", "User converted: ${user.email}, role: ${user.role}")
                    _currentUserFlow.value = user
                    Log.d("BackendAuthRepo", "Current user flow updated")
                    Result.success(user)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Ошибка регистрации: ${response.code()}"
                    Log.e("BackendAuthRepo", "SignUp error: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("BackendAuthRepo", "SignUp exception", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                 authApiService.logout()
            } catch (e: Exception) {
                Log.w("BackendAuthRepo", "Error calling backend logout (optional)", e)
            }
            tokenManager.clearToken()
            _currentUserFlow.value = null
        }
    }

    override fun isLoggedIn(): Boolean {
        return tokenManager.getToken() != null
    }

    override suspend fun fetchAndUpdateCurrentUser(): User? {
        if (!isLoggedIn()) {
            _currentUserFlow.value = null
            return null
        }
        Log.w("BackendAuthRepo", "fetchAndUpdateCurrentUser: Not implemented yet. Returning current flow value.")
        return _currentUserFlow.value
    }
}

fun UserDto.toDomainUser(): User {
    return User(
        id = this.id,
        email = this.email,
        displayName = this.displayName,
        role = try {
            UserRole.valueOf(this.role.uppercase())
        } catch (e: IllegalArgumentException) {
            Log.w("UserMapper", "Unknown role from server: ${this.role}, defaulting to PATIENT")
            UserRole.PATIENT
        },
        isEmailVerified = this.emailVerified,
        medicalPersonId = this.medicalPersonId
    )
}