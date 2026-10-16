package com.dark.tool_neuron.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.tool_neuron.data.ThemeController
import com.dark.tool_neuron.service.inference.InferenceClient
import com.dark.tool_neuron.ui.screens.system_ui.AppScaffold
import com.dark.tool_neuron.ui.theme.ToolNeuronTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeController: ThemeController

    override fun onStart() {
        super.onStart()
        InferenceClient.startAndBind(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inference service binding is owned by TNApplication so background
        // work (RAG ingest, SD pipeline, scheduled jobs) doesn't lose the
        // service the moment MainActivity is recreated or finishes.
        enableEdgeToEdge()
        setContent {
            val mode by themeController.mode.collectAsStateWithLifecycle()
            val family by themeController.family.collectAsStateWithLifecycle()
            val fontProfile by themeController.fontProfile.collectAsStateWithLifecycle()
            val effects by themeController.effects.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (mode) {
                ThemeController.Mode.SYSTEM -> systemDark
                ThemeController.Mode.LIGHT -> false
                ThemeController.Mode.DARK -> true
            }
            ToolNeuronTheme(
                darkTheme = darkTheme,
                family = family,
                fontProfile = fontProfile,
                effects = effects,
            ) {
                AppScaffold()
            }
        }
    }

}
