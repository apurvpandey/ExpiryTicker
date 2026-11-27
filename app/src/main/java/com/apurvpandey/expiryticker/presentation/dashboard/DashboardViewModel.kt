package com.apurvpandey.expiryticker.presentation.dashboard

import com.apurvpandey.expiryticker.R
import com.apurvpandey.expiryticker.presentation.common.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apurvpandey.expiryticker.core.util.DueBucket
import com.apurvpandey.expiryticker.core.util.ExpiryStatusCalculator
import com.apurvpandey.expiryticker.core.util.ItemGrouping
import com.apurvpandey.expiryticker.core.util.ItemSearch
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import com.apurvpandey.expiryticker.domain.repository.ExpiryItemRepository
import com.apurvpandey.expiryticker.domain.usecase.ItemActions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class DashboardFilter { ALL, OVERDUE, UPCOMING, COMPLETED }

enum class DashboardSort { DUE_DATE, TITLE, AMOUNT, RECENT }

data class ExpiryItemWithStatus(
    val item: ExpiryItem,
    val status: ExpiryStatus
)

/** A list section; [bucket] is null when the list is not grouped by due date. */
data class DashboardSection(
    val bucket: DueBucket?,
    val items: List<ExpiryItemWithStatus>
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val overdueCount: Int = 0,
    val next7DaysCount: Int = 0,
    val next30DaysCount: Int = 0,
    val next30DaysAmountPaise: Long = 0L,
    val totalActiveCount: Int = 0,
    val nextUp: ExpiryItemWithStatus? = null,
    val sections: List<DashboardSection> = emptyList(),
    val resultCount: Int = 0,
    val currentFilter: DashboardFilter = DashboardFilter.ALL,
    val sort: DashboardSort = DashboardSort.DUE_DATE,
    val categoryFilter: RenewalCategory? = null,
    val availableCategories: List<RenewalCategory> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false
) {
    val hasActiveRefinement: Boolean
        get() = searchQuery.isNotBlank() || categoryFilter != null
}

/** One-off events shown as a snackbar with an optional undo. */
data class DashboardEvent(val message: UiText, val undoItem: ExpiryItem?, val historyId: Long? = null)

class DashboardViewModel(
    repository: ExpiryItemRepository,
    private val actions: ItemActions
) : ViewModel() {

    private val _filter = MutableStateFlow(DashboardFilter.ALL)
    private val _sort = MutableStateFlow(DashboardSort.DUE_DATE)
    private val _category = MutableStateFlow<RenewalCategory?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    // Category names in the user's language, supplied by the screen (a ViewModel has no resources).
    private val _categoryLabels = MutableStateFlow<Map<RenewalCategory, String>>(emptyMap())
    private val _events = MutableSharedFlow<DashboardEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<DashboardEvent> = _events.asSharedFlow()

    private data class Controls(
        val filter: DashboardFilter,
        val sort: DashboardSort,
        val category: RenewalCategory?,
        val query: String,
        val searchActive: Boolean
    )

    private val controls = combine(
        _filter, _sort, _category, _searchQuery, _isSearchActive
    ) { f, s, c, q, a -> Controls(f, s, c, q, a) }

    val uiState = combine(repository.observeAll(), controls, _categoryLabels) { items, ctl, labels ->
        val today = LocalDate.now()
        val all = items.map { item ->
            ExpiryItemWithStatus(
                item = item,
                status = ExpiryStatusCalculator.calculate(item.dueDate, item.isCompleted, today)
            )
        }
        val open = all.filter { !it.item.isCompleted }
        fun daysLeft(ws: ExpiryItemWithStatus): Int? = when (val s = ws.status) {
            is ExpiryStatus.DueToday -> 0
            is ExpiryStatus.Active -> s.daysRemaining
            else -> null
        }
        val next30 = open.filter { (daysLeft(it) ?: Int.MAX_VALUE) <= 30 }

        val filtered = all
            .filter { ws ->
                when (ctl.filter) {
                    DashboardFilter.ALL -> !ws.item.isCompleted
                    DashboardFilter.OVERDUE -> ws.status is ExpiryStatus.Overdue
                    DashboardFilter.UPCOMING -> !ws.item.isCompleted && daysLeft(ws) != null
                    DashboardFilter.COMPLETED -> ws.item.isCompleted
                }
            }
            .filter { ctl.category == null || it.item.category == ctl.category }
            .filter { ws ->
                ItemSearch.matches(
                    ctl.query,
                    listOfNotNull(
                        ws.item.title,
                        ws.item.notes,
                        // Both the English name and the one the user sees, so either can be typed.
                        ws.item.category.displayName,
                        labels[ws.item.category]
                    )
                )
            }

        val sorted = when (ctl.sort) {
            DashboardSort.DUE_DATE ->
                if (ctl.filter == DashboardFilter.COMPLETED) filtered.sortedByDescending { it.item.dueDate }
                else filtered.sortedBy { it.item.dueDate }
            DashboardSort.TITLE -> filtered.sortedBy { it.item.title.lowercase() }
            DashboardSort.AMOUNT -> filtered.sortedByDescending { it.item.amountPaise ?: -1L }
            DashboardSort.RECENT -> filtered.sortedByDescending { it.item.createdAt }
        }

        val groupByDue = ctl.sort == DashboardSort.DUE_DATE && ctl.filter != DashboardFilter.COMPLETED
        val sections = if (groupByDue) {
            sorted.groupBy { ItemGrouping.bucketFor(it.item.dueDate, today) }
                .toSortedMap()
                .map { (bucket, list) -> DashboardSection(bucket, list) }
        } else if (sorted.isEmpty()) emptyList() else listOf(DashboardSection(null, sorted))

        DashboardUiState(
            isLoading = false,
            overdueCount = all.count { it.status is ExpiryStatus.Overdue },
            next7DaysCount = open.count { (daysLeft(it) ?: Int.MAX_VALUE) <= 7 },
            next30DaysCount = next30.size,
            next30DaysAmountPaise = next30.sumOf { it.item.amountPaise ?: 0L },
            totalActiveCount = open.size,
            nextUp = open.minByOrNull { it.item.dueDate },
            sections = sections,
            resultCount = sorted.size,
            currentFilter = ctl.filter,
            sort = ctl.sort,
            categoryFilter = ctl.category,
            availableCategories = all.map { it.item.category }.distinct().sortedBy { it.ordinal },
            searchQuery = ctl.query,
            isSearchActive = ctl.searchActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun setFilter(filter: DashboardFilter) { _filter.value = filter }
    fun setSort(sort: DashboardSort) { _sort.value = sort }
    fun setCategory(category: RenewalCategory?) { _category.value = category }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setCategoryLabels(labels: Map<RenewalCategory, String>) { _categoryLabels.value = labels }
    fun clearRefinements() {
        _category.value = null
        _searchQuery.value = ""
    }

    fun toggleSearch() {
        _isSearchActive.value = !_isSearchActive.value
        if (!_isSearchActive.value) _searchQuery.value = ""
    }

    fun markDone(item: ExpiryItem) {
        viewModelScope.launch {
            val outcome = actions.markDone(item)
            _events.emit(
                DashboardEvent(
                    message = UiText.of(
                        if (outcome.renewed) R.string.msg_item_renewed else R.string.msg_item_completed,
                        item.title
                    ),
                    undoItem = outcome.previous,
                    historyId = outcome.historyId
                )
            )
        }
    }

    fun delete(item: ExpiryItem) {
        viewModelScope.launch {
            actions.delete(item)
            _events.emit(DashboardEvent(UiText.of(R.string.msg_item_deleted, item.title), undoItem = item))
        }
    }

    fun undo(previous: ExpiryItem, historyId: Long?) {
        viewModelScope.launch { actions.restore(previous, historyId) }
    }

    companion object {
        fun factory(
            repository: ExpiryItemRepository,
            actions: ItemActions
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DashboardViewModel(repository, actions) as T
        }
    }
}
