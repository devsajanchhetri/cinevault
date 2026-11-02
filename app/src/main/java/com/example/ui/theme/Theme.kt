package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CineVaultColorScheme = darkColorScheme(
    primary = CinemaCrimson,
    onPrimary = PureWhite,
    primaryContainer = CinemaCrimsonDark,
    onPrimaryContainer = PureWhite,
    secondary = VaultSuccess,
    onSecondary = Obsidian,
    secondaryContainer = DeepCharcoal,
    onSecondaryContainer = PureWhite,
    tertiary = VaultAmber,
    onTertiary = Obsidian,
    background = Obsidian,
    onBackground = PureWhite,
    surface = DeepCharcoal,
    onSurface = PureWhite,
    surfaceVariant = ElevatedCharcoal,
    onSurfaceVariant = SecondaryZinc,
    error = CinemaCrimson,
    onError = PureWhite,
    outline = GlassBorderBright,
    outlineVariant = GlassBorderSubtle
)

@Composable
fun CineVaultTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CineVaultColorScheme,
        typography = Typography,
        content = content
    )
}
