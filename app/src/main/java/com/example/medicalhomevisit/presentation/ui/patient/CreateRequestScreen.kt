// com/example/medicalhomevisit/ui/patient/CreateRequestScreen.kt
package com.example.medicalhomevisit.presentation.ui.patient

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.medicalhomevisit.domain.model.RequestType
import com.example.medicalhomevisit.presentation.viewmodel.PatientUiState
import com.example.medicalhomevisit.presentation.viewmodel.PatientViewModel
import java.text.SimpleDateFormat
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
    var selectedHour by remember { mutableStateOf(9) } // 9 часов утра по умолчанию
    var selectedMinute by remember { mutableStateOf(0) }
    var address by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }

    // Состояние для отображения сообщения об ошибке выбора времени
    var showTimeError by remember { mutableStateOf(false) }
    var timeErrorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Рабочее время в часах (9:00 - 18:00)
    val workHoursStart = 9
    val workHoursEnd = 18

    // Создаем полную дату с временем
    val fullDateTime: Date? = remember(selectedDate, selectedHour, selectedMinute) {
        selectedDate?.let { date ->
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)
            calendar.time
        }
    }

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
                        label = "Плановая",
                        selected = requestType == RequestType.REGULAR,
                        onClick = { requestType = RequestType.REGULAR }
                    )

                    RequestTypeButton(
                        type = RequestType.EMERGENCY,
                        label = "Неотложная",
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

                                TextButton(onClick = {
                                    // Показываем DatePickerDialog
                                    val calendar = Calendar.getInstance()
                                    calendar.time = selectedDate!!
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val newCalendar = Calendar.getInstance()
                                            newCalendar.set(year, month, dayOfMonth)
                                            // Сохраняем только дату, сохраняя текущее выбранное время
                                            selectedDate = newCalendar.time
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).apply {
                                        val tomorrow = Calendar.getInstance().apply {
                                            add(Calendar.DAY_OF_YEAR, 1)
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                        }
                                        datePicker.minDate = tomorrow.timeInMillis
                                        show()
                                    }
                                }) {
                                    Text("Изменить")
                                }
                            } else {
                                Text("Выберите дату")

                                Spacer(modifier = Modifier.weight(1f))

                                Button(onClick = {
                                    // Показываем DatePickerDialog
                                    val calendar = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val newCalendar = Calendar.getInstance()
                                            newCalendar.set(year, month, dayOfMonth)
                                            selectedDate = newCalendar.time
                                            // По умолчанию устанавливаем 9:00 утра
                                            selectedHour = workHoursStart
                                            selectedMinute = 0
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).apply {
                                        val tomorrow = Calendar.getInstance().apply {
                                            add(Calendar.DAY_OF_YEAR, 1)
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                        }
                                        datePicker.minDate = tomorrow.timeInMillis
                                        show()
                                    }
                                }) {
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

                            if (selectedDate != null) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = timeFormatter.format(fullDateTime!!),
                                            style = MaterialTheme.typography.bodyLarge
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        Button(onClick = {
                                            // Показываем TimePickerDialog
                                            TimePickerDialog(
                                                context,
                                                { _, hourOfDay, minute ->
                                                    // Проверяем, что выбранное время находится в рабочем диапазоне
                                                    if (hourOfDay < workHoursStart || hourOfDay >= workHoursEnd) {
                                                        showTimeError = true
                                                        timeErrorMessage = "Пожалуйста, выберите время с $workHoursStart:00 до $workHoursEnd:00"
                                                    } else {
                                                        selectedHour = hourOfDay
                                                        selectedMinute = minute
                                                        showTimeError = false
                                                    }
                                                },
                                                selectedHour,
                                                selectedMinute,
                                                true // 24-часовой формат
                                            ).show()
                                        }) {
                                            Text("Выбрать время")
                                        }
                                    }

                                    // Сообщение об ошибке выбора времени
                                    if (showTimeError) {
                                        Text(
                                            text = timeErrorMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    } else {
                                        // Информационное сообщение о рабочих часах
                                        Text(
                                            text = "Рабочие часы: с $workHoursStart:00 до $workHoursEnd:00",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Сначала выберите дату",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                        preferredDate = fullDateTime, // Используем комбинированную дату с временем
                        preferredTimeRange = null, // Теперь не используем диапазоны времени
                        address = address,
                        additionalNotes = additionalNotes.ifBlank { null }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = symptoms.isNotBlank() && address.isNotBlank() &&
                        (requestType == RequestType.EMERGENCY || selectedDate != null) &&
                        !showTimeError && // Проверяем отсутствие ошибки времени
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
            overflow = TextOverflow.Ellipsis
        )
    }
}