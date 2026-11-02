package com.example.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.utilities.DateUtils
import com.example.core.utilities.MoneyUtils
import com.example.domain.MonthlyFinancialPoint
import com.example.domain.StudioFinancialSummary
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.CinemaCrimsonDark
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSpecularBottom
import com.example.ui.theme.GlassSpecularTop
import com.example.ui.theme.GlassSurfaceHigh
import com.example.ui.theme.GlassSurfaceLow
import com.example.ui.theme.GlassSurfaceMedium
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MutedZinc
import com.example.ui.theme.Obsidian
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryZinc
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultSuccess
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Original geometric camera aperture & hexagonal vault iris logo for CINEVAULT.
 * Supports colored and monochrome variants at any size.
 */
@Composable
fun CineVaultLogo(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    monochrome: Boolean = false,
    rotationDegrees: Float = 0f
) {
    val accent = if (monochrome) PureWhite else CinemaCrimson
    val bladePrimary = PureWhite
    val bladeSecondary = if (monochrome) Color(0xFFB4B4BC) else Color(0xFFD4D4D8)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val outerR = w * 0.46f
        val innerR = w * 0.36f

        // Outer hexagonal vault ring
        val hexOuter = Path()
        for (i in 0 until 6) {
            val angle = Math.toRadians((60.0 * i) - 90.0)
            val x = cx + outerR * cos(angle).toFloat()
            val y = cy + outerR * sin(angle).toFloat()
            if (i == 0) hexOuter.moveTo(x, y) else hexOuter.lineTo(x, y)
        }
        hexOuter.close()
        drawPath(
            path = hexOuter,
            brush = Brush.linearGradient(
                colors = listOf(accent, if (monochrome) SecondaryZinc else CinemaCrimsonDark),
                start = Offset.Zero,
                end = Offset(w, h)
            )
        )

        // Inner dark vault core
        val hexInner = Path()
        for (i in 0 until 6) {
            val angle = Math.toRadians((60.0 * i) - 90.0)
            val x = cx + innerR * cos(angle).toFloat()
            val y = cy + innerR * sin(angle).toFloat()
            if (i == 0) hexInner.moveTo(x, y) else hexInner.lineTo(x, y)
        }
        hexInner.close()
        drawPath(path = hexInner, color = Obsidian)

        // 6 Geometric Camera Aperture Blades
        rotate(degrees = rotationDegrees, pivot = Offset(cx, cy)) {
            for (i in 0 until 6) {
                rotate(degrees = i * 60f, pivot = Offset(cx, cy)) {
                    val bladePath = Path().apply {
                        moveTo(cx, cy - innerR * 0.88f)
                        lineTo(cx + innerR * 0.68f, cy - innerR * 0.36f)
                        lineTo(cx + innerR * 0.16f, cy - innerR * 0.14f)
                        lineTo(cx - innerR * 0.32f, cy - innerR * 0.42f)
                        close()
                    }
                    val color = when {
                        i == 5 && !monochrome -> CinemaCrimson
                        i % 2 == 0 -> bladePrimary
                        else -> bladeSecondary
                    }
                    drawPath(path = bladePath, color = color)
                }
            }
        }

        // Center precision iris ring
        drawCircle(
            color = accent,
            radius = w * 0.08f,
            center = Offset(cx, cy),
            style = Stroke(width = w * 0.03f)
        )
    }
}

/**
 * Full-screen atmospheric Liquid Glass background with subtle crimson and slate optical blooms.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Obsidian)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-left Crimson optical bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x28E50914), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.08f),
                    radius = w * 0.75f
                ),
                radius = w * 0.75f,
                center = Offset(w * 0.15f, h * 0.08f)
            )

            // Mid-right cool anamorphic reflection bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1638BDF8), Color.Transparent),
                    center = Offset(w * 0.9f, h * 0.42f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.9f, h * 0.42f)
            )

            // Bottom subtle warmth
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x18E50914), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.95f),
                    radius = w * 0.7f
                ),
                radius = w * 0.7f,
                center = Offset(w * 0.5f, h * 0.95f)
            )
        }
        content()
    }
}

/**
 * Reusable Liquid Glass Card with translucent gradient surface, specular top-left border, and soft elevation.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    accentGlow: Color? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val surfaceBrush = if (accentGlow != null) {
        Brush.linearGradient(
            colors = listOf(
                accentGlow.copy(alpha = 0.18f),
                GlassSurfaceMedium,
                GlassSurfaceLow
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                GlassSpecularTop,
                GlassSurfaceLow,
                GlassSpecularBottom
            )
        )
    }
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            accentGlow?.copy(alpha = 0.45f) ?: GlassBorderBright,
            GlassBorderSubtle
        )
    )

    val cardModifier = modifier
        .shadow(elevation = 10.dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)
        .clip(shape)
        .background(brush = surfaceBrush, shape = shape)
        .border(width = 1.dp, brush = borderBrush, shape = shape)
        .let { mod ->
            if (onClick != null) mod.clickable(onClick = onClick) else mod
        }
        .padding(contentPadding)

    Column(
        modifier = cardModifier,
        content = content
    )
}

/**
 * Reusable Liquid Glass Button supporting primary Crimson or frosted glass styling.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val shape = RoundedCornerShape(16.dp)
    val bgBrush = if (isPrimary) {
        Brush.horizontalGradient(listOf(CinemaCrimson, CinemaCrimsonDark))
    } else {
        Brush.horizontalGradient(listOf(GlassSurfaceHigh, GlassSurfaceMedium))
    }
    val borderColor = if (isPrimary) Color(0x55FFFFFF) else GlassBorderBright

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = PureWhite,
            disabledContainerColor = GlassSurfaceLow,
            disabledContentColor = MutedZinc
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .let { if (testTag != null) it.testTag(testTag) else it }
            .clip(shape)
            .background(brush = bgBrush, shape = shape)
            .border(1.dp, borderColor, shape)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(18.dp),
                tint = PureWhite
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) PureWhite else MutedZinc
        )
    }
}

/**
 * Reusable Liquid Glass Input Field with crisp borders and focus glow.
 */
@Composable
fun GlassInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    testTag: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotBlank()) {
            { Text(placeholder, color = MutedZinc) }
        } else null,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedContainerColor = GlassSurfaceLow,
            unfocusedContainerColor = Color(0x0DFFFFFF),
            focusedBorderColor = CinemaCrimson,
            unfocusedBorderColor = GlassBorderBright,
            focusedLabelColor = PureWhite,
            unfocusedLabelColor = SecondaryZinc,
            cursorColor = CinemaCrimson
        ),
        modifier = modifier
            .fillMaxWidth()
            .let { if (testTag != null) it.testTag(testTag) else it }
    )
}

/**
 * Reusable Liquid Glass Filter Chip with animated active pill.
 */
@Composable
fun GlassFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    testTag: String? = null
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) CinemaCrimson else GlassSurfaceLow,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) PureWhite.copy(alpha = 0.4f) else GlassBorderSubtle,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip_border"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .let { if (testTag != null) it.testTag(testTag) else it }
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = label,
                tint = PureWhite,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) PureWhite else SecondaryZinc,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

/**
 * Animated NPR Financial Counter using tabular JetBrains Mono or Outfit display typography.
 */
@Composable
fun AnimatedNprAmount(
    amountPaisa: Long,
    modifier: Modifier = Modifier,
    isHero: Boolean = false,
    color: Color = PureWhite
) {
    val targetNprInt = (amountPaisa / 100L).toInt()
    val animatedNpr by animateIntAsState(
        targetValue = targetNprInt,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "npr_counter"
    )
    val displayPaisa = if (kotlin.math.abs(amountPaisa - (animatedNpr.toLong() * 100L)) < 100L) {
        amountPaisa
    } else {
        animatedNpr.toLong() * 100L
    }

    Text(
        text = MoneyUtils.formatNpr(displayPaisa),
        fontFamily = if (isHero) OutfitFontFamily else JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = if (isHero) 34.sp else 17.sp,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Reusable Glass Metric Card for financial KPIs.
 */
@Composable
fun GlassMetricCard(
    title: String,
    amountPaisa: Long? = null,
    valueText: String? = null,
    subtitle: String = "",
    icon: ImageVector,
    accentColor: Color = CinemaCrimson,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryZinc,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (amountPaisa != null) {
            AnimatedNprAmount(
                amountPaisa = amountPaisa,
                isHero = false,
                color = PureWhite
            )
        } else if (valueText != null) {
            Text(
                text = valueText,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = PureWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryZinc,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Reusable Glass Studio Card displaying earnings, withdrawals, collectible balance, and collection progress.
 */
@Composable
fun GlassStudioCard(
    summary: StudioFinancialSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = (summary.collectionPercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "studio_collection_pct"
    )
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = summary.studio.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (summary.studio.archived) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ARCHIVED",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultAmber,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(VaultAmber.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                val sub = buildString {
                    if (summary.studio.contactPerson.isNotBlank()) append(summary.studio.contactPerson).append(" • ")
                    append("${summary.completedShootCount} completed")
                    if (summary.plannedShootCount > 0) append(" (${summary.plannedShootCount} planned)")
                }
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryZinc
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "COLLECTIBLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryZinc
                )
                Text(
                    text = MoneyUtils.formatNpr(summary.outstandingBalancePaisa),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (summary.outstandingBalancePaisa > 0L) VaultAmber else VaultSuccess
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = VaultSuccess,
            trackColor = GlassSurfaceHigh,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Earned: ${MoneyUtils.formatNpr(summary.totalEarningsPaisa)}",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryZinc
            )
            Text(
                text = "Collected: ${MoneyUtils.formatNpr(summary.totalWithdrawalsPaisa)} (${summary.collectionPercentage.toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                color = VaultSuccess
            )
        }
    }
}

/**
 * Reusable Glass Transaction Row for Shoots, Withdrawals, and Expenses.
 */
@Composable
fun GlassTransactionRow(
    title: String,
    subtitle: String,
    amountPaisa: Long,
    dateEpochMs: Long,
    badgeText: String,
    badgeColor: Color,
    amountColor: Color = PureWhite,
    amountPrefix: String = "",
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = badgeText.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryZinc,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${MoneyUtils.formatNpr(amountPaisa)}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = amountColor
                )
                Text(
                    text = DateUtils.formatShortDate(dateEpochMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedZinc
                )
            }
        }
    }
}

/**
 * Reusable Glass Dialog container.
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        GlassCard(
            cornerRadius = 24.dp,
            accentGlow = CinemaCrimson,
            contentPadding = PaddingValues(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = PureWhite
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

/**
 * Reusable Glass Bottom Sheet container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = DeepCharcoal,
        contentColor = PureWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = PureWhite
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

/**
 * Reusable Floating Liquid Glass Navigation Bar for the 5 primary destinations.
 */
data class GlassNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun GlassNavigationBar(
    destinations: List<GlassNavDestination>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        val barShape = RoundedCornerShape(28.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, barShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(barShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ElevatedCharcoal.copy(alpha = 0.94f),
                            DeepCharcoal.copy(alpha = 0.97f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(GlassBorderBright, GlassBorderSubtle)),
                    shape = barShape
                )
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            destinations.forEach { dest ->
                val selected = currentRoute == dest.route
                val pillBg by animateColorAsState(
                    targetValue = if (selected) CinemaCrimson.copy(alpha = 0.22f) else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "nav_pill"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (selected) CinemaCrimson else SecondaryZinc,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "nav_icon"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag(dest.testTag)
                        .clip(RoundedCornerShape(20.dp))
                        .background(pillBg)
                        .then(
                            if (selected) {
                                Modifier.border(
                                    1.dp,
                                    CinemaCrimson.copy(alpha = 0.45f),
                                    RoundedCornerShape(20.dp)
                                )
                            } else Modifier
                        )
                        .clickable { onNavigate(dest.route) }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = dest.label,
                        tint = iconTint,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) PureWhite else SecondaryZinc,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Reusable Glass Chart Container rendering Monthly Earnings, Withdrawals, Expenses, and Net Profit bars.
 */
@Composable
fun GlassChartContainer(
    title: String,
    subtitle: String,
    monthlyPoints: List<MonthlyFinancialPoint>,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryZinc
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChartLegendDot("Earned", CinemaCrimson)
                ChartLegendDot("Collected", VaultSuccess)
                ChartLegendDot("Expenses", VaultCyan)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (monthlyPoints.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Text(
                    text = "Complete a shoot or record a transaction to visualize financial trends",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryZinc
                )
            }
        } else {
            val recentPoints = monthlyPoints.takeLast(6)
            val maxVal = recentPoints.maxOfOrNull {
                max(it.earningsPaisa, max(it.withdrawalsPaisa, it.expensesPaisa))
            }?.coerceAtLeast(100L) ?: 100L

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
            ) {
                val w = size.width
                val h = size.height - 24.dp.toPx()
                val groupCount = recentPoints.size.coerceAtLeast(1)
                val groupWidth = w / groupCount
                val barWidth = (groupWidth * 0.2f).coerceAtMost(18.dp.toPx())

                // Subtle horizontal grid lines
                for (i in 0..3) {
                    val y = h * (i / 3f)
                    drawLine(
                        color = GlassBorderSubtle,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                recentPoints.forEachIndexed { idx, pt ->
                    val centerX = (idx * groupWidth) + (groupWidth / 2f)

                    val earnH = ((pt.earningsPaisa.toFloat() / maxVal.toFloat()) * h).coerceAtLeast(4f)
                    val wdH = ((pt.withdrawalsPaisa.toFloat() / maxVal.toFloat()) * h).coerceAtLeast(4f)
                    val expH = ((pt.expensesPaisa.toFloat() / maxVal.toFloat()) * h).coerceAtLeast(4f)

                    // Earnings bar
                    drawRoundRect(
                        color = CinemaCrimson,
                        topLeft = Offset(centerX - barWidth * 1.6f, h - earnH),
                        size = Size(barWidth, earnH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    // Withdrawals bar
                    drawRoundRect(
                        color = VaultSuccess,
                        topLeft = Offset(centerX - barWidth * 0.4f, h - wdH),
                        size = Size(barWidth, wdH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    // Expenses bar
                    drawRoundRect(
                        color = VaultCyan,
                        topLeft = Offset(centerX + barWidth * 0.8f, h - expH),
                        size = Size(barWidth, expH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                recentPoints.forEach { pt ->
                    Text(
                        text = pt.monthLabel.take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryZinc
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SecondaryZinc
        )
    }
}
