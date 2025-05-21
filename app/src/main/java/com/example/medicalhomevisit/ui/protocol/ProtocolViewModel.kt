// ProtocolViewModel.kt
package com.example.medicalhomevisit.ui.protocol

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.data.model.Visit
import com.example.medicalhomevisit.data.model.VisitProtocol
import com.example.medicalhomevisit.domain.model.ProtocolTemplate
import com.example.medicalhomevisit.domain.repository.ProtocolRepository
import com.example.medicalhomevisit.domain.repository.VisitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class ProtocolViewModel(
    private val protocolRepository: ProtocolRepository,
    private val visitRepository: VisitRepository,
    private val visitId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProtocolUiState>(ProtocolUiState.Loading)
    val uiState: StateFlow<ProtocolUiState> = _uiState.asStateFlow()

    private val _visitState = MutableStateFlow<VisitState>(VisitState.Loading)
    val visitState: StateFlow<VisitState> = _visitState.asStateFlow()

    private val _templates = MutableStateFlow<List<ProtocolTemplate>>(emptyList())
    val templates: StateFlow<List<ProtocolTemplate>> = _templates.asStateFlow()

    // Текущий редактируемый протокол
    private val _protocolData = MutableStateFlow(ProtocolData())
    val protocolData: StateFlow<ProtocolData> = _protocolData.asStateFlow()

    init {
        loadVisitData()
        loadProtocolData()
        loadTemplates()
    }

    private fun loadVisitData() {
        viewModelScope.launch {
            _visitState.value = VisitState.Loading
            try {
                val visit = visitRepository.getVisitById(visitId)
                _visitState.value = VisitState.Success(visit)
            } catch (e: Exception) {
                Log.e("Protocol", "Error loading visit: ${e.message}", e)
                _visitState.value = VisitState.Error(e.message ?: "Ошибка загрузки данных визита")
            }
        }
    }

    private fun loadProtocolData() {
        viewModelScope.launch {
            _uiState.value = ProtocolUiState.Loading
            try {
                val protocol = protocolRepository.getProtocolForVisit(visitId)

                if (protocol != null) {
                    // Существующий протокол
                    Log.d("Protocol", "Found existing protocol for visit $visitId")
                    _uiState.value = ProtocolUiState.Editing
                    _protocolData.value = ProtocolData(
                        id = protocol.id,
                        visitId = protocol.visitId,
                        complaints = protocol.complaints,
                        anamnesis = protocol.anamnesis,
                        objectiveStatus = protocol.objectiveStatus,
                        diagnosis = protocol.diagnosis,
                        diagnosisCode = protocol.diagnosisCode,
                        recommendations = protocol.recommendations,
                        temperature = protocol.temperature,
                        systolicBP = protocol.systolicBP,
                        diastolicBP = protocol.diastolicBP,
                        pulse = protocol.pulse,
                        additionalVitals = protocol.additionalVitals ?: emptyMap()
                    )
                } else {
                    // Новый протокол
                    Log.d("Protocol", "Creating new protocol for visit $visitId")
                    _uiState.value = ProtocolUiState.Creating
                    _protocolData.value = ProtocolData(
                        id = UUID.randomUUID().toString(),
                        visitId = visitId
                    )
                }
            } catch (e: Exception) {
                Log.e("Protocol", "Error loading protocol: ${e.message}", e)
                _uiState.value = ProtocolUiState.Error(e.message ?: "Ошибка загрузки протокола")
            }
        }
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            try {
                val templates = protocolRepository.getProtocolTemplates()
                _templates.value = templates
                Log.d("Protocol", "Loaded ${templates.size} templates")
            } catch (e: Exception) {
                Log.e("Protocol", "Error loading templates: ${e.message}", e)
                // Ошибка загрузки шаблонов не критична для работы с протоколом
            }
        }
    }

    fun updateProtocolField(field: ProtocolField, value: String) {
        val currentData = _protocolData.value
        _protocolData.value = when (field) {
            ProtocolField.COMPLAINTS -> currentData.copy(complaints = value)
            ProtocolField.ANAMNESIS -> currentData.copy(anamnesis = value)
            ProtocolField.OBJECTIVE_STATUS -> currentData.copy(objectiveStatus = value)
            ProtocolField.DIAGNOSIS -> currentData.copy(diagnosis = value)
            ProtocolField.DIAGNOSIS_CODE -> currentData.copy(diagnosisCode = value)
            ProtocolField.RECOMMENDATIONS -> currentData.copy(recommendations = value)
        }
    }

    fun updateTemperature(value: Float?) {
        _protocolData.value = _protocolData.value.copy(temperature = value)
    }

    fun updateBloodPressure(systolic: Int?, diastolic: Int?) {
        _protocolData.value = _protocolData.value.copy(
            systolicBP = systolic,
            diastolicBP = diastolic
        )
    }

    fun updatePulse(value: Int?) {
        _protocolData.value = _protocolData.value.copy(pulse = value)
    }

    fun updateAdditionalVital(key: String, value: String) {
        val currentVitals = _protocolData.value.additionalVitals.toMutableMap()
        if (value.isBlank()) {
            currentVitals.remove(key)
        } else {
            currentVitals[key] = value
        }
        _protocolData.value = _protocolData.value.copy(additionalVitals = currentVitals)
    }

    fun applyTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                val template = protocolRepository.getProtocolTemplateById(templateId)
                template?.let {
                    Log.d("Protocol", "Applying template: ${template.name}")
                    // Сохраняем текущие значения витальных показателей
                    val currentTemp = _protocolData.value.temperature
                    val currentSystolic = _protocolData.value.systolicBP
                    val currentDiastolic = _protocolData.value.diastolicBP
                    val currentPulse = _protocolData.value.pulse
                    val currentAdditionalVitals = _protocolData.value.additionalVitals

                    // Применяем шаблон, сохраняя ID и витальные показатели
                    _protocolData.value = _protocolData.value.copy(
                        complaints = it.complaints,
                        anamnesis = it.anamnesis,
                        objectiveStatus = it.objectiveStatus,
                        recommendations = it.recommendations,
                        // Сохраняем текущие значения
                        temperature = currentTemp,
                        systolicBP = currentSystolic,
                        diastolicBP = currentDiastolic,
                        pulse = currentPulse,
                        additionalVitals = currentAdditionalVitals
                    )
                }
            } catch (e: Exception) {
                Log.e("Protocol", "Error applying template: ${e.message}", e)
                // Обработка ошибки при применении шаблона
            }
        }
    }

    fun saveProtocol() {
        viewModelScope.launch {
            try {
                Log.d("Protocol", "Saving protocol")
                val currentProtocolData = _protocolData.value
                val now = Date()

                val protocol = VisitProtocol(
                    id = currentProtocolData.id,
                    visitId = currentProtocolData.visitId,
                    templateId = null, // Мы не сохраняем ссылку на шаблон
                    complaints = currentProtocolData.complaints,
                    anamnesis = currentProtocolData.anamnesis,
                    objectiveStatus = currentProtocolData.objectiveStatus,
                    diagnosis = currentProtocolData.diagnosis,
                    diagnosisCode = currentProtocolData.diagnosisCode,
                    recommendations = currentProtocolData.recommendations,
                    temperature = currentProtocolData.temperature,
                    systolicBP = currentProtocolData.systolicBP,
                    diastolicBP = currentProtocolData.diastolicBP,
                    pulse = currentProtocolData.pulse,
                    additionalVitals = if (currentProtocolData.additionalVitals.isEmpty()) null
                    else currentProtocolData.additionalVitals,
                    createdAt = now,
                    updatedAt = now
                )

                // Сохраняем в репозиторий
                val savedProtocol = protocolRepository.saveProtocol(protocol)
                Log.d("Protocol", "Protocol saved successfully with ID: ${savedProtocol.id}")

                // Обновляем UI состояние
                _uiState.value = ProtocolUiState.Saved
            } catch (e: Exception) {
                Log.e("Protocol", "Error saving protocol: ${e.message}", e)
                _uiState.value = ProtocolUiState.Error("Ошибка сохранения протокола: ${e.message}")
            }
        }
    }
}

sealed class ProtocolUiState {
    object Loading : ProtocolUiState()
    object Creating : ProtocolUiState() // Создание нового протокола
    object Editing : ProtocolUiState()  // Редактирование существующего
    object Saved : ProtocolUiState()    // Протокол успешно сохранен
    data class Error(val message: String) : ProtocolUiState()
}

sealed class VisitState {
    object Loading : VisitState()
    data class Success(val visit: Visit) : VisitState()
    data class Error(val message: String) : VisitState()
}

enum class ProtocolField {
    COMPLAINTS,
    ANAMNESIS,
    OBJECTIVE_STATUS,
    DIAGNOSIS,
    DIAGNOSIS_CODE,
    RECOMMENDATIONS
}

data class ProtocolData(
    val id: String = "",
    val visitId: String = "",
    val complaints: String = "",
    val anamnesis: String = "",
    val objectiveStatus: String = "",
    val diagnosis: String = "",
    val diagnosisCode: String = "",
    val recommendations: String = "",
    val temperature: Float? = null,
    val systolicBP: Int? = null,
    val diastolicBP: Int? = null,
    val pulse: Int? = null,
    val additionalVitals: Map<String, String> = emptyMap()
)