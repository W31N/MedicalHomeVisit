package com.example.medicalhomevisit.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(
    selectedDate: Date,
    onDateChange: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            val prevDay = Calendar.getInstance().apply {
                time = selectedDate
                add(Calendar.DAY_OF_MONTH, -1)
            }.time
            onDateChange(prevDay)
        }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Предыдущий день")
        }

        Row(
            modifier = Modifier.clickable { showDatePicker = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isToday(selectedDate)) "Сегодня" else dateFormat.format(selectedDate),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(Icons.Default.DateRange, contentDescription = "Выбрать дату")
        }

        IconButton(onClick = {
            val nextDay = Calendar.getInstance().apply {
                time = selectedDate
                add(Calendar.DAY_OF_MONTH, 1)
            }.time
            onDateChange(nextDay)
        }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Следующий день")
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.time
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateChange(Date(it))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun isToday(date: Date): Boolean {
    val today = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { time = date }
    return today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
           today.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
}