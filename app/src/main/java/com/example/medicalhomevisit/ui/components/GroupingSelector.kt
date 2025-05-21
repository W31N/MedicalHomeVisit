package com.example.medicalhomevisit.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.medicalhomevisit.ui.visitlist.GroupingType

@Composable
fun GroupingSelector(
    selectedGrouping: GroupingType,
    onGroupingSelected: (GroupingType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                when (selectedGrouping) {
                    GroupingType.NONE -> "Без группировки"
                    GroupingType.TIME -> "По времени"
                    GroupingType.STATUS -> "По статусу"
                    GroupingType.ADDRESS -> "По адресу"
                }
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Без группировки") },
                onClick = {
                    onGroupingSelected(GroupingType.NONE)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("По времени") },
                onClick = {
                    onGroupingSelected(GroupingType.TIME)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("По статусу") },
                onClick = {
                    onGroupingSelected(GroupingType.STATUS)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("По адресу") },
                onClick = {
                    onGroupingSelected(GroupingType.ADDRESS)
                    expanded = false
                }
            )
        }
    }
}