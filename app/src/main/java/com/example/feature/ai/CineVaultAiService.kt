package com.example.feature.ai

import android.util.Log
import com.example.core.utilities.DateUtils
import com.example.core.utilities.MoneyUtils
import com.example.domain.DashboardMetrics
import com.example.domain.StudioFinancialSummary
import com.example.domain.model.CineVaultConstants
import com.example.domain.model.Expense
import com.example.domain.model.Shoot
import com.example.domain.model.Withdrawal
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "CineVaultAiService"

data class ParsedNaturalLanguageShoot(
    val eventTitle: String,
    val studioName: String,
    val shootDateIso: String,
    val location: String,
    val category: String,
    val durationValue: Long,
    val durationUnit: String,
    val confirmedEarningsNpr: Double,
    val projectStatus: String,
    val notes: String
)

class CineVaultAiService {

    private val chatModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val nlShootSchema = Schema.obj(
        properties = mapOf(
            "eventTitle" to Schema.string(description = "Title or name of the cinematography shoot"),
            "studioName" to Schema.string(description = "Name of the client studio or production house"),
            "shootDateIso" to Schema.string(description = "Shoot date in YYYY-MM-DD format"),
            "location" to Schema.string(description = "City or venue of the shoot"),
            "category" to Schema.enumeration(
                values = CineVaultConstants.SHOOT_CATEGORIES,
                description = "Shoot category"
            ),
            "durationValue" to Schema.integer(description = "Duration numeric value (e.g. 1, 2)"),
            "durationUnit" to Schema.enumeration(
                values = CineVaultConstants.DURATION_UNITS,
                description = "Duration unit: Hours or Days"
            ),
            "confirmedEarningsNpr" to Schema.double(description = "Confirmed shoot earnings in Nepalese Rupees (NPR)"),
            "projectStatus" to Schema.enumeration(
                values = CineVaultConstants.PROJECT_STATUSES,
                description = "Completed if already shot/done, Planned if upcoming"
            ),
            "notes" to Schema.string(description = "Additional extracted details")
        )
    )

    private val structuredShootModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = nlShootSchema
            }
        )
    }

    /**
     * Builds a strict, factual context summary from the authenticated user's real Firestore records in NPR.
     */
    fun buildFinancialContextPrompt(
        metrics: DashboardMetrics,
        studios: List<StudioFinancialSummary>,
        shoots: List<Shoot>,
        withdrawals: List<Withdrawal>,
        expenses: List<Expense>
    ): String {
        val todayIso = DateUtils.formatIsoDate(System.currentTimeMillis())
        val studioLines = if (studios.isEmpty()) {
            "No studios recorded yet."
        } else {
            studios.joinToString("\n") { s ->
                "- ${s.studio.name}: Earned ${MoneyUtils.formatNpr(s.totalEarningsPaisa)}, Collected ${MoneyUtils.formatNpr(s.totalWithdrawalsPaisa)}, Owed/Collectible ${MoneyUtils.formatNpr(s.outstandingBalancePaisa)} (${s.completedShootCount} completed shoots, ${s.plannedShootCount} planned)"
            }
        }

        val recentShoots = if (shoots.isEmpty()) {
            "No shoots recorded yet."
        } else {
            shoots.take(15).joinToString("\n") { sh ->
                "- ${sh.eventTitle} | Studio: ${sh.studioName} | Category: ${sh.category} | Date: ${DateUtils.formatIsoDate(sh.shootDateEpochMs)} | Location: ${sh.location.ifBlank { "N/A" }} | Rate: ${MoneyUtils.formatNpr(sh.confirmedEarningsPaisa)} | Status: ${sh.projectStatus} (${sh.paymentStatus})"
            }
        }

        val recentWithdrawals = if (withdrawals.isEmpty()) {
            "No withdrawals recorded yet."
        } else {
            withdrawals.take(10).joinToString("\n") { w ->
                "- ${DateUtils.formatIsoDate(w.dateEpochMs)}: Collected ${MoneyUtils.formatNpr(w.amountPaisa)} from ${w.studioName} via ${w.paymentMethod} (Ref: ${w.referenceNumber.ifBlank { "None" }})"
            }
        }

        val recentExpenses = if (expenses.isEmpty()) {
            "No business expenses recorded yet."
        } else {
            expenses.take(10).joinToString("\n") { e ->
                "- ${DateUtils.formatIsoDate(e.dateEpochMs)}: ${e.expenseName} (${e.category}) = ${MoneyUtils.formatNpr(e.amountPaisa)} via ${e.paymentMethod}"
            }
        }

        return """
            You are CINEVAULT AI, an executive financial & cinematography business assistant for a professional filmmaker in Nepal.
            Today's date: $todayIso
            Currency: Exclusively Nepalese Rupee (NPR).
            Supported payment methods: Bank Transfer, eSewa, Cash, Khalti, Other.
            CRITICAL RULES:
            1. Never invent, hallucinate, or fabricate any studio, shoot, withdrawal, expense, or balance.
            2. Base every number strictly on the user's real CINEVAULT ledger data below.
            3. Keep answers concise, executive, well-structured, and actionable.

            === LIVE CINEVAULT LEDGER SNAPSHOT (NPR) ===
            - Available to Collect (Total Outstanding): ${MoneyUtils.formatNpr(metrics.availableToCollectPaisa)}
            - Gross Lifetime Shoot Earnings (Completed): ${MoneyUtils.formatNpr(metrics.lifetimeEarningsPaisa)}
            - Total Withdrawals Collected: ${MoneyUtils.formatNpr(metrics.totalWithdrawalsPaisa)}
            - Total Business Expenses: ${MoneyUtils.formatNpr(metrics.businessExpensesPaisa)}
            - Net Business Profit (Gross Earnings - Business Expenses): ${MoneyUtils.formatNpr(metrics.netBusinessProfitPaisa)}
            - Active Studios: ${metrics.studioCount}
            - Completed Shoots: ${metrics.completedShootCount} | Planned Shoots: ${metrics.plannedShootCount} (Planned Pipeline: ${MoneyUtils.formatNpr(metrics.plannedPipelinePaisa)})

            === STUDIOS & COLLECTIBLE BALANCES ===
            $studioLines

            === RECENT SHOOTS ===
            $recentShoots

            === RECENT WITHDRAWALS ===
            $recentWithdrawals

            === RECENT BUSINESS EXPENSES ===
            $recentExpenses
        """.trimIndent()
    }

    fun streamAssistantResponse(
        userPrompt: String,
        financialContext: String
    ): Flow<String> {
        val fullPrompt = "$financialContext\n\nUSER REQUEST:\n$userPrompt"
        return chatModel.generateContentStream(fullPrompt).map { chunk ->
            chunk.text.orEmpty()
        }
    }

    suspend fun generateQuickInsight(financialContext: String): Result<String> {
        return try {
            val response = withTimeout(60_000L) {
                chatModel.generateContent(
                    "$financialContext\n\nGenerate a 2-sentence executive financial insight summarizing collectible receivables, top-earning studio or category, and one actionable recommendation in NPR."
                )
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("AI returned an empty insight response."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Quick insight timed out", e)
            Result.failure(IllegalStateException("AI insight generation timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Quick insight failed", e)
            Result.failure(e)
        }
    }

    /**
     * Parses a natural language shoot entry into a structured review object before saving.
     * Example: "I completed a wedding shoot for ABC Studio in Kathmandu today for NPR 45,000."
     */
    suspend fun parseNaturalLanguageShoot(
        naturalText: String,
        existingStudioNames: List<String>
    ): Result<ParsedNaturalLanguageShoot> {
        val todayIso = DateUtils.formatIsoDate(System.currentTimeMillis())
        val prompt = """
            Extract the cinematography shoot details from the user's statement into the required JSON schema.
            Today's date is $todayIso.
            Known studio names in user's vault: ${existingStudioNames.joinToString(", ").ifBlank { "None yet" }}.
            If the user says "today", use "$todayIso".
            All monetary values are in Nepalese Rupees (NPR).
            User statement: "$naturalText"
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                structuredShootModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("AI returned empty JSON.")
            val obj = JSONObject(rawJson)
            val parsed = ParsedNaturalLanguageShoot(
                eventTitle = obj.optString("eventTitle", "Cinematography Shoot").ifBlank { "Cinematography Shoot" },
                studioName = obj.optString("studioName", "").trim(),
                shootDateIso = obj.optString("shootDateIso", todayIso),
                location = obj.optString("location", ""),
                category = obj.optString("category", "Wedding").takeIf { it in CineVaultConstants.SHOOT_CATEGORIES } ?: "Wedding",
                durationValue = obj.optLong("durationValue", 1L).coerceIn(1L, 365L),
                durationUnit = obj.optString("durationUnit", "Days").takeIf { it in CineVaultConstants.DURATION_UNITS } ?: "Days",
                confirmedEarningsNpr = obj.optDouble("confirmedEarningsNpr", 0.0).coerceAtLeast(0.0),
                projectStatus = obj.optString("projectStatus", "Completed").takeIf { it in CineVaultConstants.PROJECT_STATUSES } ?: "Completed",
                notes = obj.optString("notes", "")
            )
            Result.success(parsed)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "NL shoot parsing timed out", e)
            Result.failure(IllegalStateException("Natural language parsing timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "NL shoot parsing failed", e)
            Result.failure(e)
        }
    }
}
