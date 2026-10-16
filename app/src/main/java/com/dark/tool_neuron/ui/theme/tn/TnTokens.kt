package com.dark.tool_neuron.ui.theme.tn

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Canonical ToolNeuron visual token system.
 *
 * One semantic token set per theme family, resolved per light/dark mode.
 * Legacy Material roles are mapped from these tokens in [TnTheme] so that
 * first-party screens converge on a single visual language.
 */
@Immutable
data class TnThemeTokens(
    val background: Color,
    val panel: Color,
    val panelInset: Color,
    val chrome: Color,
    val border: Color,
    val borderInset: Color,
    val borderFocus: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentDim: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val disabled: Color,
    val control: Color,
    val controlPressed: Color,
    val controlSelected: Color,
    val input: Color,
    val terminalBg: Color,
    val terminalText: Color,
    val glow: Color,
    val isLight: Boolean,
)

enum class TnThemeFamily(val displayName: String, val description: String) {
    COVERT_DEFAULT("Covert Default", "Verified emerald terminal"),
    PARROT_OPS("Parrot Ops", "Khaki field terminal"),
    MATRIX_TERMINAL("Matrix Terminal", "Green on black"),
    AMBER_CRT("Amber CRT", "Warm amber phosphor"),
    GHOST_TERMINAL("Ghost Terminal", "Pale monochrome"),
    RED_TEAM("Red Team", "Red operations deck"),
    BLUE_TEAM("Blue Team", "Blue operations deck"),
    NEON_GRID("Neon Grid", "Magenta synth grid"),
    CYBERDECK("Cyberdeck", "Teal hardware"),
    COMMAND_DECK("Command Deck", "Olive command post"),
    ;

    fun tokens(light: Boolean): TnThemeTokens =
        if (light) lightTokens() else darkTokens()

    private fun darkTokens(): TnThemeTokens = when (this) {
        COVERT_DEFAULT -> dark(
            bg = 0xFF0A0F0A, panel = 0xFF101C13, panelInset = 0xFF0D160F, chrome = 0xFF0C140D,
            border = 0xFF1F4030, borderInset = 0xFF16281E, focus = 0xFF00E08A,
            textPrimary = 0xFFD7E8CE, textSecondary = 0xFF9FB89A, textMuted = 0xFF7FA893,
            accent = 0xFF00E08A, accentDim = 0xFF0E7A50, warning = 0xFFFFC24D, error = 0xFFFF5C5C,
            control = 0xFF13200F, input = 0xFF0E1710, terminalBg = 0xFF070B07, terminalText = 0xFFB9E8C9,
        )
        PARROT_OPS -> dark(
            bg = 0xFF14120A, panel = 0xFF1C1A10, panelInset = 0xFF17150C, chrome = 0xFF191709,
            border = 0xFF3D3A1E, borderInset = 0xFF2C2A15, focus = 0xFFC8E05A,
            textPrimary = 0xFFE8E4C8, textSecondary = 0xFFB8B48E, textMuted = 0xFF8C8A66,
            accent = 0xFFC8E05A, accentDim = 0xFF7A8A2E, warning = 0xFFFFB454, error = 0xFFFF6B5C,
            control = 0xFF23210F, input = 0xFF18160B, terminalBg = 0xFF0C0B05, terminalText = 0xFFD8E8A0,
        )
        MATRIX_TERMINAL -> dark(
            bg = 0xFF000000, panel = 0xFF050A05, panelInset = 0xFF030703, chrome = 0xFF040804,
            border = 0xFF123312, borderInset = 0xFF0C240C, focus = 0xFF33FF66,
            textPrimary = 0xFFB4FFB4, textSecondary = 0xFF7ACC7A, textMuted = 0xFF4F9A4F,
            accent = 0xFF33FF66, accentDim = 0xFF1A7A33, warning = 0xFFD8FF33, error = 0xFFFF4D4D,
            control = 0xFF081408, input = 0xFF040D04, terminalBg = 0xFF000000, terminalText = 0xFF66FF88,
        )
        AMBER_CRT -> dark(
            bg = 0xFF0C0803, panel = 0xFF140E05, panelInset = 0xFF100B04, chrome = 0xFF120C04,
            border = 0xFF3D2A10, borderInset = 0xFF2A1D0B, focus = 0xFFFFB000,
            textPrimary = 0xFFFFE0B0, textSecondary = 0xFFC8A870, textMuted = 0xFF8C7448,
            accent = 0xFFFFB000, accentDim = 0xFF8A5E00, warning = 0xFFFFD24D, error = 0xFFFF5C3C,
            control = 0xFF1A1206, input = 0xFF120D05, terminalBg = 0xFF080502, terminalText = 0xFFFFC860,
        )
        GHOST_TERMINAL -> dark(
            bg = 0xFF10141A, panel = 0xFF161C24, panelInset = 0xFF12181F, chrome = 0xFF141A21,
            border = 0xFF2A3542, borderInset = 0xFF202A35, focus = 0xFF9FB8D0,
            textPrimary = 0xFFDCE6F0, textSecondary = 0xFFA8B6C6, textMuted = 0xFF7A8899,
            accent = 0xFF9FB8D0, accentDim = 0xFF54687D, warning = 0xFFD8C08A, error = 0xFFE08A8A,
            control = 0xFF1A222C, input = 0xFF131921, terminalBg = 0xFF0B0F14, terminalText = 0xFFC8D8E8,
        )
        RED_TEAM -> dark(
            bg = 0xFF0F0505, panel = 0xFF160A0A, panelInset = 0xFF120707, chrome = 0xFF140808,
            border = 0xFF3D1414, borderInset = 0xFF2A0E0E, focus = 0xFFFF4D4D,
            textPrimary = 0xFFF0D0D0, textSecondary = 0xFFC09A9A, textMuted = 0xFF8A6666,
            accent = 0xFFFF4D4D, accentDim = 0xFF8A2626, warning = 0xFFFFB454, error = 0xFFFF7A5C,
            control = 0xFF1E0C0C, input = 0xFF150707, terminalBg = 0xFF0A0303, terminalText = 0xFFFF9A9A,
        )
        BLUE_TEAM -> dark(
            bg = 0xFF050A0F, panel = 0xFF0A1216, panelInset = 0xFF080F13, chrome = 0xFF091014,
            border = 0xFF14303D, borderInset = 0xFF102530, focus = 0xFF4DB8FF,
            textPrimary = 0xFFD0E0F0, textSecondary = 0xFF96B0C6, textMuted = 0xFF647E94,
            accent = 0xFF4DB8FF, accentDim = 0xFF22608A, warning = 0xFFFFC24D, error = 0xFFFF6B5C,
            control = 0xFF0C1820, input = 0xFF08121A, terminalBg = 0xFF03070B, terminalText = 0xFF9AD0FF,
        )
        NEON_GRID -> dark(
            bg = 0xFF0A0614, panel = 0xFF120A20, panelInset = 0xFF0E0718, chrome = 0xFF10091C,
            border = 0xFF2E1A4D, borderInset = 0xFF23133B, focus = 0xFFFF4DD2,
            textPrimary = 0xFFE8D0FF, textSecondary = 0xFFB89AD8, textMuted = 0xFF84689F,
            accent = 0xFFFF4DD2, accentDim = 0xFF8A2672, warning = 0xFFFFC24D, error = 0xFFFF5C6B,
            control = 0xFF180E2A, input = 0xFF100821, terminalBg = 0xFF06030C, terminalText = 0xFFFF9AE8,
        )
        CYBERDECK -> dark(
            bg = 0xFF06100E, panel = 0xFF0A1A16, panelInset = 0xFF081512, chrome = 0xFF091713,
            border = 0xFF14403A, borderInset = 0xFF0F302B, focus = 0xFF00E0C8,
            textPrimary = 0xFFC8F0E8, textSecondary = 0xFF8CC0B8, textMuted = 0xFF5E8C86,
            accent = 0xFF00E0C8, accentDim = 0xFF0A7A6E, warning = 0xFFFFC24D, error = 0xFFFF5C5C,
            control = 0xFF0C221E, input = 0xFF071713, terminalBg = 0xFF030A08, terminalText = 0xFF7AE8D8,
        )
        COMMAND_DECK -> dark(
            bg = 0xFF0E0F0A, panel = 0xFF161810, panelInset = 0xFF12140D, chrome = 0xFF14160E,
            border = 0xFF333A1E, borderInset = 0xFF262B16, focus = 0xFFB8C86A,
            textPrimary = 0xFFDDE0C8, textSecondary = 0xFFAEB294, textMuted = 0xFF7E8268,
            accent = 0xFFB8C86A, accentDim = 0xFF6A7434, warning = 0xFFFFB454, error = 0xFFFF6B5C,
            control = 0xFF1D2011, input = 0xFF15170C, terminalBg = 0xFF090A05, terminalText = 0xFFC8D890,
        )
    }

    /** Paper/light variant derived deterministically from the family's dark identity. */
    fun lightTokens(): TnThemeTokens {
        val darkT = darkTokens()
        val paper = when (this) {
            COVERT_DEFAULT -> Color(0xFFEFF2EA)
            PARROT_OPS -> Color(0xFFF2F0E2)
            MATRIX_TERMINAL -> Color(0xFFEDF5ED)
            AMBER_CRT -> Color(0xFFF7F0E4)
            GHOST_TERMINAL -> Color(0xFFF0F2F5)
            RED_TEAM -> Color(0xFFF5EDED)
            BLUE_TEAM -> Color(0xFFEDF2F7)
            NEON_GRID -> Color(0xFFF3EDF7)
            CYBERDECK -> Color(0xFFEAF4F2)
            COMMAND_DECK -> Color(0xFFF0F1E6)
        }
        val accentLight = darkT.accent.mix(Color.Black, accentLightenFactor())
        return TnThemeTokens(
            background = paper,
            panel = paper.mix(Color.White, 0.55f),
            panelInset = paper.mix(Color.Black, 0.05f),
            chrome = paper.mix(Color.Black, 0.08f),
            border = paper.mix(Color.Black, 0.28f),
            borderInset = paper.mix(Color.Black, 0.16f),
            borderFocus = accentLight,
            textPrimary = paper.mix(Color.Black, 0.92f),
            textSecondary = paper.mix(Color.Black, 0.66f),
            textMuted = paper.mix(Color.Black, 0.46f),
            accent = accentLight,
            accentDim = darkT.accentDim.mix(Color.White, 0.15f).mix(Color.Black, 0.25f),
            success = accentLight,
            warning = darkT.warning.mix(Color.Black, 0.35f),
            error = darkT.error.mix(Color.Black, 0.30f),
            disabled = paper.mix(Color.Black, 0.38f),
            control = paper.mix(Color.Black, 0.06f),
            controlPressed = paper.mix(Color.Black, 0.12f),
            controlSelected = accentLight.mix(paper, 0.80f),
            input = paper.mix(Color.White, 0.75f),
            terminalBg = paper.mix(Color.Black, 0.92f),
            terminalText = accentLight.mix(Color.White, 0.55f),
            glow = accentLight,
            isLight = true,
        )
    }

    private fun accentLightenFactor(): Float = when (this) {
        MATRIX_TERMINAL -> 0.55f
        NEON_GRID -> 0.50f
        AMBER_CRT, CYBERDECK, COVERT_DEFAULT, BLUE_TEAM -> 0.45f
        else -> 0.42f
    }

    private fun dark(
        bg: Long, panel: Long, panelInset: Long, chrome: Long,
        border: Long, borderInset: Long, focus: Long,
        textPrimary: Long, textSecondary: Long, textMuted: Long,
        accent: Long, accentDim: Long, warning: Long, error: Long,
        control: Long, input: Long, terminalBg: Long, terminalText: Long,
    ): TnThemeTokens = TnThemeTokens(
        background = Color(bg),
        panel = Color(panel),
        panelInset = Color(panelInset),
        chrome = Color(chrome),
        border = Color(border),
        borderInset = Color(borderInset),
        borderFocus = Color(focus),
        textPrimary = Color(textPrimary),
        textSecondary = Color(textSecondary),
        textMuted = Color(textMuted),
        accent = Color(accent),
        accentDim = Color(accentDim),
        success = Color(accent),
        warning = Color(warning),
        error = Color(error),
        disabled = Color(textMuted).mix(Color(0xFF000000), 0.35f),
        control = Color(control),
        controlPressed = Color(control).mix(Color.White, 0.08f),
        controlSelected = Color(control).mix(Color(accent), 0.18f),
        input = Color(input),
        terminalBg = Color(terminalBg),
        terminalText = Color(terminalText),
        glow = Color(accent),
        isLight = false,
    )
}

fun Color.mix(other: Color, t: Float): Color = Color(
    red = red + (other.red - red) * t,
    green = green + (other.green - green) * t,
    blue = blue + (other.blue - blue) * t,
    alpha = alpha + (other.alpha - alpha) * t,
)

fun Color.relativeLuminance(): Float =
    0.2126f * channelLum(red) + 0.7152f * channelLum(green) + 0.0722f * channelLum(blue)

private fun channelLum(c: Float): Float =
    if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()

fun contrastRatio(a: Color, b: Color): Float {
    val l1 = a.relativeLuminance()
    val l2 = b.relativeLuminance()
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}
