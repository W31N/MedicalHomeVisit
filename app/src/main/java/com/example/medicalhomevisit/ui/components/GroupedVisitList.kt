package com.example.medicalhomevisit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.medicalhomevisit.data.model.Visit
import com.example.medicalhomevisit.data.model.VisitStatus
import com.example.medicalhomevisit.ui.visitlist.GroupingType
import java.util.*

@Composable
fun GroupedVisitList(
    visits: List<Visit>,
    groupingType: GroupingType,
    onVisitClick: (Visit) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedVisits = when (groupingType) {
        GroupingType.NONE -> mapOf("" to visits)

        GroupingType.TIME -> visits.groupBy { visit ->
            val hour = Calendar.getInstance().apply { time = visit.scheduledTime }.get(Calendar.HOUR_OF_DAY)
            when {
                hour < 12 -> "Утро (до 12:00)"
                hour < 17 -> "День (12:00 - 17:00)"
                else -> "Вечер (после 17:00)"
            }
        }.toSortedMap(compareBy {
            when (it) {
                "Утро (до 12:00)" -> 0
                "День (12:00 - 17:00)" -> 1
                else -> 2
            }
        })

        GroupingType.STATUS -> visits.groupBy { visit ->
            when (visit.status) {
                VisitStatus.PLANNED -> "Запланировано"
                VisitStatus.IN_PROGRESS -> "В процессе"
                VisitStatus.COMPLETED -> "Завершено"
                VisitStatus.CANCELLED -> "Отменено"
            }
        }

        GroupingType.ADDRESS -> visits.groupBy { visit ->
            // Простая группировка по первому слову адреса (название улицы)
            // В реальном приложении нужна более сложная логика
            visit.address.split(" ").firstOrNull() ?: "Без адреса"
        }.toSortedMap()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedVisits.forEach { (groupName, visitsInGroup) ->
            if (groupingType != GroupingType.NONE) {
                item {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            items(visitsInGroup) { visit ->
                VisitCard(
                    visit = visit,
                    onClick = { onVisitClick(visit) }
                )
            }
        }
    }
}