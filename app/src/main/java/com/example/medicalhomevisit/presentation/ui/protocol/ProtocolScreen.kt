// ProtocolScreen.kt
package com.example.medicalhomevisit.presentation.ui.protocol

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.medicalhomevisit.domain.model.Visit
import com.example.medicalhomevisit.presentation.viewmodel.ProtocolData
import com.example.medicalhomevisit.presentation.viewmodel.ProtocolField
import com.example.medicalhomevisit.presentation.viewmodel.ProtocolUiState
import com.example.medicalhomevisit.presentation.viewmodel.ProtocolViewModel
import com.example.medicalhomevisit.presentation.viewmodel.VisitState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProtocolViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsState()
    val visitState by viewModel.visitState.collectAsState()
    val protocolData by viewModel.protocolData.collectAsState()
    val templates by viewModel.templates.collectAsState()

    // Локальное состояние для отображения диалога выбора шаблона
    var showTemplateDialog by remember { mutableStateOf(false) }

    // Обработка состояния сохранения
    LaunchedEffect(uiState) {
        if (uiState is ProtocolUiState.Saved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState) {
                            is ProtocolUiState.Creating -> "Новый протокол осмотра"
                            is ProtocolUiState.Editing -> "Редактирование протокола"
                            else -> "Протокол осмотра"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveProtocol() },
                        enabled = uiState is ProtocolUiState.Creating || uiState is ProtocolUiState.Editing
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is ProtocolUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ProtocolUiState.Creating, is ProtocolUiState.Editing -> {
                    val visit = when (visitState) {
                        is VisitState.Success -> (visitState as VisitState.Success).visit
                        else -> null
                    }

                    ProtocolContent(
                        visit = visit,
                        protocolData = protocolData,
                        onUpdateField = { field, value -> viewModel.updateProtocolField(field, value) },
                        onUpdateTemperature = { viewModel.updateTemperature(it) },
                        onUpdateBloodPressure = { systolic, diastolic -> viewModel.updateBloodPressure(systolic, diastolic) },
                        onUpdatePulse = { viewModel.updatePulse(it) },
                        onUpdateAdditionalVital = { key, value -> viewModel.updateAdditionalVital(key, value) },
                        onSelectTemplate = { showTemplateDialog = true }
                    )

                    // Диалог выбора шаблона протокола
                    if (showTemplateDialog) {
                        TemplateSelectionDialog(
                            templates = templates,
                            onSelectTemplate = {
                                viewModel.applyTemplate(it)
                                showTemplateDialog = false
                            },
                            onDismiss = { showTemplateDialog = false }
                        )
                    }
                }
                is ProtocolUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Ошибка",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (uiState as ProtocolUiState.Error).message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Вернуться назад")
                        }
                    }
                }
                else -> { /* No-op */ }
            }
        }
    }
}

@Composable
fun ProtocolContent(
    visit: Visit?,
    protocolData: ProtocolData,
    onUpdateField: (ProtocolField, String) -> Unit,
    onUpdateTemperature: (Float?) -> Unit,
    onUpdateBloodPressure: (Int?, Int?) -> Unit,
    onUpdatePulse: (Int?) -> Unit,
    onUpdateAdditionalVital: (String, String) -> Unit,
    onSelectTemplate: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Информация о визите
        visit?.let {
            VisitInfoCard(visit = it)
        }

        // Кнопка выбора шаблона
        OutlinedButton(
            onClick = onSelectTemplate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Выбрать шаблон протокола")
        }

        // Основные поля протокола
        ProtocolFieldCard(
            title = "Жалобы",
            value = protocolData.complaints,
            onValueChange = { onUpdateField(ProtocolField.COMPLAINTS, it) },
            placeholder = "Опишите жалобы пациента"
        )

        ProtocolFieldCard(
            title = "Анамнез",
            value = protocolData.anamnesis,
            onValueChange = { onUpdateField(ProtocolField.ANAMNESIS, it) },
            placeholder = "Опишите анамнез заболевания и жизни"
        )

        ProtocolFieldCard(
            title = "Объективный статус",
            value = protocolData.objectiveStatus,
            onValueChange = { onUpdateField(ProtocolField.OBJECTIVE_STATUS, it) },
            placeholder = "Опишите результаты объективного осмотра"
        )

        // Витальные показатели
        VitalSignsCard(
            temperature = protocolData.temperature,
            systolicBP = protocolData.systolicBP,
            diastolicBP = protocolData.diastolicBP,
            pulse = protocolData.pulse,
            additionalVitals = protocolData.additionalVitals,
            onUpdateTemperature = onUpdateTemperature,
            onUpdateBloodPressure = onUpdateBloodPressure,
            onUpdatePulse = onUpdatePulse,
            onUpdateAdditionalVital = onUpdateAdditionalVital
        )

        // Диагноз и рекомендации
        ProtocolFieldCard(
            title = "Диагноз",
            value = protocolData.diagnosis,
            onValueChange = { onUpdateField(ProtocolField.DIAGNOSIS, it) },
            placeholder = "Укажите клинический диагноз"
        )

        ProtocolFieldCard(
            title = "Код диагноза по МКБ-10",
            value = protocolData.diagnosisCode,
            onValueChange = { onUpdateField(ProtocolField.DIAGNOSIS_CODE, it) },
            placeholder = "Например, J06.9",
            singleLine = true
        )

        ProtocolFieldCard(
            title = "Рекомендации",
            value = protocolData.recommendations,
            onValueChange = { onUpdateField(ProtocolField.RECOMMENDATIONS, it) },
            placeholder = "Укажите назначения и рекомендации для пациента"
        )

        // Нижний отступ для удобства прокрутки
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun VisitInfoCard(visit: Visit) {
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Информация о визите",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row {
                Text(
                    text = "Дата и время:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(120.dp)
                )
                Text(
                    text = "${dateFormatter.format(visit.scheduledTime)}, ${timeFormatter.format(visit.scheduledTime)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row {
                Text(
                    text = "Причина визита:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(120.dp)
                )
                Text(
                    text = visit.reasonForVisit,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (visit.notes != null) {
                Row {
                    Text(
                        text = "Примечания:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(120.dp)
                    )
                    Text(
                        text = visit.notes,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolFieldCard(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder) },
                singleLine = singleLine,
                minLines = if (singleLine) 1 else 3,
                keyboardOptions = KeyboardOptions(
                    imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalSignsCard(
    temperature: Float?,
    systolicBP: Int?,
    diastolicBP: Int?,
    pulse: Int?,
    additionalVitals: Map<String, String>,
    onUpdateTemperature: (Float?) -> Unit,
    onUpdateBloodPressure: (Int?, Int?) -> Unit,
    onUpdatePulse: (Int?) -> Unit,
    onUpdateAdditionalVital: (String, String) -> Unit
) {
    // Локальное состояние для текстовых полей
    var tempText by remember { mutableStateOf(temperature?.toString() ?: "") }
    var systolicText by remember { mutableStateOf(systolicBP?.toString() ?: "") }
    var diastolicText by remember { mutableStateOf(diastolicBP?.toString() ?: "") }
    var pulseText by remember { mutableStateOf(pulse?.toString() ?: "") }

    // Дополнительный показатель (локальное состояние)
    var newVitalKey by remember { mutableStateOf("") }
    var newVitalValue by remember { mutableStateOf("") }
    var showAddVitalDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Показатели жизнедеятельности",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Температура
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Температура:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(120.dp)
                )

                OutlinedTextField(
                    value = tempText,
                    onValueChange = {
                        tempText = it
                        onUpdateTemperature(it.toFloatOrNull())
                    },
                    modifier = Modifier.width(100.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )

                Text(
                    text = "°C",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Артериальное давление
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "АД:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(120.dp)
                )

                OutlinedTextField(
                    value = systolicText,
                    onValueChange = {
                        systolicText = it
                        onUpdateBloodPressure(it.toIntOrNull(), diastolicText.toIntOrNull())
                    },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                Text(
                    text = "/",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                OutlinedTextField(
                    value = diastolicText,
                    onValueChange = {
                        diastolicText = it
                        onUpdateBloodPressure(systolicText.toIntOrNull(), it.toIntOrNull())
                    },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                Text(
                    text = "мм рт.ст.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Пульс
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Пульс:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(120.dp)
                )

                OutlinedTextField(
                    value = pulseText,
                    onValueChange = {
                        pulseText = it
                        onUpdatePulse(it.toIntOrNull())
                    },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )

                Text(
                    text = "уд/мин",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Дополнительные показатели
            if (additionalVitals.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Дополнительные показатели:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                additionalVitals.forEach { (key, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$key:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(120.dp)
                        )

                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        TextButton(
                            onClick = { onUpdateAdditionalVital(key, "") }
                        ) {
                            Text("Удалить")
                        }
                    }
                }
            }

            // Кнопка добавления показателя
            Button(
                onClick = { showAddVitalDialog = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Добавить показатель")
            }
        }
    }

    // Диалог добавления показателя
    if (showAddVitalDialog) {
        AlertDialog(
            onDismissRequest = { showAddVitalDialog = false },
            title = { Text("Добавить показатель") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newVitalKey,
                        onValueChange = { newVitalKey = it },
                        label = { Text("Наименование") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newVitalValue,
                        onValueChange = { newVitalValue = it },
                        label = { Text("Значение") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newVitalKey.isNotBlank() && newVitalValue.isNotBlank()) {
                            onUpdateAdditionalVital(newVitalKey, newVitalValue)
                            newVitalKey = ""
                            newVitalValue = ""
                            showAddVitalDialog = false
                        }
                    }
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddVitalDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun TemplateSelectionDialog(
    templates: List<com.example.medicalhomevisit.domain.model.ProtocolTemplate>,
    onSelectTemplate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите шаблон") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (templates.isEmpty()) {
                    Text("Нет доступных шаблонов")
                } else {
                    templates.forEach { template ->
                        Card(
                            onClick = { onSelectTemplate(template.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = template.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}