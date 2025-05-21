// com/example/medicalhomevisit/ui/patient/CreateRequestScreen.kt
package com.example.medicalhomevisit.ui.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.medicalhomevisit.data.model.RequestType
import java.text.SimpleDateFormat
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestScreen(
    viewModel: PatientViewModel,
    onBackClick: () -> Unit,
    onRequestCreated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val user by viewModel.user.collectAsState()

    var requestType by remember { mutableStateOf(RequestType.REGULAR) }
    var symptoms by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var preferredTimeRange by remember { mutableStateOf<String?>(null) }
    var address by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }

    var showTimeRangeDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    // Автоматически заполняем адрес пользователя, если он доступен
    LaunchedEffect(user) {
        if (!address.isBlank()) return@LaunchedEffect
        // Здесь нужно получить адрес из профиля пользователя,
        // если есть соответствующее поле
        // address = user?.address ?: ""
    }

    // Обработка состояния создания заявки
    LaunchedEffect(uiState) {
        if (uiState is PatientUiState.RequestCreated) {
            onRequestCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новая заявка на визит") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Тип заявки
            Column {
                Text(
                    text = "Тип заявки",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RequestTypeButton(
                        type = RequestType.REGULAR,
                        label = "Плановая",  // Изменено на "Плановая"
                        selected = requestType == RequestType.REGULAR,
                        onClick = { requestType = RequestType.REGULAR }
                    )

                    RequestTypeButton(
                        type = RequestType.EMERGENCY,
                        label = "Неотложная",  // Изменено на "Неотложная"
                        selected = requestType == RequestType.EMERGENCY,
                        onClick = { requestType = RequestType.EMERGENCY }
                    )

                    RequestTypeButton(
                        type = RequestType.CONSULTATION,
                        label = "Консультация",
                        selected = requestType == RequestType.CONSULTATION,
                        onClick = { requestType = RequestType.CONSULTATION }
                    )
                }
            }

            // Симптомы
            OutlinedTextField(
                value = symptoms,
                onValueChange = { symptoms = it },
                label = { Text("Опишите симптомы или причину визита") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                minLines = 3
            )

            // Дата и время
            if (requestType != RequestType.EMERGENCY) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Предпочтительное время визита",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Выбор даты
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            if (selectedDate != null) {
                                Text(
                                    text = dateFormatter.format(selectedDate!!),
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                TextButton(onClick = { showDatePicker = true }) {
                                    Text("Изменить")
                                }
                            } else {
                                Text("Выберите дату")

                                Spacer(modifier = Modifier.weight(1f))

                                Button(onClick = { showDatePicker = true }) {
                                    Text("Выбрать")
                                }
                            }
                        }

                        // Выбор времени
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            if (preferredTimeRange != null) {
                                Text(
                                    text = preferredTimeRange ?: "",
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                TextButton(onClick = { showTimeRangeDialog = true }) {
                                    Text("Изменить")
                                }
                            } else {
                                Text("Выберите предпочтительное время")

                                Spacer(modifier = Modifier.weight(1f))

                                Button(onClick = { showTimeRangeDialog = true }) {
                                    Text("Выбрать")
                                }
                            }
                        }
                    }
                }
            }

            // Адрес
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Адрес для визита") },
                leadingIcon = {
                    Icon(Icons.Default.Home, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                )
            )

            // Дополнительная информация
            OutlinedTextField(
                value = additionalNotes,
                onValueChange = { additionalNotes = it },
                label = { Text("Дополнительная информация (по желанию)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                minLines = 2
            )

            // Кнопка отправки
            Button(
                onClick = {
                    viewModel.createNewRequest(
                        requestType = requestType,
                        symptoms = symptoms,
                        preferredDate = selectedDate,
                        preferredTimeRange = preferredTimeRange,
                        address = address,
                        additionalNotes = additionalNotes.ifBlank { null }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = symptoms.isNotBlank() && address.isNotBlank() &&
                        (requestType == RequestType.EMERGENCY || selectedDate != null) &&
                        uiState !is PatientUiState.Loading
            ) {
                if (uiState is PatientUiState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Отправить заявку")
                }
            }
        }
    }

    // Диалог выбора даты
    // Диалог выбора даты
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        // Устанавливаем минимальную дату (завтра)
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate = Date(calendar.timeInMillis)
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Отмена")
                }
            }
        ) {
            // Используем более простую версию DatePicker
            androidx.compose.material3.DatePicker(
                state = rememberDatePickerState(
                    initialSelectedDateMillis = selectedDate?.time ?: tomorrow
                )
            )
        }
    }

    // Диалог выбора времени
    if (showTimeRangeDialog) {
        AlertDialog(
            onDismissRequest = { showTimeRangeDialog = false },
            title = { Text("Выберите время") },
            text = {
                Column {
                    val timeRanges = listOf(
                        "Утро (8:00 - 12:00)",
                        "День (12:00 - 16:00)",
                        "Вечер (16:00 - 20:00)",
                        "Любое время"
                    )

                    timeRanges.forEach { timeRange ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .selectable(
                                    selected = preferredTimeRange == timeRange,
                                    onClick = { preferredTimeRange = timeRange }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferredTimeRange == timeRange,
                                onClick = { preferredTimeRange = timeRange }
                            )
                            Text(
                                text = timeRange,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showTimeRangeDialog = false }
                ) {
                    Text("Готово")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        preferredTimeRange = null
                        showTimeRangeDialog = false
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun RowScope.RequestTypeButton(
    type: RequestType,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = if (selected) {
        ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    } else {
        ButtonDefaults.outlinedButtonColors()
    }

    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = colors,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)  // Уменьшаем padding для более компактного отображения
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,  // Ограничиваем текст одной строкой
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}