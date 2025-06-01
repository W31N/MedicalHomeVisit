    package com.example.medicalhomevisit.ui.admin

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
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import com.example.medicalhomevisit.data.model.AppointmentRequest
    import com.example.medicalhomevisit.data.model.RequestType
    import com.example.medicalhomevisit.data.model.User
    import java.text.SimpleDateFormat
    import java.util.*

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AssignRequestScreen(
        viewModel: AdminViewModel,
        request: AppointmentRequest,
        onBackClick: () -> Unit,
        onRequestAssigned: () -> Unit
    ) {
        val uiState by viewModel.uiState.collectAsState()
        val medicalStaff by viewModel.medicalStaff.collectAsState()

        var selectedStaff by remember { mutableStateOf<User?>(null) }
        var assignmentNote by remember { mutableStateOf("") }

        val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

        LaunchedEffect(uiState) {
            if (uiState is AdminUiState.RequestAssigned) {
                onRequestAssigned()
            }
        }

        LaunchedEffect(Unit) {
            viewModel.refreshData()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Назначение врача") },
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
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = when(request.requestType) {
                                RequestType.EMERGENCY -> "Неотложный визит"
                                RequestType.REGULAR -> "Плановый визит"
                                RequestType.CONSULTATION -> "Консультация"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Симптомы: ${request.symptoms}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // ИСПРАВЛЕНО: request.preferredDate -> request.preferredDateTime
                        request.preferredDateTime?.let { dateTime ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Предпочтительная дата/время: ${dateFormatter.format(dateTime)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Адрес: ${request.address}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (request.additionalNotes != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Дополнительно: ${request.additionalNotes}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Создано: ${dateFormatter.format(request.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Выберите медицинского работника",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (medicalStaff.isEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ){
                                Text(
                                    text = "Нет доступных медицинских работников",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { viewModel.refreshData() }
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Обновить")
                                }
                            }
                        } else {
                            medicalStaff.forEach { staff ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .selectable(
                                            selected = selectedStaff?.id == staff.id,
                                            onClick = { selectedStaff = staff }
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedStaff?.id == staff.id,
                                        onClick = { selectedStaff = staff }
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Text(
                                            text = staff.displayName,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = staff.role.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = assignmentNote,
                    onValueChange = { assignmentNote = it },
                    label = { Text("Примечание к назначению (опционально)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    minLines = 2
                )

                Button(
                    onClick = {
                        if (selectedStaff != null) {
                            viewModel.assignRequestToStaff(
                                requestId = request.id,
                                staffId = selectedStaff!!.id,
                                staffName = selectedStaff!!.displayName,
                                note = assignmentNote.ifBlank { null }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedStaff != null && uiState !is AdminUiState.Loading && medicalStaff.isNotEmpty()
                ) {
                    if (uiState is AdminUiState.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Назначить")
                    }
                }
            }
        }
    }