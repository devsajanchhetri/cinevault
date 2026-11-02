package com.example.feature

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.utilities.DateUtils
import com.example.core.utilities.MoneyUtils
import com.example.data.repository.CineVaultRepository
import com.example.domain.AnalyticsSnapshot
import com.example.domain.DashboardMetrics
import com.example.domain.DateRangeFilter
import com.example.domain.LedgerActivityItem
import com.example.domain.LedgerEngine
import com.example.domain.PaymentAlertItem
import com.example.domain.ScheduleConflict
import com.example.domain.StudioFinancialSummary
import com.example.domain.model.Expense
import com.example.domain.model.Shoot
import com.example.domain.model.Studio
import com.example.domain.model.UserProfile
import com.example.domain.model.UserSettings
import com.example.domain.model.VaultNotification
import com.example.domain.model.Withdrawal
import com.example.feature.ai.CineVaultAiService
import com.example.feature.ai.ParsedNaturalLanguageShoot
import com.example.feature.reports.ReportExportEngine
import com.example.feature.reports.ReportType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "CineVaultVM"

data class AiChatMessage(
    val id: String,
    val role: String, // "user" or "assistant"
    val content: String,
    val isStreaming: Boolean = false
)

data class CineVaultUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val settings: UserSettings = UserSettings(),
    val studios: List<Studio> = emptyList(),
    val shoots: List<Shoot> = emptyList(),
    val withdrawals: List<Withdrawal> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val notifications: List<VaultNotification> = emptyList(),
    val metrics: DashboardMetrics = DashboardMetrics(),
    val studioSummaries: List<StudioFinancialSummary> = emptyList(),
    val recentActivity: List<LedgerActivityItem> = emptyList(),
    val paymentAlerts: List<PaymentAlertItem> = emptyList(),
    val scheduleConflicts: List<ScheduleConflict> = emptyList(),
    val analyticsFilter: DateRangeFilter = DateRangeFilter.ALL_TIME,
    val customStartDateMs: Long? = null,
    val customEndDateMs: Long? = null,
    val analytics: AnalyticsSnapshot? = null,
    val dashboardAiInsight: String? = null,
    val isDashboardAiLoading: Boolean = false,
    val aiMessages: List<AiChatMessage> = emptyList(),
    val isAiBusy: Boolean = false,
    val parsedShootDraft: ParsedNaturalLanguageShoot? = null,
    val statusBannerMessage: String? = null,
    val errorBannerMessage: String? = null
)

class CineVaultViewModel(
    private val repository: CineVaultRepository,
    private val currentUserId: String,
    private val initialDisplayName: String = "Cinematographer",
    private val initialEmail: String = "",
    private val initialPhotoUrl: String = ""
) : ViewModel() {

    private val aiService = CineVaultAiService()

    private val _analyticsFilter = MutableStateFlow(DateRangeFilter.ALL_TIME)
    private val _customDateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)
    private val _dashboardAiInsight = MutableStateFlow<String?>(null)
    private val _isDashboardAiLoading = MutableStateFlow(false)
    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    private val _isAiBusy = MutableStateFlow(false)
    private val _parsedShootDraft = MutableStateFlow<ParsedNaturalLanguageShoot?>(null)
    private val _statusBanner = MutableStateFlow<String?>(null)
    private val _errorBanner = MutableStateFlow<String?>(null)

    private data class CoreStreams(
        val profile: UserProfile?,
        val settings: UserSettings,
        val studios: List<Studio>,
        val shoots: List<Shoot>,
        val withdrawals: List<Withdrawal>,
        val expenses: List<Expense>,
        val notifications: List<VaultNotification>
    )

    private val coreStreamsFlow = combine(
        repository.observeUserProfile(currentUserId).catch { e ->
            Log.w(TAG, "Error observing profile", e)
            emit(null)
        },
        repository.observeUserSettings(currentUserId).catch { e ->
            Log.w(TAG, "Error observing settings", e)
            emit(UserSettings(userId = currentUserId))
        },
        repository.observeStudios(currentUserId).catch { e ->
            Log.w(TAG, "Error observing studios", e)
            _errorBanner.value = e.localizedMessage ?: "Failed to load studios"
            emit(emptyList())
        },
        repository.observeShoots(currentUserId).catch { e ->
            Log.w(TAG, "Error observing shoots", e)
            _errorBanner.value = e.localizedMessage ?: "Failed to load shoots"
            emit(emptyList())
        },
        repository.observeWithdrawals(currentUserId).catch { e ->
            Log.w(TAG, "Error observing withdrawals", e)
            _errorBanner.value = e.localizedMessage ?: "Failed to load withdrawals"
            emit(emptyList())
        }
    ) { profile, settings, studios, shoots, withdrawals ->
        CoreStreams(
            profile = profile,
            settings = settings,
            studios = studios,
            shoots = shoots,
            withdrawals = withdrawals,
            expenses = emptyList(),
            notifications = emptyList()
        )
    }.combine(
        repository.observeExpenses(currentUserId).catch { e ->
            Log.w(TAG, "Error observing expenses", e)
            _errorBanner.value = e.localizedMessage ?: "Failed to load expenses"
            emit(emptyList())
        }
    ) { partial, expenses ->
        partial.copy(expenses = expenses)
    }.combine(
        repository.observeNotifications(currentUserId).catch { e ->
            Log.w(TAG, "Error observing notifications", e)
            emit(emptyList())
        }
    ) { partial, notifications ->
        partial.copy(notifications = notifications)
    }

    private data class UiOverlayState(
        val filter: DateRangeFilter,
        val customRange: Pair<Long?, Long?>,
        val aiInsight: String?,
        val isInsightLoading: Boolean,
        val aiMessages: List<AiChatMessage>,
        val isAiBusy: Boolean,
        val parsedDraft: ParsedNaturalLanguageShoot?,
        val statusMsg: String?,
        val errorMsg: String?
    )

    private val overlayFlow = combine(
        _analyticsFilter,
        _customDateRange,
        _dashboardAiInsight,
        _isDashboardAiLoading,
        _aiMessages
    ) { filter, customRange, insight, insightLoading, msgs ->
        UiOverlayState(
            filter = filter,
            customRange = customRange,
            aiInsight = insight,
            isInsightLoading = insightLoading,
            aiMessages = msgs,
            isAiBusy = false,
            parsedDraft = null,
            statusMsg = null,
            errorMsg = null
        )
    }.combine(_isAiBusy) { s, busy -> s.copy(isAiBusy = busy) }
        .combine(_parsedShootDraft) { s, draft -> s.copy(parsedDraft = draft) }
        .combine(_statusBanner) { s, status -> s.copy(statusMsg = status) }
        .combine(_errorBanner) { s, err -> s.copy(errorMsg = err) }

    val uiState: StateFlow<CineVaultUiState> = combine(
        coreStreamsFlow,
        overlayFlow
    ) { core, overlay ->
        val metrics = LedgerEngine.computeDashboardMetrics(
            studios = core.studios,
            shoots = core.shoots,
            withdrawals = core.withdrawals,
            expenses = core.expenses
        )
        val studioSummaries = LedgerEngine.computeStudioSummaries(
            studios = core.studios,
            shoots = core.shoots,
            withdrawals = core.withdrawals
        )
        val recentActivity = LedgerEngine.buildRecentActivity(
            shoots = core.shoots,
            withdrawals = core.withdrawals,
            expenses = core.expenses
        )
        val paymentAlerts = LedgerEngine.computePaymentAlerts(
            studios = core.studios,
            shoots = core.shoots,
            withdrawals = core.withdrawals,
            reminderWindowDays = core.settings.paymentReminderDays.toInt()
        )
        val scheduleConflicts = LedgerEngine.detectScheduleConflicts(core.shoots)
        val analytics = LedgerEngine.computeAnalytics(
            studios = core.studios,
            shoots = core.shoots,
            withdrawals = core.withdrawals,
            expenses = core.expenses,
            filter = overlay.filter,
            customStartEpochMs = overlay.customRange.first,
            customEndEpochMs = overlay.customRange.second
        )

        CineVaultUiState(
            isLoading = false,
            profile = core.profile,
            settings = core.settings,
            studios = core.studios,
            shoots = core.shoots,
            withdrawals = core.withdrawals,
            expenses = core.expenses,
            notifications = core.notifications,
            metrics = metrics,
            studioSummaries = studioSummaries,
            recentActivity = recentActivity,
            paymentAlerts = paymentAlerts,
            scheduleConflicts = scheduleConflicts,
            analyticsFilter = overlay.filter,
            customStartDateMs = overlay.customRange.first,
            customEndDateMs = overlay.customRange.second,
            analytics = analytics,
            dashboardAiInsight = overlay.aiInsight,
            isDashboardAiLoading = overlay.isInsightLoading,
            aiMessages = overlay.aiMessages,
            isAiBusy = overlay.isAiBusy,
            parsedShootDraft = overlay.parsedDraft,
            statusBannerMessage = overlay.statusMsg,
            errorBannerMessage = overlay.errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = CineVaultUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            repository.ensureUserInitialized(
                displayName = initialDisplayName,
                email = initialEmail,
                photoUrl = initialPhotoUrl
            ).onFailure { e ->
                Log.e(TAG, "Failed to initialize user profile", e)
            }
        }
    }

    fun dismissBanners() {
        _statusBanner.value = null
        _errorBanner.value = null
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.completeOnboarding()
                .onSuccess { _statusBanner.value = "Vault initialized in NPR" }
                .onFailure { _errorBanner.value = it.localizedMessage ?: "Failed to complete onboarding" }
        }
    }

    fun setAnalyticsFilter(filter: DateRangeFilter, customStart: Long? = null, customEnd: Long? = null) {
        _analyticsFilter.value = filter
        if (filter == DateRangeFilter.CUSTOM) {
            _customDateRange.value = customStart to customEnd
        }
    }

    // -------------------------------------------------------------------------
    // Studio Actions
    // -------------------------------------------------------------------------

    fun saveStudio(studio: Studio, isEdit: Boolean) {
        viewModelScope.launch {
            val result = if (isEdit) {
                repository.updateStudio(studio).map { studio.id }
            } else {
                repository.createStudio(studio)
            }
            result.onSuccess {
                _statusBanner.value = if (isEdit) "Studio updated" else "Studio added to Vault"
            }.onFailure { e ->
                _errorBanner.value = e.localizedMessage ?: "Failed to save studio"
            }
        }
    }

    fun toggleStudioArchive(studio: Studio) {
        viewModelScope.launch {
            repository.updateStudio(studio.copy(archived = !studio.archived))
                .onSuccess {
                    _statusBanner.value = if (!studio.archived) "Studio archived" else "Studio restored"
                }
                .onFailure { e ->
                    _errorBanner.value = e.localizedMessage ?: "Failed to update studio"
                }
        }
    }

    fun deleteStudio(studioId: String) {
        viewModelScope.launch {
            repository.deleteStudioIfNoRecords(studioId)
                .onSuccess { _statusBanner.value = "Studio deleted" }
                .onFailure { e -> _errorBanner.value = e.localizedMessage ?: "Cannot delete studio with records" }
        }
    }

    // -------------------------------------------------------------------------
    // Shoot Actions
    // -------------------------------------------------------------------------

    fun saveShoot(shoot: Shoot, isEdit: Boolean) {
        viewModelScope.launch {
            val result = if (isEdit) {
                repository.updateShoot(shoot).map { shoot.id }
            } else {
                repository.createShoot(shoot)
            }
            result.onSuccess {
                _statusBanner.value = if (isEdit) "Shoot updated" else "Shoot saved to ledger"
            }.onFailure { e ->
                _errorBanner.value = e.localizedMessage ?: "Failed to save shoot"
            }
        }
    }

    fun deleteShoot(shootId: String) {
        viewModelScope.launch {
            repository.deleteShoot(shootId)
                .onSuccess { _statusBanner.value = "Shoot removed from ledger" }
                .onFailure { e -> _errorBanner.value = e.localizedMessage ?: "Failed to delete shoot" }
        }
    }

    // -------------------------------------------------------------------------
    // Withdrawal Actions
    // -------------------------------------------------------------------------

    fun saveWithdrawal(withdrawal: Withdrawal, isEdit: Boolean) {
        viewModelScope.launch {
            val result = if (isEdit) {
                repository.updateWithdrawal(withdrawal).map { withdrawal.id }
            } else {
                repository.createWithdrawal(withdrawal)
            }
            result.onSuccess {
                _statusBanner.value = if (isEdit) "Withdrawal updated" else "Withdrawal recorded in NPR"
            }.onFailure { e ->
                _errorBanner.value = e.localizedMessage ?: "Failed to record withdrawal"
            }
        }
    }

    fun deleteWithdrawal(withdrawalId: String) {
        viewModelScope.launch {
            repository.deleteWithdrawal(withdrawalId)
                .onSuccess { _statusBanner.value = "Withdrawal deleted" }
                .onFailure { e -> _errorBanner.value = e.localizedMessage ?: "Failed to delete withdrawal" }
        }
    }

    // -------------------------------------------------------------------------
    // Expense Actions
    // -------------------------------------------------------------------------

    fun saveExpense(expense: Expense, isEdit: Boolean) {
        viewModelScope.launch {
            val result = if (isEdit) {
                repository.updateExpense(expense).map { expense.id }
            } else {
                repository.createExpense(expense)
            }
            result.onSuccess {
                _statusBanner.value = if (isEdit) "Expense updated" else "Business expense recorded"
            }.onFailure { e ->
                _errorBanner.value = e.localizedMessage ?: "Failed to save expense"
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
                .onSuccess { _statusBanner.value = "Expense deleted" }
                .onFailure { e -> _errorBanner.value = e.localizedMessage ?: "Failed to delete expense" }
        }
    }

    // -------------------------------------------------------------------------
    // Settings & Profile Actions
    // -------------------------------------------------------------------------

    fun updateSettings(newSettings: UserSettings) {
        viewModelScope.launch {
            repository.saveUserSettings(newSettings)
                .onSuccess { _statusBanner.value = "Settings updated" }
                .onFailure { e -> _errorBanner.value = e.localizedMessage ?: "Failed to save settings" }
        }
    }

    fun updateProfileDisplayName(newName: String) {
        viewModelScope.launch {
            repository.updateUserProfile(newName)
                .onSuccess { _statusBanner.value = "Profile updated" }
                .onFailure { e -> _errorBanner.value = e.localizedMessage ?: "Failed to update profile" }
        }
    }

    fun purgeAccountAndSignOut(onPurged: () -> Unit) {
        viewModelScope.launch {
            repository.purgeUserAccountData()
                .onSuccess { onPurged() }
                .onFailure { e ->
                    _errorBanner.value = e.localizedMessage ?: "Failed to delete account data"
                }
        }
    }

    // -------------------------------------------------------------------------
    // AI Business Assistant & Natural Language Entry
    // -------------------------------------------------------------------------

    fun generateDashboardAiInsight() {
        val current = uiState.value
        if (!current.settings.aiEnabled) {
            _errorBanner.value = "CINEVAULT AI is currently disabled in Settings."
            return
        }
        viewModelScope.launch {
            _isDashboardAiLoading.value = true
            val contextPrompt = aiService.buildFinancialContextPrompt(
                metrics = current.metrics,
                studios = current.studioSummaries,
                shoots = current.shoots,
                withdrawals = current.withdrawals,
                expenses = current.expenses
            )
            aiService.generateQuickInsight(contextPrompt)
                .onSuccess { _dashboardAiInsight.value = it }
                .onFailure { e ->
                    _errorBanner.value = e.localizedMessage ?: "AI insight generation failed."
                }
            _isDashboardAiLoading.value = false
        }
    }

    fun askAiAssistant(prompt: String) {
        val current = uiState.value
        if (!current.settings.aiEnabled) {
            _errorBanner.value = "CINEVAULT AI is disabled in Settings. Enable it to use AI features."
            return
        }
        if (prompt.isBlank() || current.isAiBusy) return

        val userMsg = AiChatMessage(
            id = "u_${System.currentTimeMillis()}",
            role = "user",
            content = prompt.trim()
        )
        val assistantMsgId = "a_${System.currentTimeMillis()}"
        val placeholderMsg = AiChatMessage(
            id = assistantMsgId,
            role = "assistant",
            content = "",
            isStreaming = true
        )

        _aiMessages.update { it + userMsg + placeholderMsg }
        _isAiBusy.value = true

        viewModelScope.launch {
            val contextPrompt = aiService.buildFinancialContextPrompt(
                metrics = current.metrics,
                studios = current.studioSummaries,
                shoots = current.shoots,
                withdrawals = current.withdrawals,
                expenses = current.expenses
            )
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    aiService.streamAssistantResponse(prompt, contextPrompt).collect { chunk ->
                        accumulated.append(chunk)
                        val textNow = accumulated.toString()
                        _aiMessages.update { list ->
                            list.map { msg ->
                                if (msg.id == assistantMsgId) msg.copy(content = textNow, isStreaming = true) else msg
                            }
                        }
                    }
                }
                val finalText = accumulated.toString().ifBlank { "No response returned by model." }
                _aiMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == assistantMsgId) msg.copy(content = finalText, isStreaming = false) else msg
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI streaming timed out", e)
                _aiMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == assistantMsgId) {
                            msg.copy(content = "Request timed out after 60 seconds. Please try again.", isStreaming = false)
                        } else msg
                    }
                }
                _errorBanner.value = "AI request timed out after 60 seconds."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI assistant failed", e)
                val errText = e.localizedMessage ?: "AI generation failed."
                _aiMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == assistantMsgId) {
                            msg.copy(content = "Error: $errText", isStreaming = false)
                        } else msg
                    }
                }
                _errorBanner.value = errText
            } finally {
                _isAiBusy.value = false
            }
        }
    }

    fun parseNaturalLanguageShootEntry(naturalText: String) {
        val current = uiState.value
        if (!current.settings.aiEnabled) {
            _errorBanner.value = "CINEVAULT AI is disabled in Settings."
            return
        }
        if (naturalText.isBlank() || current.isAiBusy) return

        _isAiBusy.value = true
        viewModelScope.launch {
            val studioNames = current.studios.map { it.name }
            aiService.parseNaturalLanguageShoot(naturalText, studioNames)
                .onSuccess { draft ->
                    _parsedShootDraft.value = draft
                }
                .onFailure { e ->
                    _errorBanner.value = e.localizedMessage ?: "Failed to parse shoot description."
                }
            _isAiBusy.value = false
        }
    }

    fun clearParsedShootDraft() {
        _parsedShootDraft.value = null
    }

    fun confirmAndSaveParsedShoot(draft: ParsedNaturalLanguageShoot) {
        viewModelScope.launch {
            val currentStudios = uiState.value.studios
            val matchedStudio = currentStudios.firstOrNull {
                it.name.equals(draft.studioName.trim(), ignoreCase = true)
            }
            val studioId: String
            val studioName: String
            if (matchedStudio != null) {
                studioId = matchedStudio.id
                studioName = matchedStudio.name
            } else {
                val newStudioName = draft.studioName.ifBlank { "General Production Studio" }
                val createRes = repository.createStudio(Studio(name = newStudioName))
                studioId = createRes.getOrElse {
                    _errorBanner.value = it.localizedMessage ?: "Failed to create studio for shoot"
                    return@launch
                }
                studioName = newStudioName
            }

            val shootDateMs = DateUtils.parseIsoDateOrToday(draft.shootDateIso)
            val earningsPaisa = MoneyUtils.parseNprToPaisa(draft.confirmedEarningsNpr.toString()) ?: 0L
            val shoot = Shoot(
                eventTitle = draft.eventTitle,
                studioId = studioId,
                studioName = studioName,
                shootDateEpochMs = shootDateMs,
                category = draft.category,
                location = draft.location,
                durationValue = draft.durationValue,
                durationUnit = draft.durationUnit,
                confirmedEarningsPaisa = earningsPaisa,
                expectedPaymentDateEpochMs = shootDateMs + (7L * 24L * 60L * 60L * 1000L),
                paymentStatus = "Collectible",
                projectStatus = draft.projectStatus,
                notes = draft.notes
            )

            repository.createShoot(shoot)
                .onSuccess {
                    _parsedShootDraft.value = null
                    _statusBanner.value = "Shoot '${draft.eventTitle}' confirmed & saved to ledger"
                }
                .onFailure { e ->
                    _errorBanner.value = e.localizedMessage ?: "Failed to save extracted shoot"
                }
        }
    }

    // -------------------------------------------------------------------------
    // PDF & CSV Exports
    // -------------------------------------------------------------------------

    fun exportAndShareReport(
        context: Context,
        reportType: ReportType,
        isPdf: Boolean,
        selectedStudioId: String? = null
    ) {
        val current = uiState.value
        viewModelScope.launch {
            val result = if (isPdf) {
                ReportExportEngine.generatePdfReport(
                    context = context,
                    reportType = reportType,
                    periodLabel = current.analyticsFilter.label,
                    metrics = current.metrics,
                    studios = current.studios,
                    shoots = current.shoots,
                    withdrawals = current.withdrawals,
                    expenses = current.expenses,
                    selectedStudioId = selectedStudioId
                )
            } else {
                ReportExportEngine.generateCsvReport(
                    context = context,
                    reportType = reportType,
                    studios = current.studios,
                    shoots = current.shoots,
                    withdrawals = current.withdrawals,
                    expenses = current.expenses,
                    selectedStudioId = selectedStudioId
                )
            }

            result.onSuccess { file ->
                val mime = if (isPdf) "application/pdf" else "text/csv"
                _statusBanner.value = "Generated ${file.name}"
                try {
                    ReportExportEngine.shareExportedFile(context, file, mime)
                } catch (e: Exception) {
                    Log.w(TAG, "Share sheet could not be opened in current environment", e)
                }
            }.onFailure { e ->
                _errorBanner.value = e.localizedMessage ?: "Export generation failed"
            }
        }
    }
}
