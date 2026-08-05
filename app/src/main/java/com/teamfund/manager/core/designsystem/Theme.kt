package com.teamfund.manager.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
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

// Cricket & Corporate Sports Brand Colors
val PitchGreenPrimary = Color(0xFF0F6D21)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFD4F8CD)
val OnPrimaryContainer = Color(0xFF002205)

val SportyNavySecondary = Color(0xFF283B70)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFDBE1FF)
val OnSecondaryContainer = Color(0xFF00174B)

val TrophyGoldTertiary = Color(0xFF825500)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFFFDDB3)
val OnTertiaryContainer = Color(0xFF291800)

val SurfaceLight = Color(0xFFF6FBF3)
val OnSurfaceLight = Color(0xFF181D17)
val SurfaceVariantLight = Color(0xFFDEE5D9)
val OnSurfaceVariantLight = Color(0xFF424940)
val SurfaceContainerLowLight = Color(0xFFF0F5ED)
val SurfaceContainerLight = Color(0xFFEAEFE7)
val SurfaceContainerHighLight = Color(0xFFE4E9E1)
val SurfaceContainerHighestLight = Color(0xFFDEE4DC)
val OutlineLight = Color(0xFF727A6F)

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
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
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
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF88D982),
    onPrimary = Color(0xFF003908),
    primaryContainer = Color(0xFF005312),
    onPrimaryContainer = Color(0xFFB8F5B1),
    secondary = Color(0xFFBAC3FF),
    onSecondary = Color(0xFF132863),
    secondaryContainer = Color(0xFF2D407A),
    onSecondaryContainer = Color(0xFFDEE1FF),
    tertiary = Color(0xFFE3C36C),
    onTertiary = Color(0xFF3C2F00),
    tertiaryContainer = Color(0xFF574500),
    onTertiaryContainer = Color(0xFFFFF0CB),
    background = Color(0xFF10140F),
    onBackground = Color(0xFFE0E4DA),
    surface = Color(0xFF10140F),
    onSurface = Color(0xFFE0E4DA),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD),
    surfaceContainerLow = Color(0xFF181D17),
    surfaceContainer = Color(0xFF1C211B),
    surfaceContainerHigh = Color(0xFF272B25),
    surfaceContainerHighest = Color(0xFF31362F),
    outline = Color(0xFF8C9388),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.25).sp
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
        content = content
    )
}
