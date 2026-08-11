package com.apurvpandey.expiryticker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apurvpandey.expiryticker.core.util.ExpiryStatusCalculator
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus
import com.apurvpandey.expiryticker.domain.repository.ExpiryItemRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class DashboardFilter(val label: String) {
    ALL("All"),
    OVERDUE("Overdue"),
    UPCOMING("Upcoming"),
    COMPLETED("Completed")
}

data class ExpiryItemWithStatus(
    val item: ExpiryItem,
    val status: ExpiryStatus
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val overdueCount: Int = 0,
    val next7DaysCount: Int = 0,
    val next30DaysCount: Int = 0,
    val displayItems: List<ExpiryItemWithStatus> = emptyList(),
    val currentFilter: DashboardFilter = DashboardFilter.ALL,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: ExpiryItemRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(DashboardFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)

    val uiState = combine(
        repository.observeAll(),
        _filter,
        _searchQuery,
        _isSearchActive
    ) { items, filter, query, searchActive ->
        val today = LocalDate.now()
        val allWithStatus = items.map { item ->
            ExpiryItemWithStatus(
                item = item,
                status = ExpiryStatusCalculator.calculate(item.dueDate, item.isCompleted, today)
            )
        }

        val overdueCount = allWithStatus.count { it.status is ExpiryStatus.Overdue }
        val next7DaysCount = allWithStatus.count { ws ->
            !ws.item.isCompleted && when (val s = ws.status) {
                is ExpiryStatus.DueToday -> true
                is ExpiryStatus.Active -> s.daysRemaining <= 7
                else -> false
            }
        }
        val next30DaysCount = allWithStatus.count { ws ->
            !ws.item.isCompleted && when (val s = ws.status) {
                is ExpiryStatus.DueToday -> true
                is ExpiryStatus.Active -> s.daysRemaining <= 30
                else -> false
            }
        }

        val filtered = allWithStatus
            .filter { ws ->
                when (filter) {
                    DashboardFilter.ALL -> !ws.item.isCompleted
                    DashboardFilter.OVERDUE -> ws.status is ExpiryStatus.Overdue
                    DashboardFilter.UPCOMING ->
                        !ws.item.isCompleted && (ws.status is ExpiryStatus.Active || ws.status is ExpiryStatus.DueToday)
                    DashboardFilter.COMPLETED -> ws.item.isCompleted
                }
            }
            .filter { ws ->
                query.isBlank() || ws.item.title.contains(query, ignoreCase = true)
            }
            .sortedBy { it.item.dueDate }

        DashboardUiState(
            isLoading = false,
            overdueCount = overdueCount,
            next7DaysCount = next7DaysCount,
            next30DaysCount = next30DaysCount,
            displayItems = filtered,
            currentFilter = filter,
            searchQuery = query,
            isSearchActive = searchActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun setFilter(filter: DashboardFilter) { _filter.value = filter }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun toggleSearch() {
        _isSearchActive.value = !_isSearchActive.value
        if (!_isSearchActive.value) _searchQuery.value = ""
    }

    companion object {
        fun factory(repository: ExpiryItemRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    DashboardViewModel(repository) as T
            }
    }
}
