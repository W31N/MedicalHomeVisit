package com.example.medicalhomevisit.ui.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onAuthSuccessful: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showEmailPrompt by remember { mutableStateOf(false) }
    var emailFormatError by remember { mutableStateOf<String?>(null) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }

    // Наблюдаем за состоянием авторизации
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.LoggedIn) {
            onAuthSuccessful()
        }
        // Сбрасываем подсказку, если появилась ошибка от ViewModel или другой успешный статус
        if (uiState !is AuthUiState.Initial) {
            showEmailPrompt = false
        }
        // Сбрасываем ошибку формата email, если пришла другая ошибка от ViewModel
        if (uiState is AuthUiState.Error) {
            emailFormatError = null
        }
        // Показываем диалог, когда получаем состояние PasswordResetSent
        if (uiState is AuthUiState.PasswordResetSent) {
            showResetPasswordDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Вход в систему",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                showEmailPrompt = false
                emailFormatError = null
                if (uiState is AuthUiState.Error && (uiState as AuthUiState.Error).message.startsWith("Email не может быть пустым")) {
                    viewModel.resetError() // Сбрасываем ошибку, если она была из-за пустого email
                                }
                            },
            label = { Text("Email") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            isError = emailFormatError != null
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Скрыть пароль" else "Показать пароль"
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    if (isValidEmail(email)) {
                        emailFormatError = null
                        viewModel.signIn(email, password)
                    } else {
                        emailFormatError = "Неверный формат email"
                        viewModel.resetError() // Сбрасываем другие ошибки
                    }
                }
            }),
            singleLine = true
        )
        // Область для отображения ошибок от ViewModel или подсказки про email
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .heightIn(min = 24.dp)
        ) {
            val currentError = emailFormatError ?: (uiState as? AuthUiState.Error)?.message

            if (showEmailPrompt && currentError == null) { // Показываем подсказку только если нет других ошибок
                Text(
                    text = "Пожалуйста, введите email для сброса пароля.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // Менее агрессивный цвет для подсказки
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (currentError != null) {
                Text(
                    text = currentError,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }


        Button(
            onClick = {
                showEmailPrompt = false
                if (!isValidEmail(email)) {
                    emailFormatError = "Неверный формат email"
                    viewModel.resetError() // Сбрасываем другие ошибки
                    return@Button
                }
                emailFormatError = null
                viewModel.signIn(email, password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = email.isNotBlank() && password.isNotBlank() && uiState !is AuthUiState.Loading
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Войти")
            }
        }

        TextButton(
            onClick = onNavigateToSignUp,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Нет аккаунта? Зарегистрироваться")
        }

        TextButton(
            onClick = {
                if (email.isNotBlank()) {
                    if (!isValidEmail(email)) {
                        emailFormatError = "Неверный формат email"
                        showEmailPrompt = false
                        viewModel.resetError()
                        return@TextButton
                    }
                    emailFormatError = null
                    showEmailPrompt = false
                    viewModel.resetPassword(email)
                } else {
                    showEmailPrompt = true
                    emailFormatError = null
                    viewModel.resetError()
                }
            },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text("Забыли пароль?")
        }

        if (uiState is AuthUiState.PasswordResetSent) {
            AlertDialog(
                onDismissRequest = { viewModel.resetError() },
                title = { Text("Сброс пароля") },
                text = { Text("Инструкции по сбросу пароля отправлены на указанный email.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetError() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
    if (showResetPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetPasswordDialog = false
                viewModel.resetError() // Сбрасываем состояние в ViewModel
            },
            title = { Text("Сброс пароля") },
            text = { Text("Инструкции по сбросу пароля отправлены на указанный email.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetPasswordDialog = false
                    viewModel.resetError() // Сбрасываем состояние в ViewModel
                }) {
                    Text("OK")
                }
            }
        )
    }
}

private fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}