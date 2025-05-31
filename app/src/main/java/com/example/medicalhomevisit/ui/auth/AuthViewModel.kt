// com/example/medicalhomevisit/ui/auth/AuthViewModel.kt
package com.example.medicalhomevisit.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.data.model.User
import com.example.medicalhomevisit.data.model.UserRole
import com.example.medicalhomevisit.data.remote.AuthRepository
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
            authRepository.currentUser.collect { userValue -> // Переименовал user в userValue во избежание путаницы
                _user.value = userValue
                if (userValue != null) {
                    _uiState.value = AuthUiState.LoggedIn(userValue)
                    Log.d("AuthViewModel", "currentUser collected: LoggedIn, User: ${userValue.id}, Role: ${userValue.role}")
                } else {
                    // Эта логика может быть упрощена, если _uiState уже NotLoggedIn
                    if (_uiState.value !is AuthUiState.NotLoggedIn && _uiState.value !is AuthUiState.Initial) {
                        _uiState.value = AuthUiState.NotLoggedIn
                        Log.d("AuthViewModel", "currentUser collected: NotLoggedIn (userValue is null)")
                    } else if (_uiState.value is AuthUiState.Initial) {
                        // Если при старте пользователь null и состояние Initial, это нормально, можно сделать NotLoggedIn
                        _uiState.value = AuthUiState.NotLoggedIn
                        Log.d("AuthViewModel", "currentUser collected: userValue is null, UI state was Initial, set to NotLoggedIn")
                    }
                }
            }
        }

        // Этот блок можно пока закомментировать или упростить,
        // так как основное состояние должно приходить из authRepository.currentUser
        // Если вы хотите активно проверять при инициализации:
        viewModelScope.launch {
            if (authRepository.isLoggedIn()) { // isLoggedIn теперь не suspend
                Log.d("AuthViewModel", "isLoggedIn check: true")
                // Вместо authRepository.getUser(), вызываем fetchAndUpdateCurrentUser
                // Но fetchAndUpdateCurrentUser в новом интерфейсе возвращает User?, а не Result<User?>
                // и может быть еще не реализован на бэкенде.
                // Пока что, если isLoggedIn() true, мы можем ожидать, что currentUser Flow даст нам пользователя.
                // Либо, если fetchAndUpdateCurrentUser реализован для получения данных с бэка:
                /*
                val userResult = authRepository.fetchAndUpdateCurrentUser() // Предположим, он возвращает Result<User?>
                if (userResult != null && userResult.isSuccess) { // Проверяем, что не null и успешен (если Result)
                    val fetchedUser = userResult.getOrNull()
                    if (fetchedUser != null) {
                        _uiState.value = AuthUiState.LoggedIn(fetchedUser)
                        Log.d("AuthViewModel", "fetchAndUpdateCurrentUser: LoggedIn, User: ${fetchedUser.id}")
                    } else {
                         _uiState.value = AuthUiState.NotLoggedIn
                         Log.w("AuthViewModel", "isLoggedIn is true, but fetchAndUpdateCurrentUser returned null user.")
                    }
                } else {
                    _uiState.value = AuthUiState.NotLoggedIn
                    Log.w("AuthViewModel", "isLoggedIn is true, but fetchAndUpdateCurrentUser failed or returned null.")
                }
                */
                // Упрощенный вариант для начала, полагаемся на currentUser.collect:
                Log.d("AuthViewModel", "isLoggedIn is true, currentUser Flow should provide user.")
            } else {
                Log.d("AuthViewModel", "isLoggedIn check: false")
                if (_uiState.value is AuthUiState.Initial) {
                    _uiState.value = AuthUiState.NotLoggedIn
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

    fun signUp(displayName: String, email: String, password: String, confirmPassword: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting sign up for email: $email")
                val result = authRepository.signUp(displayName, email, password, confirmPassword)

                if (result.isSuccess) {
                    val user = result.getOrNull()!!
                    Log.d(TAG, "Sign up successful, user: ${user.email}, role: ${user.role}")
                    // Если бэкенд возвращает токен, пользователь уже авторизован
                    _uiState.value = AuthUiState.LoggedIn(user)
                    Log.d(TAG, "UI state set to LoggedIn")
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Sign up failed: ${exception?.message}")
                    _uiState.value = AuthUiState.Error(
                        getLocalizedErrorMessage(exception)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign up exception", e)
                _uiState.value = AuthUiState.Error(getLocalizedErrorMessage(e))
            }
        }
    }

//    fun resetPassword(email: String) {
//        viewModelScope.launch {
//            try {
//                val result = authRepository.resetPassword(email)
//
//                if (result.isSuccess) {
//                    _uiState.value = AuthUiState.PasswordResetSent
//                } else {
//                    val exception = result.exceptionOrNull()
//                    _uiState.value = AuthUiState.Error(
//                        getLocalizedErrorMessage(exception)
//                    )
//                }
//            } catch (e: Exception) {
//                _uiState.value = AuthUiState.Error(getLocalizedErrorMessage(e))
//                Log.e(TAG, "Reset password error", e)
//            }
//        }
//    }


    // В AuthViewModel.kt
    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut() // Теперь не возвращает Result
                _uiState.value = AuthUiState.Initial // Или NotLoggedIn
                Log.d(TAG, "Sign out successful from ViewModel")
            } catch (e: Exception) { // Этот catch теперь для ошибок в самой корутине или если signOut выбросит исключение
                _uiState.value = AuthUiState.Error(e.message ?: "Неизвестная ошибка при выходе")
                Log.e(TAG, "Sign out error in ViewModel", e)
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
    data class RegistrationSuccessful(val user: User) : AuthUiState() // Новое состояние
}