package com.topjohnwu.magisk.ui

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.android.material.color.utilities.DynamicColor
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.model.ColorMode
import com.topjohnwu.magisk.core.R as CoreR

@SuppressLint("RestrictedApi")
fun dynamicColorScheme(
    seed: Color,
    isDark: Boolean,
    contrastLevel: Double = 0.0
): ColorScheme {
    val scheme = SchemeTonalSpot(Hct.fromInt(seed.toArgb()), isDark, contrastLevel)
    val dyn = MaterialDynamicColors()
    fun DynamicColor.toColor() = Color(getArgb(scheme))

    return ColorScheme(
        primary = dyn.primary().toColor(),
        onPrimary = dyn.onPrimary().toColor(),
        primaryContainer = dyn.primaryContainer().toColor(),
        onPrimaryContainer = dyn.onPrimaryContainer().toColor(),
        inversePrimary = dyn.inversePrimary().toColor(),
        secondary = dyn.secondary().toColor(),
        onSecondary = dyn.onSecondary().toColor(),
        secondaryContainer = dyn.secondaryContainer().toColor(),
        onSecondaryContainer = dyn.onSecondaryContainer().toColor(),
        tertiary = dyn.tertiary().toColor(),
        onTertiary = dyn.onTertiary().toColor(),
        tertiaryContainer = dyn.tertiaryContainer().toColor(),
        onTertiaryContainer = dyn.onTertiaryContainer().toColor(),
        background = dyn.background().toColor(),
        onBackground = dyn.onBackground().toColor(),
        surface = dyn.surface().toColor(),
        onSurface = dyn.onSurface().toColor(),
        surfaceVariant = dyn.surfaceVariant().toColor(),
        onSurfaceVariant = dyn.onSurfaceVariant().toColor(),
        surfaceTint = dyn.surfaceTint().toColor(),
        inverseSurface = dyn.inverseSurface().toColor(),
        inverseOnSurface = dyn.inverseOnSurface().toColor(),
        error = dyn.error().toColor(),
        onError = dyn.onError().toColor(),
        errorContainer = dyn.errorContainer().toColor(),
        onErrorContainer = dyn.onErrorContainer().toColor(),
        outline = dyn.outline().toColor(),
        outlineVariant = dyn.outlineVariant().toColor(),
        scrim = dyn.scrim().toColor(),
        surfaceBright = dyn.surfaceBright().toColor(),
        surfaceDim = dyn.surfaceDim().toColor(),
        surfaceContainer = dyn.surfaceContainer().toColor(),
        surfaceContainerHigh = dyn.surfaceContainerHigh().toColor(),
        surfaceContainerHighest = dyn.surfaceContainerHighest().toColor(),
        surfaceContainerLow = dyn.surfaceContainerLow().toColor(),
        surfaceContainerLowest = dyn.surfaceContainerLowest().toColor(),
        primaryFixed = dyn.primaryFixed().toColor(),
        primaryFixedDim = dyn.primaryFixedDim().toColor(),
        onPrimaryFixed = dyn.onPrimaryFixed().toColor(),
        onPrimaryFixedVariant = dyn.onPrimaryFixedVariant().toColor(),
        secondaryFixed = dyn.secondaryFixed().toColor(),
        secondaryFixedDim = dyn.secondaryFixedDim().toColor(),
        onSecondaryFixed = dyn.onSecondaryFixed().toColor(),
        onSecondaryFixedVariant = dyn.onSecondaryFixedVariant().toColor(),
        tertiaryFixed = dyn.tertiaryFixed().toColor(),
        tertiaryFixedDim = dyn.tertiaryFixedDim().toColor(),
        onTertiaryFixed = dyn.onTertiaryFixed().toColor(),
        onTertiaryFixedVariant = dyn.onTertiaryFixedVariant().toColor(),
    )
}

val MagiskAccentColor = Color(0xFF1A73E8)

data class MagiskAccentOption(
    val nameRes: Int,
    val color: Color,
)

val MagiskAccentOptions = listOf(
    MagiskAccentOption(CoreR.string.accent_blue, MagiskAccentColor),
    MagiskAccentOption(CoreR.string.accent_purple, Color(0xFF6750A4)),
    MagiskAccentOption(CoreR.string.accent_teal, Color(0xFF006A6A)),
    MagiskAccentOption(CoreR.string.accent_rose, Color(0xFF8E4A60)),
    MagiskAccentOption(CoreR.string.accent_gold, Color(0xFF725D00)),
)

object ThemeState {
    var colorMode by mutableIntStateOf(Config.colorMode)
    var accentColor by mutableIntStateOf(Config.accentColor)
}

val MagiskShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val MagiskTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun MagiskTheme(
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val mode = ColorMode.fromValue(ThemeState.colorMode)
    val context = LocalContext.current

    val isDarkTheme = mode.isDark(isDark)
    val useDynamicColor = mode.isMonet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val accentColor = Color(ThemeState.accentColor)

    val colorScheme = when {
        useDynamicColor && isDarkTheme -> dynamicDarkColorScheme(context)
        useDynamicColor && !isDarkTheme -> dynamicLightColorScheme(context)
        isDarkTheme -> dynamicColorScheme(accentColor, isDark = true)
        else -> dynamicColorScheme(accentColor, isDark = false)
    }

    // Keep Compose on the device's current density and font scale. This is
    // intentionally not a fixed DPI so display-size changes are respected.
    val resources = LocalResources.current
    val deviceDensity = Density(
        density = resources.displayMetrics.density,
        fontScale = resources.configuration.fontScale,
    )

    val activity = LocalActivity.current
    val view = LocalView.current
    if (!view.isInEditMode && activity != null) {
        SideEffect {
            val window = activity.window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDarkTheme
            controller.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    CompositionLocalProvider(LocalDensity provides deviceDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = MagiskShapes,
            typography = MagiskTypography,
            content = content
        )
    }
}
