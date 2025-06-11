package com.example.medicalhomevisit.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.domain.model.User
import com.example.medicalhomevisit.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
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
        Log.d("AuthViewModel", "===== INITIALIZING AuthViewModel =====")
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting to collect currentUser from repository")
            authRepository.currentUser.collect { userValue ->
                Log.d("AuthViewModel", "currentUser.collect triggered with userValue: $userValue")
                _user.value = userValue
                Log.d("AuthViewModel", "_user.value updated to: ${_user.value}")
                if (userValue != null) {
                    val newState = AuthUiState.LoggedIn(userValue)
                    _uiState.value = newState
                    Log.d("AuthViewModel", "_uiState.value set to LoggedIn: $newState")
                    Log.d("AuthViewModel", "User: ${userValue.id}, Role: ${userValue.role}, Email: ${userValue.email}")
                } else {
                    if (_uiState.value !is AuthUiState.NotLoggedIn && _uiState.value !is AuthUiState.Initial) {
                        _uiState.value = AuthUiState.NotLoggedIn
                        Log.d("AuthViewModel", "_uiState.value set to NotLoggedIn (userValue is null)")
                    } else if (_uiState.value is AuthUiState.Initial) {
                        _uiState.value = AuthUiState.NotLoggedIn
                        Log.d("AuthViewModel", "_uiState.value set to NotLoggedIn (was Initial)")
                    }
                }
                Log.d("AuthViewModel", "Final state after collect: ${_uiState.value}")
            }
        }

        viewModelScope.launch {
            val isLoggedIn = authRepository.isLoggedIn()
            Log.d("AuthViewModel", "Initial isLoggedIn check: $isLoggedIn")

            if (isLoggedIn) {
                Log.d("AuthViewModel", "isLoggedIn is true, waiting for currentUser Flow to provide user")
            } else {
                Log.d("AuthViewModel", "isLoggedIn is false")
                if (_uiState.value is AuthUiState.Initial) {
                    _uiState.value = AuthUiState.NotLoggedIn
                    Log.d("AuthViewModel", "Set initial state to NotLoggedIn")
                }
            }
        }

        Log.d("AuthViewModel", "===== AuthViewModel INIT COMPLETE =====")
    }

    fun signIn(email: String, password: String) {
        Log.d(TAG, "===== SIGN IN STARTED =====")
        Log.d(TAG, "signIn called with email: $email")

        val oldState = _uiState.value
        _uiState.value = AuthUiState.Loading
        Log.d(TAG, "UI state changed from $oldState to Loading")

        viewModelScope.launch {
            try {
                Log.d(TAG, "Calling authRepository.signIn...")
                val result = authRepository.signIn(email, password)
                Log.d(TAG, "authRepository.signIn completed")
                Log.d(TAG, "result.isSuccess: ${result.isSuccess}")

                if (result.isSuccess) {
                    val user = result.getOrNull()!!
                    Log.d(TAG, "Sign in SUCCESS! User: ${user.email}, Role: ${user.role}, ID: ${user.id}")

                    val newState = AuthUiState.LoggedIn(user)
                    _uiState.value = newState
                    Log.d(TAG, "_uiState.value set to: $newState")

                    Log.d(TAG, "Current _uiState.value after setting: ${_uiState.value}")
                    Log.d(TAG, "Current _user.value: ${_user.value}")

                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Sign in FAILED with exception: $exception")
                    _uiState.value = AuthUiState.Error(getLocalizedErrorMessage(exception))
                    Log.d(TAG, "_uiState.value set to Error: ${_uiState.value}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign in EXCEPTION caught: $e", e)
                _uiState.value = AuthUiState.Error(getLocalizedErrorMessage(e))
            }
        }

        Log.d(TAG, "===== SIGN IN METHOD END =====")
    }

    fun signUp(displayName: String, email: String, password: String, confirmPassword: String) {
        Log.d(TAG, "===== SIGN UP STARTED =====")
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting sign up for email: $email")
                val result = authRepository.signUp(displayName, email, password, confirmPassword)

                if (result.isSuccess) {
                    val user = result.getOrNull()!!
                    Log.d(TAG, "Sign up SUCCESS! User: ${user.email}, Role: ${user.role}")

                    _uiState.value = AuthUiState.LoggedIn(user)
                    Log.d(TAG, "UI state set to LoggedIn after signup")
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Sign up FAILED: ${exception?.message}")
                    _uiState.value = AuthUiState.Error(getLocalizedErrorMessage(exception))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign up EXCEPTION: $e", e)
                _uiState.value = AuthUiState.Error(getLocalizedErrorMessage(e))
            }
        }
    }

    fun signOut() {
        Log.d(TAG, "===== SIGN OUT STARTED =====")
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _uiState.value = AuthUiState.Initial
                Log.d(TAG, "Sign out successful, state set to Initial")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Неизвестная ошибка при выходе")
                Log.e(TAG, "Sign out error", e)
            }
        }
    }

    fun resetError() {
        Log.d(TAG, "resetError called, current state: ${_uiState.value}")
        if (_uiState.value is AuthUiState.Error || _uiState.value is AuthUiState.PasswordResetSent) {
            _uiState.value = AuthUiState.Initial
            Log.d(TAG, "Error state reset to Initial")
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}

private fun getLocalizedErrorMessage(throwable: Throwable?): String {
    if (throwable == null) return "Неизвестная ошибка"

    return when {
        throwable is FirebaseAuthInvalidCredentialsException -> {
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
    data class RegistrationSuccessful(val user: User) : AuthUiState()
}