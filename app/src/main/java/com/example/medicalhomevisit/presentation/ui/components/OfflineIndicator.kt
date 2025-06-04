// presentation/ui/components/OfflineIndicator.kt
package com.example.medicalhomevisit.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalhomevisit.domain.model.Visit
import com.example.medicalhomevisit.presentation.ui.visitlist.VisitListScreen
import com.example.medicalhomevisit.presentation.viewmodel.VisitListViewModel

@Composable
fun OfflineIndicator(
    isOffline: Boolean,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onSyncClick() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Офлайн режим",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Офлайн режим",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Нажмите для синхронизации",
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Синхронизировать",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SyncingIndicator(
    isSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isSyncing,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Синхронизация данных...",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Обновленный VisitListScreen с индикатором
@Composable
fun VisitListScreenWithOfflineIndicator(
    viewModel: VisitListViewModel,
    onVisitClick: (Visit) -> Unit,
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val filterParams by viewModel.filterParams.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Индикатор офлайн режима
        OfflineIndicator(
            isOffline = isOffline,
            onSyncClick = { viewModel.syncVisits() }
        )

        // Основной контент экрана
        VisitListScreen(
            viewModel = viewModel,
            onVisitClick = onVisitClick,
            onProfileClick = onProfileClick
        )
    }
}