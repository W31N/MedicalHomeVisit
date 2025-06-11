package com.example.medicalhomevisit

import app.cash.turbine.test
import com.example.medicalhomevisit.domain.model.User
import com.example.medicalhomevisit.domain.model.UserRole
import com.example.medicalhomevisit.domain.repository.AuthRepository
import com.example.medicalhomevisit.presentation.viewmodel.AuthUiState
import com.example.medicalhomevisit.presentation.viewmodel.AuthViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    private lateinit var authRepository: AuthRepository

    private lateinit var authViewModel: AuthViewModel

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @Before
    fun setUp() {
        authRepository = mockk()

        val userFlow = MutableStateFlow<User?>(null)
        every { authRepository.currentUser } returns userFlow
        every { authRepository.isLoggedIn() } returns false

        authViewModel = AuthViewModel(authRepository)
    }

    @Test
    fun `signIn with correct credentials should emit Loading and then LoggedIn state`() = runTest {
        val testUser = User(id = "123", email = "test@example.com", displayName = "Test User", role = UserRole.PATIENT)
        val successResult = Result.success(testUser)

        coEvery { authRepository.signIn(any(), any()) } returns successResult

        authViewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(AuthUiState.NotLoggedIn::class.java)

            authViewModel.signIn("test@example.com", "password")

            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(AuthUiState.Loading::class.java)

            val loggedInState = awaitItem()
            assertThat(loggedInState).isInstanceOf(AuthUiState.LoggedIn::class.java)
            assertThat((loggedInState as AuthUiState.LoggedIn).user).isEqualTo(testUser)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `signIn with incorrect credentials should emit Loading and then Error state`() = runTest {
        val errorMessage = "Неверный email или пароль"
        val failureResult = Result.failure<User>(Exception("INVALID_LOGIN_CREDENTIALS"))

        coEvery { authRepository.signIn(any(), any()) } returns failureResult

        authViewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(AuthUiState.NotLoggedIn::class.java)

            authViewModel.signIn("wrong@example.com", "wrongpassword")

            assertThat(awaitItem()).isInstanceOf(AuthUiState.Loading::class.java)

            val errorState = awaitItem()
            assertThat(errorState).isInstanceOf(AuthUiState.Error::class.java)

            assertThat((errorState as AuthUiState.Error).message).isEqualTo(errorMessage)

            cancelAndIgnoreRemainingEvents()
        }
    }
}


@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}