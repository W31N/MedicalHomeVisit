package com.example.medicalhomevisit.presentation.ui.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.medicalhomevisit.domain.model.AppointmentRequest
import com.example.medicalhomevisit.domain.model.RequestStatus
import com.example.medicalhomevisit.domain.model.RequestType
import com.example.medicalhomevisit.presentation.viewmodel.PatientUiState
import com.example.medicalhomevisit.presentation.viewmodel.PatientViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientRequestsScreen(
    viewModel: PatientViewModel,
    onCreateRequest: () -> Unit,
    onRequestDetails: (String) -> Unit,
    onProfileClick: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (viewModel.user.value != null && (uiState is PatientUiState.Initial || uiState is PatientUiState.NotLoggedIn)) {
            viewModel.refreshRequests()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои заявки") },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "Профиль")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateRequest,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Создать заявку",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val currentState = uiState) {
                is PatientUiState.Initial, PatientUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is PatientUiState.Empty -> {
                    EmptyStateContent()
                }
                is PatientUiState.Success -> {
                    val requests = currentState.requests
                    if (requests.isNotEmpty()) {
                        RequestsListContent(
                            requests = requests,
                            onRequestDetails = onRequestDetails
                        )
                    } else {
                        EmptyStateContent()
                    }
                }
                is PatientUiState.Error -> {
                    ErrorStateContent(
                        errorMessage = currentState.message,
                        onRetry = { viewModel.refreshRequests() }
                    )
                }
                is PatientUiState.RequestCreated, is PatientUiState.RequestCancelled -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is PatientUiState.NotLoggedIn -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Пожалуйста, войдите в систему",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestsListContent(
    requests: List<AppointmentRequest>,
    onRequestDetails: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val activeRequests = requests.filter {
            it.status == RequestStatus.NEW ||
                    it.status == RequestStatus.PENDING ||
                    it.status == RequestStatus.ASSIGNED ||
                    it.status == RequestStatus.SCHEDULED ||
                    it.status == RequestStatus.IN_PROGRESS
        }

        if (activeRequests.isNotEmpty()) {
            item {
                Text(
                    text = "Активные заявки",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(activeRequests, key = { it.id }) { request ->
                RequestCard(
                    request = request,
                    onClick = { onRequestDetails(request.id) }
                )
            }
        }

        val historyRequests = requests.filter {
            it.status == RequestStatus.COMPLETED ||
                    it.status == RequestStatus.CANCELLED
        }

        if (historyRequests.isNotEmpty()) {
            item {
                Text(
                    text = "История заявок",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            items(historyRequests, key = { it.id }) { request ->
                RequestCard(
                    request = request,
                    onClick = { onRequestDetails(request.id) }
                )
            }
        }
    }
}

@Composable
fun BoxScope.EmptyStateContent() {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Healing,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "У вас пока нет заявок на визит",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Нажмите на кнопку «+», чтобы создать новую заявку",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BoxScope.ErrorStateContent(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Повторить")
        }
    }
}

@Composable
fun RequestCard(
    request: AppointmentRequest,
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val dateTimeFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Card(
        onClick = onClick,
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
                StatusChip(status = request.status)

                Text(
                    text = "Создано: ${dateFormatter.format(request.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (request.requestType) {
                    RequestType.EMERGENCY -> "Неотложный визит"
                    RequestType.REGULAR -> "Плановый визит"
                    RequestType.CONSULTATION -> "Консультация"
                },
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = request.symptoms,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            request.preferredDateTime?.let { preferredDateTime ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateTimeFormatter.format(preferredDateTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = request.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: RequestStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        RequestStatus.NEW -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Новая"
        )
        RequestStatus.PENDING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "На рассмотрении"
        )
        RequestStatus.ASSIGNED -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Назначен врач"
        )
        RequestStatus.SCHEDULED -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.primary,
            "Запланирован"
        )
        RequestStatus.IN_PROGRESS -> Triple(
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.secondary,
            "Выполняется"
        )
        RequestStatus.COMPLETED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Выполнен"
        )
        RequestStatus.CANCELLED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Отменен"
        )
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}