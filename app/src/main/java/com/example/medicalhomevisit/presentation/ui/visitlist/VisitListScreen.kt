// VisitListScreen.kt
package com.example.medicalhomevisit.presentation.ui.visitlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.medicalhomevisit.domain.model.Visit
import com.example.medicalhomevisit.presentation.viewmodel.VisitListUiState
import com.example.medicalhomevisit.presentation.viewmodel.VisitListViewModel
import com.example.medicalhomevisit.presentation.ui.components.*


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun VisitListScreen(
    viewModel: VisitListViewModel,
    onVisitClick: (Visit) -> Unit,
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val filterParams by viewModel.filterParams.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Pull-to-refresh состояние
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState is VisitListUiState.Loading,
        onRefresh = { viewModel.loadVisits() }
    )

    // Состояние для отображения панели фильтров
    var showFilters by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Визиты на сегодня") },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
                    }
                    IconButton(onClick = { viewModel.loadVisits() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                    // Добавляем кнопку профиля
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "Профиль")
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
            Column {
                // Панель фильтров
                AnimatedVisibility(visible = showFilters) {
                    Column {
                        // Выбор даты
                        DateSelector(
                            selectedDate = filterParams.selectedDate,
                            onDateChange = { viewModel.updateSelectedDate(it) }
                        )

                        // Фильтр по статусу
                        StatusFilter(
                            selectedStatus = filterParams.selectedStatus,
                            onStatusSelected = { viewModel.updateSelectedStatus(it) }
                        )

                        // Поисковая строка
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                            onSearch = { /* Дополнительные действия при поиске */ }
                        )

                        // Выбор группировки
                        GroupingSelector(
                            selectedGrouping = filterParams.groupingType,
                            onGroupingSelected = { viewModel.updateGroupingType(it) }
                        )

                        Divider()
                    }
                }

                // Основное содержимое
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (val state = uiState) {
                        is VisitListUiState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        is VisitListUiState.Empty -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Нет визитов для отображения",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Измените параметры фильтрации или выберите другую дату",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        is VisitListUiState.Success -> {
                            Box(
                                modifier = Modifier.pullRefresh(pullRefreshState)
                            ) {
                                GroupedVisitList(
                                    visits = state.visits,
                                    groupingType = filterParams.groupingType,
                                    onVisitClick = onVisitClick
                                )

                                // Индикатор Pull-to-refresh
                                PullRefreshIndicator(
                                    refreshing = uiState is VisitListUiState.Loading,
                                    state = pullRefreshState,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            }
                        }
                        is VisitListUiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Ошибка загрузки данных",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.loadVisits() }) {
                                    Text("Повторить")
                                }
                            }
                        }
                    }
                }
            }

            // Индикатор офлайн-режима
            if (isOffline) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "Автономный режим",
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Компонент для тестирования офлайн режима (только в debug режиме)
            OfflineTestComponent(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

// Компонент для тестирования офлайн режима
@Composable
fun OfflineTestComponent(
    viewModel: VisitListViewModel,
    modifier: Modifier = Modifier
) {
    var showTestPanel by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Тестовая панель
        AnimatedVisibility(
            visible = showTestPanel,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Card(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 320.dp)
                    .offset(x = (-48).dp, y = (-48).dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "🧪 OFFLINE TEST PANEL",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { viewModel.getSyncStats() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Stats", style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = { viewModel.checkSyncStatus() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Check", style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = { viewModel.syncVisits() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Sync", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Text(
                        "Проверьте логи с тегом 'VisitListViewModel'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Кнопка для показа/скрытия панели тестирования
        FloatingActionButton(
            onClick = { showTestPanel = !showTestPanel },
            modifier = Modifier.size(40.dp),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Icon(
                if (showTestPanel) Icons.Default.Close else Icons.Default.BugReport,
                contentDescription = "Test Panel",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}