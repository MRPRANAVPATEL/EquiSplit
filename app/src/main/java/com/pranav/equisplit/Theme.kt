package com.pranav.equisplit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val Forest = Color(0xFF123E31)
internal val Mint = Color(0xFFE4F6BC)
internal val HeroMuted = Color(0xFFB7D1C2)
internal val HeroEdge = Color(0xFF306251)

private val LightColors = lightColorScheme(
    primary = Color(0xFF245D48),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCECDF),
    onPrimaryContainer = Color(0xFF123E31),
    secondary = Color(0xFF53644E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9EDDB),
    onSecondaryContainer = Color(0xFF263620),
    background = Color(0xFFF5F5ED),
    onBackground = Color(0xFF18261F),
    surface = Color(0xFFFFFEF8),
    onSurface = Color(0xFF1A2A21),
    surfaceVariant = Color(0xFFE8EDE4),
    onSurfaceVariant = Color(0xFF4A5A50),
    outline = Color(0xFF768779),
    outlineVariant = Color(0xFFD9E2D6),
    error = Color(0xFF9B3B29),
    errorContainer = Color(0xFFF9E2D9),
    onErrorContainer = Color(0xFF632716)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA6DABB),
    onPrimary = Color(0xFF123E31),
    primaryContainer = Color(0xFF225340),
    onPrimaryContainer = Color(0xFFDCF1E2),
    secondary = Color(0xFFC0D0B4),
    secondaryContainer = Color(0xFF384833),
    onSecondaryContainer = Color(0xFFE3EFD9),
    background = Color(0xFF131D18),
    onBackground = Color(0xFFE7EFE7),
    surface = Color(0xFF1E2B23),
    onSurface = Color(0xFFE9F0E8),
    surfaceVariant = Color(0xFF303E34),
    onSurfaceVariant = Color(0xFFBFCBC0),
    outline = Color(0xFFA0B0A2),
    outlineVariant = Color(0xFF3A4A40),
    error = Color(0xFFFFB6A3),
    errorContainer = Color(0xFF583329),
    onErrorContainer = Color(0xFFFFDBD1)
)

private val SplitTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 41.sp,
        letterSpacing = (-1.2).sp
    ),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 33.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp)
)

@Composable
internal fun EquiSplitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = SplitTypography,
        content = content
    )
}
