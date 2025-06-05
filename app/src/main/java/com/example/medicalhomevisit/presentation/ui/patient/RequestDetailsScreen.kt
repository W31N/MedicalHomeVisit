package com.example.medicalhomevisit.presentation.ui.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.medicalhomevisit.domain.model.AppointmentRequest
import com.example.medicalhomevisit.domain.model.RequestStatus
import com.example.medicalhomevisit.domain.model.RequestType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailsScreen(
    request: AppointmentRequest,
    onCancelRequest: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали заявки") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Статус заявки",
                            style = MaterialTheme.typography.titleMedium
                        )

                        StatusChip(status = request.status)
                    }

                    if (request.responseMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = request.responseMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Информация о заявке",
                        style = MaterialTheme.typography.titleMedium
                    )

                    InfoRow(
                        icon = Icons.Default.Category,
                        label = "Тип заявки",
                        value = when(request.requestType) {
                            RequestType.EMERGENCY -> "Неотложный визит"
                            RequestType.REGULAR -> "Плановый визит"
                            RequestType.CONSULTATION -> "Консультация"
                        }
                    )

                    InfoRow(
                        icon = Icons.Default.Healing,
                        label = "Причина визита",
                        value = request.symptoms
                    )

                    val dateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    InfoRow(
                        icon = Icons.Default.DateRange,
                        label = "Создано",
                        value = dateFormatter.format(request.createdAt)
                    )

                    if (request.preferredDateTime != null) {
                        InfoRow(
                            icon = Icons.Default.Event,
                            label = "Предпочтительная дата",
                            value = dateFormatter.format(request.preferredDateTime)
                        )
                    }

                    InfoRow(
                        icon = Icons.Default.LocationOn,
                        label = "Адрес",
                        value = request.address
                    )

                    if (request.additionalNotes.isNotBlank()) {
                        InfoRow(
                            icon = Icons.Default.Info,
                            label = "Дополнительная информация",
                            value = request.additionalNotes
                        )
                    }
                }
            }

            if (request.status == RequestStatus.NEW ||
                request.status == RequestStatus.PENDING ||
                request.status == RequestStatus.ASSIGNED) {

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Отменить заявку")
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Отмена заявки") },
            text = {
                Column {
                    Text("Вы уверены, что хотите отменить эту заявку?")

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Причина отмены") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancelRequest(cancelReason)
                        showCancelDialog = false
                    },
                    enabled = cancelReason.isNotBlank()
                ) {
                    Text("Отменить заявку")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp, end = 12.dp)
                .size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}