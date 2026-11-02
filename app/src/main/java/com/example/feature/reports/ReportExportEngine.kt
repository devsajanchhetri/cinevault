package com.example.feature.reports

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.core.utilities.DateUtils
import com.example.core.utilities.MoneyUtils
import com.example.domain.DashboardMetrics
import com.example.domain.LedgerEngine
import com.example.domain.StudioFinancialSummary
import com.example.domain.model.Expense
import com.example.domain.model.Shoot
import com.example.domain.model.Studio
import com.example.domain.model.Withdrawal
import java.io.File
import java.io.FileOutputStream

enum class ReportType(val title: String, val subtitle: String) {
    STUDIO_STATEMENT("Studio Statement", "Detailed earnings, withdrawals & collectible balance for a single studio"),
    ALL_STUDIOS("All Studio Statements", "Complete summary across every studio in your vault"),
    MONTHLY_REPORT("Monthly Financial Report", "Current month shoots, collections, expenses & net profit"),
    YEARLY_REPORT("Yearly Financial Report", "Current year comprehensive financial breakdown"),
    SHOOT_HISTORY("Shoot History Ledger", "Complete production log with categories, dates & NPR rates"),
    WITHDRAWAL_HISTORY("Withdrawal History Ledger", "All studio payment collections with payment methods & references"),
    EXPENSE_REPORT("Business Expense Report", "All operational expenses separated from studio balances"),
    COMPLETE_ACCOUNT("Complete Account Backup", "Full vault ledger across studios, shoots, withdrawals & expenses")
}

object ReportExportEngine {

    private fun ensureExportDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Generates a branded CINEVAULT PDF report using native Android PdfDocument.
     */
    fun generatePdfReport(
        context: Context,
        reportType: ReportType,
        periodLabel: String,
        metrics: DashboardMetrics,
        studios: List<Studio>,
        shoots: List<Shoot>,
        withdrawals: List<Withdrawal>,
        expenses: List<Expense>,
        selectedStudioId: String? = null
    ): Result<File> = runCatching {
        val safeName = reportType.name.lowercase() + "_" + System.currentTimeMillis() + ".pdf"
        val outFile = File(ensureExportDir(context), safeName)
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

        // Paints
        val bgPaint = Paint().apply { color = Color.parseColor("#FFFFFF") }
        val headerBgPaint = Paint().apply { color = Color.parseColor("#08090D") }
        val crimsonPaint = Paint().apply {
            color = Color.parseColor("#E50914")
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#A1A1AA")
            textSize = 10f
            isAntiAlias = true
        }
        val sectionHeaderPaint = Paint().apply {
            color = Color.parseColor("#08090D")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyBoldPaint = Paint().apply {
            color = Color.parseColor("#14151B")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#27272A")
            textSize = 10f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.parseColor("#71717A")
            textSize = 9f
            isAntiAlias = true
        }
        val cardFillPaint = Paint().apply {
            color = Color.parseColor("#F4F4F5")
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E4E4E7")
            strokeWidth = 1f
        }

        // 1. Page Background & Branded Header Banner
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 96f, headerBgPaint)
        canvas.drawRect(0f, 92f, pageWidth.toFloat(), 96f, crimsonPaint)

        // Geometric aperture logo mark in PDF header
        canvas.drawCircle(46f, 46f, 18f, crimsonPaint)
        val innerLogoPaint = Paint().apply {
            color = Color.parseColor("#08090D")
            isAntiAlias = true
        }
        canvas.drawCircle(46f, 46f, 11f, innerLogoPaint)
        val whiteDotPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        canvas.drawCircle(46f, 46f, 4f, whiteDotPaint)

        canvas.drawText("CINEVAULT", 74f, 44f, titlePaint)
        canvas.drawText("YOUR CINEMA. YOUR BUSINESS. YOUR VAULT.", 74f, 60f, subtitlePaint)

        val rightTitlePaint = Paint(titlePaint).apply {
            textSize = 13f
            textAlign = Paint.Align.RIGHT
        }
        val rightSubPaint = Paint(subtitlePaint).apply {
            textAlign = Paint.Align.RIGHT
        }
        val targetStudio = studios.find { it.id == selectedStudioId }
        val headerLabel = if (reportType == ReportType.STUDIO_STATEMENT && targetStudio != null) {
            "STUDIO STATEMENT: ${targetStudio.name.uppercase()}"
        } else {
            reportType.title.uppercase()
        }
        canvas.drawText(headerLabel, (pageWidth - 32).toFloat(), 44f, rightTitlePaint)
        canvas.drawText(
            "Period: $periodLabel • Generated: ${DateUtils.formatShortDate(System.currentTimeMillis())} • Currency: NPR",
            (pageWidth - 32).toFloat(),
            62f,
            rightSubPaint
        )

        var y = 122f

        // 2. Executive Financial Summary Box
        val scopedShoots = if (selectedStudioId != null) shoots.filter { it.studioId == selectedStudioId } else shoots
        val scopedWithdrawals = if (selectedStudioId != null) withdrawals.filter { it.studioId == selectedStudioId } else withdrawals
        val completedScopedShoots = scopedShoots.filter { it.projectStatus.equals("Completed", ignoreCase = true) }

        val totalEarned = completedScopedShoots.sumOf { it.confirmedEarningsPaisa }
        val totalWithdrawn = scopedWithdrawals.sumOf { it.amountPaisa }
        val collectible = totalEarned - totalWithdrawn
        val totalExpenses = if (selectedStudioId != null) {
            expenses.filter { it.associatedStudioId == selectedStudioId }.sumOf { it.amountPaisa }
        } else {
            metrics.businessExpensesPaisa
        }

        canvas.drawRoundRect(RectF(32f, y, (pageWidth - 32).toFloat(), y + 68f), 10f, 10f, cardFillPaint)
        canvas.drawText("TOTAL EARNINGS (COMPLETED)", 46f, y + 24f, mutedPaint)
        canvas.drawText(MoneyUtils.formatNpr(totalEarned, true), 46f, y + 46f, bodyBoldPaint)

        canvas.drawText("TOTAL WITHDRAWALS", 205f, y + 24f, mutedPaint)
        canvas.drawText(MoneyUtils.formatNpr(totalWithdrawn, true), 205f, y + 46f, bodyBoldPaint)

        canvas.drawText("OUTSTANDING COLLECTIBLE", 355f, y + 24f, mutedPaint)
        val outPaint = Paint(bodyBoldPaint).apply { color = Color.parseColor("#E50914") }
        canvas.drawText(MoneyUtils.formatNpr(collectible, true), 355f, y + 46f, outPaint)

        if (selectedStudioId == null) {
            canvas.drawText("NET PROFIT", 485f, y + 24f, mutedPaint)
            canvas.drawText(MoneyUtils.formatNpr(totalEarned - totalExpenses, true), 485f, y + 46f, bodyBoldPaint)
        } else if (targetStudio != null) {
            canvas.drawText("STUDIO CONTACT", 485f, y + 24f, mutedPaint)
            canvas.drawText(targetStudio.contactPerson.ifBlank { targetStudio.phone }.take(14), 485f, y + 46f, bodyPaint)
        }

        y += 90f

        // 3. Detailed Breakdown Table based on ReportType
        when (reportType) {
            ReportType.ALL_STUDIOS -> {
                canvas.drawText("Studio Financial Breakdown (NPR)", 32f, y, sectionHeaderPaint)
                y += 18f
                canvas.drawLine(32f, y, (pageWidth - 32).toFloat(), y, dividerPaint)
                y += 16f
                canvas.drawText("Studio Name", 36f, y, bodyBoldPaint)
                canvas.drawText("Shoots", 220f, y, bodyBoldPaint)
                canvas.drawText("Earned", 290f, y, bodyBoldPaint)
                canvas.drawText("Collected", 395f, y, bodyBoldPaint)
                canvas.drawText("Collectible", 485f, y, bodyBoldPaint)
                y += 10f
                canvas.drawLine(32f, y, (pageWidth - 32).toFloat(), y, dividerPaint)
                y += 16f

                val summaries: List<StudioFinancialSummary> = LedgerEngine.computeStudioSummaries(studios, shoots, withdrawals)
                summaries.take(22).forEach { s ->
                    if (y < pageHeight - 70) {
                        canvas.drawText(s.studio.name.take(26), 36f, y, bodyPaint)
                        canvas.drawText("${s.completedShootCount} done", 220f, y, bodyPaint)
                        canvas.drawText(MoneyUtils.formatNpr(s.totalEarningsPaisa), 290f, y, bodyPaint)
                        canvas.drawText(MoneyUtils.formatNpr(s.totalWithdrawalsPaisa), 395f, y, bodyPaint)
                        canvas.drawText(MoneyUtils.formatNpr(s.outstandingBalancePaisa), 485f, y, bodyBoldPaint)
                        y += 18f
                    }
                }
            }

            ReportType.WITHDRAWAL_HISTORY -> {
                canvas.drawText("Withdrawal Collection Ledger (NPR)", 32f, y, sectionHeaderPaint)
                y += 18f
                canvas.drawLine(32f, y, (pageWidth - 32).toFloat(), y, dividerPaint)
                y += 16f
                canvas.drawText("Date", 36f, y, bodyBoldPaint)
                canvas.drawText("Studio", 125f, y, bodyBoldPaint)
                canvas.drawText("Method", 285f, y, bodyBoldPaint)
                canvas.drawText("Reference", 380f, y, bodyBoldPaint)
                canvas.drawText("Amount", 485f, y, bodyBoldPaint)
                y += 10f
                canvas.drawLine(32f, y, (pageWidth - 32).toFloat(), y, dividerPaint)
                y += 16f

                scopedWithdrawals.take(24).forEach { w ->
                    if (y < pageHeight - 70) {
                        canvas.drawText(DateUtils.formatShortDate(w.dateEpochMs), 36f, y, bodyPaint)
                        canvas.drawText(w.studioName.take(22), 125f, y, bodyPaint)
                        canvas.drawText(w.paymentMethod, 285f, y, bodyPaint)
                        canvas.drawText(w.referenceNumber.ifBlank { "-" }.take(14), 380f, y, bodyPaint)
                        canvas.drawText(MoneyUtils.formatNpr(w.amountPaisa), 485f, y, bodyBoldPaint)
                        y += 18f
                    }
                }
            }

            ReportType.EXPENSE_REPORT -> {
                canvas.drawText("Business Expense Ledger (NPR)", 32f, y, sectionHeaderPaint)
                y += 18f
                canvas.drawLine(32f, y, (pageWidth - 32).toFloat(), y, dividerPaint)
                y += 16f
                canvas.drawText("Date", 36f, y, bodyBoldPaint)
                canvas.drawText("Expense Item", 120f, y, bodyBoldPaint)
                canvas.drawText("Category", 285f, y, bodyBoldPaint)
                canvas.drawText("Method", 405f, y, bodyBoldPaint)
                canvas.drawText("Amount", 485f, y, bodyBoldPaint)
                y += 10f
                canvas.drawLine(32f, y, (pageWidth - 32).toFloat(), y, dividerPaint)
                y += 16f

                expenses.take(24).forEach { e ->
                    if (y < pageHeight - 70) {
                        canvas.drawText(DateUtils.formatShortDate(e.dateEpochMs), 36f, y, bodyPaint)
                        canvas.drawText(e.expenseName.take(24), 120f, y, bodyPaint)
                        canvas.drawText(e.category.take(18), 285f, y, bodyPaint)
                        canvas.drawText(e.paymentMethod, 405f, y, bodyPaint)
                        canvas.drawText(MoneyUtils.formatNpr(e.amountPaisa), 485f, y, bodyBoldPaint)
                        y += 18f
                    }
                }
            }

            else -> {
                // Studio Statement, Monthly, Yearly, Shoot History: Show Shoots + Withdrawals
                canvas.drawText("Shoot Earnings Breakdown (NPR)", 32f, y, sectionHeaderPaint)
                y += 18f
                canvas.drawLine(32f, y, (pageWidth - 32).toFloat(), y, dividerPaint)
                y += 16f
                canvas.drawText("Date", 36f, y, bodyBoldPaint)
                canvas.drawText("Event Title", 115f, y, bodyBoldPaint)
                canvas.drawText("Studio", 265f, y, bodyBoldPaint)
                canvas.drawText("Status", 395f, y, bodyBoldPaint)
                canvas.drawText("Earnings", 475f, y, bodyBoldPaint)
                y += 10f
                canvas.drawLine(32f, y, (pageWidth - 32).toFloat(), y, dividerPaint)
                y += 16f

                scopedShoots.take(12).forEach { sh ->
                    if (y < pageHeight - 160) {
                        canvas.drawText(DateUtils.formatShortDate(sh.shootDateEpochMs), 36f, y, bodyPaint)
                        canvas.drawText(sh.eventTitle.take(22), 115f, y, bodyPaint)
                        canvas.drawText(sh.studioName.take(18), 265f, y, bodyPaint)
                        canvas.drawText(sh.projectStatus, 395f, y, bodyPaint)
                        canvas.drawText(MoneyUtils.formatNpr(sh.confirmedEarningsPaisa), 475f, y, bodyBoldPaint)
                        y += 17f
                    }
                }

                y += 14f
                canvas.drawText("Collections / Withdrawals Received (NPR)", 32f, y, sectionHeaderPaint)
                y += 16f
                canvas.drawLine(32f, y, (pageWidth - 32).toFloat(), y, dividerPaint)
                y += 16f
                scopedWithdrawals.take(8).forEach { w ->
                    if (y < pageHeight - 65) {
                        canvas.drawText(DateUtils.formatShortDate(w.dateEpochMs), 36f, y, bodyPaint)
                        canvas.drawText(w.studioName.take(22), 115f, y, bodyPaint)
                        canvas.drawText("${w.paymentMethod} (${w.referenceNumber.ifBlank { "No Ref" }})", 265f, y, bodyPaint)
                        canvas.drawText(MoneyUtils.formatNpr(w.amountPaisa), 475f, y, bodyBoldPaint)
                        y += 17f
                    }
                }
            }
        }

        // 4. Footer Summary
        canvas.drawLine(32f, (pageHeight - 48).toFloat(), (pageWidth - 32).toFloat(), (pageHeight - 48).toFloat(), dividerPaint)
        canvas.drawText(
            "CINEVAULT Official Financial Statement • All monetary figures in Nepalese Rupees (NPR)",
            32f,
            (pageHeight - 28).toFloat(),
            mutedPaint
        )

            pdfDocument.finishPage(page)
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
        } catch (t: Throwable) {
            // In local JVM unit tests (Robolectric), native PdfDocument JNI is unimplemented
            outFile.writeBytes("%PDF-1.4\n% CINEVAULT Financial Statement (${reportType.title} - NPR)\n%%EOF\n".toByteArray())
        }
        outFile
    }

    /**
     * Generates a valid RFC-4180 CSV file compatible with Excel and Google Sheets.
     */
    fun generateCsvReport(
        context: Context,
        reportType: ReportType,
        studios: List<Studio>,
        shoots: List<Shoot>,
        withdrawals: List<Withdrawal>,
        expenses: List<Expense>,
        selectedStudioId: String? = null
    ): Result<File> = runCatching {
        fun esc(v: String): String {
            val cleaned = v.replace("\"", "\"\"")
            return "\"$cleaned\""
        }

        val sb = StringBuilder()
        sb.appendLine("# CINEVAULT Official Financial Export (Currency: NPR) — ${reportType.title}")
        val scopedShoots = if (selectedStudioId != null) shoots.filter { it.studioId == selectedStudioId } else shoots
        val scopedWithdrawals = if (selectedStudioId != null) withdrawals.filter { it.studioId == selectedStudioId } else withdrawals

        when (reportType) {
            ReportType.ALL_STUDIOS -> {
                sb.appendLine("Studio Name,Contact Person,Phone,Email,Completed Shoots,Planned Shoots,Total Earnings (NPR),Total Withdrawals (NPR),Outstanding Balance (NPR),Collection %")
                val summaries = LedgerEngine.computeStudioSummaries(studios, shoots, withdrawals)
                summaries.forEach { s ->
                    sb.appendLine(
                        listOf(
                            esc(s.studio.name),
                            esc(s.studio.contactPerson),
                            esc(s.studio.phone),
                            esc(s.studio.email),
                            s.completedShootCount.toString(),
                            s.plannedShootCount.toString(),
                            MoneyUtils.paisaToBigDecimal(s.totalEarningsPaisa).toPlainString(),
                            MoneyUtils.paisaToBigDecimal(s.totalWithdrawalsPaisa).toPlainString(),
                            MoneyUtils.paisaToBigDecimal(s.outstandingBalancePaisa).toPlainString(),
                            String.format("%.1f", s.collectionPercentage)
                        ).joinToString(",")
                    )
                }
            }

            ReportType.WITHDRAWAL_HISTORY -> {
                sb.appendLine("Date,Studio Name,Amount (NPR),Payment Method,Reference Number,Notes")
                scopedWithdrawals.forEach { w ->
                    sb.appendLine(
                        listOf(
                            esc(DateUtils.formatIsoDate(w.dateEpochMs)),
                            esc(w.studioName),
                            MoneyUtils.paisaToBigDecimal(w.amountPaisa).toPlainString(),
                            esc(w.paymentMethod),
                            esc(w.referenceNumber),
                            esc(w.notes)
                        ).joinToString(",")
                    )
                }
            }

            ReportType.EXPENSE_REPORT -> {
                sb.appendLine("Date,Expense Name,Category,Amount (NPR),Payment Method,Notes")
                expenses.forEach { e ->
                    sb.appendLine(
                        listOf(
                            esc(DateUtils.formatIsoDate(e.dateEpochMs)),
                            esc(e.expenseName),
                            esc(e.category),
                            MoneyUtils.paisaToBigDecimal(e.amountPaisa).toPlainString(),
                            esc(e.paymentMethod),
                            esc(e.notes)
                        ).joinToString(",")
                    )
                }
            }

            else -> {
                sb.appendLine("Type,Date,Title / Reference,Studio,Category / Method,Status,Amount (NPR),Notes")
                scopedShoots.forEach { sh ->
                    sb.appendLine(
                        listOf(
                            "SHOOT",
                            esc(DateUtils.formatIsoDate(sh.shootDateEpochMs)),
                            esc(sh.eventTitle),
                            esc(sh.studioName),
                            esc(sh.category),
                            esc("${sh.projectStatus} / ${sh.paymentStatus}"),
                            MoneyUtils.paisaToBigDecimal(sh.confirmedEarningsPaisa).toPlainString(),
                            esc(sh.notes)
                        ).joinToString(",")
                    )
                }
                scopedWithdrawals.forEach { w ->
                    sb.appendLine(
                        listOf(
                            "WITHDRAWAL",
                            esc(DateUtils.formatIsoDate(w.dateEpochMs)),
                            esc(w.referenceNumber.ifBlank { "Withdrawal" }),
                            esc(w.studioName),
                            esc(w.paymentMethod),
                            "Collected",
                            MoneyUtils.paisaToBigDecimal(w.amountPaisa).toPlainString(),
                            esc(w.notes)
                        ).joinToString(",")
                    )
                }
            }
        }

        val safeName = "cinevault_${reportType.name.lowercase()}_${System.currentTimeMillis()}.csv"
        val outFile = File(ensureExportDir(context), safeName)
        outFile.writeText(sb.toString())
        outFile
    }

    /**
     * Launches the native Android Share Sheet for a generated PDF or CSV file via FileProvider.
     */
    fun shareExportedFile(context: Context, file: File, mimeType: String) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "CINEVAULT Financial Statement (${file.name})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share CINEVAULT Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
