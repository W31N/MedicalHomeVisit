// VisitListScreen.kt
package com.example.medicalhomevisit.presentation.ui.visitlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
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
        }
    }
}