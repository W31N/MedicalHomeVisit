package com.example.medicalhomevisit.presentation.ui.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
    // Используем uiState для определения общего состояния экрана
    val uiState by viewModel.uiState.collectAsState()
    // Используем requests для списка, который будет отображаться в Success и Empty состояниях,
    // управляемых через uiState.
    // val requests by viewModel.requests.collectAsState() // Можно получать список из uiState.Success

    // Запускаем загрузку/обновление при первом появлении экрана или когда uiState это Initial/NotLoggedIn
    // Либо можно добавить SwipeRefresh
    LaunchedEffect(Unit) { // Unit гарантирует, что это запустится один раз при композиции
        if (viewModel.user.value != null && (uiState is PatientUiState.Initial || uiState is PatientUiState.NotLoggedIn)) {
            viewModel.refreshRequests()
        }
    }
    // Или, если мы хотим обновлять при каждом входе на экран (если пользователь уже есть):
    /*
    LaunchedEffect(viewModel.user.value) {
        if (viewModel.user.value != null) {
            viewModel.refreshRequests()
        }
    }
    */


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
            when (val currentState = uiState) { // Используем currentState для удобства
                is PatientUiState.Initial, PatientUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is PatientUiState.Empty -> {
                    EmptyStateContent() // Вынесем в отдельный Composable для чистоты
                }
                is PatientUiState.Success -> {
                    // Данные берем из currentState.requests, так как Success теперь их содержит
                    val requests = currentState.requests
                    if (requests.isNotEmpty()) {
                        RequestsListContent(
                            requests = requests,
                            onRequestDetails = onRequestDetails
                        )
                    } else {
                        // Этот случай не должен возникать, если Empty состояние обрабатывается отдельно,
                        // но на всякий случай.
                        EmptyStateContent()
                    }
                }
                is PatientUiState.Error -> {
                    ErrorStateContent(
                        errorMessage = currentState.message,
                        onRetry = { viewModel.refreshRequests() } // Используем refreshRequests для повторной загрузки
                    )
                }
                is PatientUiState.RequestCreated, is PatientUiState.RequestCancelled -> {
                    // Эти состояния временные. После них ViewModel должна перейти в Success/Empty/Error.
                    // Можно показать CircularProgressIndicator, пока ViewModel не обновит uiState
                    // или запустить LaunchedEffect для сброса в предыдущее состояние после показа Snackbar/Toast.
                    // В текущей ViewModel, после RequestCreated/RequestCancelled, вызывается loadRequests,
                    // что приведет к Loading -> Success/Empty/Error.
                    // Для простоты, пока можно показывать предыдущий список или загрузку.
                    // Либо PatientViewModel.resetUiStateToDefault() должен быть вызван после обработки этих состояний.
                    // Сейчас PatientViewModel.loadRequests() после этих действий обновит uiState.
                    // Покажем загрузку, ожидая обновления списка.
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
                            Icons.Default.Login,
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
                        // Можно добавить кнопку для перехода на экран входа
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
        // Активные заявки
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
            items(activeRequests, key = { it.id }) { request -> // Добавляем key для лучшей производительности
                RequestCard(
                    request = request,
                    onClick = { onRequestDetails(request.id) }
                )
            }
        }

        // История заявок
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
            items(historyRequests, key = { it.id }) { request -> // Добавляем key
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
            Icons.Default.Healing, // Можно заменить на более подходящую иконку для "пусто"
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
            Icons.Default.ErrorOutline, // Иконка для ошибки
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Убрал специфичную обработку "FAILED_PRECONDITION", так как это лучше делать в ViewModel или при формировании сообщения об ошибке
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


@OptIn(ExperimentalMaterial3Api::class)
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
            // Статус и дата
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(status = request.status)

                Text(
                    // Используем request.createdAt, которое должно быть non-null
                    text = "Создано: ${dateFormatter.format(request.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Тип заявки и симптомы
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

            // Дата визита (если есть)
            // ИСПРАВЛЕНО: request.preferredDate -> request.preferredDateTime
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
                        // ИСПРАВЛЕНО: request.preferredDate -> preferredDateTime
                        text = dateTimeFormatter.format(preferredDateTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }


            // Адрес
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
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), // Увеличил alpha для лучшей читаемости
            MaterialTheme.colorScheme.primary,
            "Запланирован"
        )
        RequestStatus.IN_PROGRESS -> Triple(
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f), // Увеличил alpha
            MaterialTheme.colorScheme.secondary,
            "Выполняется"
        )
        RequestStatus.COMPLETED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant, // Был surfaceVariant, может быть лучше surfaceContainerHighest
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
        shape = MaterialTheme.shapes.small, // Можно попробовать RoundedCornerShape(8.dp)
        tonalElevation = 1.dp // Небольшая тень для выделения
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}