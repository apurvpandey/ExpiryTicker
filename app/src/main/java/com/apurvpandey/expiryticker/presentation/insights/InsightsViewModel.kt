package com.apurvpandey.expiryticker.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apurvpandey.expiryticker.core.util.Insights
import com.apurvpandey.expiryticker.core.util.SpendInsights
import com.apurvpandey.expiryticker.domain.repository.ExpiryItemRepository
import com.apurvpandey.expiryticker.domain.repository.RenewalHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class InsightsViewModel(
    items: ExpiryItemRepository,
    history: RenewalHistoryRepository
) : ViewModel() {

    /** Null while loading. */
    val insights: StateFlow<Insights?> = combine(items.observeAll(), history.observeAll()) { i, h ->
        SpendInsights.compute(i, h, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        fun factory(items: ExpiryItemRepository, history: RenewalHistoryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = InsightsViewModel(items, history) as T
            }
    }
}
