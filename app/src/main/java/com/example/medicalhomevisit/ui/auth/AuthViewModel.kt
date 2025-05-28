// com/example/medicalhomevisit/ui/auth/AuthViewModel.kt
package com.example.medicalhomevisit.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.data.model.User
import com.example.medicalhomevisit.data.model.UserRole
import com.example.medicalhomevisit.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException // Импорт для конкретного исключения
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    init {
        Log.d("AuthViewModel", "Initializing AuthViewModel...")
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _user.value = user
                if (user != null) {
                    _uiState.value = AuthUiState.LoggedIn(user)
                    Log.d("AuthViewModel", "currentUser collected: LoggedIn, User: ${user.id}, Role: ${user.role}")
                } else {
                    if (_uiState.value !is AuthUiState.Initial && _uiState.value !is AuthUiState.NotLoggedIn) {
                        _uiState.value = AuthUiState.NotLoggedIn
                        Log.d("AuthViewModel", "currentUser collected: NotLoggedIn (user is null)")
                    } else if (_uiState.value is AuthUiState.Initial) {
                        Log.d("AuthViewModel", "currentUser collected: user is null, UI state is Initial")
                    }
                }
            }
        }

        viewModelScope.launch {
            val isLoggedIn = authRepository.isLoggedIn()
            Log.d("AuthViewModel", "isLoggedIn check: $isLoggedIn")
            if (isLoggedIn) {
                val user = authRepository.getUser()
                if (user != null) {
                    _uiState.value = AuthUiState.LoggedIn(user)
                    Log.d("AuthViewModel", "getUser check: LoggedIn, User: ${user.id}, Role: ${user.role}")
                } else {
                    _uiState.value = AuthUiState.NotLoggedIn // Если залогинен, но данные пользователя не получены
                    Log.w("AuthViewModel", "isLoggedIn is true, but getUser() returned null. Setting NotLoggedIn.")
                }
            } else {
                // Если пользователь не залогинен и uiState все еще Initial, можно установить NotLoggedIn
                if (_uiState.value is AuthUiState.Initial) {
                    _uiState.value = AuthUiState.NotLoggedIn
                    Log.d("AuthViewModel", "isLoggedIn is false, setting NotLoggedIn.")
                }
            }
        }
    }

    fun getCurrentUserRole(): UserRole? {
        return _user.value?.role
    }

    fun signIn(email: String, password: String) {
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val result = authRepository.signIn(email, password)

                if (result.isSuccess) {
                    _uiState.value = AuthUiState.LoggedIn(result.getOrNull()!!)
                } else {
                    val exception = result.exceptionOrNull()
                    _uiState.value = AuthUiState.Error(
                        getLocalizedErrorMessage(exception)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(getLocalizedErrorMessage(e))
                Log.e(TAG, "Sign in error", e)
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                // По умолчанию устанавливаем роль PATIENT
                val result = authRepository.signUp(email, password, displayName, UserRole.PATIENT)

                if (result.isSuccess) {
                    _uiState.value = AuthUiState.LoggedIn(result.getOrNull()!!)
                } else {
                    val exception = result.exceptionOrNull()
                    _uiState.value = AuthUiState.Error(
                        getLocalizedErrorMessage(exception)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(getLocalizedErrorMessage(e))
                Log.e(TAG, "Sign up error", e)
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            try {
                val result = authRepository.resetPassword(email)

                if (result.isSuccess) {
                    _uiState.value = AuthUiState.PasswordResetSent
                } else {
                    val exception = result.exceptionOrNull()
                    _uiState.value = AuthUiState.Error(
                        getLocalizedErrorMessage(exception)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(getLocalizedErrorMessage(e))
                Log.e(TAG, "Reset password error", e)
            }
        }
    }


    fun signOut() {
        viewModelScope.launch {
            try {
                val result = authRepository.signOut()

                if (result.isSuccess) {
                    _uiState.value = AuthUiState.Initial
                } else {
                    val exception = result.exceptionOrNull()
                    _uiState.value = AuthUiState.Error(
                        exception?.message ?: "Ошибка при выходе"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Неизвестная ошибка")
                Log.e(TAG, "Sign out error", e)
            }
        }
    }

    fun resetError() {
        // Сбрасываем состояние ошибки или состояние сброса пароля
        if (_uiState.value is AuthUiState.Error || _uiState.value is AuthUiState.PasswordResetSent) {
            _uiState.value = AuthUiState.Initial
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}

private fun getLocalizedErrorMessage(throwable: Throwable?): String {
    if (throwable == null) return "Неизвестная ошибка"

    return when {
        // Обработка конкретных случаев Firebase Auth
        throwable is FirebaseAuthInvalidCredentialsException -> {
            // Проверяем код ошибки или сообщение
            when {
                throwable.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                    "Неверный email или пароль"
                throwable.message?.contains("The email address is badly formatted") == true ->
                    "Неверный формат email"
                throwable.message?.contains("The password is invalid") == true ->
                    "Неверный пароль"
                else -> "Ошибка авторизации: ${throwable.localizedMessage}"
            }
        }
        // Можно добавить другие типы ошибок Firebase Auth
        throwable.message?.contains("There is no user record") == true ->
            "Пользователь с таким email не найден"
        throwable.message?.contains("The email address is already in use") == true ->
            "Email уже используется другим пользователем"
        throwable.message?.contains("Password should be at least 6 characters") == true ->
            "Пароль должен содержать не менее 6 символов"
        throwable.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
            "Неверный email или пароль"
        else -> throwable.localizedMessage ?: "Неизвестная ошибка"
    }
}


sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class LoggedIn(val user: User) : AuthUiState()
    object NotLoggedIn : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    object PasswordResetSent : AuthUiState()
}