package com.apurvpandey.expiryticker.presentation.details

import com.apurvpandey.expiryticker.data.files.AttachmentTooLargeException
import com.apurvpandey.expiryticker.R
import com.apurvpandey.expiryticker.presentation.common.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apurvpandey.expiryticker.core.util.CurrencyFormatter
import com.apurvpandey.expiryticker.core.util.ExpiryStatusCalculator
import com.apurvpandey.expiryticker.data.files.AttachmentStore
import com.apurvpandey.expiryticker.domain.model.Attachment
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalRecord
import com.apurvpandey.expiryticker.domain.repository.AttachmentRepository
import com.apurvpandey.expiryticker.domain.repository.ExpiryItemRepository
import com.apurvpandey.expiryticker.domain.repository.RenewalHistoryRepository
import com.apurvpandey.expiryticker.domain.usecase.ItemActions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ExpiryDetailUiState(
    val isLoading: Boolean = true,
    val item: ExpiryItem? = null,
    val status: ExpiryStatus? = null,
    val history: List<RenewalRecord> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val showDeleteDialog: Boolean = false,
    val showRenewDialog: Boolean = false,
    val renewAmountError: UiText? = null,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: UiText? = null
)

class ExpiryDetailViewModel(
    private val itemId: Long,
    private val repository: ExpiryItemRepository,
    private val actions: ItemActions,
    private val historyRepository: RenewalHistoryRepository,
    private val attachmentRepository: AttachmentRepository,
    private val store: AttachmentStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpiryDetailUiState())
    val uiState: StateFlow<ExpiryDetailUiState> = _uiState.asStateFlow()

    init {
        observeItem()
        viewModelScope.launch {
            historyRepository.observeForItem(itemId).collect { list ->
                _uiState.update { it.copy(history = list) }
            }
        }
        viewModelScope.launch {
            attachmentRepository.observeForItem(itemId).collect { list ->
                _uiState.update { it.copy(attachments = list) }
            }
        }
    }

    private fun observeItem() {
        viewModelScope.launch {
            repository.observeById(itemId).collect { item ->
                if (item != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            item = item,
                            status = ExpiryStatusCalculator.calculate(item.dueDate, item.isCompleted, LocalDate.now())
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
    fun showRenewDialog() = _uiState.update { it.copy(showRenewDialog = true, renewAmountError = null) }
    fun dismissRenewDialog() = _uiState.update { it.copy(showRenewDialog = false, renewAmountError = null) }
    fun clearSnackbarMessage() = _uiState.update { it.copy(snackbarMessage = null) }
    fun showMessage(message: UiText) = _uiState.update { it.copy(snackbarMessage = message) }

    fun deleteItem() {
        val item = _uiState.value.item ?: return
        viewModelScope.launch {
            actions.delete(item)
            _uiState.update { it.copy(showDeleteDialog = false, isDeleted = true) }
        }
    }

    /** Completes or renews the item, recording what it cost; a blank [amountText] keeps the expected cost. */
    fun confirmRenew(amountText: String) {
        val item = _uiState.value.item ?: return
        val amount = if (amountText.isBlank()) item.amountPaise else CurrencyFormatter.parseToPaise(amountText)
        if (amountText.isNotBlank() && amount == null) {
            _uiState.update { it.copy(renewAmountError = UiText.of(R.string.err_amount_short)) }
            return
        }
        viewModelScope.launch {
            val outcome = actions.markDone(item, amount)
            _uiState.update {
                it.copy(
                    showRenewDialog = false,
                    renewAmountError = null,
                    snackbarMessage = UiText.of(if (outcome.renewed) R.string.msg_marked_renewed else R.string.msg_marked_completed)
                )
            }
        }
    }

    fun reopen() {
        val item = _uiState.value.item ?: return
        viewModelScope.launch {
            actions.reopen(item)
            _uiState.update { it.copy(snackbarMessage = UiText.of(R.string.msg_moved_back)) }
        }
    }

    fun addAttachment(uri: android.net.Uri) {
        viewModelScope.launch {
            runCatching {
                val stored = store.import(uri)
                attachmentRepository.add(
                    Attachment(
                        itemId = itemId, displayName = stored.displayName, mimeType = stored.mimeType,
                        fileName = stored.fileName, sizeBytes = stored.sizeBytes
                    )
                )
            }.onSuccess {
                showMessage(UiText.of(R.string.msg_file_attached))
            }.onFailure {
                showMessage(
                    (it as? AttachmentTooLargeException)
                        ?.let { e -> UiText.of(R.string.err_file_too_large, e.maxMegabytes.toInt()) }
                        ?: UiText.of(R.string.err_attach_failed)
                )
            }
        }
    }

    /** Registers a photo the camera wrote to [fileName]; deletes the empty file if capture was cancelled. */
    fun onPhotoCaptured(fileName: String, success: Boolean) {
        val file = store.file(fileName)
        if (!success || !file.exists() || file.length() == 0L) {
            file.delete()
            return
        }
        viewModelScope.launch {
            attachmentRepository.add(
                Attachment(
                    itemId = itemId, displayName = "Photo ${LocalDate.now()}", mimeType = "image/jpeg",
                    fileName = fileName, sizeBytes = file.length()
                )
            )
            showMessage(UiText.of(R.string.msg_photo_attached))
        }
    }

    fun removeAttachment(attachment: Attachment) {
        viewModelScope.launch {
            attachmentRepository.deleteById(attachment.id)
            store.delete(attachment.fileName)
            showMessage(UiText.of(R.string.msg_attachment_removed))
        }
    }

    companion object {
        fun factory(
            itemId: Long,
            repository: ExpiryItemRepository,
            actions: ItemActions,
            historyRepository: RenewalHistoryRepository,
            attachmentRepository: AttachmentRepository,
            store: AttachmentStore
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ExpiryDetailViewModel(itemId, repository, actions, historyRepository, attachmentRepository, store) as T
        }
    }
}
