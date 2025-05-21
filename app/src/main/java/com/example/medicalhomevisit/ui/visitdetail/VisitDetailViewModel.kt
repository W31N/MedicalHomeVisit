package com.example.medicalhomevisit.ui.visitdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.data.model.Patient
import com.example.medicalhomevisit.data.model.Visit
import com.example.medicalhomevisit.data.model.VisitStatus
import com.example.medicalhomevisit.domain.repository.PatientRepository
import com.example.medicalhomevisit.domain.repository.VisitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VisitDetailViewModel(
    private val visitRepository: VisitRepository,
    private val patientRepository: PatientRepository,
    private val visitId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<VisitDetailUiState>(VisitDetailUiState.Loading)
    val uiState: StateFlow<VisitDetailUiState> = _uiState.asStateFlow()

    private val _patientState = MutableStateFlow<PatientState>(PatientState.Loading)
    val patientState: StateFlow<PatientState> = _patientState.asStateFlow()

    init {
        loadVisitDetails()
    }

    private fun loadVisitDetails() {
        viewModelScope.launch {
            _uiState.value = VisitDetailUiState.Loading

            try {
                // Получаем визит из репозитория
                val visit = visitRepository.getVisitById(visitId)
                _uiState.value = VisitDetailUiState.Success(visit)

                // Загружаем информацию о пациенте
                loadPatientDetails(visit.patientId)
            } catch (e: Exception) {
                Log.e("VisitDetail", "Error loading visit: ${e.message}", e)
                _uiState.value = VisitDetailUiState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    private fun loadPatientDetails(patientId: String) {
        viewModelScope.launch {
            _patientState.value = PatientState.Loading

            try {
                // Получаем пациента из репозитория
                val patient = patientRepository.getPatientById(patientId)
                _patientState.value = PatientState.Success(patient)
            } catch (e: Exception) {
                Log.e("VisitDetail", "Error loading patient: ${e.message}", e)
                _patientState.value = PatientState.Error(e.message ?: "Ошибка загрузки данных пациента")
            }
        }
    }

    fun updateVisitStatus(newStatus: VisitStatus) {
        viewModelScope.launch {
            try {
                Log.d("VisitDetail", "Updating status to: $newStatus")

                // Обновляем статус в репозитории
                visitRepository.updateVisitStatus(visitId, newStatus)

                // Обновляем UI - перезагружаем данные из репозитория
                loadVisitDetails()

                Log.d("VisitDetail", "Status updated successfully")
            } catch (e: Exception) {
                Log.e("VisitDetail", "Error updating status: ${e.message}", e)
                // Обработка ошибки
            }
        }
    }
}

sealed class VisitDetailUiState {
    object Loading : VisitDetailUiState()
    data class Success(val visit: Visit) : VisitDetailUiState()
    data class Error(val message: String) : VisitDetailUiState()
}

sealed class PatientState {
    object Loading : PatientState()
    data class Success(val patient: Patient) : PatientState()
    data class Error(val message: String) : PatientState()
}