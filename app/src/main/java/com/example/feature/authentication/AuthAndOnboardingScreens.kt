package com.example.feature.authentication

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraRoll
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.core.designsystem.CineVaultLogo
import com.example.core.designsystem.GlassButton
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.LiquidGlassBackground
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassSurfaceHigh
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryZinc
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultSuccess
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Silent Auto-Sign-In on App Startup using a single GetGoogleIdOption per request.
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

/**
 * Interactive Sign-In using a single GetSignInWithGoogleOption per request.
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type received.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
        }
    }
}

/**
 * Full Sign-Out clearing both Firebase Auth session and Credential Manager state.
 */
fun signOutCineVault(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

/**
 * Animated CINEVAULT Splash Screen with rotating camera aperture iris.
 */
@Composable
fun CineVaultSplashScreen(
    onSplashFinished: () -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (startAnim) 120f else 0f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "aperture_spin"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(1250L)
        onSplashFinished()
    }

    LiquidGlassBackground {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .testTag("splash_screen")
        ) {
            CineVaultLogo(
                size = 108.dp,
                rotationDegrees = rotation
            )
            Spacer(modifier = Modifier.height(24.dp))
            AnimatedVisibility(
                visible = startAnim,
                enter = fadeIn(tween(600))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CINEVAULT",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        letterSpacing = 4.sp,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "YOUR CINEMA. YOUR BUSINESS. YOUR VAULT.",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        color = CinemaCrimson,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Liquid Glass Authentication Screen with Google Sign-In via Credential Manager.
 */
@Composable
fun CineVaultAuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* Stay on interactive sign-in screen */ },
            scope = scope
        )
    }

    LiquidGlassBackground {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                CineVaultLogo(size = 84.dp)
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "CINEVAULT",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    letterSpacing = 3.sp,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "YOUR CINEMA. YOUR BUSINESS. YOUR VAULT.",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = CinemaCrimson,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))

                GlassCard(
                    cornerRadius = 26.dp,
                    accentGlow = CinemaCrimson,
                    contentPadding = PaddingValues(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Executive Production Ledger",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Manage studio receivables, shoot schedules, eSewa/Khalti/Bank collections, business expenses, PDF statements, and AI financial intelligence in NPR.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryZinc
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    AuthFeatureRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Precision NPR Studio Ledgers",
                        subtitle = "Derived balances from completed shoots & real collections"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AuthFeatureRow(
                        icon = Icons.Default.PictureAsPdf,
                        title = "Instant PDF & CSV Statements",
                        subtitle = "Share branded studio statements directly via WhatsApp or Email"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AuthFeatureRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "CINEVAULT AI Intelligence",
                        subtitle = "Natural language shoot entry, smart pricing & payment alerts"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (errorMessage != null) {
                        GlassCard(
                            cornerRadius = 14.dp,
                            accentGlow = VaultAmber,
                            contentPadding = PaddingValues(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = VaultAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (isLoading) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CinemaCrimson,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp
                            )
                        }
                    } else {
                        GlassButton(
                            text = "Sign in with Google",
                            icon = Icons.Default.Security,
                            isPrimary = true,
                            testTag = "google_sign_in_button",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Protected by Firebase Authentication & Zero-Trust UID Isolation",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryZinc,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(CinemaCrimson.copy(alpha = 0.16f))
                .border(1.dp, GlassBorderBright, CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = CinemaCrimson,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = PureWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryZinc
            )
        }
    }
}

/**
 * First-Login Onboarding Screen confirming NPR currency and explaining CINEVAULT's strict zero state.
 */
@Composable
fun CineVaultOnboardingScreen(
    displayName: String,
    onCompleteOnboarding: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }

    LiquidGlassBackground {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp)
        ) {
            GlassCard(
                cornerRadius = 26.dp,
                accentGlow = CinemaCrimson,
                contentPadding = PaddingValues(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .testTag("onboarding_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CineVaultLogo(size = 44.dp)
                    Text(
                        text = "STEP ${step + 1} OF 2",
                        style = MaterialTheme.typography.labelSmall,
                        color = CinemaCrimson
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (step == 0) {
                    Text(
                        text = "Welcome to CINEVAULT, $displayName",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your cloud vault has been initialized with a strict zero state. Every balance is calculated dynamically from your real studio shoots, withdrawals, and expenses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryZinc
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    GlassCard(
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "PRIMARY VAULT CURRENCY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryZinc
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Nepalese Rupee (NPR)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "NPR Active",
                                tint = VaultSuccess,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Supported Payment Channels: Bank Transfer, eSewa, Cash, Khalti & Other.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryZinc
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                    GlassButton(
                        text = "Continue",
                        isPrimary = true,
                        testTag = "onboarding_next_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { step = 1 }
                    )
                } else {
                    Text(
                        text = "Built for Cinematographers",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthFeatureRow(
                        icon = Icons.Default.CameraRoll,
                        title = "Completed Shoots Drive Collectibles",
                        subtitle = "Schedule planned shoots on the calendar; once marked Completed, they immediately enter the studio's collectible ledger."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthFeatureRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Separate Business Expense Ledger",
                        subtitle = "Equipment rentals, assistant fees, and travel deduct from Net Profit without reducing what studios owe you."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthFeatureRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Natural Language AI Entry",
                        subtitle = "Type 'I completed a wedding shoot for ABC Studio in Kathmandu today for NPR 45,000' and review before saving."
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GlassButton(
                            text = "Back",
                            isPrimary = false,
                            modifier = Modifier.weight(1f),
                            onClick = { step = 0 }
                        )
                        GlassButton(
                            text = "Enter Vault",
                            isPrimary = true,
                            testTag = "onboarding_finish_button",
                            modifier = Modifier.weight(1.5f),
                            onClick = onCompleteOnboarding
                        )
                    }
                }
            }
        }
    }
}

/**
 * Optional App Lock Screen using Android Keyguard / Device Credential authentication.
 */
@Composable
fun CineVaultAppLockGate(
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var errorText by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onUnlocked()
        } else {
            errorText = "Device authentication was cancelled."
        }
    }

    fun triggerUnlock() {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        @Suppress("DEPRECATION")
        val intent = km?.createConfirmDeviceCredentialIntent(
            "Unlock CINEVAULT",
            "Authenticate with your device PIN, pattern, or biometric to access your financial vault."
        )
        if (intent != null) {
            launcher.launch(intent)
        } else {
            // Device has no secure lock screen configured; allow entry
            onUnlocked()
        }
    }

    LaunchedEffect(Unit) {
        triggerUnlock()
    }

    LiquidGlassBackground {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            GlassCard(
                cornerRadius = 24.dp,
                accentGlow = CinemaCrimson,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(GlassSurfaceHigh)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Vault Locked",
                            tint = CinemaCrimson,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "CINEVAULT Locked",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorText ?: "Verify your identity to access your cinematography financial records.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryZinc,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    GlassButton(
                        text = "Unlock Vault",
                        icon = Icons.Default.Lock,
                        isPrimary = true,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { triggerUnlock() }
                    )
                }
            }
        }
    }
}
