package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val RepPulsMinimalColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = TextOnSolidWhite,
    primaryContainer = SoftBlueTint,
    onPrimaryContainer = RoyalBlue,
    secondary = BrightBlue,
    onSecondary = TextOnSolidWhite,
    secondaryContainer = CardIceBlue,
    onSecondaryContainer = TextPrimaryNavy,
    tertiary = TealGreen,
    onTertiary = TextOnSolidWhite,
    tertiaryContainer = TealSoftTint,
    onTertiaryContainer = TealGreen,
    background = BgPureWhite,
    onBackground = TextPrimaryNavy,
    surface = CardIceBlue,
    onSurface = TextPrimaryNavy,
    surfaceVariant = CardIceBlue,
    onSurfaceVariant = TextSecondarySlate,
    surfaceContainerLowest = BgPureWhite,
    surfaceContainerLow = CardIceBlue,
    surfaceContainer = CardIceBlue,
    surfaceContainerHigh = CardIceBlueActive,
    surfaceContainerHighest = SoftBlueTint,
    outline = BorderBlueFocus,
    outlineVariant = BorderIceBlue,
    error = SubtleDeleteRed,
    onError = TextOnSolidWhite,
    errorContainer = SubtleDeleteBg,
    onErrorContainer = SubtleDeleteRed
)

val RepPulsShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun RepPulsTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = RepPulsMinimalColorScheme,
        typography = RepPulsTypography,
        shapes = RepPulsShapes,
        content = content
    )
}
