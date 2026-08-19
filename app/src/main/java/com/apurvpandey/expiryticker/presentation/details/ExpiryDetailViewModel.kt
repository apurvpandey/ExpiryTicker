package com.apurvpandey.expiryticker.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apurvpandey.expiryticker.core.util.ExpiryStatusCalculator
import com.apurvpandey.expiryticker.core.util.RecurrenceCalculator
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.repository.ExpiryItemRepository
import com.apurvpandey.expiryticker.notification.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class ExpiryDetailUiState(
    val isLoading: Boolean = true,
    val item: ExpiryItem? = null,
    val status: ExpiryStatus? = null,
    val showDeleteDialog: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

class ExpiryDetailViewModel(
    private val itemId: Long,
    private val repository: ExpiryItemRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpiryDetailUiState())
    val uiState: StateFlow<ExpiryDetailUiState> = _uiState.asStateFlow()

    init {
        observeItem()
    }

    private fun observeItem() {
        viewModelScope.launch {
            repository.observeById(itemId).collect { item ->
                if (item != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            item = item,
                            status = ExpiryStatusCalculator.calculate(
                                item.dueDate, item.isCompleted, LocalDate.now()
                            )
                        )
                    }
                } else if (!_uiState.value.isDeleted) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun showDeleteConfirmation() = _uiState.update { it.copy(showDeleteDialog = true) }
    fun dismissDeleteConfirmation() = _uiState.update { it.copy(showDeleteDialog = false) }
    fun clearSnackbarMessage() = _uiState.update { it.copy(snackbarMessage = null) }

    fun deleteItem() {
        val item = _uiState.value.item ?: return
        viewModelScope.launch {
            reminderScheduler.cancelReminder(item.id)
            repository.delete(item)
            _uiState.update { it.copy(showDeleteDialog = false, isDeleted = true) }
        }
    }

    fun markRenewed() {
        val item = _uiState.value.item ?: return
        viewModelScope.launch {
            val now = Instant.now()
            if (item.recurrence != RecurrenceType.NONE) {
                val nextDueDate = RecurrenceCalculator.calculateNextDueDate(item.dueDate, item.recurrence)
                val updated = item.copy(
                    dueDate = nextDueDate,
                    lastRenewedAt = now,
                    updatedAt = now
                )
                repository.update(updated)
                reminderScheduler.cancelReminder(item.id)
                reminderScheduler.scheduleReminder(updated)
                _uiState.update { it.copy(snackbarMessage = "Marked as renewed") }
            } else {
                val completed = item.copy(
                    isCompleted = true,
                    lastRenewedAt = now,
                    updatedAt = now
                )
                repository.update(completed)
                reminderScheduler.cancelReminder(item.id)
                _uiState.update { it.copy(snackbarMessage = "Marked as completed") }
            }
        }
    }

    companion object {
        fun factory(
            itemId: Long,
            repository: ExpiryItemRepository,
            reminderScheduler: ReminderScheduler
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ExpiryDetailViewModel(itemId, repository, reminderScheduler) as T
        }
    }
}
