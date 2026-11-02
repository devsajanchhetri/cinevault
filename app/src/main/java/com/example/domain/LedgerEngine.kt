package com.example.domain

import com.example.core.utilities.DateUtils
import com.example.domain.model.Expense
import com.example.domain.model.Shoot
import com.example.domain.model.Studio
import com.example.domain.model.Withdrawal
import java.util.Calendar
import kotlin.math.max

enum class DateRangeFilter(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3 Months"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Range")
}

data class StudioFinancialSummary(
    val studio: Studio,
    val totalEarningsPaisa: Long,
    val totalWithdrawalsPaisa: Long,
    val outstandingBalancePaisa: Long,
    val completedShootCount: Int,
    val plannedShootCount: Int,
    val totalShootCount: Int,
    val collectionPercentage: Float
)

data class DashboardMetrics(
    val availableToCollectPaisa: Long = 0L,
    val lifetimeEarningsPaisa: Long = 0L,
    val totalWithdrawalsPaisa: Long = 0L,
    val outstandingBalancePaisa: Long = 0L,
    val businessExpensesPaisa: Long = 0L,
    val netBusinessProfitPaisa: Long = 0L,
    val studioCount: Int = 0,
    val shootCount: Int = 0,
    val completedShootCount: Int = 0,
    val plannedShootCount: Int = 0,
    val plannedPipelinePaisa: Long = 0L
)

data class PaymentAlertItem(
    val shootId: String,
    val eventTitle: String,
    val studioId: String,
    val studioName: String,
    val shootEarningsPaisa: Long,
    val studioOutstandingPaisa: Long,
    val expectedPaymentDateEpochMs: Long,
    val daysOverdue: Int,
    val isOverdue: Boolean
)

data class ScheduleConflict(
    val dateEpochMs: Long,
    val formattedDate: String,
    val shoots: List<Shoot>
)

data class MonthlyFinancialPoint(
    val monthKey: String,
    val monthLabel: String,
    val earningsPaisa: Long,
    val withdrawalsPaisa: Long,
    val expensesPaisa: Long,
    val netProfitPaisa: Long
)

data class CategoryBreakdown(
    val category: String,
    val amountPaisa: Long,
    val count: Int,
    val sharePercentage: Float
)

data class AnalyticsSnapshot(
    val filter: DateRangeFilter,
    val totalEarningsPaisa: Long,
    val totalWithdrawalsPaisa: Long,
    val totalExpensesPaisa: Long,
    val netProfitPaisa: Long,
    val outstandingBalancePaisa: Long,
    val averageEarningsPerShootPaisa: Long,
    val highestEarningShoot: Shoot?,
    val bestPerformingStudio: StudioFinancialSummary?,
    val bestEarningMonth: MonthlyFinancialPoint?,
    val monthlySeries: List<MonthlyFinancialPoint>,
    val studioPerformance: List<StudioFinancialSummary>,
    val shootCategoryPerformance: List<CategoryBreakdown>,
    val expenseCategoryBreakdown: List<CategoryBreakdown>
)

sealed interface LedgerActivityItem {
    val id: String
    val title: String
    val subtitle: String
    val amountPaisa: Long
    val dateEpochMs: Long

    data class ShootActivity(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val amountPaisa: Long,
        override val dateEpochMs: Long,
        val isCompleted: Boolean
    ) : LedgerActivityItem

    data class WithdrawalActivity(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val amountPaisa: Long,
        override val dateEpochMs: Long,
        val paymentMethod: String
    ) : LedgerActivityItem

    data class ExpenseActivity(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val amountPaisa: Long,
        override val dateEpochMs: Long,
        val category: String
    ) : LedgerActivityItem
}

object LedgerEngine {

    /**
     * Strict rule: Only Completed shoots add to the collectible earnings ledger.
     * Business expenses are kept strictly separate from studio collectible balances.
     */
    fun computeDashboardMetrics(
        studios: List<Studio>,
        shoots: List<Shoot>,
        withdrawals: List<Withdrawal>,
        expenses: List<Expense>
    ): DashboardMetrics {
        val completedShoots = shoots.filter { it.projectStatus.equals("Completed", ignoreCase = true) }
        val plannedShoots = shoots.filter { !it.projectStatus.equals("Completed", ignoreCase = true) }

        val lifetimeEarningsPaisa = completedShoots.sumOf { max(0L, it.confirmedEarningsPaisa) }
        val totalWithdrawalsPaisa = withdrawals.sumOf { max(0L, it.amountPaisa) }
        val availableToCollectPaisa = lifetimeEarningsPaisa - totalWithdrawalsPaisa
        val businessExpensesPaisa = expenses.sumOf { max(0L, it.amountPaisa) }
        val netBusinessProfitPaisa = lifetimeEarningsPaisa - businessExpensesPaisa
        val plannedPipelinePaisa = plannedShoots.sumOf { max(0L, it.confirmedEarningsPaisa) }

        val activeStudios = studios.filter { !it.archived }

        return DashboardMetrics(
            availableToCollectPaisa = availableToCollectPaisa,
            lifetimeEarningsPaisa = lifetimeEarningsPaisa,
            totalWithdrawalsPaisa = totalWithdrawalsPaisa,
            outstandingBalancePaisa = availableToCollectPaisa,
            businessExpensesPaisa = businessExpensesPaisa,
            netBusinessProfitPaisa = netBusinessProfitPaisa,
            studioCount = activeStudios.size,
            shootCount = shoots.size,
            completedShootCount = completedShoots.size,
            plannedShootCount = plannedShoots.size,
            plannedPipelinePaisa = plannedPipelinePaisa
        )
    }

    fun computeStudioSummaries(
        studios: List<Studio>,
        shoots: List<Shoot>,
        withdrawals: List<Withdrawal>
    ): List<StudioFinancialSummary> {
        return studios.map { studio ->
            val studioShoots = shoots.filter { it.studioId == studio.id }
            val completedShoots = studioShoots.filter { it.projectStatus.equals("Completed", ignoreCase = true) }
            val plannedShoots = studioShoots.filter { !it.projectStatus.equals("Completed", ignoreCase = true) }
            val studioWithdrawals = withdrawals.filter { it.studioId == studio.id }

            val earnings = completedShoots.sumOf { max(0L, it.confirmedEarningsPaisa) }
            val collected = studioWithdrawals.sumOf { max(0L, it.amountPaisa) }
            val outstanding = earnings - collected
            val collectionPct = if (earnings > 0L) {
                ((collected.toDouble() / earnings.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)
            } else {
                0f
            }

            StudioFinancialSummary(
                studio = studio,
                totalEarningsPaisa = earnings,
                totalWithdrawalsPaisa = collected,
                outstandingBalancePaisa = outstanding,
                completedShootCount = completedShoots.size,
                plannedShootCount = plannedShoots.size,
                totalShootCount = studioShoots.size,
                collectionPercentage = collectionPct
            )
        }.sortedByDescending { it.outstandingBalancePaisa }
    }

    fun detectScheduleConflicts(shoots: List<Shoot>): List<ScheduleConflict> {
        return shoots
            .filter { it.shootDateEpochMs > 0L }
            .groupBy { DateUtils.startOfDayMillis(it.shootDateEpochMs) }
            .filter { (_, dayShoots) -> dayShoots.size > 1 }
            .map { (dayEpoch, dayShoots) ->
                ScheduleConflict(
                    dateEpochMs = dayEpoch,
                    formattedDate = DateUtils.formatShortDate(dayEpoch),
                    shoots = dayShoots.sortedBy { it.eventTitle }
                )
            }
            .sortedByDescending { it.dateEpochMs }
    }

    fun detectConflictsForDate(
        shootDateEpochMs: Long,
        existingShoots: List<Shoot>,
        excludeShootId: String = ""
    ): List<Shoot> {
        if (shootDateEpochMs <= 0L) return emptyList()
        return existingShoots.filter {
            it.id != excludeShootId && DateUtils.isSameCalendarDay(it.shootDateEpochMs, shootDateEpochMs)
        }
    }

    fun computePaymentAlerts(
        studios: List<Studio>,
        shoots: List<Shoot>,
        withdrawals: List<Withdrawal>,
        nowEpochMs: Long = System.currentTimeMillis(),
        reminderWindowDays: Int = 7
    ): List<PaymentAlertItem> {
        val studioSummaries = computeStudioSummaries(studios, shoots, withdrawals).associateBy { it.studio.id }
        val dayMs = 24L * 60L * 60L * 1000L
        val upcomingThreshold = nowEpochMs + (reminderWindowDays * dayMs)

        return shoots
            .filter { shoot ->
                val isCompleted = shoot.projectStatus.equals("Completed", ignoreCase = true)
                val isUnsettled = !shoot.paymentStatus.equals("Settled", ignoreCase = true)
                val studioBalance = studioSummaries[shoot.studioId]?.outstandingBalancePaisa ?: 0L
                val hasExpectedDate = shoot.expectedPaymentDateEpochMs in 1..upcomingThreshold
                isCompleted && isUnsettled && studioBalance > 0L && (hasExpectedDate || shoot.paymentStatus.equals("Overdue", ignoreCase = true))
            }
            .map { shoot ->
                val due = if (shoot.expectedPaymentDateEpochMs > 0L) shoot.expectedPaymentDateEpochMs else shoot.shootDateEpochMs
                val diffDays = ((nowEpochMs - due) / dayMs).toInt()
                val isOverdue = nowEpochMs > due || shoot.paymentStatus.equals("Overdue", ignoreCase = true)
                val studioBalance = studioSummaries[shoot.studioId]?.outstandingBalancePaisa ?: shoot.confirmedEarningsPaisa
                PaymentAlertItem(
                    shootId = shoot.id,
                    eventTitle = shoot.eventTitle,
                    studioId = shoot.studioId,
                    studioName = shoot.studioName,
                    shootEarningsPaisa = shoot.confirmedEarningsPaisa,
                    studioOutstandingPaisa = studioBalance,
                    expectedPaymentDateEpochMs = due,
                    daysOverdue = diffDays,
                    isOverdue = isOverdue
                )
            }
            .sortedWith(compareByDescending<PaymentAlertItem> { it.isOverdue }.thenByDescending { it.daysOverdue })
    }

    fun buildRecentActivity(
        shoots: List<Shoot>,
        withdrawals: List<Withdrawal>,
        expenses: List<Expense>,
        limit: Int = 15
    ): List<LedgerActivityItem> {
        val items = mutableListOf<LedgerActivityItem>()
        shoots.forEach { s ->
            val completed = s.projectStatus.equals("Completed", ignoreCase = true)
            items.add(
                LedgerActivityItem.ShootActivity(
                    id = "shoot_${s.id}",
                    title = s.eventTitle,
                    subtitle = "${s.studioName} • ${s.category} (${s.projectStatus})",
                    amountPaisa = s.confirmedEarningsPaisa,
                    dateEpochMs = s.shootDateEpochMs,
                    isCompleted = completed
                )
            )
        }
        withdrawals.forEach { w ->
            items.add(
                LedgerActivityItem.WithdrawalActivity(
                    id = "wd_${w.id}",
                    title = "Withdrawal • ${w.studioName}",
                    subtitle = "${w.paymentMethod}${if (w.referenceNumber.isNotBlank()) " • Ref: ${w.referenceNumber}" else ""}",
                    amountPaisa = w.amountPaisa,
                    dateEpochMs = w.dateEpochMs,
                    paymentMethod = w.paymentMethod
                )
            )
        }
        expenses.forEach { e ->
            items.add(
                LedgerActivityItem.ExpenseActivity(
                    id = "exp_${e.id}",
                    title = e.expenseName,
                    subtitle = "${e.category} • ${e.paymentMethod}",
                    amountPaisa = e.amountPaisa,
                    dateEpochMs = e.dateEpochMs,
                    category = e.category
                )
            )
        }
        return items.sortedByDescending { it.dateEpochMs }.take(limit)
    }

    fun resolveDateBounds(
        filter: DateRangeFilter,
        nowEpochMs: Long = System.currentTimeMillis(),
        customStartEpochMs: Long? = null,
        customEndEpochMs: Long? = null
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowEpochMs }
        return when (filter) {
            DateRangeFilter.ALL_TIME -> 0L to Long.MAX_VALUE
            DateRangeFilter.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val start = DateUtils.startOfDayMillis(cal.timeInMillis)
                start to DateUtils.endOfDayMillis(nowEpochMs)
            }
            DateRangeFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = DateUtils.startOfDayMillis(cal.timeInMillis)
                start to Long.MAX_VALUE
            }
            DateRangeFilter.LAST_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -1)
                val start = DateUtils.startOfDayMillis(cal.timeInMillis)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = DateUtils.endOfDayMillis(cal.timeInMillis)
                start to end
            }
            DateRangeFilter.LAST_3_MONTHS -> {
                cal.add(Calendar.MONTH, -3)
                DateUtils.startOfDayMillis(cal.timeInMillis) to Long.MAX_VALUE
            }
            DateRangeFilter.THIS_YEAR -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                DateUtils.startOfDayMillis(cal.timeInMillis) to Long.MAX_VALUE
            }
            DateRangeFilter.CUSTOM -> {
                val start = customStartEpochMs?.let { DateUtils.startOfDayMillis(it) } ?: 0L
                val end = customEndEpochMs?.let { DateUtils.endOfDayMillis(it) } ?: Long.MAX_VALUE
                start to end
            }
        }
    }

    fun computeAnalytics(
        studios: List<Studio>,
        shoots: List<Shoot>,
        withdrawals: List<Withdrawal>,
        expenses: List<Expense>,
        filter: DateRangeFilter,
        nowEpochMs: Long = System.currentTimeMillis(),
        customStartEpochMs: Long? = null,
        customEndEpochMs: Long? = null
    ): AnalyticsSnapshot {
        val (startMs, endMs) = resolveDateBounds(filter, nowEpochMs, customStartEpochMs, customEndEpochMs)

        val filteredShoots = shoots.filter { it.shootDateEpochMs in startMs..endMs }
        val completedShoots = filteredShoots.filter { it.projectStatus.equals("Completed", ignoreCase = true) }
        val filteredWithdrawals = withdrawals.filter { it.dateEpochMs in startMs..endMs }
        val filteredExpenses = expenses.filter { it.dateEpochMs in startMs..endMs }

        val totalEarnings = completedShoots.sumOf { max(0L, it.confirmedEarningsPaisa) }
        val totalWithdrawals = filteredWithdrawals.sumOf { max(0L, it.amountPaisa) }
        val totalExpenses = filteredExpenses.sumOf { max(0L, it.amountPaisa) }
        val netProfit = totalEarnings - totalExpenses

        val allTimeEarnings = shoots
            .filter { it.projectStatus.equals("Completed", ignoreCase = true) }
            .sumOf { max(0L, it.confirmedEarningsPaisa) }
        val allTimeWithdrawals = withdrawals.sumOf { max(0L, it.amountPaisa) }
        val outstandingBalance = allTimeEarnings - allTimeWithdrawals

        val avgEarningsPerShoot = if (completedShoots.isNotEmpty()) {
            totalEarnings / completedShoots.size
        } else {
            0L
        }

        val highestShoot = completedShoots.maxByOrNull { it.confirmedEarningsPaisa }
        val studioPerformance = computeStudioSummaries(studios, filteredShoots, filteredWithdrawals)
        val bestStudio = studioPerformance.maxByOrNull { it.totalEarningsPaisa }?.takeIf { it.totalEarningsPaisa > 0L }

        // Group by YYYY-MM for monthly series
        val monthKeys = mutableSetOf<String>()
        fun epochToYearMonth(epoch: Long): Pair<String, String> {
            val cal = Calendar.getInstance().apply { timeInMillis = epoch }
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val key = String.format("%04d-%02d", y, m)
            val label = DateUtils.formatMonthYear(epoch)
            return key to label
        }

        val monthLabels = mutableMapOf<String, String>()
        completedShoots.forEach {
            val (k, l) = epochToYearMonth(it.shootDateEpochMs)
            monthKeys.add(k)
            monthLabels[k] = l
        }
        filteredWithdrawals.forEach {
            val (k, l) = epochToYearMonth(it.dateEpochMs)
            monthKeys.add(k)
            monthLabels[k] = l
        }
        filteredExpenses.forEach {
            val (k, l) = epochToYearMonth(it.dateEpochMs)
            monthKeys.add(k)
            monthLabels[k] = l
        }

        val monthlySeries = monthKeys.sorted().map { key ->
            val mEarnings = completedShoots
                .filter { epochToYearMonth(it.shootDateEpochMs).first == key }
                .sumOf { max(0L, it.confirmedEarningsPaisa) }
            val mWithdrawals = filteredWithdrawals
                .filter { epochToYearMonth(it.dateEpochMs).first == key }
                .sumOf { max(0L, it.amountPaisa) }
            val mExpenses = filteredExpenses
                .filter { epochToYearMonth(it.dateEpochMs).first == key }
                .sumOf { max(0L, it.amountPaisa) }
            MonthlyFinancialPoint(
                monthKey = key,
                monthLabel = monthLabels[key] ?: key,
                earningsPaisa = mEarnings,
                withdrawalsPaisa = mWithdrawals,
                expensesPaisa = mExpenses,
                netProfitPaisa = mEarnings - mExpenses
            )
        }

        val bestMonth = monthlySeries.maxByOrNull { it.earningsPaisa }?.takeIf { it.earningsPaisa > 0L }

        val shootCatBreakdown = completedShoots
            .groupBy { it.category.ifBlank { "Other" } }
            .map { (cat, list) ->
                val sum = list.sumOf { max(0L, it.confirmedEarningsPaisa) }
                val pct = if (totalEarnings > 0L) ((sum.toDouble() / totalEarnings.toDouble()) * 100.0).toFloat() else 0f
                CategoryBreakdown(category = cat, amountPaisa = sum, count = list.size, sharePercentage = pct)
            }
            .sortedByDescending { it.amountPaisa }

        val expenseCatBreakdown = filteredExpenses
            .groupBy { it.category.ifBlank { "Other" } }
            .map { (cat, list) ->
                val sum = list.sumOf { max(0L, it.amountPaisa) }
                val pct = if (totalExpenses > 0L) ((sum.toDouble() / totalExpenses.toDouble()) * 100.0).toFloat() else 0f
                CategoryBreakdown(category = cat, amountPaisa = sum, count = list.size, sharePercentage = pct)
            }
            .sortedByDescending { it.amountPaisa }

        return AnalyticsSnapshot(
            filter = filter,
            totalEarningsPaisa = totalEarnings,
            totalWithdrawalsPaisa = totalWithdrawals,
            totalExpensesPaisa = totalExpenses,
            netProfitPaisa = netProfit,
            outstandingBalancePaisa = outstandingBalance,
            averageEarningsPerShootPaisa = avgEarningsPerShoot,
            highestEarningShoot = highestShoot,
            bestPerformingStudio = bestStudio,
            bestEarningMonth = bestMonth,
            monthlySeries = monthlySeries,
            studioPerformance = studioPerformance,
            shootCategoryPerformance = shootCatBreakdown,
            expenseCategoryBreakdown = expenseCatBreakdown
        )
    }
}
