package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val ShieldDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = CyanSecondary,
    onSecondary = CyanOnSecondary,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = CyanOnContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    error = CrimsonError,
    onError = CrimsonOnError,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonOnContainer,
    background = ObsidianBg,
    onBackground = SlateTextPrimary,
    surface = ObsidianSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = SlateTextSecondary,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutline.copy(alpha = 0.5f)
)

private val ShieldLightColorScheme = lightColorScheme(
    primary = TealLightPrimary,
    onPrimary = TealLightOnPrimary,
    primaryContainer = TealLightContainer,
    onPrimaryContainer = TealLightOnContainer,
    secondary = CobaltLightSecondary,
    onSecondary = CobaltLightOnSecondary,
    secondaryContainer = CobaltLightContainer,
    onSecondaryContainer = CobaltLightOnContainer,
    tertiary = AmberLightTertiary,
    onTertiary = AmberLightOnTertiary,
    tertiaryContainer = AmberLightContainer,
    onTertiaryContainer = AmberLightOnContainer,
    error = CrimsonLightError,
    onError = CrimsonLightOnError,
    errorContainer = CrimsonLightContainer,
    onErrorContainer = CrimsonLightOnContainer,
    background = IceBg,
    onBackground = LightTextPrimary,
    surface = IceSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = IceSurfaceHigh,
    onSurfaceVariant = LightTextSecondary,
    outline = IceOutline,
    outlineVariant = IceOutline.copy(alpha = 0.6f)
)

val ShieldShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ShieldCoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ShieldDarkColorScheme
        else -> ShieldLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShieldTypography,
        shapes = ShieldShapes,
        content = content
    )
}
