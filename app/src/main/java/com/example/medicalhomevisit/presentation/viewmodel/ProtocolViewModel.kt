package com.example.medicalhomevisit.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalhomevisit.data.repository.SimpleOfflineProtocolRepository
import com.example.medicalhomevisit.domain.model.Visit
import com.example.medicalhomevisit.domain.model.VisitProtocol
import com.example.medicalhomevisit.domain.repository.ProtocolRepository
import com.example.medicalhomevisit.domain.repository.VisitRepository
import com.example.medicalhomevisit.domain.model.ProtocolTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProtocolViewModel @Inject constructor(
    private val protocolRepository: ProtocolRepository,
    private val visitRepository: VisitRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "ProtocolViewModel"
    }

    private val visitId: String = checkNotNull(savedStateHandle["visitId"]) {
        "ProtocolViewModel должен получить visitId как аргумент навигации"
    }

    private val _uiState = MutableStateFlow<ProtocolUiState>(ProtocolUiState.Loading)
    val uiState: StateFlow<ProtocolUiState> = _uiState.asStateFlow()

    private val _visitState = MutableStateFlow<VisitState>(VisitState.Loading)
    val visitState: StateFlow<VisitState> = _visitState.asStateFlow()

    private val _templates = MutableStateFlow<List<ProtocolTemplate>>(emptyList())
    val templates: StateFlow<List<ProtocolTemplate>> = _templates.asStateFlow()

    // Текущий редактируемый протокол
    private val _protocolData = MutableStateFlow(ProtocolData())
    val protocolData: StateFlow<ProtocolData> = _protocolData.asStateFlow()

    // Флаг офлайн режима
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    init {
        Log.d(TAG, "Initialized with visitId: $visitId")
        loadVisitData()
        loadProtocolData()
        loadTemplates()
        observeProtocolChanges()
    }

    private fun observeProtocolChanges() {
        // Наблюдаем за изменениями протокола в реальном времени
        val offlineRepo = protocolRepository as? SimpleOfflineProtocolRepository
        if (offlineRepo != null) {
            viewModelScope.launch {
                try {
                    offlineRepo.observeProtocolForVisit(visitId).collectLatest { protocol ->
                        if (protocol != null) {
                            Log.d(TAG, "Protocol updated from database")
                            updateProtocolDataFromDomain(protocol)
                            _uiState.value = ProtocolUiState.Editing
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error observing protocol changes: ${e.message}", e)
                }
            }
        }
    }

    private fun loadVisitData() {
        viewModelScope.launch {
            _visitState.value = VisitState.Loading
            try {
                val visit = visitRepository.getVisitById(visitId)
                _visitState.value = VisitState.Success(visit)
                _isOffline.value = false
                Log.d(TAG, "Visit data loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading visit: ${e.message}", e)

                // Пробуем получить из кэша
                try {
                    val cachedVisits = visitRepository.getCachedVisits()
                    val cachedVisit = cachedVisits.find { it.id == visitId }
                    if (cachedVisit != null) {
                        _visitState.value = VisitState.Success(cachedVisit)
                        _isOffline.value = true
                        Log.d(TAG, "Loaded visit from cache (offline mode)")
                    } else {
                        _visitState.value = VisitState.Error(e.message ?: "Ошибка загрузки данных визита")
                    }
                } catch (cacheEx: Exception) {
                    _visitState.value = VisitState.Error(e.message ?: "Ошибка загрузки данных визита")
                }
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
                    Log.d(TAG, "Found existing protocol for visit $visitId")
                    updateProtocolDataFromDomain(protocol)
                    _uiState.value = ProtocolUiState.Editing
                } else {
                    // Новый протокол
                    Log.d(TAG, "Creating new protocol for visit $visitId")
                    _protocolData.value = ProtocolData(
                        id = "local_proto_${UUID.randomUUID()}",
                        visitId = visitId
                    )
                    _uiState.value = ProtocolUiState.Creating
                }

                _isOffline.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error loading protocol: ${e.message}", e)

                // В офлайн режиме пытаемся загрузить из кэша
                try {
                    val cachedProtocol = protocolRepository.getCachedProtocolForVisit(visitId)
                    if (cachedProtocol != null) {
                        updateProtocolDataFromDomain(cachedProtocol)
                        _uiState.value = ProtocolUiState.Editing
                        _isOffline.value = true
                        Log.d(TAG, "Loaded protocol from cache (offline mode)")
                    } else {
                        // Новый протокол в офлайн режиме
                        _protocolData.value = ProtocolData(
                            id = "local_proto_${UUID.randomUUID()}",
                            visitId = visitId
                        )
                        _uiState.value = ProtocolUiState.Creating
                        _isOffline.value = true
                    }
                } catch (cacheEx: Exception) {
                    _uiState.value = ProtocolUiState.Error(e.message ?: "Ошибка загрузки протокола")
                }
            }
        }
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            try {
                val templates = protocolRepository.getProtocolTemplates()
                _templates.value = templates
                Log.d(TAG, "Loaded ${templates.size} templates")

                if (templates.isNotEmpty()) {
                    _isOffline.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading templates: ${e.message}", e)
                _isOffline.value = true
            }
        }
    }

    fun updateProtocolField(field: ProtocolField, value: String) {
        // Сначала обновляем локально для немедленного отклика UI
        val currentData = _protocolData.value
        _protocolData.value = when (field) {
            ProtocolField.COMPLAINTS -> currentData.copy(complaints = value)
            ProtocolField.ANAMNESIS -> currentData.copy(anamnesis = value)
            ProtocolField.OBJECTIVE_STATUS -> currentData.copy(objectiveStatus = value)
            ProtocolField.DIAGNOSIS -> currentData.copy(diagnosis = value)
            ProtocolField.DIAGNOSIS_CODE -> currentData.copy(diagnosisCode = value)
            ProtocolField.RECOMMENDATIONS -> currentData.copy(recommendations = value)
        }

        // Затем сохраняем в репозиторий (с автоматической синхронизацией)
        viewModelScope.launch {
            try {
                val fieldName = when (field) {
                    ProtocolField.COMPLAINTS -> "complaints"
                    ProtocolField.ANAMNESIS -> "anamnesis"
                    ProtocolField.OBJECTIVE_STATUS -> "objectiveStatus"
                    ProtocolField.DIAGNOSIS -> "diagnosis"
                    ProtocolField.DIAGNOSIS_CODE -> "diagnosisCode"
                    ProtocolField.RECOMMENDATIONS -> "recommendations"
                }

                protocolRepository.updateProtocolField(visitId, fieldName, value)
                _isOffline.value = false
                Log.d(TAG, "Protocol field '$fieldName' updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating field on server: ${e.message}", e)
                _isOffline.value = true
                // UI уже обновлен локально, данные будут синхронизированы позже
            }
        }
    }

    fun updateTemperature(value: Float?) {
        _protocolData.value = _protocolData.value.copy(temperature = value)
        updateVitalsInRepository()
    }

    fun updateBloodPressure(systolic: Int?, diastolic: Int?) {
        _protocolData.value = _protocolData.value.copy(
            systolicBP = systolic,
            diastolicBP = diastolic
        )
        updateVitalsInRepository()
    }

    fun updatePulse(value: Int?) {
        _protocolData.value = _protocolData.value.copy(pulse = value)
        updateVitalsInRepository()
    }

    private fun updateVitalsInRepository() {
        viewModelScope.launch {
            try {
                val currentData = _protocolData.value
                protocolRepository.updateVitals(
                    visitId = visitId,
                    temperature = currentData.temperature,
                    systolicBP = currentData.systolicBP,
                    diastolicBP = currentData.diastolicBP,
                    pulse = currentData.pulse
                )
                _isOffline.value = false
                Log.d(TAG, "Vitals updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating vitals: ${e.message}", e)
                _isOffline.value = true
            }
        }
    }

    fun updateAdditionalVital(key: String, value: String) {
        val currentVitals = _protocolData.value.additionalVitals.toMutableMap()
        if (value.isBlank()) {
            currentVitals.remove(key)
        } else {
            currentVitals[key] = value
        }
        _protocolData.value = _protocolData.value.copy(additionalVitals = currentVitals)

        // Для дополнительных витальных показателей сохраняем весь протокол
        saveProtocolInBackground()
    }

    fun applyTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Applying template $templateId")
                val updatedProtocol = protocolRepository.applyTemplate(visitId, templateId)

                // Обновляем локальные данные
                updateProtocolDataFromDomain(updatedProtocol)
                _isOffline.value = false
                Log.d(TAG, "Template applied successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error applying template: ${e.message}", e)
                _isOffline.value = true

                // Fallback: применяем шаблон локально
                val template = _templates.value.find { it.id == templateId }
                if (template != null) {
                    val currentData = _protocolData.value
                    _protocolData.value = currentData.copy(
                        complaints = template.complaints.ifBlank { currentData.complaints },
                        anamnesis = template.anamnesis.ifBlank { currentData.anamnesis },
                        objectiveStatus = template.objectiveStatus.ifBlank { currentData.objectiveStatus },
                        recommendations = template.recommendations.ifBlank { currentData.recommendations }
                    )

                    // Сохраняем изменения
                    saveProtocolInBackground()
                    Log.d(TAG, "Template applied locally (offline mode)")
                }
            }
        }
    }

    fun saveProtocol() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Saving protocol")
                val currentProtocolData = _protocolData.value
                val now = Date()

                val protocol = VisitProtocol(
                    id = currentProtocolData.id.ifBlank { "local_proto_${UUID.randomUUID()}" },
                    visitId = currentProtocolData.visitId,
                    templateId = null,
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
                    additionalVitals = currentProtocolData.additionalVitals.takeIf { it.isNotEmpty() },
                    createdAt = now,
                    updatedAt = now
                )

                val savedProtocol = protocolRepository.saveProtocol(protocol)
                updateProtocolDataFromDomain(savedProtocol)

                _uiState.value = ProtocolUiState.Saved
                _isOffline.value = false
                Log.d(TAG, "Protocol saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving protocol: ${e.message}", e)
                _isOffline.value = true

                // В офлайн режиме все равно помечаем как сохраненный
                // Данные будут синхронизированы позже
                _uiState.value = ProtocolUiState.Saved
            }
        }
    }

    private fun saveProtocolInBackground() {
        viewModelScope.launch {
            try {
                val currentProtocolData = _protocolData.value
                val protocol = VisitProtocol(
                    id = currentProtocolData.id.ifBlank { "local_proto_${UUID.randomUUID()}" },
                    visitId = currentProtocolData.visitId,
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
                    additionalVitals = currentProtocolData.additionalVitals.takeIf { it.isNotEmpty() },
                    createdAt = Date(),
                    updatedAt = Date()
                )

                protocolRepository.saveProtocol(protocol)
                _isOffline.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error saving protocol in background: ${e.message}", e)
                _isOffline.value = true
            }
        }
    }

    fun syncData() {
        if (!_isOffline.value) return

        viewModelScope.launch {
            try {
                Log.d(TAG, "Syncing protocol data")
                protocolRepository.syncProtocols()

                // Перезагружаем данные после синхронизации
                loadProtocolData()
                loadTemplates()

                _isOffline.value = false
                Log.d(TAG, "Data synced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing data: ${e.message}", e)
                _isOffline.value = true
            }
        }
    }

    fun retry() {
        loadVisitData()
        loadProtocolData()
        loadTemplates()
    }

    fun setEditingMode() {
        if (_protocolData.value.id.isNotEmpty()) {
            _uiState.value = ProtocolUiState.Editing
        }
    }

    // ===== ОТЛАДОЧНЫЕ МЕТОДЫ =====

    fun getOfflineStats() {
        viewModelScope.launch {
            try {
                val repo = protocolRepository as? SimpleOfflineProtocolRepository
                if (repo != null) {
                    val unsyncedCount = repo.getUnsyncedCount()
                    val unsyncedProtocols = repo.getUnsyncedProtocols()

                    Log.d(TAG, "📊 PROTOCOL OFFLINE STATS:")
                    Log.d(TAG, "   - Unsynced count: $unsyncedCount")
                    Log.d(TAG, "   - Unsynced protocols: ${unsyncedProtocols.map { it.id }}")
                    Log.d(TAG, "   - Current protocol visit: $visitId")
                    Log.d(TAG, "   - Is offline: ${_isOffline.value}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting offline stats: ${e.message}")
            }
        }
    }

    private fun updateProtocolDataFromDomain(protocol: VisitProtocol) {
        _protocolData.value = ProtocolData(
            id = protocol.id ?: "",
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
    }
}

// Остальные классы остаются без изменений
sealed class ProtocolUiState {
    object Loading : ProtocolUiState()
    object Creating : ProtocolUiState()
    object Editing : ProtocolUiState()
    object Saved : ProtocolUiState()
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