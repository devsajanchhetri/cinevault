package com.example.feature.studios

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraRoll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.GlassBottomSheet
import com.example.core.designsystem.GlassButton
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.GlassDialog
import com.example.core.designsystem.GlassFilterChip
import com.example.core.designsystem.GlassInputField
import com.example.core.designsystem.GlassMetricCard
import com.example.core.designsystem.GlassStudioCard
import com.example.core.designsystem.GlassTransactionRow
import com.example.domain.StudioFinancialSummary
import com.example.domain.model.Studio
import com.example.feature.CineVaultUiState
import com.example.feature.reports.ReportType
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryZinc
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultSuccess

@Composable
fun StudiosScreen(
    uiState: CineVaultUiState,
    selectedStudioId: String?,
    onSelectStudio: (String?) -> Unit,
    onAddStudioClick: () -> Unit,
    onEditStudio: (Studio) -> Unit,
    onToggleArchive: (Studio) -> Unit,
    onDeleteStudio: (String) -> Unit,
    onAddShootForStudio: (Studio) -> Unit,
    onRecordWithdrawalForStudio: (Studio) -> Unit,
    onExportStudioStatement: (String, Boolean) -> Unit
) {
    val selectedSummary = uiState.studioSummaries.find { it.studio.id == selectedStudioId }

    if (selectedSummary != null) {
        BackHandler { onSelectStudio(null) }
        StudioDetailDashboard(
            summary = selectedSummary,
            uiState = uiState,
            onBack = { onSelectStudio(null) },
            onEdit = { onEditStudio(selectedSummary.studio) },
            onToggleArchive = { onToggleArchive(selectedSummary.studio) },
            onDelete = { onDeleteStudio(selectedSummary.studio.id) },
            onAddShoot = { onAddShootForStudio(selectedSummary.studio) },
            onRecordWithdrawal = { onRecordWithdrawalForStudio(selectedSummary.studio) },
            onExportPdf = { onExportStudioStatement(selectedSummary.studio.id, true) },
            onExportCsv = { onExportStudioStatement(selectedSummary.studio.id, false) }
        )
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var showArchived by remember { mutableStateOf(false) }

    val filteredSummaries = remember(uiState.studioSummaries, searchQuery, showArchived) {
        uiState.studioSummaries.filter { s ->
            val matchesArchive = if (showArchived) s.studio.archived else !s.studio.archived
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                s.studio.name.lowercase().contains(q) ||
                s.studio.contactPerson.lowercase().contains(q) ||
                s.studio.phone.lowercase().contains(q) ||
                s.studio.address.lowercase().contains(q)
            matchesArchive && matchesQuery
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("studios_screen")
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Production Studios",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite
                    )
                    Text(
                        text = "Manage client studios, receivables & statements",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                }
                GlassButton(
                    text = "New Studio",
                    icon = Icons.Default.Add,
                    testTag = "add_studio_button",
                    onClick = onAddStudioClick
                )
            }
        }

        item {
            GlassInputField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search studios, contacts, or phone",
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = SecondaryZinc)
                },
                testTag = "studio_search_input"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassFilterChip(
                    label = "Active Studios (${uiState.studioSummaries.count { !it.studio.archived }})",
                    selected = !showArchived,
                    onClick = { showArchived = false }
                )
                GlassFilterChip(
                    label = "Archived (${uiState.studioSummaries.count { it.studio.archived }})",
                    selected = showArchived,
                    onClick = { showArchived = true }
                )
            }
        }

        if (filteredSummaries.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (showArchived) "No archived studios" else "No matching studios found",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Create a studio profile to link shoots, track withdrawals, and export NPR statements.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                }
            }
        } else {
            items(filteredSummaries, key = { it.studio.id }) { summary ->
                GlassStudioCard(
                    summary = summary,
                    onClick = { onSelectStudio(summary.studio.id) }
                )
            }
        }
    }
}

@Composable
private fun StudioDetailDashboard(
    summary: StudioFinancialSummary,
    uiState: CineVaultUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    onAddShoot: () -> Unit,
    onRecordWithdrawal: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val studio = summary.studio
    val studioShoots = uiState.shoots.filter { it.studioId == studio.id }
    val studioWithdrawals = uiState.withdrawals.filter { it.studioId == studio.id }
    val hasFinancialRecords = studioShoots.isNotEmpty() || studioWithdrawals.isNotEmpty()

    if (showDeleteDialog) {
        GlassDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = if (hasFinancialRecords) "Deletion Blocked by Ledger Guard" else "Delete Studio?"
        ) {
            if (hasFinancialRecords) {
                Text(
                    text = "${studio.name} has ${studioShoots.size} shoots and ${studioWithdrawals.size} withdrawals. To protect financial accuracy, studios with ledger records cannot be deleted. You can archive this studio instead.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryZinc
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(
                        text = "Cancel",
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = { showDeleteDialog = false }
                    )
                    GlassButton(
                        text = "Archive Studio",
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showDeleteDialog = false
                            onToggleArchive()
                        }
                    )
                }
            } else {
                Text(
                    text = "Are you sure you want to permanently delete '${studio.name}'?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryZinc
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(
                        text = "Cancel",
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = { showDeleteDialog = false }
                    )
                    GlassButton(
                        text = "Delete",
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showDeleteDialog = false
                            onDelete()
                            onBack()
                        }
                    )
                }
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                    Column {
                        Text(
                            text = studio.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = PureWhite
                        )
                        Text(
                            text = studio.contactPerson.ifBlank { "Studio Ledger" },
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryZinc
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Studio", tint = PureWhite)
                    }
                    IconButton(onClick = onToggleArchive) {
                        Icon(
                            imageVector = if (studio.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = "Archive Studio",
                            tint = VaultAmber
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Studio", tint = CinemaCrimson)
                    }
                }
            }
        }

        // Studio Financial Summary Grid
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassMetricCard(
                    title = "Outstanding Balance",
                    amountPaisa = summary.outstandingBalancePaisa,
                    subtitle = "${summary.collectionPercentage.toInt()}% collected",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = if (summary.outstandingBalancePaisa > 0L) VaultAmber else VaultSuccess,
                    modifier = Modifier.weight(1f)
                )
                GlassMetricCard(
                    title = "Total Shoot Earnings",
                    amountPaisa = summary.totalEarningsPaisa,
                    subtitle = "${summary.completedShootCount} completed shoots",
                    icon = Icons.Default.CameraRoll,
                    accentColor = CinemaCrimson,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassMetricCard(
                    title = "Total Withdrawals",
                    amountPaisa = summary.totalWithdrawalsPaisa,
                    subtitle = "${studioWithdrawals.size} payments received",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = VaultSuccess,
                    modifier = Modifier.weight(1f)
                )
                GlassMetricCard(
                    title = "Total Shoots",
                    valueText = "${summary.totalShootCount}",
                    subtitle = "${summary.plannedShootCount} planned upcoming",
                    icon = Icons.Default.Business,
                    accentColor = CinemaCrimson,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Studio Contact Info & Quick Actions
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                if (studio.phone.isNotBlank() || studio.email.isNotBlank() || studio.address.isNotBlank()) {
                    Text(
                        text = listOfNotNull(
                            studio.phone.takeIf { it.isNotBlank() },
                            studio.email.takeIf { it.isNotBlank() },
                            studio.address.takeIf { it.isNotBlank() }
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (studio.notes.isNotBlank()) {
                    Text(
                        text = studio.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Add Shoot",
                        icon = Icons.Default.CameraRoll,
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        onClick = onAddShoot
                    )
                    GlassButton(
                        text = "Withdrawal",
                        icon = Icons.Default.AccountBalanceWallet,
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = onRecordWithdrawal
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "PDF Statement",
                        icon = Icons.Default.PictureAsPdf,
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = onExportPdf
                    )
                    GlassButton(
                        text = "Export CSV",
                        icon = Icons.Default.Share,
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = onExportCsv
                    )
                }
            }
        }

        // Complete Studio Transaction History
        item {
            Text(
                text = "STUDIO SHOOTS (${studioShoots.size})",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryZinc
            )
        }
        items(studioShoots, key = { "st_shoot_${it.id}" }) { shoot ->
            val isCompleted = shoot.projectStatus.equals("Completed", ignoreCase = true)
            GlassTransactionRow(
                title = shoot.eventTitle,
                subtitle = "${shoot.category} • ${shoot.durationValue} ${shoot.durationUnit} • ${shoot.location.ifBlank { "Nepal" }}",
                amountPaisa = shoot.confirmedEarningsPaisa,
                dateEpochMs = shoot.shootDateEpochMs,
                badgeText = shoot.projectStatus,
                badgeColor = if (isCompleted) CinemaCrimson else VaultAmber,
                amountPrefix = if (isCompleted) "+" else "Planned "
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "STUDIO WITHDRAWALS (${studioWithdrawals.size})",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryZinc
            )
        }
        items(studioWithdrawals, key = { "st_wd_${it.id}" }) { wd ->
            GlassTransactionRow(
                title = "Collected via ${wd.paymentMethod}",
                subtitle = if (wd.referenceNumber.isNotBlank()) "Ref: ${wd.referenceNumber}" else wd.notes.ifBlank { "Payment received" },
                amountPaisa = wd.amountPaisa,
                dateEpochMs = wd.dateEpochMs,
                badgeText = wd.paymentMethod,
                badgeColor = VaultSuccess,
                amountColor = VaultSuccess,
                amountPrefix = "Collected "
            )
        }
    }
}

@Composable
fun StudioFormBottomSheet(
    initialStudio: Studio?,
    onDismiss: () -> Unit,
    onSave: (Studio, Boolean) -> Unit
) {
    val isEdit = initialStudio != null
    var name by remember { mutableStateOf(initialStudio?.name ?: "") }
    var contactPerson by remember { mutableStateOf(initialStudio?.contactPerson ?: "") }
    var phone by remember { mutableStateOf(initialStudio?.phone ?: "") }
    var email by remember { mutableStateOf(initialStudio?.email ?: "") }
    var address by remember { mutableStateOf(initialStudio?.address ?: "") }
    var notes by remember { mutableStateOf(initialStudio?.notes ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    GlassBottomSheet(
        onDismissRequest = onDismiss,
        title = if (isEdit) "Edit Studio" else "New Production Studio"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (validationError != null) {
                Text(
                    text = validationError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaCrimson
                )
            }
            GlassInputField(
                value = name,
                onValueChange = { name = it },
                label = "Studio Name *",
                placeholder = "e.g. Himalayan Cinema Frames",
                testTag = "studio_name_input"
            )
            GlassInputField(
                value = contactPerson,
                onValueChange = { contactPerson = it },
                label = "Contact Person",
                placeholder = "e.g. Suman Shrestha",
                testTag = "studio_contact_input"
            )
            GlassInputField(
                value = phone,
                onValueChange = { phone = it },
                label = "Phone Number",
                placeholder = "e.g. 9841000000",
                testTag = "studio_phone_input"
            )
            GlassInputField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                placeholder = "e.g. contact@studio.com.np"
            )
            GlassInputField(
                value = address,
                onValueChange = { address = it },
                label = "Address / City",
                placeholder = "e.g. Kathmandu, Nepal"
            )
            GlassInputField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notes",
                singleLine = false
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton(
                    text = "Cancel",
                    isPrimary = false,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                )
                GlassButton(
                    text = if (isEdit) "Update Studio" else "Save Studio",
                    isPrimary = true,
                    testTag = "save_studio_submit_button",
                    modifier = Modifier.weight(1.4f),
                    onClick = {
                        if (name.isBlank()) {
                            validationError = "Studio name is required."
                            return@GlassButton
                        }
                        val resultStudio = (initialStudio ?: Studio()).copy(
                            name = name.trim(),
                            contactPerson = contactPerson.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            address = address.trim(),
                            notes = notes.trim()
                        )
                        onSave(resultStudio, isEdit)
                        onDismiss()
                    }
                )
            }
        }
    }
}
