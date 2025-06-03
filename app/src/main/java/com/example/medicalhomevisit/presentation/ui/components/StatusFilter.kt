// StatusFilter.kt
package com.example.medicalhomevisit.presentation.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.medicalhomevisit.domain.model.VisitStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusFilter(
    selectedStatus: VisitStatus?,
    onStatusSelected: (VisitStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = when(selectedStatus) {
            null -> 0
            VisitStatus.PLANNED -> 1
            VisitStatus.IN_PROGRESS -> 2
            VisitStatus.COMPLETED -> 3
            VisitStatus.CANCELLED -> 4
        },
        edgePadding = 16.dp,
        modifier = modifier
    ) {
        Tab(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            text = { Text("Все") }
        )
        Tab(
            selected = selectedStatus == VisitStatus.PLANNED,
            onClick = { onStatusSelected(VisitStatus.PLANNED) },
            text = { Text("Запланировано") }
        )
        Tab(
            selected = selectedStatus == VisitStatus.IN_PROGRESS,
            onClick = { onStatusSelected(VisitStatus.IN_PROGRESS) },
            text = { Text("В процессе") }
        )
        Tab(
            selected = selectedStatus == VisitStatus.COMPLETED,
            onClick = { onStatusSelected(VisitStatus.COMPLETED) },
            text = { Text("Завершено") }
        )
        Tab(
            selected = selectedStatus == VisitStatus.CANCELLED,
            onClick = { onStatusSelected(VisitStatus.CANCELLED) },
            text = { Text("Отменено") }
        )
    }
}