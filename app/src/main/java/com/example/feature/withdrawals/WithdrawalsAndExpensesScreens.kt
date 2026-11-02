package com.example.feature.withdrawals

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import com.example.core.designsystem.GlassMetricCard
import com.example.core.designsystem.GlassTransactionRow
import com.example.core.utilities.DateUtils
import com.example.core.utilities.MoneyUtils
import com.example.domain.model.CineVaultConstants
import com.example.domain.model.Expense
import com.example.domain.model.Shoot
import com.example.domain.model.Studio
import com.example.domain.model.Withdrawal
import com.example.feature.CineVaultUiState
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryZinc
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultSuccess

@Composable
fun WithdrawalsManagementScreen(
    uiState: CineVaultUiState,
    onBack: () -> Unit,
    onRecordWithdrawalClick: () -> Unit,
    onEditWithdrawal: (Withdrawal) -> Unit,
    onDeleteWithdrawal: (String) -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    BackHandler { onBack() }
    var searchRef by remember { mutableStateOf("") }
    var selectedStudioId by remember { mutableStateOf("All") }
    var selectedPaymentMethod by remember { mutableStateOf("All") }
    var selectedWithdrawal by remember { mutableStateOf<Withdrawal?>(null) }

    val filteredWithdrawals = remember(uiState.withdrawals, searchRef, selectedStudioId, selectedPaymentMethod) {
        uiState.withdrawals.filter { w ->
            val q = searchRef.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                w.referenceNumber.lowercase().contains(q) ||
                w.studioName.lowercase().contains(q) ||
                w.notes.lowercase().contains(q)
            val matchesStudio = selectedStudioId == "All" || w.studioId == selectedStudioId
            val matchesMethod = selectedPaymentMethod == "All" || w.paymentMethod == selectedPaymentMethod
            matchesSearch && matchesStudio && matchesMethod
        }
    }

    if (selectedWithdrawal != null) {
        val wd = selectedWithdrawal!!
        GlassDialog(
            onDismissRequest = { selectedWithdrawal = null },
            title = "Withdrawal • ${wd.studioName}"
        ) {
            Text(
                text = "Amount: ${MoneyUtils.formatNpr(wd.amountPaisa)}\nDate: ${DateUtils.formatShortDate(wd.dateEpochMs)}\nPayment Method: ${wd.paymentMethod}\nReference: ${wd.referenceNumber.ifBlank { "None" }}\nNotes: ${wd.notes.ifBlank { "None" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryZinc
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    text = "Delete",
                    icon = Icons.Default.Delete,
                    isPrimary = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val id = wd.id
                        selectedWithdrawal = null
                        onDeleteWithdrawal(id)
                    }
                )
                GlassButton(
                    text = "Edit",
                    icon = Icons.Default.Edit,
                    isPrimary = true,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val target = wd
                        selectedWithdrawal = null
                        onEditWithdrawal(target)
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
            .testTag("withdrawals_screen")
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
                            text = "Withdrawals Ledger",
                            style = MaterialTheme.typography.headlineSmall,
                            color = PureWhite
                        )
                        Text(
                            text = "Bank Transfer • eSewa • Cash • Khalti • Other",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryZinc
                        )
                    }
                }
                GlassButton(
                    text = "Record",
                    icon = Icons.Default.Add,
                    testTag = "record_withdrawal_button",
                    onClick = onRecordWithdrawalClick
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassMetricCard(
                    title = "Total Collected",
                    amountPaisa = uiState.metrics.totalWithdrawalsPaisa,
                    subtitle = "${uiState.withdrawals.size} records",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = VaultSuccess,
                    modifier = Modifier.weight(1f)
                )
                GlassMetricCard(
                    title = "Remaining Collectible",
                    amountPaisa = uiState.metrics.availableToCollectPaisa,
                    subtitle = "Earned minus Withdrawals",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = VaultAmber,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    text = "Export PDF",
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

        item {
            GlassInputField(
                value = searchRef,
                onValueChange = { searchRef = it },
                label = "Search reference number, studio, or notes",
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SecondaryZinc) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                GlassFilterChip(
                    label = "All Methods",
                    selected = selectedPaymentMethod == "All",
                    onClick = { selectedPaymentMethod = "All" }
                )
                CineVaultConstants.PAYMENT_METHODS.forEach { pm ->
                    GlassFilterChip(
                        label = pm,
                        selected = selectedPaymentMethod == pm,
                        onClick = { selectedPaymentMethod = pm }
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

        if (filteredWithdrawals.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No withdrawals recorded",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Record payments collected via Bank Transfer, eSewa, Cash, Khalti, or Other to reduce studio collectible balances.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                }
            }
        } else {
            items(filteredWithdrawals, key = { it.id }) { wd ->
                GlassTransactionRow(
                    title = wd.studioName,
                    subtitle = "${wd.paymentMethod}${if (wd.referenceNumber.isNotBlank()) " • Ref: ${wd.referenceNumber}" else ""}",
                    amountPaisa = wd.amountPaisa,
                    dateEpochMs = wd.dateEpochMs,
                    badgeText = wd.paymentMethod,
                    badgeColor = VaultSuccess,
                    amountColor = VaultSuccess,
                    amountPrefix = "Collected ",
                    onClick = { selectedWithdrawal = wd }
                )
            }
        }
    }
}

@Composable
fun WithdrawalFormBottomSheet(
    initialWithdrawal: Withdrawal?,
    studios: List<Studio>,
    preselectedStudio: Studio? = null,
    onDismiss: () -> Unit,
    onSave: (Withdrawal, Boolean) -> Unit
) {
    val isEdit = initialWithdrawal != null
    var selectedStudio by remember {
        mutableStateOf(
            studios.find { it.id == initialWithdrawal?.studioId }
                ?: preselectedStudio
                ?: studios.firstOrNull { !it.archived }
        )
    }
    var amountInput by remember {
        mutableStateOf(initialWithdrawal?.let { MoneyUtils.paisaToInputString(it.amountPaisa) } ?: "")
    }
    var dateIso by remember {
        mutableStateOf(DateUtils.formatIsoDate(initialWithdrawal?.dateEpochMs ?: System.currentTimeMillis()))
    }
    var paymentMethod by remember { mutableStateOf(initialWithdrawal?.paymentMethod ?: "Bank Transfer") }
    var referenceNumber by remember { mutableStateOf(initialWithdrawal?.referenceNumber ?: "") }
    var notes by remember { mutableStateOf(initialWithdrawal?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    GlassBottomSheet(
        onDismissRequest = onDismiss,
        title = if (isEdit) "Edit Withdrawal" else "Record Studio Withdrawal"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaCrimson
                )
            }

            Text(
                text = "STUDIO *",
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

            GlassInputField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = "Withdrawal Amount (NPR) *",
                placeholder = "e.g. 20000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                testTag = "withdrawal_amount_input"
            )

            Text(
                text = "PAYMENT METHOD *",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryZinc
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                CineVaultConstants.PAYMENT_METHODS.forEach { pm ->
                    GlassFilterChip(
                        label = pm,
                        selected = paymentMethod == pm,
                        onClick = { paymentMethod = pm }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassInputField(
                    value = dateIso,
                    onValueChange = { dateIso = it },
                    label = "Date (YYYY-MM-DD)",
                    modifier = Modifier.weight(1f)
                )
                GlassInputField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = "Reference Number",
                    placeholder = "e.g. ESW-99887",
                    modifier = Modifier.weight(1f),
                    testTag = "withdrawal_ref_input"
                )
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
                    text = if (isEdit) "Update Withdrawal" else "Save Withdrawal",
                    isPrimary = true,
                    testTag = "save_withdrawal_submit_button",
                    modifier = Modifier.weight(1.4f),
                    onClick = {
                        val st = selectedStudio
                        if (st == null) {
                            errorMsg = "Please select a studio."
                            return@GlassButton
                        }
                        val paisa = MoneyUtils.parseNprToPaisa(amountInput)
                        if (paisa == null || paisa <= 0L) {
                            errorMsg = "Withdrawal amount must be greater than NPR 0."
                            return@GlassButton
                        }
                        val wd = (initialWithdrawal ?: Withdrawal()).copy(
                            studioId = st.id,
                            studioName = st.name,
                            amountPaisa = paisa,
                            dateEpochMs = DateUtils.parseIsoDateOrToday(dateIso),
                            paymentMethod = paymentMethod,
                            referenceNumber = referenceNumber.trim(),
                            notes = notes.trim()
                        )
                        onSave(wd, isEdit)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun ExpensesManagementScreen(
    uiState: CineVaultUiState,
    onBack: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    BackHandler { onBack() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }

    val filteredExpenses = remember(uiState.expenses, searchQuery, selectedCategory) {
        uiState.expenses.filter { e ->
            val q = searchQuery.trim().lowercase()
            val matchesQ = q.isEmpty() ||
                e.expenseName.lowercase().contains(q) ||
                e.category.lowercase().contains(q) ||
                e.notes.lowercase().contains(q)
            val matchesCat = selectedCategory == "All" || e.category == selectedCategory
            matchesQ && matchesCat
        }
    }

    if (selectedExpense != null) {
        val exp = selectedExpense!!
        GlassDialog(
            onDismissRequest = { selectedExpense = null },
            title = exp.expenseName
        ) {
            Text(
                text = "Category: ${exp.category}\nAmount: ${MoneyUtils.formatNpr(exp.amountPaisa)}\nDate: ${DateUtils.formatShortDate(exp.dateEpochMs)}\nPayment Method: ${exp.paymentMethod}\nReceipt Attached: ${if (exp.receiptUri.isNotBlank()) "Yes" else "No"}\nNotes: ${exp.notes.ifBlank { "None" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryZinc
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    text = "Delete",
                    icon = Icons.Default.Delete,
                    isPrimary = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val id = exp.id
                        selectedExpense = null
                        onDeleteExpense(id)
                    }
                )
                GlassButton(
                    text = "Edit",
                    icon = Icons.Default.Edit,
                    isPrimary = true,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val target = exp
                        selectedExpense = null
                        onEditExpense(target)
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
            .testTag("expenses_screen")
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
                            text = "Business Expenses",
                            style = MaterialTheme.typography.headlineSmall,
                            color = PureWhite
                        )
                        Text(
                            text = "Separate from studio collectible balances",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryZinc
                        )
                    }
                }
                GlassButton(
                    text = "Add Expense",
                    icon = Icons.Default.Add,
                    testTag = "add_expense_button",
                    onClick = onAddExpenseClick
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassMetricCard(
                    title = "Gross Shoot Earnings",
                    amountPaisa = uiState.metrics.lifetimeEarningsPaisa,
                    subtitle = "Completed shoots",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = PureWhite,
                    modifier = Modifier.weight(1f)
                )
                GlassMetricCard(
                    title = "Total Expenses",
                    amountPaisa = uiState.metrics.businessExpensesPaisa,
                    subtitle = "${uiState.expenses.size} entries",
                    icon = Icons.Default.ReceiptLong,
                    accentColor = VaultCyan,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            GlassMetricCard(
                title = "Net Business Profit (Gross Earnings - Business Expenses)",
                amountPaisa = uiState.metrics.netBusinessProfitPaisa,
                subtitle = "Studio collectible balances remain untouched by internal expenses",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = if (uiState.metrics.netBusinessProfitPaisa >= 0L) VaultSuccess else CinemaCrimson,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    text = "Export PDF",
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

        item {
            GlassInputField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search expenses",
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SecondaryZinc) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                GlassFilterChip(
                    label = "All Categories",
                    selected = selectedCategory == "All",
                    onClick = { selectedCategory = "All" }
                )
                CineVaultConstants.EXPENSE_CATEGORIES.forEach { cat ->
                    GlassFilterChip(
                        label = cat,
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat }
                    )
                }
            }
        }

        if (filteredExpenses.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No business expenses recorded",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Track lens rentals, lighting, assistant payments, travel, and software in NPR.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                }
            }
        } else {
            items(filteredExpenses, key = { it.id }) { exp ->
                GlassTransactionRow(
                    title = exp.expenseName,
                    subtitle = "${exp.category} • ${exp.paymentMethod}${if (exp.receiptUri.isNotBlank()) " • Receipt Attached" else ""}",
                    amountPaisa = exp.amountPaisa,
                    dateEpochMs = exp.dateEpochMs,
                    badgeText = exp.category.take(12),
                    badgeColor = VaultCyan,
                    amountColor = VaultCyan,
                    amountPrefix = "-",
                    onClick = { selectedExpense = exp }
                )
            }
        }
    }
}

@Composable
fun ExpenseFormBottomSheet(
    initialExpense: Expense?,
    studios: List<Studio>,
    shoots: List<Shoot>,
    onDismiss: () -> Unit,
    onSave: (Expense, Boolean) -> Unit
) {
    val isEdit = initialExpense != null
    var expenseName by remember { mutableStateOf(initialExpense?.expenseName ?: "") }
    var category by remember { mutableStateOf(initialExpense?.category ?: "Camera Equipment") }
    var amountInput by remember {
        mutableStateOf(initialExpense?.let { MoneyUtils.paisaToInputString(it.amountPaisa) } ?: "")
    }
    var dateIso by remember {
        mutableStateOf(DateUtils.formatIsoDate(initialExpense?.dateEpochMs ?: System.currentTimeMillis()))
    }
    var paymentMethod by remember { mutableStateOf(initialExpense?.paymentMethod ?: "Bank Transfer") }
    var associatedShootId by remember { mutableStateOf(initialExpense?.associatedShootId ?: "") }
    var associatedStudioId by remember { mutableStateOf(initialExpense?.associatedStudioId ?: "") }
    var receiptUri by remember { mutableStateOf(initialExpense?.receiptUri ?: "") }
    var notes by remember { mutableStateOf(initialExpense?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker per Google Play policy
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            receiptUri = uri.toString()
        }
    }

    GlassBottomSheet(
        onDismissRequest = onDismiss,
        title = if (isEdit) "Edit Business Expense" else "Add Business Expense"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaCrimson
                )
            }

            GlassInputField(
                value = expenseName,
                onValueChange = { expenseName = it },
                label = "Expense Name *",
                placeholder = "e.g. Sony 24-70mm GM II Lens Rental",
                testTag = "expense_name_input"
            )

            GlassInputField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = "Expense Amount (NPR) *",
                placeholder = "e.g. 3500",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                testTag = "expense_amount_input"
            )

            Text(
                text = "CATEGORY *",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryZinc
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                CineVaultConstants.EXPENSE_CATEGORIES.forEach { cat ->
                    GlassFilterChip(
                        label = cat,
                        selected = category == cat,
                        onClick = { category = cat }
                    )
                }
            }

            Text(
                text = "PAYMENT METHOD *",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryZinc
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                CineVaultConstants.PAYMENT_METHODS.forEach { pm ->
                    GlassFilterChip(
                        label = pm,
                        selected = paymentMethod == pm,
                        onClick = { paymentMethod = pm }
                    )
                }
            }

            GlassInputField(
                value = dateIso,
                onValueChange = { dateIso = it },
                label = "Date (YYYY-MM-DD)"
            )

            if (studios.isNotEmpty()) {
                Text(
                    text = "OPTIONAL ASSOCIATED STUDIO",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryZinc
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    GlassFilterChip(
                        label = "None",
                        selected = associatedStudioId.isEmpty(),
                        onClick = { associatedStudioId = "" }
                    )
                    studios.forEach { st ->
                        GlassFilterChip(
                            label = st.name,
                            selected = associatedStudioId == st.id,
                            onClick = { associatedStudioId = st.id }
                        )
                    }
                }
            }

            if (shoots.isNotEmpty()) {
                Text(
                    text = "OPTIONAL ASSOCIATED SHOOT",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryZinc
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    GlassFilterChip(
                        label = "None",
                        selected = associatedShootId.isEmpty(),
                        onClick = { associatedShootId = "" }
                    )
                    shoots.take(10).forEach { sh ->
                        GlassFilterChip(
                            label = sh.eventTitle,
                            selected = associatedShootId == sh.id,
                            onClick = { associatedShootId = sh.id }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (receiptUri.isNotBlank()) "Receipt Attached" else "No Receipt Image Attached",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (receiptUri.isNotBlank()) VaultSuccess else SecondaryZinc
                )
                GlassButton(
                    text = if (receiptUri.isNotBlank()) "Change Receipt" else "Attach Receipt",
                    icon = Icons.Default.AttachFile,
                    isPrimary = false,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
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
                    text = if (isEdit) "Update Expense" else "Save Expense",
                    isPrimary = true,
                    testTag = "save_expense_submit_button",
                    modifier = Modifier.weight(1.4f),
                    onClick = {
                        if (expenseName.isBlank()) {
                            errorMsg = "Expense name is required."
                            return@GlassButton
                        }
                        val paisa = MoneyUtils.parseNprToPaisa(amountInput)
                        if (paisa == null || paisa <= 0L) {
                            errorMsg = "Expense amount must be greater than NPR 0."
                            return@GlassButton
                        }
                        val exp = (initialExpense ?: Expense()).copy(
                            expenseName = expenseName.trim(),
                            category = category,
                            amountPaisa = paisa,
                            dateEpochMs = DateUtils.parseIsoDateOrToday(dateIso),
                            associatedShootId = associatedShootId,
                            associatedStudioId = associatedStudioId,
                            paymentMethod = paymentMethod,
                            receiptUri = receiptUri,
                            notes = notes.trim()
                        )
                        onSave(exp, isEdit)
                        onDismiss()
                    }
                )
            }
        }
    }
}
