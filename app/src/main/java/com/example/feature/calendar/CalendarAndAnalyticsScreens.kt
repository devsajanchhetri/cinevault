package com.example.feature.calendar

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraRoll
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.GlassButton
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.GlassChartContainer
import com.example.core.designsystem.GlassFilterChip
import com.example.core.designsystem.GlassInputField
import com.example.core.designsystem.GlassMetricCard
import com.example.core.designsystem.GlassStudioCard
import com.example.core.designsystem.GlassTransactionRow
import com.example.core.utilities.DateUtils
import com.example.core.utilities.MoneyUtils
import com.example.domain.DateRangeFilter
import com.example.domain.model.Shoot
import com.example.feature.CineVaultUiState
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurfaceHigh
import com.example.ui.theme.GlassSurfaceLow
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryZinc
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultPurple
import com.example.ui.theme.VaultSuccess
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScheduleScreen(
    uiState: CineVaultUiState,
    onBack: () -> Unit,
    onAddShootClick: () -> Unit,
    onEditShoot: (Shoot) -> Unit
) {
    BackHandler { onBack() }

    val nowCal = remember { Calendar.getInstance() }
    var displayedYear by remember { mutableIntStateOf(nowCal.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(nowCal.get(Calendar.MONTH)) }
    var selectedDayOfMonth by remember { mutableIntStateOf(nowCal.get(Calendar.DAY_OF_MONTH)) }
    var studioFilterId by remember { mutableStateOf("All") }

    val filteredShoots = remember(uiState.shoots, studioFilterId) {
        if (studioFilterId == "All") uiState.shoots
        else uiState.shoots.filter { it.studioId == studioFilterId }
    }

    val monthHeaderLabel = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    }

    val shootsByDay = remember(filteredShoots, displayedYear, displayedMonth) {
        val map = mutableMapOf<Int, MutableList<Shoot>>()
        val cal = Calendar.getInstance()
        filteredShoots.forEach { sh ->
            cal.timeInMillis = sh.shootDateEpochMs
            if (cal.get(Calendar.YEAR) == displayedYear && cal.get(Calendar.MONTH) == displayedMonth) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                map.getOrPut(day) { mutableListOf() }.add(sh)
            }
        }
        map
    }

    val selectedDayShoots = shootsByDay[selectedDayOfMonth].orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_screen")
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
                            text = "Production Calendar",
                            style = MaterialTheme.typography.headlineSmall,
                            color = PureWhite
                        )
                        Text(
                            text = "Schedule shoots & detect booking conflicts",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryZinc
                        )
                    }
                }
                GlassButton(
                    text = "Schedule",
                    icon = Icons.Default.Add,
                    onClick = onAddShootClick
                )
            }
        }

        // Studio filter chips
        if (uiState.studios.isNotEmpty()) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    GlassFilterChip(
                        label = "All Studios",
                        selected = studioFilterId == "All",
                        onClick = { studioFilterId = "All" }
                    )
                    uiState.studios.forEach { st ->
                        GlassFilterChip(
                            label = st.name,
                            selected = studioFilterId == st.id,
                            onClick = { studioFilterId = st.id }
                        )
                    }
                }
            }
        }

        // Schedule Conflict Detector Banner
        if (uiState.scheduleConflicts.isNotEmpty()) {
            item {
                GlassCard(
                    cornerRadius = 18.dp,
                    accentGlow = VaultAmber
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Conflicts",
                            tint = VaultAmber
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.scheduleConflicts.size} Date Conflict(s) Detected",
                            style = MaterialTheme.typography.titleSmall,
                            color = PureWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    uiState.scheduleConflicts.take(3).forEach { conflict ->
                        Text(
                            text = "• ${conflict.formattedDate}: ${conflict.shoots.joinToString(" vs. ") { "${it.eventTitle} (${it.studioName})" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultAmber
                        )
                    }
                }
            }
        }

        // Monthly Calendar Grid Card
        item {
            GlassCard(cornerRadius = 22.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            if (displayedMonth == 0) {
                                displayedMonth = 11
                                displayedYear -= 1
                            } else {
                                displayedMonth -= 1
                            }
                            selectedDayOfMonth = 1
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month", tint = PureWhite)
                    }
                    Text(
                        text = monthHeaderLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                    IconButton(
                        onClick = {
                            if (displayedMonth == 11) {
                                displayedMonth = 0
                                displayedYear += 1
                            } else {
                                displayedMonth += 1
                            }
                            selectedDayOfMonth = 1
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month", tint = PureWhite)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { d ->
                        Text(
                            text = d,
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryZinc,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, displayedYear)
                    set(Calendar.MONTH, displayedMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val firstDayOffset = cal.get(Calendar.DAY_OF_WEEK) - 1
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val totalCells = ((firstDayOffset + daysInMonth + 6) / 7) * 7

                for (week in 0 until (totalCells / 7)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        for (dow in 0 until 7) {
                            val cellIndex = week * 7 + dow
                            val dayNum = cellIndex - firstDayOffset + 1
                            if (dayNum in 1..daysInMonth) {
                                val dayShoots = shootsByDay[dayNum].orEmpty()
                                val isSelected = dayNum == selectedDayOfMonth
                                val hasConflict = dayShoots.size > 1
                                val hasCompleted = dayShoots.any { it.projectStatus.equals("Completed", ignoreCase = true) }
                                val hasPlanned = dayShoots.any { !it.projectStatus.equals("Completed", ignoreCase = true) }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) CinemaCrimson.copy(alpha = 0.28f)
                                            else GlassSurfaceLow
                                        )
                                        .border(
                                            1.dp,
                                            when {
                                                hasConflict -> VaultAmber
                                                isSelected -> CinemaCrimson
                                                else -> GlassBorderSubtle
                                            },
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedDayOfMonth = dayNum }
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = PureWhite
                                        )
                                        if (dayShoots.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                if (hasCompleted) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(VaultSuccess)
                                                    )
                                                }
                                                if (hasPlanned) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(VaultAmber)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).height(42.dp))
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Shoots
        item {
            Text(
                text = "SELECTED DAY ($selectedDayOfMonth $monthHeaderLabel) • ${selectedDayShoots.size} SHOOT(S)",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryZinc
            )
        }

        if (selectedDayShoots.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No shoots scheduled on this date",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                }
            }
        } else {
            items(selectedDayShoots, key = { "cal_sel_${it.id}" }) { sh ->
                val isCompleted = sh.projectStatus.equals("Completed", ignoreCase = true)
                GlassTransactionRow(
                    title = sh.eventTitle,
                    subtitle = "${sh.studioName} • ${sh.category} • ${sh.location.ifBlank { "Nepal" }}",
                    amountPaisa = sh.confirmedEarningsPaisa,
                    dateEpochMs = sh.shootDateEpochMs,
                    badgeText = sh.projectStatus,
                    badgeColor = if (isCompleted) VaultSuccess else VaultAmber,
                    onClick = { onEditShoot(sh) }
                )
            }
        }

        // Upcoming Planned vs Completed Overview
        val plannedUpcoming = filteredShoots.filter { !it.projectStatus.equals("Completed", ignoreCase = true) }
        val completedShoots = filteredShoots.filter { it.projectStatus.equals("Completed", ignoreCase = true) }

        item {
            Text(
                text = "PLANNED PROJECTS (${plannedUpcoming.size}) — PIPELINE FORECAST",
                style = MaterialTheme.typography.labelSmall,
                color = VaultAmber
            )
        }
        items(plannedUpcoming.take(6), key = { "plan_${it.id}" }) { sh ->
            GlassTransactionRow(
                title = sh.eventTitle,
                subtitle = "${sh.studioName} • ${sh.category} • Tap to mark Completed",
                amountPaisa = sh.confirmedEarningsPaisa,
                dateEpochMs = sh.shootDateEpochMs,
                badgeText = "PLANNED",
                badgeColor = VaultAmber,
                amountPrefix = "Forecast ",
                onClick = { onEditShoot(sh) }
            )
        }

        item {
            Text(
                text = "COMPLETED PROJECTS (${completedShoots.size}) — IN COLLECTIBLE LEDGER",
                style = MaterialTheme.typography.labelSmall,
                color = VaultSuccess
            )
        }
        items(completedShoots.take(6), key = { "done_${it.id}" }) { sh ->
            GlassTransactionRow(
                title = sh.eventTitle,
                subtitle = "${sh.studioName} • ${sh.category}",
                amountPaisa = sh.confirmedEarningsPaisa,
                dateEpochMs = sh.shootDateEpochMs,
                badgeText = "COMPLETED",
                badgeColor = VaultSuccess,
                amountPrefix = "+",
                onClick = { onEditShoot(sh) }
            )
        }
    }
}

@Composable
fun AnalyticsInsightsScreen(
    uiState: CineVaultUiState,
    onFilterChanged: (DateRangeFilter, Long?, Long?) -> Unit,
    onOpenReports: () -> Unit
) {
    val analytics = uiState.analytics ?: return
    var customStartIso by remember {
        mutableStateOf(DateUtils.formatIsoDate(uiState.customStartDateMs ?: (System.currentTimeMillis() - 30L * 86400000L)))
    }
    var customEndIso by remember {
        mutableStateOf(DateUtils.formatIsoDate(uiState.customEndDateMs ?: System.currentTimeMillis()))
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("insights_screen")
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Financial Analytics",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite
                    )
                    Text(
                        text = "Real-time NPR earnings, studio & category performance",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                }
                GlassButton(
                    text = "Export",
                    icon = Icons.Default.PictureAsPdf,
                    isPrimary = false,
                    onClick = onOpenReports
                )
            }
        }

        // Date Range Filter Chips
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                DateRangeFilter.entries.forEach { f ->
                    GlassFilterChip(
                        label = f.label,
                        selected = uiState.analyticsFilter == f,
                        onClick = {
                            if (f == DateRangeFilter.CUSTOM) {
                                onFilterChanged(
                                    f,
                                    DateUtils.parseIsoDateOrToday(customStartIso),
                                    DateUtils.parseIsoDateOrToday(customEndIso)
                                )
                            } else {
                                onFilterChanged(f, null, null)
                            }
                        }
                    )
                }
            }

            if (uiState.analyticsFilter == DateRangeFilter.CUSTOM) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassInputField(
                        value = customStartIso,
                        onValueChange = { customStartIso = it },
                        label = "Start (YYYY-MM-DD)",
                        modifier = Modifier.weight(1f)
                    )
                    GlassInputField(
                        value = customEndIso,
                        onValueChange = { customEndIso = it },
                        label = "End (YYYY-MM-DD)",
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Apply",
                        onClick = {
                            onFilterChanged(
                                DateRangeFilter.CUSTOM,
                                DateUtils.parseIsoDateOrToday(customStartIso),
                                DateUtils.parseIsoDateOrToday(customEndIso)
                            )
                        }
                    )
                }
            }
        }

        // Core Analytics KPI Grid
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassMetricCard(
                    title = "Period Earnings",
                    amountPaisa = analytics.totalEarningsPaisa,
                    subtitle = "Completed shoots",
                    icon = Icons.Default.CameraRoll,
                    accentColor = CinemaCrimson,
                    modifier = Modifier.weight(1f)
                )
                GlassMetricCard(
                    title = "Period Withdrawals",
                    amountPaisa = analytics.totalWithdrawalsPaisa,
                    subtitle = "Collected in NPR",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = VaultSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassMetricCard(
                    title = "Period Expenses",
                    amountPaisa = analytics.totalExpensesPaisa,
                    subtitle = "Business overhead",
                    icon = Icons.Default.ReceiptLong,
                    accentColor = VaultCyan,
                    modifier = Modifier.weight(1f)
                )
                GlassMetricCard(
                    title = "Period Net Profit",
                    amountPaisa = analytics.netProfitPaisa,
                    subtitle = "Earnings - Expenses",
                    icon = Icons.Default.TrendingUp,
                    accentColor = if (analytics.netProfitPaisa >= 0L) VaultSuccess else CinemaCrimson,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassMetricCard(
                    title = "Avg Earnings / Shoot",
                    amountPaisa = analytics.averageEarningsPerShootPaisa,
                    subtitle = "Completed shoot average",
                    icon = Icons.Default.CameraRoll,
                    accentColor = VaultPurple,
                    modifier = Modifier.weight(1f)
                )
                GlassMetricCard(
                    title = "Total Outstanding",
                    amountPaisa = analytics.outstandingBalancePaisa,
                    subtitle = "Available to collect",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = VaultAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Executive Highlights Card (Highest Shoot, Best Studio, Best Month)
        item {
            GlassCard(cornerRadius = 22.dp) {
                Text(
                    text = "Executive Performance Highlights",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(10.dp))
                HighlightLine(
                    label = "Highest Earning Shoot",
                    value = analytics.highestEarningShoot?.let {
                        "${it.eventTitle} (${MoneyUtils.formatNpr(it.confirmedEarningsPaisa)})"
                    } ?: "No completed shoots in period"
                )
                Spacer(modifier = Modifier.height(8.dp))
                HighlightLine(
                    label = "Best Performing Studio",
                    value = analytics.bestPerformingStudio?.let {
                        "${it.studio.name} (${MoneyUtils.formatNpr(it.totalEarningsPaisa)})"
                    } ?: "No studio earnings in period"
                )
                Spacer(modifier = Modifier.height(8.dp))
                HighlightLine(
                    label = "Best Earning Month",
                    value = analytics.bestEarningMonth?.let {
                        "${it.monthLabel} (${MoneyUtils.formatNpr(it.earningsPaisa)})"
                    } ?: "No monthly data yet"
                )
            }
        }

        // Monthly Trends Chart
        item {
            GlassChartContainer(
                title = "Monthly Earnings, Withdrawals & Expenses",
                subtitle = "Filter: ${uiState.analyticsFilter.label}",
                monthlyPoints = analytics.monthlySeries
            )
        }

        // Category Performance Breakdown
        item {
            GlassCard(cornerRadius = 22.dp) {
                Text(
                    text = "Shoot Category Performance",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (analytics.shootCategoryPerformance.isEmpty()) {
                    Text(
                        text = "No completed shoot categories in selected period.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc
                    )
                } else {
                    analytics.shootCategoryPerformance.forEach { cat ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${cat.category} (${cat.count} shoots)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureWhite
                                )
                                Text(
                                    text = "${MoneyUtils.formatNpr(cat.amountPaisa)} (${cat.sharePercentage.toInt()}%)",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 13.sp,
                                    color = CinemaCrimson
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (cat.sharePercentage / 100f).coerceIn(0f, 1f) },
                                color = CinemaCrimson,
                                trackColor = GlassSurfaceHigh,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }
        }

        // Studio Performance & Outstanding Payments
        item {
            Text(
                text = "STUDIO PERFORMANCE & OUTSTANDING PAYMENTS",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryZinc
            )
        }

        items(analytics.studioPerformance, key = { "an_st_${it.studio.id}" }) { summary ->
            GlassStudioCard(
                summary = summary,
                onClick = {}
            )
        }
    }
}

@Composable
private fun HighlightLine(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryZinc
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = PureWhite
        )
    }
}
