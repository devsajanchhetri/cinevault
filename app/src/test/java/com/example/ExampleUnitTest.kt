package com.example

import com.example.core.utilities.MoneyUtils
import com.example.domain.DateRangeFilter
import com.example.domain.LedgerEngine
import com.example.domain.model.Expense
import com.example.domain.model.Shoot
import com.example.domain.model.Studio
import com.example.domain.model.Withdrawal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun moneyUtils_nprParsingAndFormatting_handlesPaisaAccurately() {
        assertEquals(4500000L, MoneyUtils.parseNprToPaisa("45,000"))
        assertEquals(12550L, MoneyUtils.parseNprToPaisa("NPR 125.50"))
        assertEquals(0L, MoneyUtils.parseNprToPaisa("0"))
        assertNull(MoneyUtils.parseNprToPaisa("-100"))
        assertNull(MoneyUtils.parseNprToPaisa("abc"))

        assertEquals("NPR 45,000", MoneyUtils.formatNpr(4500000L))
        assertEquals("NPR 125.50", MoneyUtils.formatNpr(12550L))
        assertEquals("NPR 0", MoneyUtils.formatNpr(0L))
    }

    @Test
    fun ledgerEngine_strictZeroState_returnsZeroAcrossAllMetrics() {
        val metrics = LedgerEngine.computeDashboardMetrics(
            studios = emptyList(),
            shoots = emptyList(),
            withdrawals = emptyList(),
            expenses = emptyList()
        )
        assertEquals(0L, metrics.availableToCollectPaisa)
        assertEquals(0L, metrics.lifetimeEarningsPaisa)
        assertEquals(0L, metrics.totalWithdrawalsPaisa)
        assertEquals(0L, metrics.businessExpensesPaisa)
        assertEquals(0L, metrics.netBusinessProfitPaisa)
        assertEquals(0L, metrics.plannedPipelinePaisa)
        assertEquals(0, metrics.shootCount)
        assertEquals(0, metrics.studioCount)
    }

    @Test
    fun ledgerEngine_completedVsPlannedShootsAndSeparateExpenses_computesExactBalances() {
        val studioA = Studio(id = "st_1", name = "Himalayan Cinema Studio", archived = false)
        val studioB = Studio(id = "st_2", name = "Everest Frames", archived = false)

        val shoots = listOf(
            Shoot(
                id = "sh_1",
                studioId = "st_1",
                studioName = studioA.name,
                eventTitle = "Kathmandu Wedding Film",
                category = "Wedding",
                confirmedEarningsPaisa = 50_000_00L, // NPR 50,000 Completed
                projectStatus = "Completed",
                shootDateEpochMs = 1710000000000L
            ),
            Shoot(
                id = "sh_2",
                studioId = "st_1",
                studioName = studioA.name,
                eventTitle = "Upcoming Commercial Ad",
                category = "Commercial",
                confirmedEarningsPaisa = 30_000_00L, // NPR 30,000 Planned (must NOT enter collectible ledger)
                projectStatus = "Planned",
                shootDateEpochMs = 1715000000000L
            ),
            Shoot(
                id = "sh_3",
                studioId = "st_2",
                studioName = studioB.name,
                eventTitle = "Music Video DOP",
                category = "Music Video",
                confirmedEarningsPaisa = 25_000_00L, // NPR 25,000 Completed
                projectStatus = "Completed",
                shootDateEpochMs = 1712000000000L
            )
        )

        val withdrawals = listOf(
            Withdrawal(
                id = "wd_1",
                studioId = "st_1",
                studioName = studioA.name,
                amountPaisa = 20_000_00L, // NPR 20,000 via eSewa
                paymentMethod = "eSewa",
                dateEpochMs = 1711000000000L
            )
        )

        val expenses = listOf(
            Expense(
                id = "ex_1",
                expenseName = "GMaster Lens Rental",
                category = "Lens Rental",
                amountPaisa = 5_000_00L, // NPR 5,000 via Khalti
                paymentMethod = "Khalti",
                dateEpochMs = 1711500000000L
            )
        )

        val metrics = LedgerEngine.computeDashboardMetrics(
            studios = listOf(studioA, studioB),
            shoots = shoots,
            withdrawals = withdrawals,
            expenses = expenses
        )

        // Completed earnings = 50,000 + 25,000 = 75,000 NPR
        assertEquals(75_000_00L, metrics.lifetimeEarningsPaisa)
        // Planned pipeline = 30,000 NPR
        assertEquals(30_000_00L, metrics.plannedPipelinePaisa)
        // Total withdrawn = 20,000 NPR
        assertEquals(20_000_00L, metrics.totalWithdrawalsPaisa)
        // Available to Collect = Completed Shoot Earnings (75,000) - Studio Withdrawals (20,000) = 55,000 NPR
        // Business expenses (5,000) do NOT reduce studio collectible balance!
        assertEquals(55_000_00L, metrics.availableToCollectPaisa)
        // Business Expenses = 5,000 NPR
        assertEquals(5_000_00L, metrics.businessExpensesPaisa)
        // Net Business Profit = Completed Shoot Earnings (75,000) - Business Expenses (5,000) = 70,000 NPR
        assertEquals(70_000_00L, metrics.netBusinessProfitPaisa)

        // Check per-studio summaries
        val summaries = LedgerEngine.computeStudioSummaries(
            studios = listOf(studioA, studioB),
            shoots = shoots,
            withdrawals = withdrawals
        )
        val summaryA = summaries.first { it.studio.id == "st_1" }
        assertEquals(50_000_00L, summaryA.totalEarningsPaisa)
        assertEquals(1, summaryA.plannedShootCount)
        assertEquals(20_000_00L, summaryA.totalWithdrawalsPaisa)
        assertEquals(30_000_00L, summaryA.outstandingBalancePaisa)
        assertEquals(40f, summaryA.collectionPercentage, 0.01f)
    }

    @Test
    fun ledgerEngine_detectsScheduleConflictsAndAnalytics() {
        val studio = Studio(id = "st_1", name = "Cine Studio")
        val sameDayEpoch = 1712000000000L
        val shoots = listOf(
            Shoot(
                id = "s1",
                studioId = "st_1",
                studioName = studio.name,
                eventTitle = "Morning Reception Shoot",
                category = "Wedding",
                shootDateEpochMs = sameDayEpoch,
                confirmedEarningsPaisa = 40_000_00L,
                projectStatus = "Completed"
            ),
            Shoot(
                id = "s2",
                studioId = "st_1",
                studioName = studio.name,
                eventTitle = "Corporate Event Coverage",
                category = "Event",
                shootDateEpochMs = sameDayEpoch,
                confirmedEarningsPaisa = 25_000_00L,
                projectStatus = "Planned"
            )
        )

        val conflicts = LedgerEngine.detectScheduleConflicts(shoots)
        assertEquals(1, conflicts.size)
        assertEquals(2, conflicts.first().shoots.size)

        val analytics = LedgerEngine.computeAnalytics(
            studios = listOf(studio),
            shoots = shoots,
            withdrawals = emptyList(),
            expenses = emptyList(),
            filter = DateRangeFilter.ALL_TIME
        )
        assertNotNull(analytics.highestEarningShoot)
        assertEquals("Morning Reception Shoot", analytics.highestEarningShoot?.eventTitle)
        assertTrue(analytics.shootCategoryPerformance.isNotEmpty())
    }
}
