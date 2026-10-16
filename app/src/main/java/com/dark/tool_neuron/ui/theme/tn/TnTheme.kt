package com.dark.tool_neuron.ui.theme.tn

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dark.tool_neuron.ui.theme.FigtreeFontFamily
import com.dark.tool_neuron.ui.theme.MapleMonoFontFamily

@Immutable
data class TnEffects(
    val crtScanlines: Boolean = false,
    val glow: Boolean = false,
) {
    companion object {
        val NONE = TnEffects()
    }
}

enum class TnFontProfile(val displayName: String, val description: String) {
    CHROME_MONO("Chrome mono", "Monospace chrome, readable body"),
    FULL_MONO("Full mono", "Monospace everywhere"),
}

val LocalTnTokens = staticCompositionLocalOf { TnThemeFamily.COVERT_DEFAULT.tokens(light = false) }
val LocalTnMono = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }
val LocalTnEffects = staticCompositionLocalOf { TnEffects.NONE }
val LocalTnFontProfile = staticCompositionLocalOf { TnFontProfile.CHROME_MONO }

/** Canonical access point for theme tokens (mirrors MaterialTheme ergonomics). */
object TnTheme {
    val tokens: TnThemeTokens
        @Composable @ReadOnlyComposable get() = LocalTnTokens.current

    val mono: FontFamily
        @Composable @ReadOnlyComposable get() = LocalTnMono.current

    val effects: TnEffects
        @Composable @ReadOnlyComposable get() = LocalTnEffects.current

    val fontProfile: TnFontProfile
        @Composable @ReadOnlyComposable get() = LocalTnFontProfile.current
}

/** Hard-rect radius policy: panels 0dp, controls 2dp, dialogs 4dp. */
val TnShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp),
)

private fun tnTypography(profile: TnFontProfile): Typography {
    val base = Typography()
    val chrome: FontFamily = MapleMonoFontFamily
    val body: FontFamily = when (profile) {
        TnFontProfile.CHROME_MONO -> FigtreeFontFamily
        TnFontProfile.FULL_MONO -> MapleMonoFontFamily
    }
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = chrome, fontWeight = FontWeight.Medium),
        displayMedium = base.displayMedium.copy(fontFamily = chrome, fontWeight = FontWeight.Medium),
        displaySmall = base.displaySmall.copy(fontFamily = chrome, fontWeight = FontWeight.Medium),
        headlineLarge = base.headlineLarge.copy(fontFamily = chrome, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = chrome, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = chrome, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = chrome, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = chrome, fontWeight = FontWeight.Medium),
        titleSmall = base.titleSmall.copy(fontFamily = chrome, fontWeight = FontWeight.Medium),
        labelLarge = base.labelLarge.copy(fontFamily = chrome, fontWeight = FontWeight.Medium),
        labelMedium = base.labelMedium.copy(fontFamily = chrome, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontFamily = chrome, fontWeight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.copy(fontFamily = body, fontWeight = FontWeight.Normal),
        bodyMedium = base.bodyMedium.copy(fontFamily = body, fontWeight = FontWeight.Normal),
        bodySmall = base.bodySmall.copy(fontFamily = body, fontWeight = FontWeight.Normal),
    )
}

private fun onColorFor(c: Color): Color =
    if (c.relativeLuminance() > 0.4f) Color(0xFF06110A) else Color(0xFFF4F8F0)

@Composable
fun TnTheme(
    family: TnThemeFamily = TnThemeFamily.COVERT_DEFAULT,
    dark: Boolean = true,
    fontProfile: TnFontProfile = TnFontProfile.CHROME_MONO,
    effects: TnEffects = TnEffects.NONE,
    content: @Composable () -> Unit,
) {
    val tokens = family.tokens(light = !dark)
    val scheme = MaterialTheme.colorScheme.copy(
        primary = tokens.accent,
        onPrimary = onColorFor(tokens.accent),
        primaryContainer = tokens.accentDim,
        onPrimaryContainer = if (tokens.isLight) tokens.textPrimary else tokens.textPrimary,
        secondary = tokens.accentDim,
        onSecondary = onColorFor(tokens.accentDim),
        secondaryContainer = tokens.controlSelected,
        onSecondaryContainer = tokens.textPrimary,
        tertiary = tokens.accent,
        onTertiary = onColorFor(tokens.accent),
        background = tokens.background,
        onBackground = tokens.textPrimary,
        surface = tokens.panel,
        onSurface = tokens.textPrimary,
        surfaceVariant = tokens.panelInset,
        onSurfaceVariant = tokens.textSecondary,
        surfaceTint = tokens.accent,
        inverseSurface = tokens.textPrimary,
        inverseOnSurface = tokens.background,
        inversePrimary = tokens.accentDim,
        error = tokens.error,
        onError = onColorFor(tokens.error),
        errorContainer = tokens.error.mix(tokens.panel, 0.75f),
        onErrorContainer = tokens.error,
        outline = tokens.border,
        outlineVariant = tokens.borderInset,
        scrim = Color(0xCC000000),
        surfaceBright = tokens.panel.mix(Color.White, if (tokens.isLight) 0.15f else 0.08f),
        surfaceDim = tokens.background.mix(Color.Black, 0.25f),
        surfaceContainer = tokens.panel,
        surfaceContainerHigh = tokens.panel.mix(Color.White, 0.06f),
        surfaceContainerHighest = tokens.panel.mix(Color.White, 0.10f),
        surfaceContainerLow = tokens.panelInset,
        surfaceContainerLowest = tokens.background,
    )
    CompositionLocalProvider(
        LocalTnTokens provides tokens,
        LocalTnMono provides MapleMonoFontFamily,
        LocalTnEffects provides effects,
        LocalTnFontProfile provides fontProfile,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = TnShapes,
            typography = tnTypography(fontProfile),
            content = content,
        )
    }
}
