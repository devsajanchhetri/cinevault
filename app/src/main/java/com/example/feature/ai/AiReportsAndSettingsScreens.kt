package com.example.feature.ai

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.GlassBottomSheet
import com.example.core.designsystem.GlassButton
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.GlassDialog
import com.example.core.designsystem.GlassFilterChip
import com.example.core.designsystem.GlassInputField
import com.example.core.designsystem.GlassTransactionRow
import com.example.core.utilities.MoneyUtils
import com.example.domain.model.CineVaultConstants
import com.example.domain.model.UserSettings
import com.example.feature.CineVaultUiState
import com.example.feature.reports.ReportType
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryZinc
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultPurple
import com.example.ui.theme.VaultSuccess

enum class AiAssistantMode(val title: String) {
    FINANCIAL_QA("Financial Q&A"),
    BUSINESS_INTEL("Business Intelligence"),
    SMART_PRICING("Smart Pricing"),
    PAYMENT_REMINDER("Payment Assistant"),
    NL_ENTRY("NL Shoot Entry")
}

@Composable
fun CineVaultAiScreen(
    uiState: CineVaultUiState,
    onAskPrompt: (String) -> Unit,
    onParseNaturalLanguageShoot: (String) -> Unit,
    onConfirmParsedShoot: (ParsedNaturalLanguageShoot) -> Unit,
    onDismissParsedShoot: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(AiAssistantMode.FINANCIAL_QA) }
    var inputText by remember { mutableStateOf("") }

    // Review confirmation modal before saving any AI-extracted shoot
    val draft = uiState.parsedShootDraft
    if (draft != null) {
        NaturalLanguageShootReviewSheet(
            initialDraft = draft,
            onDismiss = onDismissParsedShoot,
            onConfirmSave = onConfirmParsedShoot
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_assistant_screen")
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "CINEVAULT AI",
                        tint = CinemaCrimson,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CINEVAULT AI",
                            style = MaterialTheme.typography.headlineMedium,
                            color = PureWhite
                        )
                        Text(
                            text = "Grounded in your live NPR ledger • Secured by App Check",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryZinc
                        )
                    }
                }
            }
        }

        if (!uiState.settings.aiEnabled) {
            item {
                GlassCard(accentGlow = VaultAmber) {
                    Text(
                        text = "CINEVAULT AI is Disabled",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "You have disabled AI capabilities in Settings. Enable CINEVAULT AI in Settings to use Financial Q&A, Smart Pricing, and Natural Language Entry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassButton(
                        text = "Open Settings",
                        onClick = onOpenSettings
                    )
                }
            }
            return@LazyColumn
        }

        // Mode Selector Chips
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                AiAssistantMode.entries.forEach { mode ->
                    GlassFilterChip(
                        label = mode.title,
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode }
                    )
                }
            }
        }

        // Quick Prompts / Mode Card
        item {
            GlassCard(
                cornerRadius = 22.dp,
                accentGlow = if (selectedMode == AiAssistantMode.NL_ENTRY) VaultPurple else CinemaCrimson
            ) {
                when (selectedMode) {
                    AiAssistantMode.FINANCIAL_QA -> {
                        Text(
                            text = "Financial Assistant Prompts",
                            style = MaterialTheme.typography.titleSmall,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val prompts = listOf(
                            "How much money am I owed across all studios?",
                            "What did I earn this month in NPR?",
                            "Which studio owes me the most right now?",
                            "How much did I withdraw so far and what is my net profit?"
                        )
                        prompts.forEach { p ->
                            QuickPromptButton(text = p, onClick = { onAskPrompt(p) })
                        }
                    }

                    AiAssistantMode.BUSINESS_INTEL -> {
                        Text(
                            text = "Business Intelligence Analysis",
                            style = MaterialTheme.typography.titleSmall,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val prompts = listOf(
                            "Analyze my monthly performance and collection efficiency.",
                            "Compare my studios by earnings and payment reliability.",
                            "Identify any delayed or overdue studio payments.",
                            "Which shoot categories are most profitable for my business?"
                        )
                        prompts.forEach { p ->
                            QuickPromptButton(text = p, onClick = { onAskPrompt(p) })
                        }
                    }

                    AiAssistantMode.SMART_PRICING -> {
                        Text(
                            text = "Smart Pricing & Quotation Estimator",
                            style = MaterialTheme.typography.titleSmall,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val prompts = listOf(
                            "Suggest a quotation range in NPR for a 2-day Wedding shoot in Kathmandu based on my historical rates.",
                            "What is my average project rate for Commercial vs Music Video shoots?",
                            "Recommend a package pricing tier for a 3-day destination Pre-Wedding shoot in Pokhara."
                        )
                        prompts.forEach { p ->
                            QuickPromptButton(text = p, onClick = { onAskPrompt(p) })
                        }
                    }

                    AiAssistantMode.PAYMENT_REMINDER -> {
                        Text(
                            text = "Payment Reminder Generator (WhatsApp & Email)",
                            style = MaterialTheme.typography.titleSmall,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val owingStudios = uiState.studioSummaries.filter { it.outstandingBalancePaisa > 0L }
                        if (owingStudios.isEmpty()) {
                            Text(
                                text = "All studios are currently settled (NPR 0 outstanding). You can still draft a sample reminder below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryZinc
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            QuickPromptButton(
                                text = "Draft a polite WhatsApp & Email payment reminder template in NPR",
                                onClick = {
                                    onAskPrompt("Draft a professional WhatsApp message and formal Email payment reminder for a client studio in Nepal.")
                                }
                            )
                        } else {
                            owingStudios.forEach { s ->
                                QuickPromptButton(
                                    text = "Draft WhatsApp & Email reminder for ${s.studio.name} (${MoneyUtils.formatNpr(s.outstandingBalancePaisa)} due)",
                                    onClick = {
                                        onAskPrompt(
                                            "Draft a polite, professional WhatsApp message and an Email payment reminder for ${s.studio.name} (Contact: ${s.studio.contactPerson.ifBlank { "Team" }}) regarding the outstanding collectible balance of ${MoneyUtils.formatNpr(s.outstandingBalancePaisa)} from ${s.completedShootCount} completed shoot(s). Mention payment options: Bank Transfer, eSewa, or Khalti."
                                        )
                                    }
                                )
                            }
                        }
                    }

                    AiAssistantMode.NL_ENTRY -> {
                        Text(
                            text = "Natural Language Shoot Entry",
                            style = MaterialTheme.typography.titleSmall,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Describe a shoot in plain English. CINEVAULT AI extracts the structured fields and shows a review confirmation screen before saving anything to your ledger.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryZinc
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        QuickPromptButton(
                            text = "Example: \"I completed a wedding shoot for ABC Studio in Kathmandu today for NPR 45,000.\"",
                            onClick = {
                                inputText = "I completed a wedding shoot for ABC Studio in Kathmandu today for NPR 45,000."
                            }
                        )
                    }
                }
            }
        }

        // Input Box
        item {
            GlassCard(cornerRadius = 20.dp) {
                GlassInputField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = if (selectedMode == AiAssistantMode.NL_ENTRY) {
                        "Describe shoot (e.g. Completed wedding for ABC Studio today for NPR 45,000)"
                    } else {
                        "Ask CINEVAULT AI about your finances, studios, or pricing..."
                    },
                    singleLine = false,
                    testTag = "ai_prompt_input"
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isAiBusy) {
                        CircularProgressIndicator(
                            color = CinemaCrimson,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    if (selectedMode == AiAssistantMode.NL_ENTRY) {
                        GlassButton(
                            text = "Extract & Review Shoot",
                            icon = Icons.Default.AutoAwesome,
                            enabled = !uiState.isAiBusy && inputText.isNotBlank(),
                            testTag = "ai_parse_shoot_button",
                            onClick = {
                                val text = inputText
                                onParseNaturalLanguageShoot(text)
                            }
                        )
                    } else {
                        GlassButton(
                            text = "Ask CINEVAULT AI",
                            icon = Icons.AutoMirrored.Filled.Send,
                            enabled = !uiState.isAiBusy && inputText.isNotBlank(),
                            testTag = "ai_send_button",
                            onClick = {
                                val q = inputText
                                inputText = ""
                                onAskPrompt(q)
                            }
                        )
                    }
                }
            }
        }

        // Conversation History
        items(uiState.aiMessages.reversed(), key = { it.id }) { msg ->
            val isUser = msg.role == "user"
            GlassCard(
                cornerRadius = 18.dp,
                accentGlow = if (isUser) null else CinemaCrimson
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isUser) "YOU" else "CINEVAULT AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) SecondaryZinc else CinemaCrimson
                    )
                    if (!isUser && !msg.isStreaming && msg.content.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg.content)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share via WhatsApp / Email"))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share response",
                                tint = SecondaryZinc,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = msg.content.ifBlank { if (msg.isStreaming) "Analyzing your NPR ledger..." else "" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = PureWhite
                )
            }
        }
    }
}

@Composable
private fun QuickPromptButton(
    text: String,
    onClick: () -> Unit
) {
    GlassCard(
        cornerRadius = 14.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = PureWhite
        )
    }
}

@Composable
private fun NaturalLanguageShootReviewSheet(
    initialDraft: ParsedNaturalLanguageShoot,
    onDismiss: () -> Unit,
    onConfirmSave: (ParsedNaturalLanguageShoot) -> Unit
) {
    var eventTitle by remember { mutableStateOf(initialDraft.eventTitle) }
    var studioName by remember { mutableStateOf(initialDraft.studioName) }
    var shootDateIso by remember { mutableStateOf(initialDraft.shootDateIso) }
    var location by remember { mutableStateOf(initialDraft.location) }
    var category by remember { mutableStateOf(initialDraft.category) }
    var earningsNpr by remember { mutableStateOf(initialDraft.confirmedEarningsNpr.toLong().toString()) }
    var projectStatus by remember { mutableStateOf(initialDraft.projectStatus) }
    var notes by remember { mutableStateOf(initialDraft.notes) }

    GlassBottomSheet(
        onDismissRequest = onDismiss,
        title = "Review Extracted Shoot Before Saving"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "CINEVAULT AI never modifies your financial ledger without your explicit confirmation. Review or edit the extracted details below:",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryZinc
            )
            GlassInputField(
                value = eventTitle,
                onValueChange = { eventTitle = it },
                label = "Event Title"
            )
            GlassInputField(
                value = studioName,
                onValueChange = { studioName = it },
                label = "Studio Name"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassInputField(
                    value = shootDateIso,
                    onValueChange = { shootDateIso = it },
                    label = "Date (YYYY-MM-DD)",
                    modifier = Modifier.weight(1f)
                )
                GlassInputField(
                    value = earningsNpr,
                    onValueChange = { earningsNpr = it },
                    label = "Earnings (NPR)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            GlassInputField(
                value = location,
                onValueChange = { location = it },
                label = "Location"
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
                value = notes,
                onValueChange = { notes = it },
                label = "Notes"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton(
                    text = "Discard",
                    isPrimary = false,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                )
                GlassButton(
                    text = "Confirm & Save to Ledger",
                    icon = Icons.Default.CheckCircle,
                    isPrimary = true,
                    testTag = "confirm_nl_shoot_button",
                    modifier = Modifier.weight(1.5f),
                    onClick = {
                        onConfirmSave(
                            initialDraft.copy(
                                eventTitle = eventTitle.trim(),
                                studioName = studioName.trim(),
                                shootDateIso = shootDateIso.trim(),
                                location = location.trim(),
                                category = category,
                                confirmedEarningsNpr = earningsNpr.toDoubleOrNull() ?: 0.0,
                                projectStatus = projectStatus,
                                notes = notes.trim()
                            )
                        )
                    }
                )
            }
        }
    }
}

/**
 * Reports & Exports Screen supporting PDF and CSV exports for all 7 required report types.
 */
@Composable
fun ReportsExportScreen(
    uiState: CineVaultUiState,
    onBack: () -> Unit,
    onExportReport: (ReportType, Boolean, String?) -> Unit
) {
    BackHandler { onBack() }
    var selectedStudioId by remember {
        mutableStateOf(uiState.studios.firstOrNull()?.id)
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen")
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                }
                Column {
                    Text(
                        text = "Reports & Statements",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite
                    )
                    Text(
                        text = "Native Android PDF & Excel/Sheets CSV exports in NPR",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                }
            }
        }

        if (uiState.studios.isNotEmpty()) {
            item {
                Text(
                    text = "SELECT STUDIO (FOR SINGLE STUDIO STATEMENT)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryZinc
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
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

        items(ReportType.entries) { type ->
            val targetStudio = if (type == ReportType.STUDIO_STATEMENT) selectedStudioId else null
            GlassCard(
                cornerRadius = 20.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = type.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = type.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryZinc
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(
                        text = "Share PDF",
                        icon = Icons.Default.PictureAsPdf,
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        onClick = { onExportReport(type, true, targetStudio) }
                    )
                    GlassButton(
                        text = "Export CSV",
                        icon = Icons.Default.Share,
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = { onExportReport(type, false, targetStudio) }
                    )
                }
            }
        }
    }
}

/**
 * Global Search Screen searching across Studios, Shoots, Withdrawals, and Expenses.
 */
@Composable
fun GlobalSearchScreen(
    uiState: CineVaultUiState,
    onBack: () -> Unit,
    onSelectStudio: (String) -> Unit
) {
    BackHandler { onBack() }
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()

    val matchingStudios = remember(uiState.studioSummaries, q) {
        if (q.isEmpty()) emptyList()
        else uiState.studioSummaries.filter {
            it.studio.name.lowercase().contains(q) || it.studio.contactPerson.lowercase().contains(q)
        }
    }
    val matchingShoots = remember(uiState.shoots, q) {
        if (q.isEmpty()) emptyList()
        else uiState.shoots.filter {
            it.eventTitle.lowercase().contains(q) || it.studioName.lowercase().contains(q) || it.location.lowercase().contains(q)
        }
    }
    val matchingWithdrawals = remember(uiState.withdrawals, q) {
        if (q.isEmpty()) emptyList()
        else uiState.withdrawals.filter {
            it.studioName.lowercase().contains(q) || it.referenceNumber.lowercase().contains(q) || it.paymentMethod.lowercase().contains(q)
        }
    }
    val matchingExpenses = remember(uiState.expenses, q) {
        if (q.isEmpty()) emptyList()
        else uiState.expenses.filter {
            it.expenseName.lowercase().contains(q) || it.category.lowercase().contains(q)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                }
                Text(
                    text = "Global Vault Search",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PureWhite
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            GlassInputField(
                value = query,
                onValueChange = { query = it },
                label = "Search studios, shoots, references, or expenses...",
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SecondaryZinc) },
                testTag = "global_search_input"
            )
        }

        if (q.isNotEmpty()) {
            item {
                Text(
                    text = "Found ${matchingStudios.size} studios, ${matchingShoots.size} shoots, ${matchingWithdrawals.size} withdrawals, ${matchingExpenses.size} expenses",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryZinc
                )
            }
            items(matchingStudios, key = { "gs_st_${it.studio.id}" }) { s ->
                GlassCard(onClick = { onSelectStudio(s.studio.id) }) {
                    Text(s.studio.name, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    Text(
                        "Collectible: ${MoneyUtils.formatNpr(s.outstandingBalancePaisa)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultAmber
                    )
                }
            }
            items(matchingShoots, key = { "gs_sh_${it.id}" }) { sh ->
                GlassTransactionRow(
                    title = sh.eventTitle,
                    subtitle = "${sh.studioName} • ${sh.category}",
                    amountPaisa = sh.confirmedEarningsPaisa,
                    dateEpochMs = sh.shootDateEpochMs,
                    badgeText = sh.projectStatus,
                    badgeColor = CinemaCrimson
                )
            }
            items(matchingWithdrawals, key = { "gs_wd_${it.id}" }) { wd ->
                GlassTransactionRow(
                    title = "Withdrawal • ${wd.studioName}",
                    subtitle = "${wd.paymentMethod} • Ref: ${wd.referenceNumber.ifBlank { "-" }}",
                    amountPaisa = wd.amountPaisa,
                    dateEpochMs = wd.dateEpochMs,
                    badgeText = wd.paymentMethod,
                    badgeColor = VaultSuccess,
                    amountColor = VaultSuccess
                )
            }
            items(matchingExpenses, key = { "gs_ex_${it.id}" }) { exp ->
                GlassTransactionRow(
                    title = exp.expenseName,
                    subtitle = "${exp.category} • ${exp.paymentMethod}",
                    amountPaisa = exp.amountPaisa,
                    dateEpochMs = exp.dateEpochMs,
                    badgeText = "EXPENSE",
                    badgeColor = VaultCyan,
                    amountColor = VaultCyan
                )
            }
        }
    }
}

/**
 * Settings & Profile Screen with NPR currency confirmation, AI toggle, App Lock toggle, Account Backup export, Logout, and Account Deletion.
 */
@Composable
fun SettingsAndProfileScreen(
    uiState: CineVaultUiState,
    onBack: () -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onUpdateSettings: (UserSettings) -> Unit,
    onExportFullBackupCsv: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccountAndPurge: () -> Unit
) {
    BackHandler { onBack() }
    var displayName by remember(uiState.profile?.displayName) {
        mutableStateOf(uiState.profile?.displayName ?: "Cinematographer")
    }
    var reminderDays by remember(uiState.settings.paymentReminderDays) {
        mutableStateOf(uiState.settings.paymentReminderDays.toString())
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        GlassDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = "Delete Account & Purge Vault?"
        ) {
            Text(
                text = "This action permanently deletes all your studios, shoots, withdrawals, expenses, and profile records from Cloud Firestore and signs you out. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryZinc
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    text = "Cancel",
                    isPrimary = false,
                    modifier = Modifier.weight(1f),
                    onClick = { showDeleteConfirm = false }
                )
                GlassButton(
                    text = "Purge & Delete",
                    icon = Icons.Default.DeleteForever,
                    isPrimary = true,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccountAndPurge()
                    }
                )
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                }
                Column {
                    Text(
                        text = "Profile & Vault Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite
                    )
                    Text(
                        text = uiState.profile?.email ?: "Authenticated Cinematographer",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                }
            }
        }

        // Profile Card
        item {
            GlassCard(cornerRadius = 22.dp) {
                Text(
                    text = "Cinematographer Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassInputField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = "Display Name"
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Vault Currency: Nepalese Rupee (NPR)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Payment Channels: Bank Transfer, eSewa, Cash, Khalti, Other",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryZinc
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                GlassButton(
                    text = "Save Profile Name",
                    onClick = { onUpdateDisplayName(displayName) }
                )
            }
        }

        // Security, AI & Reminders
        item {
            GlassCard(cornerRadius = 22.dp) {
                Text(
                    text = "Security, AI & Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchRow(
                    title = "Enable CINEVAULT AI Assistant",
                    subtitle = "Allow Gemini financial Q&A, smart pricing & natural language entry",
                    checked = uiState.settings.aiEnabled,
                    onCheckedChange = {
                        onUpdateSettings(uiState.settings.copy(aiEnabled = it))
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingSwitchRow(
                    title = "Device Biometric / Credential App Lock",
                    subtitle = "Require device unlock when launching CINEVAULT",
                    checked = uiState.settings.appLockEnabled,
                    onCheckedChange = {
                        onUpdateSettings(uiState.settings.copy(appLockEnabled = it))
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingSwitchRow(
                    title = "Payment & Schedule Alerts",
                    subtitle = "Highlight upcoming and overdue studio collections on Home",
                    checked = uiState.settings.notificationsEnabled,
                    onCheckedChange = {
                        onUpdateSettings(uiState.settings.copy(notificationsEnabled = it))
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassInputField(
                        value = reminderDays,
                        onValueChange = { reminderDays = it },
                        label = "Payment Alert Window (Days)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Apply",
                        isPrimary = false,
                        onClick = {
                            val d = reminderDays.toLongOrNull()?.coerceIn(1L, 90L) ?: 7L
                            onUpdateSettings(uiState.settings.copy(paymentReminderDays = d))
                        }
                    )
                }
            }
        }

        // Data Backup, Sign Out & Account Deletion
        item {
            GlassCard(cornerRadius = 22.dp) {
                Text(
                    text = "Vault Data & Account Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlassButton(
                    text = "Export Complete Account Ledger (CSV)",
                    icon = Icons.Default.Share,
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportFullBackupCsv
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassButton(
                    text = "Sign Out",
                    icon = Icons.Default.Logout,
                    isPrimary = false,
                    testTag = "sign_out_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSignOut
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassButton(
                    text = "Delete Account & Purge Vault Data",
                    icon = Icons.Default.DeleteForever,
                    isPrimary = true,
                    testTag = "delete_account_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDeleteConfirm = true }
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = PureWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryZinc
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = CinemaCrimson
            )
        )
    }
}
