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
import kotlinx.coroutines.flow.collectLatest
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

    // Флаг офлайн режима
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    init {
        loadVisitDetails()
        observeVisitChanges()
    }

    private fun observeVisitChanges() {
        // Наблюдение за изменениями визита в реальном времени (если поддерживается репозиторием)
        viewModelScope.launch {
            try {
                visitRepository.observeVisits().collectLatest { visits ->
                    val updatedVisit = visits.find { it.id == visitId }
                    updatedVisit?.let {
                        _uiState.value = VisitDetailUiState.Success(it)
                    }
                }
            } catch (e: Exception) {
                Log.e("VisitDetail", "Error observing visits: ${e.message}", e)
            }
        }
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

                // Проверяем, есть ли кэшированные данные
                try {
                    val cachedVisits = visitRepository.getCachedVisits()
                    val cachedVisit = cachedVisits.find { it.id == visitId }
                    if (cachedVisit != null) {
                        _uiState.value = VisitDetailUiState.Success(cachedVisit)
                        _isOffline.value = true
                        loadPatientDetails(cachedVisit.patientId)
                    } else {
                        _uiState.value = VisitDetailUiState.Error(e.message ?: "Неизвестная ошибка")
                    }
                } catch (cacheEx: Exception) {
                    _uiState.value = VisitDetailUiState.Error(e.message ?: "Неизвестная ошибка")
                }
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

                // Для наблюдения за изменениями пациента в реальном времени
                observePatientChanges(patientId)
            } catch (e: Exception) {
                Log.e("VisitDetail", "Error loading patient: ${e.message}", e)

                // Проверяем кэш
                try {
                    val cachedPatients = patientRepository.getCachedPatients()
                    val cachedPatient = cachedPatients.find { it.id == patientId }
                    if (cachedPatient != null) {
                        _patientState.value = PatientState.Success(cachedPatient)
                        _isOffline.value = true
                    } else {
                        _patientState.value = PatientState.Error(e.message ?: "Ошибка загрузки данных пациента")
                    }
                } catch (cacheEx: Exception) {
                    _patientState.value = PatientState.Error(e.message ?: "Ошибка загрузки данных пациента")
                }
            }
        }
    }

    private fun observePatientChanges(patientId: String) {
        viewModelScope.launch {
            try {
                patientRepository.observePatient(patientId).collectLatest { patient ->
                    _patientState.value = PatientState.Success(patient)
                }
            } catch (e: Exception) {
                // Ошибка при наблюдении игнорируется, так как у нас уже есть данные
                Log.e("VisitDetail", "Error observing patient: ${e.message}", e)
            }
        }
    }

    fun updateVisitStatus(newStatus: VisitStatus) {
        viewModelScope.launch {
            try {
                Log.d("VisitDetail", "Updating status to: $newStatus")

                // Для мгновенного обновления UI без ожидания перезагрузки
                (_uiState.value as? VisitDetailUiState.Success)?.let { currentState ->
                    val updatedVisit = currentState.visit.copy(status = newStatus)
                    _uiState.value = VisitDetailUiState.Success(updatedVisit)
                }

                // Обновляем статус в репозитории
                visitRepository.updateVisitStatus(visitId, newStatus)

                // Если мы не наблюдаем за изменениями в реальном времени,
                // обновляем UI - перезагружаем данные из репозитория
                // Убираем эту строку, если используем observeVisitChanges
                // loadVisitDetails()

                Log.d("VisitDetail", "Status updated successfully")
            } catch (e: Exception) {
                Log.e("VisitDetail", "Error updating status: ${e.message}", e)
                _isOffline.value = true
                // В случае ошибки возвращаем предыдущее состояние
                loadVisitDetails()
            }
        }
    }

    // Метод для повторной синхронизации при восстановлении соединения
    fun syncData() {
        viewModelScope.launch {
            try {
                visitRepository.syncVisits()
                _isOffline.value = false
                loadVisitDetails()
            } catch (e: Exception) {
                Log.e("VisitDetail", "Error syncing data: ${e.message}", e)
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