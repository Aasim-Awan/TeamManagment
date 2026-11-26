package com.teamfund.manager.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Charcoal controls, paper-white cards and a lime accent.
val PitchGreenPrimary = Color(0xFF20231E)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFD5F58A)
val OnPrimaryContainer = Color(0xFF23310F)

val SportyNavySecondary = Color(0xFF4F6132)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFEBF2E0)
val OnSecondaryContainer = Color(0xFF29351B)

val TrophyGoldTertiary = Color(0xFF785500)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFFFF3D6)
val OnTertiaryContainer = Color(0xFF523900)

val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF20231E)
val SurfaceVariantLight = Color(0xFFE9ECE7)
val OnSurfaceVariantLight = Color(0xFF5D6258)
val SurfaceContainerLowLight = Color(0xFFFFFFFF)
val SurfaceContainerLight = Color(0xFFF2F4F0)
val SurfaceContainerHighLight = Color(0xFFEBEEE8)
val SurfaceContainerHighestLight = Color(0xFFE2E7DC)
val OutlineLight = Color(0xFF72786A)

val ExpenseError = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF410002)

private val LightColorScheme = lightColorScheme(
    primary = PitchGreenPrimary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = SportyNavySecondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = TrophyGoldTertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = Color(0xFFF3F4F1),
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    surfaceDim = Color(0xFFE2E7DC),
    surfaceBright = SurfaceLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceTint = PitchGreenPrimary,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    error = ExpenseError,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = OutlineLight,
    outlineVariant = Color(0xFFE2E6DD)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD5F58A),
    onPrimary = Color(0xFF23310F),
    primaryContainer = Color(0xFF384923),
    onPrimaryContainer = Color(0xFFD5F58A),
    secondary = Color(0xFFBFCDA0),
    onSecondary = Color(0xFF29351B),
    secondaryContainer = Color(0xFF3D4A2A),
    onSecondaryContainer = Color(0xFFDDE8C8),
    tertiary = Color(0xFFE3C36C),
    onTertiary = Color(0xFF3C2F00),
    tertiaryContainer = Color(0xFF574500),
    onTertiaryContainer = Color(0xFFFFF0CB),
    background = Color(0xFF181C16),
    onBackground = Color(0xFFE4E7DE),
    surface = Color(0xFF181C16),
    onSurface = Color(0xFFE4E7DE),
    surfaceVariant = Color(0xFF434A3C),
    onSurfaceVariant = Color(0xFFC4CAB9),
    surfaceContainerLowest = Color(0xFF12150F),
    surfaceContainerLow = Color(0xFF22271F),
    surfaceContainer = Color(0xFF282D24),
    surfaceContainerHigh = Color(0xFF31362C),
    surfaceContainerHighest = Color(0xFF3B4136),
    outline = Color(0xFF8F9686),
    outlineVariant = Color(0xFF434A3C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)

@Composable
fun TeamFundTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
