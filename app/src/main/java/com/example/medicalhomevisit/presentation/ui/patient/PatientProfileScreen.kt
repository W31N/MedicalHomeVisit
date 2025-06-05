package com.example.medicalhomevisit.presentation.ui.patient

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.medicalhomevisit.domain.model.Gender
import com.example.medicalhomevisit.presentation.viewmodel.PatientProfileUiState
import com.example.medicalhomevisit.presentation.viewmodel.PatientProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(
    viewModel: PatientProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val profileData by viewModel.profileData.collectAsState()
    val context = LocalContext.current

    var dateOfBirth by remember { mutableStateOf<Date?>(null) }
    var gender by remember { mutableStateOf(Gender.UNKNOWN) }
    var address by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var policyNumber by remember { mutableStateOf("") }
    var allergiesText by remember { mutableStateOf("") }
    var chronicConditionsText by remember { mutableStateOf("") }

    val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    LaunchedEffect(profileData) {
        dateOfBirth = profileData.dateOfBirth
        gender = profileData.gender
        address = profileData.address
        phoneNumber = profileData.phoneNumber
        policyNumber = profileData.policyNumber
        allergiesText = profileData.allergies.joinToString(", ")
        chronicConditionsText = profileData.chronicConditions.joinToString(", ")
    }

    LaunchedEffect(uiState) {
        if (uiState is PatientProfileUiState.Updated) {
            kotlinx.coroutines.delay(2000)
            viewModel.resetToSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мой профиль") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val allergiesList = allergiesText.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            val conditionsList = chronicConditionsText.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }

                            viewModel.updateProfile(
                                dateOfBirth = dateOfBirth,
                                gender = gender,
                                address = address,
                                phoneNumber = phoneNumber,
                                policyNumber = policyNumber,
                                allergies = allergiesList,
                                chronicConditions = conditionsList
                            )
                        },
                        enabled = uiState !is PatientProfileUiState.Loading
                    ) {
                        if (uiState is PatientProfileUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Сохранить")
                        }
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
            when (val currentState = uiState){
                is PatientProfileUiState.Loading -> {
                    if (profileData.fullName.isEmpty()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        ProfileForm(
                            profileData = profileData,
                            dateOfBirth = dateOfBirth,
                            gender = gender,
                            address = address,
                            phoneNumber = phoneNumber,
                            policyNumber = policyNumber,
                            allergiesText = allergiesText,
                            chronicConditionsText = chronicConditionsText,
                            onDateOfBirthChange = { dateOfBirth = it },
                            onGenderChange = { gender = it },
                            onAddressChange = { address = it },
                            onPhoneNumberChange = { phoneNumber = it },
                            onPolicyNumberChange = { policyNumber = it },
                            onAllergiesTextChange = { allergiesText = it },
                            onChronicConditionsTextChange = { chronicConditionsText = it },
                            dateFormatter = dateFormatter,
                            context = context,
                            enabled = false
                        )
                    }
                }
                is PatientProfileUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = currentState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Text("Повторить")
                        }
                    }
                }
                is PatientProfileUiState.Success, is PatientProfileUiState.Updated -> {
                    ProfileForm(
                        profileData = profileData,
                        dateOfBirth = dateOfBirth,
                        gender = gender,
                        address = address,
                        phoneNumber = phoneNumber,
                        policyNumber = policyNumber,
                        allergiesText = allergiesText,
                        chronicConditionsText = chronicConditionsText,
                        onDateOfBirthChange = { dateOfBirth = it },
                        onGenderChange = { gender = it },
                        onAddressChange = { address = it },
                        onPhoneNumberChange = { phoneNumber = it },
                        onPolicyNumberChange = { policyNumber = it },
                        onAllergiesTextChange = { allergiesText = it },
                        onChronicConditionsTextChange = { chronicConditionsText = it },
                        dateFormatter = dateFormatter,
                        context = context,
                        enabled = true
                    )

                    if (currentState is PatientProfileUiState.Updated) {
                        LaunchedEffect(Unit) {
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileForm(
    profileData: com.example.medicalhomevisit.presentation.viewmodel.PatientProfileData,
    dateOfBirth: Date?,
    gender: Gender,
    address: String,
    phoneNumber: String,
    policyNumber: String,
    allergiesText: String,
    chronicConditionsText: String,
    onDateOfBirthChange: (Date?) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onAddressChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onPolicyNumberChange: (String) -> Unit,
    onAllergiesTextChange: (String) -> Unit,
    onChronicConditionsTextChange: (String) -> Unit,
    dateFormatter: SimpleDateFormat,
    context: android.content.Context,
    enabled: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = profileData.fullName,
                onValueChange = { },
                label = { Text("ФИО") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            )
        }

        item {
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

                if (dateOfBirth != null) {
                    Text(
                        text = "Дата рождения: ${dateFormatter.format(dateOfBirth)}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )

                    if (enabled) {
                        TextButton(onClick = {
                            val calendar = Calendar.getInstance()
                            calendar.time = dateOfBirth
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCalendar = Calendar.getInstance()
                                    newCalendar.set(year, month, dayOfMonth)
                                    onDateOfBirthChange(newCalendar.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Text("Изменить")
                        }
                    }
                } else {
                    Text(
                        text = "Дата рождения не указана",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )

                    if (enabled) {
                        Button(onClick = {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCalendar = Calendar.getInstance()
                                    newCalendar.set(year, month, dayOfMonth)
                                    onDateOfBirthChange(newCalendar.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Text("Выбрать")
                        }
                    }
                }
            }
        }

        item {
            GenderSelection(
                selectedGender = gender,
                onGenderChange = onGenderChange,
                enabled = enabled
            )
        }

        item {
            OutlinedTextField(
                value = address,
                onValueChange = onAddressChange,
                label = { Text("Адрес") },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Home, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }

        item {
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                label = { Text("Номер телефона") },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )
            )
        }

        item {
            OutlinedTextField(
                value = policyNumber,
                onValueChange = onPolicyNumberChange,
                label = { Text("Номер полиса") },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Badge, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }

        item {
            OutlinedTextField(
                value = allergiesText,
                onValueChange = onAllergiesTextChange,
                label = { Text("Аллергии (через запятую)") },
                placeholder = { Text("Пенициллин, орехи, пыльца") },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Warning, contentDescription = null)
                },
                minLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }

        item {
            OutlinedTextField(
                value = chronicConditionsText,
                onValueChange = onChronicConditionsTextChange,
                label = { Text("Хронические заболевания (через запятую)") },
                placeholder = { Text("Гипертония, диабет") },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Healing, contentDescription = null)
                },
                minLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
        }
    }
}

@Composable
private fun GenderSelection(
    selectedGender: Gender,
    onGenderChange: (Gender) -> Unit,
    enabled: Boolean
) {
    Column {
        Text(
            text = "Пол",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Gender.values().forEach { gender ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = selectedGender == gender,
                            onClick = { if (enabled) onGenderChange(gender) }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedGender == gender,
                        onClick = { if (enabled) onGenderChange(gender) },
                        enabled = enabled
                    )
                    Text(
                        text = when (gender) {
                            Gender.MALE -> "Мужской"
                            Gender.FEMALE -> "Женский"
                            Gender.UNKNOWN -> "Не указан"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}