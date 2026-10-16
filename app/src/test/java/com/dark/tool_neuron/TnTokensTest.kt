package com.dark.tool_neuron

import com.dark.tool_neuron.ui.theme.tn.TnThemeFamily
import com.dark.tool_neuron.ui.theme.tn.contrastRatio
import com.dark.tool_neuron.ui.theme.tn.mix
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TnTokensTest {

    @Test
    fun everyFamilyDefinesDarkAndLightTokens() {
        for (family in TnThemeFamily.entries) {
            val dark = family.tokens(light = false)
            val light = family.tokens(light = true)
            assertTrue("${family.name} dark should not be light", !dark.isLight)
            assertTrue("${family.name} light should be light", light.isLight)
        }
        assertEquals(10, TnThemeFamily.entries.size)
    }

    @Test
    fun textPrimaryMeetsReadableContrastInAllVariants() {
        for (family in TnThemeFamily.entries) {
            for (light in listOf(false, true)) {
                val t = family.tokens(light)
                val ratio = contrastRatio(t.textPrimary, t.background)
                assertTrue(
                    "${family.name} light=$light textPrimary contrast $ratio below 4.5",
                    ratio >= 4.5f,
                )
                val secondary = contrastRatio(t.textSecondary, t.background)
                assertTrue(
                    "${family.name} light=$light textSecondary contrast $secondary below 3.0",
                    secondary >= 3.0f,
                )
            }
        }
    }

    @Test
    fun accentMeetsInterfaceContrastInAllVariants() {
        for (family in TnThemeFamily.entries) {
            for (light in listOf(false, true)) {
                val t = family.tokens(light)
                val ratio = contrastRatio(t.accent, t.background)
                assertTrue(
                    "${family.name} light=$light accent contrast $ratio below 3.0",
                    ratio >= 3.0f,
                )
            }
        }
    }

    @Test
    fun lightVariantIsDeterministic() {
        val a = TnThemeFamily.COVERT_DEFAULT.tokens(light = true)
        val b = TnThemeFamily.COVERT_DEFAULT.tokens(light = true)
        assertEquals(a, b)
    }

    @Test
    fun mixProducesBoundedInterpolation() {
        val black = Color(0xFF000000)
        val white = Color(0xFFFFFFFF)
        assertEquals(black, black.mix(white, 0f))
        assertEquals(white, black.mix(white, 1f))
    }
}
