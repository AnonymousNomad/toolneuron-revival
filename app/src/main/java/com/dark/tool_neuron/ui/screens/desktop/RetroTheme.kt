package com.dark.tool_neuron.ui.screens.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dark.tool_neuron.ui.theme.tn.LocalTnTokens
import com.dark.tool_neuron.ui.theme.tn.TnTheme

/**
 * Retro design-language accessors. Backed by the canonical ToolNeuron token
 * system ([TnTheme.tokens]) so every retro surface follows the active theme
 * family instead of a fixed palette.
 */
object Retro {
    val Background: Color
        @Composable @ReadOnlyComposable get() = LocalTnTokens.current.background
    val Panel: Color
        @Composable @ReadOnlyComposable get() = LocalTnTokens.current.panel
    val PanelAlt: Color
        @Composable @ReadOnlyComposable get() = LocalTnTokens.current.panelInset
    val Border: Color
        @Composable @ReadOnlyComposable get() = LocalTnTokens.current.border
    val Emerald: Color
        @Composable @ReadOnlyComposable get() = LocalTnTokens.current.accent
    val EmeraldDim: Color
        @Composable @ReadOnlyComposable get() = LocalTnTokens.current.accentDim
    val Amber: Color
        @Composable @ReadOnlyComposable get() = LocalTnTokens.current.warning
    val TextDim: Color
        @Composable @ReadOnlyComposable get() = LocalTnTokens.current.textSecondary
    val Error: Color
        @Composable @ReadOnlyComposable get() = LocalTnTokens.current.error
    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() = LocalTnTokens.current.textPrimary
}

val RetroMono: FontFamily
    @Composable @ReadOnlyComposable get() = TnTheme.mono

@Composable
fun RetroPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = Retro.Border,
    background: Color = Retro.Panel,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = background,
        shape = RectangleShape,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(modifier = Modifier.padding(10.dp), content = content)
    }
}

@Composable
fun RetroButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = Retro.Emerald,
) {
    val color = if (enabled) accent else Retro.TextDim
    Surface(
        modifier = modifier.clickable(enabled = enabled) { onClick() },
        color = Retro.PanelAlt,
        shape = RectangleShape,
        border = BorderStroke(1.dp, if (enabled) accent else Retro.Border),
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontFamily = RetroMono,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
fun RetroIconTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Retro.Emerald,
    note: String? = null,
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            color = Retro.PanelAlt,
            shape = RectangleShape,
            border = BorderStroke(1.dp, Retro.Border),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp),
                tint = accent,
            )
        }
        Text(
            text = label.uppercase(),
            fontFamily = RetroMono,
            style = MaterialTheme.typography.labelSmall,
            color = Retro.Emerald,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        if (note != null) {
            Text(
                text = note,
                fontFamily = RetroMono,
                style = MaterialTheme.typography.labelSmall,
                color = Retro.TextDim,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun RetroHeader(title: String, right: String? = null) {
    Surface(color = Retro.Panel, shape = RectangleShape, border = BorderStroke(1.dp, Retro.Border)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.uppercase(),
                fontFamily = RetroMono,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Retro.Emerald,
            )
            if (right != null) {
                Text(
                    text = right,
                    fontFamily = RetroMono,
                    style = MaterialTheme.typography.labelSmall,
                    color = Retro.TextDim,
                )
            }
        }
    }
}

@Composable
fun RetroStatusDot(ok: Boolean) {
    Spacer(
        modifier = Modifier
            .size(8.dp)
            .padding(0.dp),
    )
    Surface(color = if (ok) Retro.Emerald else Retro.Error, shape = RectangleShape) {
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
fun RetroKeyValue(key: String, value: String, valueColor: Color = Retro.Emerald) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = key.uppercase() + ":",
            fontFamily = RetroMono,
            style = MaterialTheme.typography.labelSmall,
            color = Retro.TextDim,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            fontFamily = RetroMono,
            style = MaterialTheme.typography.labelSmall,
            color = valueColor,
        )
    }
}

@Composable
fun RetroDivider() {
    Surface(color = Retro.Border, shape = RectangleShape) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp),
        )
    }
}
