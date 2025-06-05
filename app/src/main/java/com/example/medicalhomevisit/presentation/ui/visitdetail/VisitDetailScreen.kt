package com.example.medicalhomevisit.presentation.ui.visitdetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medicalhomevisit.domain.model.Gender
import com.example.medicalhomevisit.domain.model.Patient
import com.example.medicalhomevisit.domain.model.Visit
import com.example.medicalhomevisit.domain.model.VisitStatus
import com.example.medicalhomevisit.presentation.viewmodel.PatientState
import com.example.medicalhomevisit.presentation.viewmodel.VisitDetailUiState
import com.example.medicalhomevisit.presentation.viewmodel.VisitDetailViewModel
import com.example.medicalhomevisit.presentation.ui.components.StatusChip
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailScreen(
    viewModel: VisitDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProtocol: (String) -> Unit
) {
    val visitState by viewModel.uiState.collectAsState()
    val patientState by viewModel.patientState.collectAsState()

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали визита") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val vState = visitState) {
                is VisitDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is VisitDetailUiState.Success -> {
                    val visit = vState.visit
                    val patient = when (val pState = patientState) {
                        is PatientState.Success -> pState.patient
                        else -> null
                    }

                    VisitDetailContent(
                        visit = visit,
                        patient = patient,
                        onCallPatient = {
                            patient?.phoneNumber?.let { phone ->
                                if (phone != "Телефон не указан" && phone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:$phone")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        },
                        onOpenMap = {
                            if (visit.address != "Адрес не указан" && visit.address.isNotBlank()) {
                                val mapIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("geo:0,0?q=${Uri.encode(visit.address)}")
                                }
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://maps.google.com/?q=${Uri.encode(visit.address)}")
                                    }
                                    context.startActivity(webIntent)
                                }
                            }
                        },
                        onStatusChange = { newStatus ->
                            viewModel.updateVisitStatus(newStatus)
                        },
                        onCreateProtocol = {
                            if (viewModel.canCreateProtocol()) {
                                onNavigateToProtocol(visit.id)
                            }
                        }
                    )
                }
                is VisitDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Ошибка загрузки данных визита",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = vState.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { onNavigateBack() }) {
                            Text("Вернуться к списку визитов")
                        }
                    }
                }
            }

            if (visitState is VisitDetailUiState.Success && patientState is PatientState.Error) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ошибка загрузки данных пациента:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = (patientState as PatientState.Error).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun VisitDetailContent(
    visit: Visit,
    patient: Patient?,
    onCallPatient: () -> Unit,
    onOpenMap: () -> Unit,
    onStatusChange: (VisitStatus) -> Unit,
    onCreateProtocol: () -> Unit
) {
    val scrollState = rememberScrollState()
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Дата и время:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${dateFormatter.format(visit.scheduledTime)}, ${timeFormatter.format(visit.scheduledTime)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Статус:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    StatusChip(status = visit.status)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (visit.status) {
                        VisitStatus.PLANNED -> {
                            Button(
                                onClick = { onStatusChange(VisitStatus.IN_PROGRESS) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Начать визит")
                            }
                        }
                        VisitStatus.IN_PROGRESS -> {
                            Button(
                                onClick = { onCreateProtocol() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Оформить протокол")
                            }
                            Button(
                                onClick = { onStatusChange(VisitStatus.COMPLETED) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Завершить")
                            }
                        }
                        VisitStatus.COMPLETED -> {
                            Button(
                                onClick = { onCreateProtocol() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Просмотреть/Изменить протокол")
                            }
                        }
                        VisitStatus.CANCELLED -> {
                            Text("Визит отменен", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Информация о пациенте",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (patient != null) {
                    InfoRow(title = "ФИО:", value = patient.fullName)
                    InfoRow(
                        title = "Дата рождения:",
                        value = patient.dateOfBirth?.let { "${dateFormatter.format(it)} (${patient.age ?: "возраст не указан"})" } ?: "Не указана"
                    )
                    InfoRow(
                        title = "Пол:",
                        value = when (patient.gender) {
                            Gender.MALE -> "Мужской"
                            Gender.FEMALE -> "Женский"
                            Gender.UNKNOWN -> "Не указан"
                        }
                    )
                    InfoRow(title = "Номер полиса:", value = patient.policyNumber.ifBlank { "Не указан" })

                    if (!patient.allergies.isNullOrEmpty()) {
                        InfoRow(title = "Аллергии:", value = patient.allergies.joinToString(", "))
                    } else {
                        InfoRow(title = "Аллергии:", value = "Нет данных")
                    }

                    if (!patient.chronicConditions.isNullOrEmpty()) {
                        InfoRow(
                            title = "Хронические заболевания:",
                            value = patient.chronicConditions.joinToString(", ")
                        )
                    } else {
                        InfoRow(title = "Хронические заболевания:", value = "Нет данных")
                    }


                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onCallPatient,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = patient.phoneNumber != "Телефон не указан" && patient.phoneNumber.isNotBlank()
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Позвонить пациенту (${patient.phoneNumber})")
                    }
                } else {
                    Text("Данные пациента загружаются или отсутствуют...")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Детали вызова",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                InfoRow(title = "Причина визита:", value = visit.reasonForVisit.ifBlank { "Не указана" })
                InfoRow(title = "Адрес:", value = visit.address.ifBlank { "Не указан" })

                if (visit.notes.isNotBlank()) {
                    InfoRow(title = "Примечания к визиту:", value = visit.notes)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onOpenMap,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = visit.address != "Адрес не указан" && visit.address.isNotBlank()
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Открыть адрес на карте")
                }
            }
        }
    }
}

@Composable
fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(180.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}