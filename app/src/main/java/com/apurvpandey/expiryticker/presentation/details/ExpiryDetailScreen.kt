package com.apurvpandey.expiryticker.presentation.details

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apurvpandey.expiryticker.AppContainer
import com.apurvpandey.expiryticker.core.util.CurrencyFormatter
import com.apurvpandey.expiryticker.core.util.FileSize
import com.apurvpandey.expiryticker.core.util.PriceHistory
import com.apurvpandey.expiryticker.core.util.ReminderText
import com.apurvpandey.expiryticker.data.files.AttachmentStore
import com.apurvpandey.expiryticker.domain.model.Attachment
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalRecord
import com.apurvpandey.expiryticker.presentation.components.CategoryIconBadge
import com.apurvpandey.expiryticker.presentation.components.countdown
import com.apurvpandey.expiryticker.presentation.components.urgencyBrush
import com.apurvpandey.expiryticker.presentation.theme.ExpiryTheme
import com.apurvpandey.expiryticker.presentation.theme.urgencyColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val shortDate = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
fun ExpiryDetailRoute(
    container: AppContainer,
    itemId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val viewModel: ExpiryDetailViewModel = viewModel(
        key = "detail_$itemId",
        factory = ExpiryDetailViewModel.factory(
            itemId = itemId,
            repository = container.expiryItemRepository,
            actions = container.itemActions,
            historyRepository = container.renewalHistoryRepository,
            attachmentRepository = container.attachmentRepository,
            store = container.attachmentStore
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val store = container.attachmentStore

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbarMessage()
        }
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::addAttachment)
    }
    // The camera writes into a file we created; remember its name across the round trip.
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        pendingPhoto?.let { viewModel.onPhotoCaptured(it, success) }
        pendingPhoto = null
    }

    ExpiryDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onNavigateToEdit = { uiState.item?.id?.let(onNavigateToEdit) },
        onRenewClicked = viewModel::showRenewDialog,
        onRenewConfirmed = viewModel::confirmRenew,
        onRenewDismissed = viewModel::dismissRenewDialog,
        onReopen = viewModel::reopen,
        onDeleteClicked = viewModel::showDeleteConfirmation,
        onDeleteConfirmed = viewModel::deleteItem,
        onDeleteDismissed = viewModel::dismissDeleteConfirmation,
        onOpenWebsite = { url ->
            if (!ExternalActions.openUrl(context, url)) viewModel.showMessage("Could not open the link")
        },
        onCall = { phone ->
            if (!ExternalActions.dial(context, phone)) viewModel.showMessage("Could not open the dialer")
        },
        onAddToCalendar = {
            uiState.item?.let {
                if (!ExternalActions.addToCalendar(context, it)) viewModel.showMessage("No calendar app found")
            }
        },
        onAttachFile = { pickFile.launch(arrayOf("image/*", "application/pdf")) },
        onTakePhoto = {
            val (name, uri) = store.newPhotoTarget()
            pendingPhoto = name
            try {
                takePhoto.launch(uri)
            } catch (e: android.content.ActivityNotFoundException) {
                pendingPhoto = null
                viewModel.showMessage("No camera app found")
            }
        },
        onOpenAttachment = { attachment ->
            if (!ExternalActions.openAttachment(context, store, attachment)) {
                viewModel.showMessage("No app can open this file")
            }
        },
        onRemoveAttachment = viewModel::removeAttachment
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryDetailScreen(
    uiState: ExpiryDetailUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onRenewClicked: () -> Unit,
    onRenewConfirmed: (String) -> Unit,
    onRenewDismissed: () -> Unit,
    onReopen: () -> Unit,
    onDeleteClicked: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onOpenWebsite: (String) -> Unit = {},
    onCall: (String) -> Unit = {},
    onAddToCalendar: () -> Unit = {},
    onAttachFile: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    onOpenAttachment: (Attachment) -> Unit = {},
    onRemoveAttachment: (Attachment) -> Unit = {}
) {
    val item = uiState.item

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        },
        bottomBar = {
            if (item != null) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider()
                    if (item.isCompleted) {
                        OutlinedButton(
                            onClick = onReopen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Icon(Icons.Outlined.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Reopen", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        val renewLabel = if (item.recurrence != RecurrenceType.NONE) "Mark as renewed" else "Mark as completed"
                        Button(
                            onClick = onRenewClicked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(renewLabel, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            item == null -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text("Item not found.") }

            else -> DetailContent(
                item = item,
                uiState = uiState,
                onDeleteClicked = onDeleteClicked,
                onOpenWebsite = onOpenWebsite,
                onCall = onCall,
                onAddToCalendar = onAddToCalendar,
                onAttachFile = onAttachFile,
                onTakePhoto = onTakePhoto,
                onOpenAttachment = onOpenAttachment,
                onRemoveAttachment = onRemoveAttachment,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDeleteDismissed,
            title = { Text("Delete item?") },
            text = { Text("\"${item?.title}\" and its history and attachments will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = onDeleteConfirmed,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = onDeleteDismissed) { Text("Cancel") } }
        )
    }

    if (uiState.showRenewDialog && item != null) {
        RenewDialog(
            item = item,
            error = uiState.renewAmountError,
            onConfirm = onRenewConfirmed,
            onDismiss = onRenewDismissed
        )
    }
}

@Composable
private fun RenewDialog(
    item: ExpiryItem,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val renews = item.recurrence != RecurrenceType.NONE
    var amount by remember {
        mutableStateOf(item.amountPaise?.let(CurrencyFormatter::toEditableText) ?: "")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (renews) "Mark as renewed" else "Mark as completed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "What did it cost this time? It is saved in the history so you can track price changes.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { raw -> amount = raw.filter { it.isDigit() || it == '.' } },
                    label = { Text("Amount paid (${CurrencyFormatter.symbol()}, optional)") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(amount) }) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    item: ExpiryItem,
    uiState: ExpiryDetailUiState,
    onDeleteClicked: () -> Unit,
    onOpenWebsite: (String) -> Unit,
    onCall: (String) -> Unit,
    onAddToCalendar: () -> Unit,
    onAttachFile: () -> Unit,
    onTakePhoto: () -> Unit,
    onOpenAttachment: (Attachment) -> Unit,
    onRemoveAttachment: (Attachment) -> Unit,
    modifier: Modifier = Modifier
) {
    val longDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DetailHero(item = item, status = uiState.status)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.renewalUrl.isNotBlank()) {
                ActionChip("Website", Icons.Outlined.Language) { onOpenWebsite(item.renewalUrl) }
            }
            if (item.phone.isNotBlank()) {
                ActionChip("Call", Icons.Outlined.Phone) { onCall(item.phone) }
            }
            ActionChip("Add to calendar", Icons.Outlined.CalendarMonth, onAddToCalendar)
        }

        InfoCard {
            DetailRow(Icons.Outlined.Event, "Due date", item.dueDate.format(longDate))
            DetailRow(Icons.Outlined.NotificationsActive, "Reminders", ReminderText.summary(item))
            DetailRow(Icons.Outlined.Repeat, "Repeats", item.recurrence.displayName)
            if (item.amountPaise != null) {
                DetailRow(Icons.Outlined.Payments, "Expected cost", CurrencyFormatter.format(item.amountPaise))
            }
            item.lastRenewedAt?.let { instant ->
                DetailRow(
                    Icons.Outlined.History, "Last renewed",
                    instant.atZone(ZoneId.systemDefault()).toLocalDate().format(longDate)
                )
            }
        }

        AttachmentsCard(
            attachments = uiState.attachments,
            onAttachFile = onAttachFile,
            onTakePhoto = onTakePhoto,
            onOpen = onOpenAttachment,
            onRemove = onRemoveAttachment
        )

        if (uiState.history.isNotEmpty()) {
            HistoryCard(uiState.history)
        }

        if (item.notes.isNotBlank()) {
            InfoCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CardLabel("NOTES")
                    Text(item.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        TextButton(
            onClick = onDeleteClicked,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("Delete item", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ActionChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
    )
}

@Composable
private fun AttachmentsCard(
    attachments: List<Attachment>,
    onAttachFile: () -> Unit,
    onTakePhoto: () -> Unit,
    onOpen: (Attachment) -> Unit,
    onRemove: (Attachment) -> Unit
) {
    var toRemove by remember { mutableStateOf<Attachment?>(null) }
    InfoCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(Modifier.padding(horizontal = 16.dp)) { CardLabel("ATTACHMENTS") }
            if (attachments.isEmpty()) {
                Text(
                    "Keep the policy, receipt or certificate with the item.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            attachments.forEach { att ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = "Open ${att.displayName}") { onOpen(att) }
                        .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = when {
                            att.isImage -> Icons.Outlined.Image
                            att.mimeType == "application/pdf" -> Icons.Outlined.PictureAsPdf
                            else -> Icons.AutoMirrored.Outlined.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(att.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            FileSize.format(att.sizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { toRemove = att }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Remove ${att.displayName}")
                    }
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onAttachFile) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Attach file")
                }
                TextButton(onClick = onTakePhoto) {
                    Icon(Icons.Outlined.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Take photo")
                }
            }
        }
    }
    toRemove?.let { att ->
        AlertDialog(
            onDismissRequest = { toRemove = null },
            title = { Text("Remove attachment?") },
            text = { Text("\"${att.displayName}\" will be deleted from this device.") },
            confirmButton = {
                TextButton(
                    onClick = { onRemove(att); toRemove = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { toRemove = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HistoryCard(history: List<RenewalRecord>) {
    InfoCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CardLabel("HISTORY")
            // `history` is newest first, so the entry to compare against is the next one down.
            history.forEachIndexed { index, record ->
                val previous = history.getOrNull(index + 1)
                val change = PriceHistory.changePercent(previous?.amountPaise, record.amountPaise)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            record.renewedAt.atZone(ZoneId.systemDefault()).toLocalDate().format(shortDate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            record.dueAfter?.let { "Next due ${it.format(shortDate)}" } ?: "Completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        record.amountPaise?.let {
                            Text(CurrencyFormatter.format(it), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        if (change != null && change != 0) {
                            // A price rise is the thing worth noticing, so it gets the warning colour.
                            val colors = ExpiryTheme.colors
                            Text(
                                text = (if (change > 0) "▲ +" else "▼ ") + "$change%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (change > 0) colors.overdue.accent else colors.ok.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Gradient header that follows the item's urgency and leads with the countdown. */
@Composable
private fun DetailHero(item: ExpiryItem, status: ExpiryStatus?) {
    val accent = status?.urgencyColors()?.accent ?: MaterialTheme.colorScheme.primary
    val countdown = status?.countdown()
    val headline = when (status) {
        is ExpiryStatus.Overdue, is ExpiryStatus.Active -> "${countdown?.value} ${countdown?.caption}"
        is ExpiryStatus.DueToday -> "Due today"
        is ExpiryStatus.Completed -> "Completed"
        null -> ""
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(urgencyBrush(accent))
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CategoryIconBadge(category = item.category, size = 52.dp, iconSize = 26.dp)
            Column {
                Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(item.category.displayName, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            }
        }
        if (headline.isNotEmpty()) {
            Text(headline, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
private fun CardLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
