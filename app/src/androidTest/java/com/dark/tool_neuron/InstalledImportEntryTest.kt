package com.dark.tool_neuron

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dark.tool_neuron.ui.screens.model_store.ImportLocalCard
import com.dark.tool_neuron.ui.screens.model_store.InstalledModelsEmptyState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstalledImportEntryTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_showsMessageAndImportAffordance() {
        composeRule.setContent {
            InstalledModelsEmptyState(onImport = {})
        }
        composeRule.onNodeWithText("No installed models").assertIsDisplayed()
        composeRule.onNodeWithText("Import local model").assertIsDisplayed()
    }

    @Test
    fun emptyState_importTap_invokesCallback() {
        var clicked = false
        composeRule.setContent {
            InstalledModelsEmptyState(onImport = { clicked = true })
        }
        composeRule.onNodeWithText("Import local model").performClick()
        composeRule.waitForIdle()
        assertTrue(clicked)
    }

    @Test
    fun importCard_rendersAndInvokes() {
        var clicked = false
        composeRule.setContent {
            ImportLocalCard(onClick = { clicked = true })
        }
        composeRule.onNodeWithText("Import local model").assertIsDisplayed()
        composeRule.onNodeWithText("Pick a .gguf chat or embedding model from storage").assertIsDisplayed()
        composeRule.onNodeWithText("Import local model").performClick()
        composeRule.waitForIdle()
        assertTrue(clicked)
    }
}
