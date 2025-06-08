package com.example.medicalhomevisit

import com.example.medicalhomevisit.data.di.OfflinePatientRepository
import com.example.medicalhomevisit.data.sync.SyncManager
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
class VisitListViewModelTest {

    @MockK
    private lateinit var visitRepository: VisitRepository

    @MockK
    private lateinit var authRepository: AuthRepository

    @MockK
    private lateinit var syncManager: SyncManager

    @MockK
    @OfflinePatientRepository
    private lateinit var patientRepository: PatientRepository

    @MockK
    private lateinit var protocolTemplateRepository: ProtocolTemplateRepository

    private lateinit var viewModel: VisitListViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testUser = User(
        id = "user123",
        email = "doctor@test.com",
        displayName = "Dr. Test",
        role = UserRole.MEDICAL_STAFF,
        medicalPersonId = "medical123"
    )

    private val testVisits = listOf(
        Visit(
            id = "visit1",
            patientId = "patient1",
            scheduledTime = Date(),
            status = VisitStatus.PLANNED,
            address = "Test Address 1",
            reasonForVisit = "Regular checkup",
            assignedStaffId = "medical123"
        ),
        Visit(
            id = "visit2",
            patientId = "patient2",
            scheduledTime = Date(),
            status = VisitStatus.IN_PROGRESS,
            address = "Test Address 2",
            reasonForVisit = "Emergency visit",
            assignedStaffId = "medical123"
        )
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // Мокаем репозитории
        every { authRepository.currentUser } returns MutableStateFlow(testUser)
        every { visitRepository.observeVisits() } returns flowOf(testVisits)
        coEvery { visitRepository.getVisitsForStaff(any()) } returns testVisits
        coEvery { visitRepository.updateVisitStatus(any(), any()) } just Runs

        every { syncManager.setupPeriodicSync() } just Runs
        every { syncManager.syncNow() } just Runs

        coEvery { patientRepository.getPatientById(any()) } returns Patient(
            id = "patient1",
            fullName = "Test Patient",
            address = "Test Address",
            phoneNumber = "123456789",
            policyNumber = "POL123"
        )

        coEvery { protocolTemplateRepository.refreshTemplates() } returns Result.success(Unit)

        viewModel = VisitListViewModel(
            visitRepository,
            authRepository,
            syncManager,
            patientRepository,
            protocolTemplateRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadVisits should return success with visits list`() = runTest {
        // When
        viewModel.loadVisits()
        advanceUntilIdle()

        // Then
        coVerify { visitRepository.getVisitsForStaff(testUser.id) }

        val uiState = viewModel.uiState.value
        assertTrue(uiState is VisitListUiState.Success)
        assertEquals(2, (uiState as VisitListUiState.Success).visits.size)
    }

    @Test
    fun `updateVisitStatus should call repository with correct parameters`() = runTest {
        // Given
        val visitId = "visit1"
        val newStatus = VisitStatus.COMPLETED

        // When
        viewModel.updateVisitStatus(visitId, newStatus)
        advanceUntilIdle()

        // Then
        coVerify { visitRepository.updateVisitStatus(visitId, newStatus) }
    }

    @Test
    fun `updateSelectedStatus should filter visits by status`() = runTest {
        // Given
        advanceUntilIdle()

        // When
        viewModel.updateSelectedStatus(VisitStatus.PLANNED)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState is VisitListUiState.Success)
        val visits = (uiState as VisitListUiState.Success).visits
        assertEquals(1, visits.size)
        assertEquals(VisitStatus.PLANNED, visits.first().status)
    }

    @Test
    fun `updateSearchQuery should filter visits by query`() = runTest {
        // Given
        advanceUntilIdle()

        // When
        viewModel.updateSearchQuery("Address 1")
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState is VisitListUiState.Success)
        val visits = (uiState as VisitListUiState.Success).visits
        assertEquals(1, visits.size)
        assertTrue(visits.first().address.contains("Address 1"))
    }
}