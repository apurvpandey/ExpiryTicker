package com.apurvpandey.expiryticker.presentation.dashboard

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apurvpandey.expiryticker.AppContainer
import com.apurvpandey.expiryticker.R
import com.apurvpandey.expiryticker.core.util.CurrencyFormatter
import com.apurvpandey.expiryticker.core.util.DueBucket
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import com.apurvpandey.expiryticker.presentation.components.ExpiryItemCard
import com.apurvpandey.expiryticker.presentation.components.AllClearHero
import com.apurvpandey.expiryticker.presentation.components.NextUpHero
import com.apurvpandey.expiryticker.presentation.components.SummaryCard
import com.apurvpandey.expiryticker.presentation.theme.ExpiryTheme
import com.apurvpandey.expiryticker.presentation.theme.ExpiryTickerTheme
import kotlinx.coroutines.launch
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
        factory = DashboardViewModel.factory(container.expiryItemRepository, container.itemActions)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            // A new event replaces the previous snackbar instead of queueing behind it.
            snackbarHostState.currentSnackbarData?.dismiss()
            launch {
                val result = snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.undoItem?.let { "Undo" },
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    event.undoItem?.let(viewModel::undo)
                }
            }
        }
    }

    DashboardScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateToAdd = onNavigateToAdd,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToSettings = onNavigateToSettings,
        onFilterSelected = viewModel::setFilter,
        onSortSelected = viewModel::setSort,
        onCategorySelected = viewModel::setCategory,
        onSearchQueryChanged = viewModel::setSearchQuery,
        onToggleSearch = viewModel::toggleSearch,
        onClearRefinements = viewModel::clearRefinements,
        onMarkDone = viewModel::markDone,
        onDelete = viewModel::delete
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onFilterSelected: (DashboardFilter) -> Unit,
    onSortSelected: (DashboardSort) -> Unit = {},
    onCategorySelected: (RenewalCategory?) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onClearRefinements: () -> Unit = {},
    onMarkDone: (ExpiryItem) -> Unit = {},
    onDelete: (ExpiryItem) -> Unit = {}
) {
    val todayLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
    }
    val listState = rememberLazyListState()
    val isFabExpanded by remember {
        derivedStateOf { !listState.isScrollInProgress && listState.firstVisibleItemIndex < 2 }
    }
    val searchFocus = remember { FocusRequester() }
    var sortMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSearchActive) {
        if (uiState.isSearchActive) searchFocus.requestFocus()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        Box {
                            IconButton(onClick = { sortMenuOpen = true }) {
                                Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = sortMenuOpen,
                                onDismissRequest = { sortMenuOpen = false }
                            ) {
                                DashboardSort.entries.forEach { sort ->
                                    DropdownMenuItem(
                                        text = { Text(sort.label) },
                                        leadingIcon = {
                                            if (sort == uiState.sort) {
                                                Icon(Icons.Default.Check, contentDescription = "Selected")
                                            }
                                        },
                                        onClick = {
                                            sortMenuOpen = false
                                            onSortSelected(sort)
                                        }
                                    )
                                }
                            }
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
                        placeholder = { Text("Search title, notes or category") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search text")
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .focusRequester(searchFocus)
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                expanded = isFabExpanded,
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add item") },
                text = { Text("Add item") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!uiState.isLoading) {
                item(key = "hero") {
                    val next = uiState.nextUp
                    if (next != null) {
                        NextUpHero(
                            item = next.item,
                            status = next.status,
                            onOpen = { onNavigateToDetail(next.item.id) },
                            onMarkDone = { onMarkDone(next.item) },
                            modifier = Modifier.animateItem()
                        )
                    } else if (uiState.totalActiveCount == 0 && uiState.sections.isNotEmpty()) {
                        AllClearHero(modifier = Modifier.animateItem())
                    }
                }
            }

            if (!uiState.isLoading) item {
                val c = ExpiryTheme.colors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCard(
                        label = "Overdue",
                        count = uiState.overdueCount,
                        accent = c.overdue.accent,
                        onClick = { onFilterSelected(DashboardFilter.OVERDUE) },
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        label = "Next 7 days",
                        count = uiState.next7DaysCount,
                        accent = c.soon.accent,
                        onClick = { onFilterSelected(DashboardFilter.UPCOMING) },
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        label = "Next 30 days",
                        count = uiState.next30DaysCount,
                        accent = c.ok.accent,
                        onClick = { onFilterSelected(DashboardFilter.UPCOMING) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (uiState.next30DaysAmountPaise > 0) {
                item {
                    Text(
                        text = "${CurrencyFormatter.format(uiState.next30DaysAmountPaise)} to pay in the next 30 days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            if (!uiState.isLoading) item {
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

            if (uiState.availableCategories.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.availableCategories) { category ->
                            val selected = uiState.categoryFilter == category
                            FilterChip(
                                selected = selected,
                                onClick = { onCategorySelected(if (selected) null else category) },
                                label = { Text(category.displayName) }
                            )
                        }
                    }
                }
            }

            if (!uiState.isLoading && uiState.sections.isEmpty()) {
                item(key = "empty_state") {
                    if (uiState.hasActiveRefinement) {
                        NoMatchesState(onClear = onClearRefinements)
                    } else {
                        AnimatedContent(
                            targetState = uiState.currentFilter,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "empty_content"
                        ) { filter ->
                            EmptyState(filter = filter, onAddItem = onNavigateToAdd)
                        }
                    }
                }
            } else {
                uiState.sections.forEach { section ->
                    section.bucket?.let { bucket ->
                        item(key = "header_${bucket.name}") {
                            SectionHeader(
                                bucket = bucket,
                                count = section.items.size,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                    items(section.items, key = { it.item.id }) { ws ->
                        SwipeableItemCard(
                            ws = ws,
                            canComplete = !ws.item.isCompleted,
                            onClick = { onNavigateToDetail(ws.item.id) },
                            onMarkDone = { onMarkDone(ws.item) },
                            onDelete = { onDelete(ws.item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(bucket: DueBucket, count: Int, modifier: Modifier = Modifier) {
    val color = if (bucket == DueBucket.OVERDUE) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = "${bucket.label}  ·  $count",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp)
            .semantics { contentDescription = "${bucket.label}, $count items" }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableItemCard(
    ws: ExpiryItemWithStatus,
    canComplete: Boolean,
    onClick: () -> Unit,
    onMarkDone: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> if (canComplete) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMarkDone()
                    // Recurring items stay in the list with a new date, so snap back.
                    ws.item.recurrence == RecurrenceType.NONE
                } else false
                SwipeToDismissBoxValue.EndToStart -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )
    // The swipe state is saved per item; a restored (undone) item must not come back dismissed.
    LaunchedEffect(Unit) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) {
            state.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = canComplete,
        backgroundContent = {
            val toEnd = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val bg = when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg, MaterialTheme.shapes.large)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (toEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (state.dismissDirection != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        imageVector = if (toEnd) Icons.Default.Check else Icons.Default.Delete,
                        contentDescription = if (toEnd) "Mark done" else "Delete",
                        tint = if (toEnd) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    ) {
        ExpiryItemCard(item = ws.item, status = ws.status, onClick = onClick)
    }
}

@Composable
private fun NoMatchesState(onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Outlined.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("No matches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Nothing fits your search or category.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        OutlinedButton(onClick = onClear) { Text("Clear search and category") }
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
    if (filter == DashboardFilter.ALL) {
        IllustrationEmptyState(onAddItem = onAddItem)
        return
    }

    val (icon, title, subtitle) = when (filter) {
        DashboardFilter.ALL -> Triple(Icons.Outlined.Inbox, "", "")
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
            Icons.Outlined.TaskAlt,
            "No completed items",
            "Items you mark as renewed or completed will appear here."
        )
    }

    EmptyStateContent(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onAddItem = onAddItem
    )
}

@Composable
private fun IllustrationEmptyState(onAddItem: () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(durationMillis = 200))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp, horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.img_empty_state),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
            Text(
                text = "Nothing to track yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Add an expiry or renewal and ExpiryTicker will remind you before it's due.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onAddItem) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Add your first item")
            }
        }
    }
}

@Composable
private fun EmptyStateContent(
    icon: ImageVector,
    title: String,
    subtitle: String,
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
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
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────

// ─── Previews ───────────────────────────────────────────────────────────────

private val previewToday: LocalDate = LocalDate.now()

private val previewItems = listOf(
    ExpiryItemWithStatus(
        ExpiryItem(
            id = 1L, title = "Car Insurance", category = RenewalCategory.INSURANCE,
            dueDate = previewToday.plusDays(5), recurrence = RecurrenceType.YEARLY
        ),
        ExpiryStatus.Active(5)
    ),
    ExpiryItemWithStatus(
        ExpiryItem(
            id = 2L, title = "Passport", category = RenewalCategory.TRAVEL,
            dueDate = previewToday.minusDays(2), amountPaise = 150000
        ),
        ExpiryStatus.Overdue(2)
    ),
    ExpiryItemWithStatus(
        ExpiryItem(
            id = 3L, title = "Netflix", category = RenewalCategory.SUBSCRIPTION,
            dueDate = previewToday.plusDays(22), recurrence = RecurrenceType.MONTHLY,
            amountPaise = 64900
        ),
        ExpiryStatus.Active(22)
    ),
)

private fun previewState() = DashboardUiState(
    isLoading = false,
    overdueCount = 1,
    next7DaysCount = 1,
    next30DaysCount = 2,
    next30DaysAmountPaise = 64900,
    totalActiveCount = 3,
    sections = listOf(
        DashboardSection(DueBucket.OVERDUE, listOf(previewItems[1])),
        DashboardSection(DueBucket.THIS_WEEK, listOf(previewItems[0])),
        DashboardSection(DueBucket.THIS_MONTH, listOf(previewItems[2]))
    ),
    resultCount = 3,
    availableCategories = listOf(RenewalCategory.INSURANCE, RenewalCategory.TRAVEL, RenewalCategory.SUBSCRIPTION)
)

@Preview(showBackground = true, name = "Dashboard - Normal")
@Composable
private fun DashboardPreviewNormal() {
    ExpiryTickerTheme {
        DashboardScreen(
            uiState = previewState(),
            onNavigateToAdd = {}, onNavigateToDetail = {},
            onNavigateToSettings = {}, onFilterSelected = {},
            onSearchQueryChanged = {}, onToggleSearch = {}
        )
    }
}

@Preview(showBackground = true, name = "Dashboard - Empty")
@Composable
private fun DashboardPreviewEmpty() {
    ExpiryTickerTheme {
        DashboardScreen(
            uiState = DashboardUiState(isLoading = false),
            onNavigateToAdd = {}, onNavigateToDetail = {},
            onNavigateToSettings = {}, onFilterSelected = {},
            onSearchQueryChanged = {}, onToggleSearch = {}
        )
    }
}

@Preview(
    showBackground = true, name = "Dashboard - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DashboardPreviewDark() {
    ExpiryTickerTheme {
        DashboardScreen(
            uiState = previewState(),
            onNavigateToAdd = {}, onNavigateToDetail = {},
            onNavigateToSettings = {}, onFilterSelected = {},
            onSearchQueryChanged = {}, onToggleSearch = {}
        )
    }
}
