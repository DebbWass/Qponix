package com.cupons.sms.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Dark Color Scheme ───
private val DarkColorScheme = darkColorScheme(
    primary              = PrimaryGreen,
    onPrimary            = TextOnPrimary,
    primaryContainer     = CardHighlight,
    onPrimaryContainer   = PrimaryGreenLight,
    secondary            = SecondaryAmber,
    onSecondary          = Color.White,
    secondaryContainer   = BalanceChipBg,
    onSecondaryContainer = SecondaryAmber,
    tertiary             = GradientEnd,
    error                = ErrorRed,
    onError              = Color.White,
    background           = BgDeep,
    onBackground         = TextPrimary,
    surface              = SurfaceLight,
    onSurface            = TextPrimary,
    surfaceVariant       = CardBackground,
    onSurfaceVariant     = TextSecondary,
    outline              = PrimaryGreen.copy(alpha = CardBorderAlpha),
    outlineVariant       = Color.White.copy(alpha = 0.08f),
    scrim                = Color.Black.copy(alpha = 0.7f)
)

// ─── Typography ───
private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight    = FontWeight.ExtraBold,
        fontSize      = 30.sp,
        letterSpacing = (-0.5).sp,
        color         = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontWeight    = FontWeight.Bold,
        fontSize      = 24.sp,
        letterSpacing = (-0.3).sp,
        color         = TextPrimary
    ),
    headlineSmall = TextStyle(
        fontWeight    = FontWeight.Bold,
        fontSize      = 20.sp,
        color         = TextPrimary
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 18.sp,
        color      = TextPrimary
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 16.sp,
        color      = TextPrimary
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        color      = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 16.sp,
        color      = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        color      = TextPrimary
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        color      = TextSecondary
    ),
    labelLarge = TextStyle(
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 14.sp,
        letterSpacing = 0.5.sp,
        color         = TextPrimary
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,
        color      = TextSecondary
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        color      = TextSecondary
    )
)

@Composable
fun CuponsSMSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
