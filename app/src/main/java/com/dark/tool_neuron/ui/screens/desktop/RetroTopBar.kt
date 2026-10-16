package com.dark.tool_neuron.ui.screens.desktop

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.dark.tool_neuron.ui.components.ActionButton
import com.dark.tool_neuron.ui.icons.TnIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title.uppercase(),
                fontFamily = RetroMono,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Retro.Emerald,
            )
        },
        navigationIcon = {
            ActionButton(
                onClickListener = onBack,
                icon = TnIcons.ArrowLeft,
                contentDescription = "Back",
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Retro.Background),
    )
}
