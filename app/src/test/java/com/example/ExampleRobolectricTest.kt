package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.domain.DashboardMetrics
import com.example.domain.model.Expense
import com.example.domain.model.Shoot
import com.example.domain.model.Studio
import com.example.domain.model.Withdrawal
import com.example.feature.CineVaultUiState
import com.example.feature.dashboard.DashboardScreen
import com.example.feature.reports.ReportExportEngine
import com.example.feature.reports.ReportType
import com.example.ui.theme.CineVaultTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appName_isCineVault() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("CINEVAULT", context.getString(R.string.app_name))
    }

    @Test
    fun dashboardScreen_rendersStrictZeroStateInNpr() {
        composeTestRule.setContent {
            CineVaultTheme {
                DashboardScreen(
                    uiState = CineVaultUiState(
                        isLoading = false,
                        metrics = DashboardMetrics()
                    ),
                    onQuickAddShoot = {},
                    onQuickAddStudio = {},
                    onQuickRecordWithdrawal = {},
                    onQuickAddExpense = {},
                    onOpenStudioDetail = {},
                    onOpenWithdrawalsLedger = {},
                    onOpenExpensesLedger = {},
                    onOpenCalendar = {},
                    onOpenReports = {},
                    onOpenGlobalSearch = {},
                    onOpenSettings = {},
                    onGenerateAiInsight = {},
                    onDraftPaymentReminder = { _, _ -> }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("CINEVAULT").assertExists()
        composeTestRule.onNodeWithText("AVAILABLE TO COLLECT").assertExists()
        composeTestRule.onNodeWithText("NPR LEDGER").assertExists()
    }

    @Test
    fun reportExportEngine_generatesValidPdfAndCsvFiles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val studio = Studio(id = "st_1", name = "Kathmandu Cinema Works", contactPerson = "Aarav")
        val shoot = Shoot(
            id = "sh_1",
            eventTitle = "Himalayan Documentary",
            studioId = "st_1",
            studioName = studio.name,
            category = "Documentary",
            confirmedEarningsPaisa = 60_000_00L,
            projectStatus = "Completed"
        )
        val withdrawal = Withdrawal(
            id = "wd_1",
            studioId = "st_1",
            studioName = studio.name,
            amountPaisa = 25_000_00L,
            paymentMethod = "Bank Transfer",
            referenceNumber = "NABIL-001"
        )
        val expense = Expense(
            id = "ex_1",
            expenseName = "Drone Battery Rental",
            category = "Drone Equipment",
            amountPaisa = 4_000_00L,
            paymentMethod = "eSewa"
        )

        val pdfResult = ReportExportEngine.generatePdfReport(
            context = context,
            reportType = ReportType.STUDIO_STATEMENT,
            periodLabel = "All Time",
            metrics = DashboardMetrics(
                availableToCollectPaisa = 35_000_00L,
                lifetimeEarningsPaisa = 60_000_00L,
                totalWithdrawalsPaisa = 25_000_00L,
                businessExpensesPaisa = 4_000_00L,
                netBusinessProfitPaisa = 56_000_00L
            ),
            studios = listOf(studio),
            shoots = listOf(shoot),
            withdrawals = listOf(withdrawal),
            expenses = listOf(expense),
            selectedStudioId = "st_1"
        )
        assertTrue(pdfResult.isSuccess)
        val pdfFile = pdfResult.getOrNull()!!
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.name.endsWith(".pdf"))

        val csvResult = ReportExportEngine.generateCsvReport(
            context = context,
            reportType = ReportType.COMPLETE_ACCOUNT,
            studios = listOf(studio),
            shoots = listOf(shoot),
            withdrawals = listOf(withdrawal),
            expenses = listOf(expense)
        )
        assertTrue(csvResult.isSuccess)
        val csvFile = csvResult.getOrNull()!!
        assertTrue(csvFile.exists())
        val content = csvFile.readText()
        assertTrue(content.contains("CINEVAULT"))
        assertTrue(content.contains("Kathmandu Cinema Works"))
        assertTrue(content.contains("Bank Transfer"))
    }
}
