package com.apurvpandey.expiryticker.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apurvpandey.expiryticker.AppContainer
import com.apurvpandey.expiryticker.presentation.components.ExpiryItemCard
import com.apurvpandey.expiryticker.presentation.components.SummaryCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardRoute(
    container: AppContainer,
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(container.expiryItemRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardScreen(
        uiState = uiState,
        onNavigateToAdd = onNavigateToAdd,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToSettings = onNavigateToSettings,
        onFilterSelected = viewModel::setFilter,
        onSearchQueryChanged = viewModel::setSearchQuery,
        onToggleSearch = viewModel::toggleSearch
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onFilterSelected: (DashboardFilter) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleSearch: () -> Unit
) {
    val todayLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "ExpiryTicker",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = todayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onToggleSearch) {
                            Icon(
                                if (uiState.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (uiState.isSearchActive) "Close search" else "Search"
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                AnimatedVisibility(
                    visible = uiState.isSearchActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = { Text("Search items…") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New reminder") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        label = "Overdue",
                        count = uiState.overdueCount,
                        icon = Icons.Outlined.Warning,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        label = "Due soon",
                        count = uiState.next7DaysCount,
                        icon = Icons.Outlined.Schedule,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DashboardFilter.entries) { filter ->
                        FilterChip(
                            selected = uiState.currentFilter == filter,
                            onClick = { onFilterSelected(filter) },
                            label = { Text(filter.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = filterIcon(filter),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            if (!uiState.isLoading && uiState.displayItems.isEmpty()) {
                item {
                    EmptyState(
                        filter = uiState.currentFilter,
                        onAddItem = onNavigateToAdd
                    )
                }
            } else {
                items(uiState.displayItems, key = { it.item.id }) { ws ->
                    ExpiryItemCard(
                        item = ws.item,
                        status = ws.status,
                        onClick = { onNavigateToDetail(ws.item.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

private fun filterIcon(filter: DashboardFilter): ImageVector = when (filter) {
    DashboardFilter.ALL -> Icons.AutoMirrored.Outlined.List
    DashboardFilter.OVERDUE -> Icons.Outlined.Warning
    DashboardFilter.UPCOMING -> Icons.Outlined.Schedule
    DashboardFilter.COMPLETED -> Icons.Outlined.CheckCircle
}

@Composable
private fun EmptyState(filter: DashboardFilter, onAddItem: () -> Unit) {
    val (icon, title, subtitle) = when (filter) {
        DashboardFilter.ALL -> Triple(
            Icons.Outlined.Inbox,
            "Nothing to track yet",
            "Add your first expiry or renewal and ExpiryTicker will remind you before it's due."
        )
        DashboardFilter.OVERDUE -> Triple(
            Icons.Outlined.CheckCircle,
            "You're all caught up",
            "No overdue items right now."
        )
        DashboardFilter.UPCOMING -> Triple(
            Icons.Outlined.DateRange,
            "Nothing coming up",
            "No renewals or expiries in the near future."
        )
        DashboardFilter.COMPLETED -> Triple(
            Icons.Outlined.CheckCircle,
            "No completed items",
            "Items you mark as renewed or completed will appear here."
        )
    }

    EmptyStateContent(
        icon = icon,
        title = title,
        subtitle = subtitle,
        showAddButton = filter == DashboardFilter.ALL,
        onAddItem = onAddItem
    )
}

@Composable
private fun EmptyStateContent(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showAddButton: Boolean,
    onAddItem: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (showAddButton) {
            Spacer(Modifier.height(4.dp))
            androidx.compose.material3.Button(onClick = onAddItem) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Add your first item")
            }
        }
    }
}
