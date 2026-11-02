package com.example.feature.shoots

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.GlassBottomSheet
import com.example.core.designsystem.GlassButton
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.GlassDialog
import com.example.core.designsystem.GlassFilterChip
import com.example.core.designsystem.GlassInputField
import com.example.core.designsystem.GlassTransactionRow
import com.example.core.utilities.DateUtils
import com.example.core.utilities.MoneyUtils
import com.example.domain.LedgerEngine
import com.example.domain.model.CineVaultConstants
import com.example.domain.model.Shoot
import com.example.domain.model.Studio
import com.example.feature.CineVaultUiState
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryZinc
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultSuccess

@Composable
fun ShootsScreen(
    uiState: CineVaultUiState,
    onAddShootClick: () -> Unit,
    onEditShoot: (Shoot) -> Unit,
    onDeleteShoot: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedStudioId by remember { mutableStateOf("All") }
    var selectedProjectStatus by remember { mutableStateOf("All") }
    var sortByHighestEarnings by remember { mutableStateOf(false) }
    var shootToViewOrDelete by remember { mutableStateOf<Shoot?>(null) }

    val filteredShoots = remember(
        uiState.shoots,
        searchQuery,
        selectedCategory,
        selectedStudioId,
        selectedProjectStatus,
        sortByHighestEarnings
    ) {
        val list = uiState.shoots.filter { s ->
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                s.eventTitle.lowercase().contains(q) ||
                s.studioName.lowercase().contains(q) ||
                s.location.lowercase().contains(q) ||
                s.notes.lowercase().contains(q)
            val matchesCat = selectedCategory == "All" || s.category == selectedCategory
            val matchesStudio = selectedStudioId == "All" || s.studioId == selectedStudioId
            val matchesStatus = selectedProjectStatus == "All" || s.projectStatus.equals(selectedProjectStatus, ignoreCase = true)
            matchesSearch && matchesCat && matchesStudio && matchesStatus
        }
        if (sortByHighestEarnings) {
            list.sortedByDescending { it.confirmedEarningsPaisa }
        } else {
            list.sortedByDescending { it.shootDateEpochMs }
        }
    }

    if (shootToViewOrDelete != null) {
        val shoot = shootToViewOrDelete!!
        GlassDialog(
            onDismissRequest = { shootToViewOrDelete = null },
            title = shoot.eventTitle
        ) {
            Text(
                text = "Studio: ${shoot.studioName}\nCategory: ${shoot.category}\nDate: ${DateUtils.formatShortDate(shoot.shootDateEpochMs)}\nLocation: ${shoot.location.ifBlank { "Not specified" }}\nDuration: ${shoot.durationValue} ${shoot.durationUnit}\nConfirmed Earnings: ${MoneyUtils.formatNpr(shoot.confirmedEarningsPaisa)}\nProject Status: ${shoot.projectStatus} (${shoot.paymentStatus})",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryZinc
            )
            if (shoot.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Notes: ${shoot.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PureWhite
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    text = "Delete",
                    icon = Icons.Default.Delete,
                    isPrimary = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val id = shoot.id
                        shootToViewOrDelete = null
                        onDeleteShoot(id)
                    }
                )
                GlassButton(
                    text = "Edit Shoot",
                    icon = Icons.Default.Edit,
                    isPrimary = true,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val target = shoot
                        shootToViewOrDelete = null
                        onEditShoot(target)
                    }
                )
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("shoots_screen")
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Shoots & Production Log",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite
                    )
                    Text(
                        text = "Completed shoots immediately enter studio collectible balances",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                }
                GlassButton(
                    text = "Add Shoot",
                    icon = Icons.Default.Add,
                    testTag = "add_shoot_button",
                    onClick = onAddShootClick
                )
            }
        }

        item {
            GlassInputField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search shoots by title, studio, or location",
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = SecondaryZinc)
                },
                testTag = "shoot_search_input"
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Status & Sort chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf("All", "Completed", "Planned").forEach { st ->
                    GlassFilterChip(
                        label = st,
                        selected = selectedProjectStatus == st,
                        onClick = { selectedProjectStatus = st }
                    )
                }
                GlassFilterChip(
                    label = if (sortByHighestEarnings) "Sorted: Highest NPR" else "Sort: By Date",
                    selected = sortByHighestEarnings,
                    onClick = { sortByHighestEarnings = !sortByHighestEarnings }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category filter chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                GlassFilterChip(
                    label = "All Categories",
                    selected = selectedCategory == "All",
                    onClick = { selectedCategory = "All" }
                )
                CineVaultConstants.SHOOT_CATEGORIES.forEach { cat ->
                    GlassFilterChip(
                        label = cat,
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat }
                    )
                }
            }

            if (uiState.studios.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    GlassFilterChip(
                        label = "All Studios",
                        selected = selectedStudioId == "All",
                        onClick = { selectedStudioId = "All" }
                    )
                    uiState.studios.forEach { st ->
                        GlassFilterChip(
                            label = st.name,
                            selected = selectedStudioId == st.id,
                            onClick = { selectedStudioId = st.id }
                        )
                    }
                }
            }
        }

        if (filteredShoots.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No shoots match current filters",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add a wedding, commercial, music video, or documentary shoot to track confirmed NPR earnings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                }
            }
        } else {
            items(filteredShoots, key = { it.id }) { shoot ->
                val isCompleted = shoot.projectStatus.equals("Completed", ignoreCase = true)
                GlassTransactionRow(
                    title = shoot.eventTitle,
                    subtitle = "${shoot.studioName} • ${shoot.category} • ${shoot.durationValue} ${shoot.durationUnit} • ${shoot.location.ifBlank { "Nepal" }}",
                    amountPaisa = shoot.confirmedEarningsPaisa,
                    dateEpochMs = shoot.shootDateEpochMs,
                    badgeText = if (isCompleted) "COMPLETED" else "PLANNED",
                    badgeColor = if (isCompleted) VaultSuccess else VaultAmber,
                    amountColor = if (isCompleted) PureWhite else SecondaryZinc,
                    amountPrefix = if (isCompleted) "+" else "Est. ",
                    onClick = { shootToViewOrDelete = shoot }
                )
            }
        }
    }
}

@Composable
fun ShootFormBottomSheet(
    initialShoot: Shoot?,
    studios: List<Studio>,
    existingShoots: List<Shoot>,
    preselectedStudio: Studio? = null,
    onDismiss: () -> Unit,
    onSave: (Shoot, Boolean) -> Unit,
    onRequestCreateStudio: () -> Unit
) {
    val isEdit = initialShoot != null
    var eventTitle by remember { mutableStateOf(initialShoot?.eventTitle ?: "") }
    var selectedStudio by remember {
        mutableStateOf(
            studios.find { it.id == initialShoot?.studioId }
                ?: preselectedStudio
                ?: studios.firstOrNull { !it.archived }
        )
    }
    var shootDateIso by remember {
        mutableStateOf(
            DateUtils.formatIsoDate(initialShoot?.shootDateEpochMs ?: System.currentTimeMillis())
        )
    }
    var category by remember { mutableStateOf(initialShoot?.category ?: "Wedding") }
    var location by remember { mutableStateOf(initialShoot?.location ?: "") }
    var durationValue by remember { mutableStateOf((initialShoot?.durationValue ?: 1L).toString()) }
    var durationUnit by remember { mutableStateOf(initialShoot?.durationUnit ?: "Days") }
    var earningsInput by remember {
        mutableStateOf(
            initialShoot?.let { MoneyUtils.paisaToInputString(it.confirmedEarningsPaisa) } ?: ""
        )
    }
    var expectedPaymentIso by remember {
        mutableStateOf(
            DateUtils.formatIsoDate(
                initialShoot?.expectedPaymentDateEpochMs
                    ?: (System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L)
            )
        )
    }
    var paymentStatus by remember { mutableStateOf(initialShoot?.paymentStatus ?: "Collectible") }
    var projectStatus by remember { mutableStateOf(initialShoot?.projectStatus ?: "Completed") }
    var notes by remember { mutableStateOf(initialShoot?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val parsedDateEpoch = remember(shootDateIso) { DateUtils.parseIsoDateOrToday(shootDateIso) }
    val sameDayConflicts = remember(parsedDateEpoch, existingShoots, initialShoot) {
        LedgerEngine.detectConflictsForDate(
            shootDateEpochMs = parsedDateEpoch,
            existingShoots = existingShoots,
            excludeShootId = initialShoot?.id ?: ""
        )
    }

    GlassBottomSheet(
        onDismissRequest = onDismiss,
        title = if (isEdit) "Edit Shoot" else "Record Cinematography Shoot"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (studios.isEmpty()) {
                GlassCard(accentGlow = VaultAmber) {
                    Text(
                        text = "Create a Studio first to assign this shoot.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassButton(
                        text = "Create Studio Now",
                        onClick = {
                            onDismiss()
                            onRequestCreateStudio()
                        }
                    )
                }
                return@Column
            }

            if (errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaCrimson
                )
            }

            if (sameDayConflicts.isNotEmpty()) {
                GlassCard(
                    cornerRadius = 14.dp,
                    accentGlow = VaultAmber,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Schedule Conflict",
                            tint = VaultAmber
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = "Schedule Conflict Alert: ${sameDayConflicts.size} other shoot(s) scheduled on $shootDateIso (${sameDayConflicts.joinToString { it.eventTitle }})",
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultAmber
                        )
                    }
                }
            }

            GlassInputField(
                value = eventTitle,
                onValueChange = { eventTitle = it },
                label = "Event Title *",
                placeholder = "e.g. Aarav & Priya Wedding Film",
                testTag = "shoot_title_input"
            )

            Text(
                text = "SELECT STUDIO *",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryZinc
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                studios.filter { !it.archived || it.id == selectedStudio?.id }.forEach { st ->
                    GlassFilterChip(
                        label = st.name,
                        selected = selectedStudio?.id == st.id,
                        onClick = { selectedStudio = st }
                    )
                }
            }

            Text(
                text = "PROJECT STATUS (COMPLETED ADDS TO COLLECTIBLE LEDGER)",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryZinc
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CineVaultConstants.PROJECT_STATUSES.forEach { st ->
                    GlassFilterChip(
                        label = st,
                        selected = projectStatus == st,
                        onClick = { projectStatus = st }
                    )
                }
            }

            GlassInputField(
                value = earningsInput,
                onValueChange = { earningsInput = it },
                label = "Confirmed Shoot Earnings (NPR) *",
                placeholder = "e.g. 45000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                testTag = "shoot_earnings_input"
            )

            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryZinc
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                CineVaultConstants.SHOOT_CATEGORIES.forEach { cat ->
                    GlassFilterChip(
                        label = cat,
                        selected = category == cat,
                        onClick = { category = cat }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassInputField(
                    value = shootDateIso,
                    onValueChange = { shootDateIso = it },
                    label = "Shoot Date (YYYY-MM-DD)",
                    modifier = Modifier.weight(1f)
                )
                GlassInputField(
                    value = expectedPaymentIso,
                    onValueChange = { expectedPaymentIso = it },
                    label = "Expected Payment Date",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassInputField(
                    value = durationValue,
                    onValueChange = { durationValue = it },
                    label = "Duration",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CineVaultConstants.DURATION_UNITS.forEach { unit ->
                        GlassFilterChip(
                            label = unit,
                            selected = durationUnit == unit,
                            onClick = { durationUnit = unit }
                        )
                    }
                }
            }

            GlassInputField(
                value = location,
                onValueChange = { location = it },
                label = "Location",
                placeholder = "e.g. Soaltee Hotel, Kathmandu"
            )

            Text(
                text = "PAYMENT STATUS",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryZinc
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                CineVaultConstants.PAYMENT_STATUSES.forEach { ps ->
                    GlassFilterChip(
                        label = ps,
                        selected = paymentStatus == ps,
                        onClick = { paymentStatus = ps }
                    )
                }
            }

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
                    text = if (isEdit) "Update Shoot" else "Save Shoot",
                    isPrimary = true,
                    testTag = "save_shoot_submit_button",
                    modifier = Modifier.weight(1.4f),
                    onClick = {
                        val st = selectedStudio
                        if (eventTitle.isBlank()) {
                            errorMsg = "Event title is required."
                            return@GlassButton
                        }
                        if (st == null) {
                            errorMsg = "Please select a studio."
                            return@GlassButton
                        }
                        val paisa = MoneyUtils.parseNprToPaisa(earningsInput.ifBlank { "0" })
                        if (paisa == null || paisa < 0L) {
                            errorMsg = "Please enter a valid NPR earnings amount."
                            return@GlassButton
                        }
                        val dur = durationValue.toLongOrNull()?.coerceIn(1L, 365L) ?: 1L
                        val shootObj = (initialShoot ?: Shoot()).copy(
                            eventTitle = eventTitle.trim(),
                            studioId = st.id,
                            studioName = st.name,
                            shootDateEpochMs = DateUtils.parseIsoDateOrToday(shootDateIso),
                            category = category,
                            location = location.trim(),
                            durationValue = dur,
                            durationUnit = durationUnit,
                            confirmedEarningsPaisa = paisa,
                            expectedPaymentDateEpochMs = DateUtils.parseIsoDateOrToday(expectedPaymentIso),
                            paymentStatus = paymentStatus,
                            projectStatus = projectStatus,
                            notes = notes.trim()
                        )
                        onSave(shootObj, isEdit)
                        onDismiss()
                    }
                )
            }
        }
    }
}
