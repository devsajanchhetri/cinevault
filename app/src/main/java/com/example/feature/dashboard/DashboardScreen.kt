package com.example.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraRoll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.AnimatedNprAmount
import com.example.core.designsystem.CineVaultLogo
import com.example.core.designsystem.GlassButton
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.GlassChartContainer
import com.example.core.designsystem.GlassMetricCard
import com.example.core.designsystem.GlassStudioCard
import com.example.core.designsystem.GlassTransactionRow
import com.example.core.utilities.DateUtils
import com.example.core.utilities.MoneyUtils
import com.example.domain.LedgerActivityItem
import com.example.feature.CineVaultUiState
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassSurfaceHigh
import com.example.ui.theme.GlassSurfaceLow
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryZinc
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultPurple
import com.example.ui.theme.VaultSuccess

@Composable
fun DashboardScreen(
    uiState: CineVaultUiState,
    onQuickAddShoot: () -> Unit,
    onQuickAddStudio: () -> Unit,
    onQuickRecordWithdrawal: () -> Unit,
    onQuickAddExpense: () -> Unit,
    onOpenStudioDetail: (String) -> Unit,
    onOpenWithdrawalsLedger: () -> Unit,
    onOpenExpensesLedger: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onGenerateAiInsight: () -> Unit,
    onDraftPaymentReminder: (String, Long) -> Unit
) {
    val metrics = uiState.metrics

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
    ) {
        // 1. Top Executive Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CineVaultLogo(size = 42.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CINEVAULT",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 2.sp,
                            color = PureWhite
                        )
                        Text(
                            text = uiState.profile?.displayName?.ifBlank { "Cinematographer" } ?: "Cinematographer",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryZinc
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HeaderIconButton(
                        icon = Icons.Default.Search,
                        contentDescription = "Global Search",
                        testTag = "open_search_button",
                        onClick = onOpenGlobalSearch
                    )
                    HeaderIconButton(
                        icon = Icons.Default.CalendarMonth,
                        contentDescription = "Production Calendar",
                        testTag = "open_calendar_button",
                        onClick = onOpenCalendar
                    )
                    HeaderIconButton(
                        icon = Icons.Default.PictureAsPdf,
                        contentDescription = "Reports & Exports",
                        testTag = "open_reports_button",
                        onClick = onOpenReports
                    )
                    HeaderIconButton(
                        icon = Icons.Default.Settings,
                        contentDescription = "Settings & Profile",
                        testTag = "open_settings_button",
                        onClick = onOpenSettings
                    )
                }
            }
        }

        // 2. Hero Financial Spotlight: AVAILABLE TO COLLECT
        item {
            GlassCard(
                cornerRadius = 26.dp,
                accentGlow = CinemaCrimson,
                contentPadding = PaddingValues(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_spotlight_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "AVAILABLE TO COLLECT",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        color = CinemaCrimson
                    )
                    Text(
                        text = "NPR LEDGER",
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlassSurfaceHigh)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                AnimatedNprAmount(
                    amountPaisa = metrics.availableToCollectPaisa,
                    isHero = true,
                    color = PureWhite,
                    modifier = Modifier.testTag("available_balance_text")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Derived from Completed Shoot Earnings minus Studio Withdrawals",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryZinc
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 2x3 Executive KPI Grid inside Spotlight
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SpotlightSubMetric(
                        label = "Lifetime Earnings",
                        value = MoneyUtils.formatNpr(metrics.lifetimeEarningsPaisa),
                        valueColor = PureWhite,
                        modifier = Modifier.weight(1f)
                    )
                    SpotlightSubMetric(
                        label = "Total Withdrawals",
                        value = MoneyUtils.formatNpr(metrics.totalWithdrawalsPaisa),
                        valueColor = VaultSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SpotlightSubMetric(
                        label = "Business Expenses",
                        value = MoneyUtils.formatNpr(metrics.businessExpensesPaisa),
                        valueColor = VaultCyan,
                        modifier = Modifier.weight(1f)
                    )
                    SpotlightSubMetric(
                        label = "Net Business Profit",
                        value = MoneyUtils.formatNpr(metrics.netBusinessProfitPaisa),
                        valueColor = if (metrics.netBusinessProfitPaisa >= 0L) VaultSuccess else CinemaCrimson,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SpotlightSubMetric(
                        label = "Active Studios",
                        value = "${metrics.studioCount}",
                        valueColor = PureWhite,
                        modifier = Modifier.weight(1f)
                    )
                    SpotlightSubMetric(
                        label = "Shoots (Done / Total)",
                        value = "${metrics.completedShootCount} / ${metrics.shootCount}",
                        valueColor = PureWhite,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Quick Actions Row
        item {
            Column {
                Text(
                    text = "QUICK ACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryZinc
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    QuickActionTile(
                        title = "Add Shoot",
                        icon = Icons.Default.CameraRoll,
                        accent = CinemaCrimson,
                        testTag = "quick_add_shoot",
                        onClick = onQuickAddShoot
                    )
                    QuickActionTile(
                        title = "Add Studio",
                        icon = Icons.Default.Business,
                        accent = VaultPurple,
                        testTag = "quick_add_studio",
                        onClick = onQuickAddStudio
                    )
                    QuickActionTile(
                        title = "Record Withdrawal",
                        icon = Icons.Default.AccountBalanceWallet,
                        accent = VaultSuccess,
                        testTag = "quick_record_withdrawal",
                        onClick = onQuickRecordWithdrawal
                    )
                    QuickActionTile(
                        title = "Add Expense",
                        icon = Icons.Default.ReceiptLong,
                        accent = VaultCyan,
                        testTag = "quick_add_expense",
                        onClick = onQuickAddExpense
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassMetricCard(
                        title = "Withdrawals Ledger",
                        amountPaisa = metrics.totalWithdrawalsPaisa,
                        subtitle = "${uiState.withdrawals.size} collections • Tap to manage",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = VaultSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenWithdrawalsLedger
                    )
                    GlassMetricCard(
                        title = "Expenses Ledger",
                        amountPaisa = metrics.businessExpensesPaisa,
                        subtitle = "${uiState.expenses.size} items • Tap to manage",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = VaultCyan,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenExpensesLedger
                    )
                }
            }
        }

        // 4. Section D: Payment Alerts (Pending & Overdue)
        if (uiState.paymentAlerts.isNotEmpty()) {
            item {
                GlassCard(
                    cornerRadius = 20.dp,
                    accentGlow = VaultAmber
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Payment Alerts",
                                tint = VaultAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Payment Alerts (${uiState.paymentAlerts.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    uiState.paymentAlerts.take(3).forEach { alert ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${alert.studioName} • ${alert.eventTitle}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = PureWhite
                                )
                                val statusLabel = if (alert.isOverdue && alert.daysOverdue > 0) {
                                    "Overdue by ${alert.daysOverdue} days • Studio owes ${MoneyUtils.formatNpr(alert.studioOutstandingPaisa)}"
                                } else {
                                    "Due ${DateUtils.formatShortDate(alert.expectedPaymentDateEpochMs)} • Studio owes ${MoneyUtils.formatNpr(alert.studioOutstandingPaisa)}"
                                }
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultAmber
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            GlassButton(
                                text = "Remind",
                                isPrimary = false,
                                onClick = {
                                    onDraftPaymentReminder(alert.studioName, alert.studioOutstandingPaisa)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 5. Section E: AI Business Insights
        item {
            GlassCard(
                cornerRadius = 22.dp,
                accentGlow = VaultPurple
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Insights",
                            tint = VaultPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Business Insights",
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhite
                        )
                    }
                    if (uiState.isDashboardAiLoading) {
                        CircularProgressIndicator(
                            color = VaultPurple,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        GlassButton(
                            text = if (uiState.dashboardAiInsight == null) "Analyze Vault" else "Refresh",
                            isPrimary = false,
                            testTag = "generate_ai_insight_button",
                            onClick = onGenerateAiInsight
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = uiState.dashboardAiInsight
                        ?: "Tap 'Analyze Vault' to generate live Gemini executive insights from your actual NPR studio balances, completed shoots, and expenses.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.dashboardAiInsight != null) PureWhite else SecondaryZinc
                )
            }
        }

        // 6. Section C: Financial Trends Chart
        item {
            GlassChartContainer(
                title = "Financial Trends (NPR)",
                subtitle = "Monthly Completed Earnings vs. Withdrawals & Expenses",
                monthlyPoints = uiState.analytics?.monthlySeries ?: emptyList()
            )
        }

        // 7. Section A: Studio Spotlight
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "STUDIO SPOTLIGHT",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryZinc
                )
                Text(
                    text = "${uiState.studioSummaries.size} Studios",
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaCrimson
                )
            }
        }

        if (uiState.studioSummaries.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No studios added yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add your first production studio or client house to track shoot earnings and collectible NPR balances.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassButton(
                        text = "Create First Studio",
                        icon = Icons.Default.AddCircle,
                        onClick = onQuickAddStudio
                    )
                }
            }
        } else {
            items(uiState.studioSummaries.take(4), key = { "spot_${it.studio.id}" }) { summary ->
                GlassStudioCard(
                    summary = summary,
                    onClick = { onOpenStudioDetail(summary.studio.id) }
                )
            }
        }

        // 8. Section B: Recent Activity
        item {
            Text(
                text = "RECENT LEDGER ACTIVITY",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryZinc
            )
        }

        if (uiState.recentActivity.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Zero Ledger Transactions",
                        style = MaterialTheme.typography.titleSmall,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your account starts at strict zero state. Record a shoot, withdrawal, or business expense to populate your live feed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                }
            }
        } else {
            items(uiState.recentActivity.take(8), key = { it.id }) { act ->
                when (act) {
                    is LedgerActivityItem.ShootActivity -> {
                        GlassTransactionRow(
                            title = act.title,
                            subtitle = act.subtitle,
                            amountPaisa = act.amountPaisa,
                            dateEpochMs = act.dateEpochMs,
                            badgeText = if (act.isCompleted) "SHOOT DONE" else "PLANNED",
                            badgeColor = if (act.isCompleted) CinemaCrimson else VaultAmber,
                            amountColor = PureWhite,
                            amountPrefix = "+"
                        )
                    }
                    is LedgerActivityItem.WithdrawalActivity -> {
                        GlassTransactionRow(
                            title = act.title,
                            subtitle = act.subtitle,
                            amountPaisa = act.amountPaisa,
                            dateEpochMs = act.dateEpochMs,
                            badgeText = act.paymentMethod,
                            badgeColor = VaultSuccess,
                            amountColor = VaultSuccess,
                            amountPrefix = "Collected "
                        )
                    }
                    is LedgerActivityItem.ExpenseActivity -> {
                        GlassTransactionRow(
                            title = act.title,
                            subtitle = act.subtitle,
                            amountPaisa = act.amountPaisa,
                            dateEpochMs = act.dateEpochMs,
                            badgeText = "EXPENSE",
                            badgeColor = VaultCyan,
                            amountColor = VaultCyan,
                            amountPrefix = "-"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .testTag(testTag)
            .clip(CircleShape)
            .background(GlassSurfaceLow)
            .border(1.dp, GlassBorderBright, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = PureWhite,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun SpotlightSubMetric(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurfaceLow)
            .border(1.dp, GlassBorderBright.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = SecondaryZinc,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = valueColor
        )
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurfaceLow)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = PureWhite
        )
    }
}
