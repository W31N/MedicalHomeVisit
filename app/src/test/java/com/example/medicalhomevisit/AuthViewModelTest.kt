package com.example.medicalhomevisit

import com.example.medicalhomevisit.domain.model.User
import com.example.medicalhomevisit.domain.model.UserRole
import com.example.medicalhomevisit.domain.repository.AuthRepository
import com.example.medicalhomevisit.presentation.viewmodel.AuthUiState
import com.example.medicalhomevisit.presentation.viewmodel.AuthViewModel
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @MockK
    private lateinit var authRepository: AuthRepository

    private lateinit var authViewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { authRepository.currentUser } returns MutableStateFlow(null)
        every { authRepository.isLoggedIn() } returns false

        authViewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signIn with valid credentials should return success`() = runTest {
        val expectedUser = User(
            id = "test_id",
            email = "doctor@test.com",
            displayName = "Dr. Test",
            role = UserRole.MEDICAL_STAFF
        )
        coEvery { authRepository.signIn(any(), any()) } returns Result.success(expectedUser)

        authViewModel.signIn("doctor@test.com", "password123")
        advanceUntilIdle()

        coVerify { authRepository.signIn("doctor@test.com", "password123") }

        val currentState = authViewModel.uiState.value
        assert(currentState is AuthUiState.LoggedIn)
        assertEquals(expectedUser, (currentState as AuthUiState.LoggedIn).user)
    }

    @Test
    fun `signIn with invalid credentials should return error`() = runTest {
        val errorMessage = "Invalid credentials"
        coEvery { authRepository.signIn(any(), any()) } returns Result.failure(Exception(errorMessage))

        authViewModel.signIn("invalid@test.com", "wrongpassword")
        advanceUntilIdle()

        coVerify { authRepository.signIn("invalid@test.com", "wrongpassword") }

        val currentState = authViewModel.uiState.value
        assert(currentState is AuthUiState.Error)
        assertEquals(errorMessage, (currentState as AuthUiState.Error).message)
    }

    @Test
    fun `signUp with valid data should return success`() = runTest {
        val expectedUser = User(
            id = "new_user_id",
            email = "newuser@test.com",
            displayName = "New User",
            role = UserRole.PATIENT
        )
        coEvery {
            authRepository.signUp(any(), any(), any(), any())
        } returns Result.success(expectedUser)

        authViewModel.signUp("New User", "newuser@test.com", "password123", "password123")
        advanceUntilIdle()

        coVerify {
            authRepository.signUp("New User", "newuser@test.com", "password123", "password123")
        }

        val currentState = authViewModel.uiState.value
        assert(currentState is AuthUiState.LoggedIn)
        assertEquals(expectedUser, (currentState as AuthUiState.LoggedIn).user)
    }
}