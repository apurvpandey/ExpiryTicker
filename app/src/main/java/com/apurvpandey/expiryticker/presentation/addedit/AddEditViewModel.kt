package com.apurvpandey.expiryticker.presentation.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apurvpandey.expiryticker.core.util.CurrencyFormatter
import com.apurvpandey.expiryticker.data.preferences.AppPreferencesDataStore
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import com.apurvpandey.expiryticker.domain.repository.ExpiryItemRepository
import com.apurvpandey.expiryticker.notification.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class AddEditUiState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val title: String = "",
    val titleError: String? = null,
    val selectedCategory: RenewalCategory? = null,
    val categoryError: String? = null,
    val dueDate: LocalDate? = null,
    val dueDateError: String? = null,
    val reminderDaysBefore: Int = 7,
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val amountText: String = "",
    val amountError: String? = null,
    val notes: String = "",
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null
)

class AddEditViewModel(
    private val repository: ExpiryItemRepository,
    private val reminderScheduler: ReminderScheduler,
    private val appPreferences: AppPreferencesDataStore,
    private val editItemId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        if (editItemId != null) {
            loadExistingItem(editItemId)
        } else {
            loadDefaultReminder()
        }
    }

    private fun loadDefaultReminder() {
        viewModelScope.launch {
            val days = appPreferences.defaultReminderDays.first()
            _uiState.update { it.copy(reminderDaysBefore = days) }
        }
    }

    private fun loadExistingItem(id: Long) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val item = repository.observeById(id).first()
            if (item != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEditMode = true,
                        title = item.title,
                        selectedCategory = item.category,
                        dueDate = item.dueDate,
                        reminderDaysBefore = item.reminderDaysBefore,
                        recurrence = item.recurrence,
                        amountText = item.amountPaise?.let { p -> (p / 100.0).toString() } ?: "",
                        notes = item.notes
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTitleChange(value: String) =
        _uiState.update { it.copy(title = value, titleError = null) }

    fun onCategoryChange(value: RenewalCategory) =
        _uiState.update { it.copy(selectedCategory = value, categoryError = null) }

    fun onDueDateChange(value: LocalDate) =
        _uiState.update { it.copy(dueDate = value, dueDateError = null) }

    fun onReminderChange(days: Int) = _uiState.update { it.copy(reminderDaysBefore = days) }

    fun onRecurrenceChange(value: RecurrenceType) = _uiState.update { it.copy(recurrence = value) }

    fun onAmountChange(raw: String) {
        val filtered = buildString {
            var hasDot = false
            for (ch in raw) {
                when {
                    ch.isDigit() -> append(ch)
                    ch == '.' && !hasDot -> { append(ch); hasDot = true }
                }
            }
        }
        _uiState.update { it.copy(amountText = filtered, amountError = null) }
    }

    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun save() {
        if (!validate()) return

        val state = _uiState.value
        val amountPaise: Long? = if (state.amountText.isBlank()) null
        else CurrencyFormatter.parseToPaise(state.amountText)

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val now = Instant.now()
                if (editItemId != null) {
                    val existing = repository.observeById(editItemId).first() ?: return@launch
                    val updated = existing.copy(
                        title = state.title.trim(),
                        category = state.selectedCategory!!,
                        dueDate = state.dueDate!!,
                        reminderDaysBefore = state.reminderDaysBefore,
                        recurrence = state.recurrence,
                        notes = state.notes.trim(),
                        amountPaise = amountPaise,
                        updatedAt = now
                    )
                    repository.update(updated)
                    reminderScheduler.cancelReminder(editItemId)
                    reminderScheduler.scheduleReminder(updated)
                } else {
                    val newItem = ExpiryItem(
                        title = state.title.trim(),
                        category = state.selectedCategory!!,
                        dueDate = state.dueDate!!,
                        reminderDaysBefore = state.reminderDaysBefore,
                        recurrence = state.recurrence,
                        notes = state.notes.trim(),
                        amountPaise = amountPaise,
                        createdAt = now,
                        updatedAt = now
                    )
                    val id = repository.add(newItem)
                    reminderScheduler.scheduleReminder(newItem.copy(id = id))
                }
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Failed to save. Please try again.")
                }
            }
        }
    }

    private fun validate(): Boolean {
        val state = _uiState.value
        var valid = true
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Title cannot be empty") }
            valid = false
        }
        if (state.selectedCategory == null) {
            _uiState.update { it.copy(categoryError = "Please select a category") }
            valid = false
        }
        if (state.dueDate == null) {
            _uiState.update { it.copy(dueDateError = "Please select a due date") }
            valid = false
        }
        if (state.amountText.isNotBlank()) {
            val parsed = CurrencyFormatter.parseToPaise(state.amountText)
            if (parsed == null) {
                _uiState.update { it.copy(amountError = "Enter a valid amount (e.g. 1500 or 1500.50)") }
                valid = false
            }
        }
        return valid
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    companion object {
        fun factory(
            repository: ExpiryItemRepository,
            reminderScheduler: ReminderScheduler,
            appPreferences: AppPreferencesDataStore,
            editItemId: Long?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                AddEditViewModel(repository, reminderScheduler, appPreferences, editItemId) as T
        }
    }
}
