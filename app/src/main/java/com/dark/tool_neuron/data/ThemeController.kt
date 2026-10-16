package com.dark.tool_neuron.data

import com.dark.tool_neuron.ui.theme.ColorPalette
import com.dark.tool_neuron.ui.theme.tn.TnEffects
import com.dark.tool_neuron.ui.theme.tn.TnFontProfile
import com.dark.tool_neuron.ui.theme.tn.TnThemeFamily
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeController @Inject constructor(
    private val prefs: AppPreferences,
) {
    enum class Mode { SYSTEM, LIGHT, DARK }

    private val _mode = MutableStateFlow(readMode())
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    private val _palette = MutableStateFlow(readPalette())
    val palette: StateFlow<ColorPalette> = _palette.asStateFlow()

    private val _family = MutableStateFlow(readFamily())
    val family: StateFlow<TnThemeFamily> = _family.asStateFlow()

    private val _fontProfile = MutableStateFlow(readFontProfile())
    val fontProfile: StateFlow<TnFontProfile> = _fontProfile.asStateFlow()

    private val _effects = MutableStateFlow(readEffects())
    val effects: StateFlow<TnEffects> = _effects.asStateFlow()

    fun setMode(value: Mode) {
        prefs.putString(KEY_MODE, value.name)
        _mode.value = value
    }

    fun setPalette(value: ColorPalette) {
        prefs.putString(KEY_PALETTE, value.name)
        _palette.value = value
    }

    fun setFamily(value: TnThemeFamily) {
        prefs.putString(KEY_FAMILY, value.name)
        _family.value = value
    }

    fun setFontProfile(value: TnFontProfile) {
        prefs.putString(KEY_FONT_PROFILE, value.name)
        _fontProfile.value = value
    }

    fun setEffects(value: TnEffects) {
        prefs.putBoolean(KEY_EFFECTS_CRT, value.crtScanlines)
        prefs.putBoolean(KEY_EFFECTS_GLOW, value.glow)
        _effects.value = value
    }

    private fun readMode(): Mode {
        val raw = prefs.getString(KEY_MODE, Mode.SYSTEM.name)
        return runCatching { Mode.valueOf(raw) }.getOrDefault(Mode.SYSTEM)
    }

    private fun readPalette(): ColorPalette {
        val raw = prefs.getString(KEY_PALETTE, ColorPalette.DYNAMIC.name)
        return runCatching { ColorPalette.valueOf(raw) }.getOrDefault(ColorPalette.DYNAMIC)
    }

    private fun readFamily(): TnThemeFamily {
        val raw = prefs.getString(KEY_FAMILY, "")
        if (raw.isNotBlank()) {
            return runCatching { TnThemeFamily.valueOf(raw) }.getOrDefault(TnThemeFamily.COVERT_DEFAULT)
        }
        return legacyFamilyFor(readPalette())
    }

    private fun readFontProfile(): TnFontProfile {
        val raw = prefs.getString(KEY_FONT_PROFILE, TnFontProfile.CHROME_MONO.name)
        return runCatching { TnFontProfile.valueOf(raw) }.getOrDefault(TnFontProfile.CHROME_MONO)
    }

    private fun readEffects(): TnEffects = TnEffects(
        crtScanlines = prefs.getBoolean(KEY_EFFECTS_CRT, false),
        glow = prefs.getBoolean(KEY_EFFECTS_GLOW, false),
    )

    companion object {
        private const val KEY_MODE = "theme_mode"
        private const val KEY_PALETTE = "theme_palette"
        private const val KEY_FAMILY = "theme_family"
        private const val KEY_FONT_PROFILE = "theme_font_profile"
        private const val KEY_EFFECTS_CRT = "theme_effects_crt"
        private const val KEY_EFFECTS_GLOW = "theme_effects_glow"

        /** One-time migration path for installs that only ever chose a legacy palette. */
        fun legacyFamilyFor(palette: ColorPalette): TnThemeFamily = when (palette) {
            ColorPalette.DYNAMIC -> TnThemeFamily.COVERT_DEFAULT
            ColorPalette.NEON_LIME -> TnThemeFamily.PARROT_OPS
            ColorPalette.OCEAN_CYAN -> TnThemeFamily.BLUE_TEAM
            ColorPalette.VIOLET_DUSK -> TnThemeFamily.NEON_GRID
            ColorPalette.AMBER_RUST -> TnThemeFamily.AMBER_CRT
            ColorPalette.ROSE_PINK -> TnThemeFamily.RED_TEAM
            ColorPalette.MONO_SLATE -> TnThemeFamily.GHOST_TERMINAL
        }
    }
}
