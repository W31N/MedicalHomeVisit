package com.example.medicalhomevisit

import com.example.medicalhomevisit.domain.model.*
import com.example.medicalhomevisit.domain.repository.*
import com.example.medicalhomevisit.presentation.viewmodel.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.util.*

@ExperimentalCoroutinesApi
class PatientViewModelTest {

    @MockK
    private lateinit var appointmentRequestRepository: AppointmentRequestRepository

    @MockK
    private lateinit var authRepository: AuthRepository

    private lateinit var viewModel: PatientViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testUser = User(
        id = "patient123",
        email = "patient@test.com",
        displayName = "Test Patient",
        role = UserRole.PATIENT
    )

    private val testRequests = listOf(
        AppointmentRequest(
            id = "request1",
            patientId = "patient123",
            patientName = "Test Patient",
            patientPhone = "123456789",
            address = "Test Address",
            requestType = RequestType.REGULAR,
            symptoms = "Головная боль",
            status = RequestStatus.NEW
        ),
        AppointmentRequest(
            id = "request2",
            patientId = "patient123",
            patientName = "Test Patient",
            patientPhone = "123456789",
            address = "Test Address 2",
            requestType = RequestType.EMERGENCY,
            symptoms = "Высокая температура",
            status = RequestStatus.ASSIGNED
        )
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { authRepository.currentUser } returns MutableStateFlow(testUser)
        coEvery { appointmentRequestRepository.getMyRequests() } returns Result.success(testRequests)
        coEvery { appointmentRequestRepository.createRequest(any()) } returns Result.success(testRequests.first())
        coEvery { appointmentRequestRepository.cancelRequest(any(), any()) } returns Result.success(
            testRequests.first().copy(status = RequestStatus.CANCELLED)
        )

        viewModel = PatientViewModel(appointmentRequestRepository, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load should fetch patient requests successfully`() = runTest {
        advanceUntilIdle()

        coVerify { appointmentRequestRepository.getMyRequests() }

        val uiState = viewModel.uiState.value
        assertTrue(uiState is PatientUiState.Success)
        assertEquals(2, (uiState as PatientUiState.Success).requests.size)

        val requests = viewModel.requests.value
        assertEquals(2, requests.size)
    }

    @Test
    fun `createNewRequest should call repository and update state`() = runTest {
        val requestType = RequestType.REGULAR
        val symptoms = "Test symptoms"
        val address = "Test address"
        val preferredDate = Date()

        viewModel.createNewRequest(
            requestType = requestType,
            symptoms = symptoms,
            preferredDate = preferredDate,
            preferredTimeRange = "morning",
            address = address,
            additionalNotes = "Test notes"
        )
        advanceUntilIdle()

        coVerify {
            appointmentRequestRepository.createRequest(
                match { request ->
                    request.requestType == requestType &&
                            request.symptoms == symptoms &&
                            request.address == address
                }
            )
        }

        val uiState = viewModel.uiState.value
        assertTrue(uiState is PatientUiState.RequestCreated)
    }

    @Test
    fun `cancelRequest should call repository with correct parameters`() = runTest {
        val requestId = "request1"
        val reason = "Не нужен больше"

        viewModel.cancelRequest(requestId, reason)
        advanceUntilIdle()

        coVerify { appointmentRequestRepository.cancelRequest(requestId, reason) }

        val uiState = viewModel.uiState.value
        assertTrue(uiState is PatientUiState.RequestCancelled)
    }

    @Test
    fun `refreshRequests should reload data from repository`() = runTest {
        viewModel.refreshRequests()
        advanceUntilIdle()

        coVerify(exactly = 2) { appointmentRequestRepository.getMyRequests() }
    }

    @Test
    fun `when user is null should show NotLoggedIn state`() = runTest {
        every { authRepository.currentUser } returns MutableStateFlow(null)

        val viewModelWithoutUser = PatientViewModel(appointmentRequestRepository, authRepository)

        advanceUntilIdle()

        val uiState = viewModelWithoutUser.uiState.value
        assertTrue(uiState is PatientUiState.NotLoggedIn)
    }
}