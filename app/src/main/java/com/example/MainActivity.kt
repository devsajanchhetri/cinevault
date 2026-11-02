package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraRoll
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.GlassNavDestination
import com.example.core.designsystem.GlassNavigationBar
import com.example.core.designsystem.LiquidGlassBackground
import com.example.core.security.configureAppCheck
import com.example.core.utilities.MoneyUtils
import com.example.data.repository.CineVaultRepository
import com.example.domain.model.Expense
import com.example.domain.model.Shoot
import com.example.domain.model.Studio
import com.example.domain.model.Withdrawal
import com.example.feature.CineVaultViewModel
import com.example.feature.ai.CineVaultAiScreen
import com.example.feature.ai.GlobalSearchScreen
import com.example.feature.ai.ReportsExportScreen
import com.example.feature.ai.SettingsAndProfileScreen
import com.example.feature.authentication.CineVaultAppLockGate
import com.example.feature.authentication.CineVaultAuthScreen
import com.example.feature.authentication.CineVaultOnboardingScreen
import com.example.feature.authentication.CineVaultSplashScreen
import com.example.feature.authentication.signOutCineVault
import com.example.feature.calendar.AnalyticsInsightsScreen
import com.example.feature.calendar.CalendarScheduleScreen
import com.example.feature.dashboard.DashboardScreen
import com.example.feature.reports.ReportType
import com.example.feature.shoots.ShootFormBottomSheet
import com.example.feature.shoots.ShootsScreen
import com.example.feature.studios.StudioFormBottomSheet
import com.example.feature.studios.StudiosScreen
import com.example.feature.withdrawals.ExpenseFormBottomSheet
import com.example.feature.withdrawals.ExpensesManagementScreen
import com.example.feature.withdrawals.WithdrawalFormBottomSheet
import com.example.feature.withdrawals.WithdrawalsManagementScreen
import com.example.ui.theme.CineVaultTheme
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.PureWhite
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultSuccess
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

enum class PrimaryVaultTab(val route: String) {
    HOME("home"),
    STUDIOS("studios"),
    SHOOTS("shoots"),
    INSIGHTS("insights"),
    AI_ASSISTANT("ai_assistant")
}

enum class SecondaryOverlayScreen {
    NONE,
    WITHDRAWALS,
    EXPENSES,
    CALENDAR,
    REPORTS,
    SEARCH,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            CineVaultTheme {
                CineVaultAppRoot()
            }
        }
    }
}

@Composable
fun CineVaultAppRoot() {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (showSplash) {
        CineVaultSplashScreen(onSplashFinished = { showSplash = false })
        return
    }

    val activeUser = currentUser
    if (activeUser == null) {
        CineVaultAuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
        return
    }

    val context = LocalContext.current
    val appContext = context.applicationContext
    val viewModel: CineVaultViewModel = viewModel(
        key = "vault_vm_${activeUser.uid}",
        factory = viewModelFactory {
            initializer {
                CineVaultViewModel(
                    repository = CineVaultRepository(appContext),
                    currentUserId = activeUser.uid,
                    initialDisplayName = activeUser.displayName ?: "Cinematographer",
                    initialEmail = activeUser.email ?: "",
                    initialPhotoUrl = activeUser.photoUrl?.toString() ?: ""
                )
            }
        }
    )

    CineVaultAuthenticatedShell(
        viewModel = viewModel,
        onSignedOut = { currentUser = null }
    )
}

@Composable
fun CineVaultAuthenticatedShell(
    viewModel: CineVaultViewModel,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isAppUnlockedInSession by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(PrimaryVaultTab.HOME) }
    var overlayScreen by rememberSaveable { mutableStateOf(SecondaryOverlayScreen.NONE) }
    var selectedStudioDetailId by rememberSaveable { mutableStateOf<String?>(null) }

    // Modal Form Bottom Sheet states
    var showStudioForm by remember { mutableStateOf(false) }
    var editingStudio by remember { mutableStateOf<Studio?>(null) }

    var showShootForm by remember { mutableStateOf(false) }
    var editingShoot by remember { mutableStateOf<Shoot?>(null) }
    var preselectedStudioForShoot by remember { mutableStateOf<Studio?>(null) }

    var showWithdrawalForm by remember { mutableStateOf(false) }
    var editingWithdrawal by remember { mutableStateOf<Withdrawal?>(null) }
    var preselectedStudioForWithdrawal by remember { mutableStateOf<Studio?>(null) }

    var showExpenseForm by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    // 1. First-login onboarding flow
    if (uiState.profile != null && !uiState.profile!!.onboardingCompleted) {
        CineVaultOnboardingScreen(
            displayName = uiState.profile?.displayName ?: "Cinematographer",
            onCompleteOnboarding = { viewModel.completeOnboarding() }
        )
        return
    }

    // 2. Optional Device Biometric / Credential App Lock Gate
    if (uiState.settings.appLockEnabled && !isAppUnlockedInSession) {
        CineVaultAppLockGate(
            onUnlocked = { isAppUnlockedInSession = true }
        )
        return
    }

    val navItems = remember {
        listOf(
            GlassNavDestination(
                route = PrimaryVaultTab.HOME.route,
                label = "Home",
                selectedIcon = Icons.Default.Dashboard,
                unselectedIcon = Icons.Default.Dashboard,
                testTag = "nav_home"
            ),
            GlassNavDestination(
                route = PrimaryVaultTab.STUDIOS.route,
                label = "Studios",
                selectedIcon = Icons.Default.Business,
                unselectedIcon = Icons.Default.Business,
                testTag = "nav_studios"
            ),
            GlassNavDestination(
                route = PrimaryVaultTab.SHOOTS.route,
                label = "Shoots",
                selectedIcon = Icons.Default.CameraRoll,
                unselectedIcon = Icons.Default.CameraRoll,
                testTag = "nav_shoots"
            ),
            GlassNavDestination(
                route = PrimaryVaultTab.INSIGHTS.route,
                label = "Insights",
                selectedIcon = Icons.Default.Insights,
                unselectedIcon = Icons.Default.Insights,
                testTag = "nav_insights"
            ),
            GlassNavDestination(
                route = PrimaryVaultTab.AI_ASSISTANT.route,
                label = "AI",
                selectedIcon = Icons.Default.AutoAwesome,
                unselectedIcon = Icons.Default.AutoAwesome,
                testTag = "nav_ai"
            )
        )
    }

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (overlayScreen == SecondaryOverlayScreen.NONE) {
                    GlassNavigationBar(
                        destinations = navItems,
                        currentRoute = selectedTab.route,
                        onNavigate = { route ->
                            val target = PrimaryVaultTab.entries.find { it.route == route } ?: PrimaryVaultTab.HOME
                            selectedTab = target
                            overlayScreen = SecondaryOverlayScreen.NONE
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = innerPadding.calculateTopPadding() * 0.1f)
            ) {
                when (overlayScreen) {
                    SecondaryOverlayScreen.WITHDRAWALS -> {
                        WithdrawalsManagementScreen(
                            uiState = uiState,
                            onBack = { overlayScreen = SecondaryOverlayScreen.NONE },
                            onRecordWithdrawalClick = {
                                editingWithdrawal = null
                                preselectedStudioForWithdrawal = null
                                showWithdrawalForm = true
                            },
                            onEditWithdrawal = { wd ->
                                editingWithdrawal = wd
                                showWithdrawalForm = true
                            },
                            onDeleteWithdrawal = { id -> viewModel.deleteWithdrawal(id) },
                            onExportPdf = {
                                viewModel.exportAndShareReport(context, ReportType.WITHDRAWAL_HISTORY, isPdf = true)
                            },
                            onExportCsv = {
                                viewModel.exportAndShareReport(context, ReportType.WITHDRAWAL_HISTORY, isPdf = false)
                            }
                        )
                    }

                    SecondaryOverlayScreen.EXPENSES -> {
                        ExpensesManagementScreen(
                            uiState = uiState,
                            onBack = { overlayScreen = SecondaryOverlayScreen.NONE },
                            onAddExpenseClick = {
                                editingExpense = null
                                showExpenseForm = true
                            },
                            onEditExpense = { exp ->
                                editingExpense = exp
                                showExpenseForm = true
                            },
                            onDeleteExpense = { id -> viewModel.deleteExpense(id) },
                            onExportPdf = {
                                viewModel.exportAndShareReport(context, ReportType.EXPENSE_REPORT, isPdf = true)
                            },
                            onExportCsv = {
                                viewModel.exportAndShareReport(context, ReportType.EXPENSE_REPORT, isPdf = false)
                            }
                        )
                    }

                    SecondaryOverlayScreen.CALENDAR -> {
                        CalendarScheduleScreen(
                            uiState = uiState,
                            onBack = { overlayScreen = SecondaryOverlayScreen.NONE },
                            onAddShootClick = {
                                editingShoot = null
                                preselectedStudioForShoot = null
                                showShootForm = true
                            },
                            onEditShoot = { sh ->
                                editingShoot = sh
                                showShootForm = true
                            }
                        )
                    }

                    SecondaryOverlayScreen.REPORTS -> {
                        ReportsExportScreen(
                            uiState = uiState,
                            onBack = { overlayScreen = SecondaryOverlayScreen.NONE },
                            onExportReport = { reportType, isPdf, studioId ->
                                viewModel.exportAndShareReport(
                                    context = context,
                                    reportType = reportType,
                                    isPdf = isPdf,
                                    selectedStudioId = studioId
                                )
                            }
                        )
                    }

                    SecondaryOverlayScreen.SEARCH -> {
                        GlobalSearchScreen(
                            uiState = uiState,
                            onBack = { overlayScreen = SecondaryOverlayScreen.NONE },
                            onSelectStudio = { studioId ->
                                overlayScreen = SecondaryOverlayScreen.NONE
                                selectedStudioDetailId = studioId
                                selectedTab = PrimaryVaultTab.STUDIOS
                            }
                        )
                    }

                    SecondaryOverlayScreen.SETTINGS -> {
                        SettingsAndProfileScreen(
                            uiState = uiState,
                            onBack = { overlayScreen = SecondaryOverlayScreen.NONE },
                            onUpdateDisplayName = { name -> viewModel.updateProfileDisplayName(name) },
                            onUpdateSettings = { newSettings -> viewModel.updateSettings(newSettings) },
                            onExportFullBackupCsv = {
                                viewModel.exportAndShareReport(
                                    context = context,
                                    reportType = ReportType.COMPLETE_ACCOUNT,
                                    isPdf = false
                                )
                            },
                            onSignOut = {
                                signOutCineVault(
                                    credentialManager = credentialManager,
                                    onSignOutComplete = onSignedOut,
                                    scope = scope
                                )
                            },
                            onDeleteAccountAndPurge = {
                                viewModel.purgeAccountAndSignOut {
                                    signOutCineVault(
                                        credentialManager = credentialManager,
                                        onSignOutComplete = onSignedOut,
                                        scope = scope
                                    )
                                }
                            }
                        )
                    }

                    SecondaryOverlayScreen.NONE -> {
                        when (selectedTab) {
                            PrimaryVaultTab.HOME -> {
                                DashboardScreen(
                                    uiState = uiState,
                                    onQuickAddShoot = {
                                        editingShoot = null
                                        preselectedStudioForShoot = null
                                        showShootForm = true
                                    },
                                    onQuickAddStudio = {
                                        editingStudio = null
                                        showStudioForm = true
                                    },
                                    onQuickRecordWithdrawal = {
                                        editingWithdrawal = null
                                        preselectedStudioForWithdrawal = null
                                        showWithdrawalForm = true
                                    },
                                    onQuickAddExpense = {
                                        editingExpense = null
                                        showExpenseForm = true
                                    },
                                    onOpenStudioDetail = { studioId ->
                                        selectedStudioDetailId = studioId
                                        selectedTab = PrimaryVaultTab.STUDIOS
                                    },
                                    onOpenWithdrawalsLedger = {
                                        overlayScreen = SecondaryOverlayScreen.WITHDRAWALS
                                    },
                                    onOpenExpensesLedger = {
                                        overlayScreen = SecondaryOverlayScreen.EXPENSES
                                    },
                                    onOpenCalendar = {
                                        overlayScreen = SecondaryOverlayScreen.CALENDAR
                                    },
                                    onOpenReports = {
                                        overlayScreen = SecondaryOverlayScreen.REPORTS
                                    },
                                    onOpenGlobalSearch = {
                                        overlayScreen = SecondaryOverlayScreen.SEARCH
                                    },
                                    onOpenSettings = {
                                        overlayScreen = SecondaryOverlayScreen.SETTINGS
                                    },
                                    onGenerateAiInsight = {
                                        viewModel.generateDashboardAiInsight()
                                    },
                                    onDraftPaymentReminder = { studioName, balancePaisa ->
                                        selectedTab = PrimaryVaultTab.AI_ASSISTANT
                                        viewModel.askAiAssistant(
                                            "Draft a polite, professional payment follow-up message for $studioName regarding our outstanding cinematography balance of ${MoneyUtils.formatNpr(balancePaisa)}."
                                        )
                                    }
                                )
                            }

                            PrimaryVaultTab.STUDIOS -> {
                                StudiosScreen(
                                    uiState = uiState,
                                    selectedStudioId = selectedStudioDetailId,
                                    onSelectStudio = { id -> selectedStudioDetailId = id },
                                    onAddStudioClick = {
                                        editingStudio = null
                                        showStudioForm = true
                                    },
                                    onEditStudio = { st ->
                                        editingStudio = st
                                        showStudioForm = true
                                    },
                                    onToggleArchive = { st -> viewModel.toggleStudioArchive(st) },
                                    onDeleteStudio = { id ->
                                        viewModel.deleteStudio(id)
                                        if (selectedStudioDetailId == id) selectedStudioDetailId = null
                                    },
                                    onAddShootForStudio = { st ->
                                        editingShoot = null
                                        preselectedStudioForShoot = st
                                        showShootForm = true
                                    },
                                    onRecordWithdrawalForStudio = { st ->
                                        editingWithdrawal = null
                                        preselectedStudioForWithdrawal = st
                                        showWithdrawalForm = true
                                    },
                                    onExportStudioStatement = { studioId, isPdf ->
                                        viewModel.exportAndShareReport(
                                            context = context,
                                            reportType = ReportType.STUDIO_STATEMENT,
                                            isPdf = isPdf,
                                            selectedStudioId = studioId
                                        )
                                    }
                                )
                            }

                            PrimaryVaultTab.SHOOTS -> {
                                ShootsScreen(
                                    uiState = uiState,
                                    onAddShootClick = {
                                        editingShoot = null
                                        preselectedStudioForShoot = null
                                        showShootForm = true
                                    },
                                    onEditShoot = { sh ->
                                        editingShoot = sh
                                        showShootForm = true
                                    },
                                    onDeleteShoot = { id -> viewModel.deleteShoot(id) }
                                )
                            }

                            PrimaryVaultTab.INSIGHTS -> {
                                AnalyticsInsightsScreen(
                                    uiState = uiState,
                                    onFilterChanged = { filter, start, end ->
                                        viewModel.setAnalyticsFilter(filter, start, end)
                                    },
                                    onOpenReports = {
                                        overlayScreen = SecondaryOverlayScreen.REPORTS
                                    }
                                )
                            }

                            PrimaryVaultTab.AI_ASSISTANT -> {
                                CineVaultAiScreen(
                                    uiState = uiState,
                                    onAskPrompt = { prompt -> viewModel.askAiAssistant(prompt) },
                                    onParseNaturalLanguageShoot = { text ->
                                        viewModel.parseNaturalLanguageShootEntry(text)
                                    },
                                    onConfirmParsedShoot = { draft ->
                                        viewModel.confirmAndSaveParsedShoot(draft)
                                    },
                                    onDismissParsedShoot = {
                                        viewModel.clearParsedShootDraft()
                                    },
                                    onOpenSettings = {
                                        overlayScreen = SecondaryOverlayScreen.SETTINGS
                                    }
                                )
                            }
                        }
                    }
                }

                // Floating Status / Error Toast Banner
                val bannerText = uiState.errorBannerMessage ?: uiState.statusBannerMessage
                val isErrorBanner = uiState.errorBannerMessage != null
                AnimatedVisibility(
                    visible = bannerText != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    GlassCard(
                        cornerRadius = 16.dp,
                        accentGlow = if (isErrorBanner) VaultAmber else VaultSuccess,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.dismissBanners() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isErrorBanner) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                                    contentDescription = "Status",
                                    tint = if (isErrorBanner) VaultAmber else VaultSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = bannerText ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PureWhite
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissBanners() },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modal Bottom Sheets for Studio, Shoot, Withdrawal, and Expense creation/editing
        if (showStudioForm) {
            StudioFormBottomSheet(
                initialStudio = editingStudio,
                onDismiss = {
                    showStudioForm = false
                    editingStudio = null
                },
                onSave = { studio, isEdit ->
                    viewModel.saveStudio(studio, isEdit)
                }
            )
        }

        if (showShootForm) {
            ShootFormBottomSheet(
                initialShoot = editingShoot,
                studios = uiState.studios,
                existingShoots = uiState.shoots,
                preselectedStudio = preselectedStudioForShoot,
                onDismiss = {
                    showShootForm = false
                    editingShoot = null
                    preselectedStudioForShoot = null
                },
                onSave = { shoot, isEdit ->
                    viewModel.saveShoot(shoot, isEdit)
                },
                onRequestCreateStudio = {
                    showShootForm = false
                    editingStudio = null
                    showStudioForm = true
                }
            )
        }

        if (showWithdrawalForm) {
            WithdrawalFormBottomSheet(
                initialWithdrawal = editingWithdrawal,
                studios = uiState.studios,
                preselectedStudio = preselectedStudioForWithdrawal,
                onDismiss = {
                    showWithdrawalForm = false
                    editingWithdrawal = null
                    preselectedStudioForWithdrawal = null
                },
                onSave = { wd, isEdit ->
                    viewModel.saveWithdrawal(wd, isEdit)
                }
            )
        }

        if (showExpenseForm) {
            ExpenseFormBottomSheet(
                initialExpense = editingExpense,
                studios = uiState.studios,
                shoots = uiState.shoots,
                onDismiss = {
                    showExpenseForm = false
                    editingExpense = null
                },
                onSave = { exp, isEdit ->
                    viewModel.saveExpense(exp, isEdit)
                }
            )
        }
    }
}
