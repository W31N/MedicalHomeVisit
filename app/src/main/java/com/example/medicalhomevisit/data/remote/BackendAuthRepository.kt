package com.example.medicalhomevisit.data.remote

import android.util.Log
import com.example.medicalhomevisit.data.model.User
import com.example.medicalhomevisit.data.model.UserRole
import com.example.medicalhomevisit.data.remote.dtos.LoginRequestDto
import com.example.medicalhomevisit.data.remote.dtos.PatientSelfRegisterRequestDto
import com.example.medicalhomevisit.data.remote.dtos.UserDto
import com.example.medicalhomevisit.data.remote.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class BackendAuthRepository(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    private val _currentUserFlow = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUserFlow.asStateFlow()

    init {
        // При инициализации проверяем, есть ли сохраненный токен,
        // и пытаемся загрузить данные пользователя.
        // Это можно сделать в ViewModel при старте приложения.
        // Здесь просто инициализируем _currentUserFlow на основе сохраненного токена
        // (без запроса к /me для простоты начальной настройки)
        // Более полная реализация должна бы запросить /api/users/me
        if (isLoggedIn()) {
            // В реальном приложении здесь стоит загрузить данные пользователя с сервера
            // или из сохраненных локально после предыдущего входа.
            // Для примера, если бы мы сохраняли UserDto:
            // val savedUserDto = tokenManager.getUserInfo() // если бы такой метод был
            // _currentUserFlow.value = savedUserDto?.toDomainUser()
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
                    // tokenManager.saveUserInfo(user) // Опционально, если сохраняете UserDto
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
                    val loginResponse = response.body()!! // Теперь это LoginResponseDto
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
                // Опционально: если ваш бэкенд имеет эндпоинт /logout
                // authApiService.logout()
            } catch (e: Exception) {
                Log.w("BackendAuthRepo", "Error calling backend logout (optional)", e)
            }
            tokenManager.clearToken()
            _currentUserFlow.value = null
        }
    }

    override fun isLoggedIn(): Boolean {
        return tokenManager.getToken() != null
        // Для более надежной проверки можно декодировать токен и проверять срок его действия
        // (но не для проверки подлинности, а для UI/UX).
    }

    // Этот метод может быть вызван, например, при старте приложения, если токен есть
    override suspend fun fetchAndUpdateCurrentUser(): User? {
        if (!isLoggedIn()) {
            _currentUserFlow.value = null
            return null
        }
        // TODO: Реализовать эндпоинт на бэкенде типа GET /api/users/me или /api/auth/profile
        // который по валидному токену вернет UserDto
        // Пример:
        // return try {
        //     val response = authApiService.getCurrentUserProfile() // Предполагаемый новый метод в AuthApiService
        //     if (response.isSuccessful && response.body() != null) {
        //         val user = response.body()!!.toDomainUser()
        //         _currentUserFlow.value = user
        //         user
        //     } else {
        //         tokenManager.clearToken() // Токен невалиден
        //         _currentUserFlow.value = null
        //         null
        //     }
        // } catch (e: Exception) {
        //     Log.e("BackendAuthRepo", "Error fetching current user profile", e)
        //     tokenManager.clearToken()
        //     _currentUserFlow.value = null
        //     null
        // }
        Log.w("BackendAuthRepo", "fetchAndUpdateCurrentUser: Not implemented yet. Returning current flow value.")
        return _currentUserFlow.value // Пока возвращаем то, что есть
    }
}

// Функция-расширение для маппинга DTO в доменную модель User
// Ее лучше вынести в отдельный файл-маппер или в компаньон объекта User
fun UserDto.toDomainUser(): User {
    return User(
        id = this.id,
        email = this.email,
        displayName = this.displayName,
        role = try {
            UserRole.valueOf(this.role.uppercase())
        } catch (e: IllegalArgumentException) {
            Log.w("UserMapper", "Unknown role from server: ${this.role}, defaulting to PATIENT")
            UserRole.PATIENT // Роль по умолчанию или обработка ошибки
        },
        isEmailVerified = this.emailVerified
    )
}