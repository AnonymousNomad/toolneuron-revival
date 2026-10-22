package com.dark.tool_neuron

import com.dark.tool_neuron.laptop.WorkstationApp
import com.dark.tool_neuron.model.NavScreens
import com.dark.tool_neuron.ui.screens.desktop.WorkstationRoutes
import org.junit.Assert.*
import org.junit.Test

class WorkstationNavigationTest {
    @Test fun switchingWindowsCannotRestoreTheStackJustPopped() {
        val options = WorkstationRoutes.navigationOptions()
        assertFalse(options.shouldRestoreState())
        assertFalse(options.shouldPopUpToSaveState())
        assertTrue(options.shouldLaunchSingleTop())
    }

    @Test fun modelAndSettingsChildrenKeepTheirOwningWindowAndNormalBack() {
        for (route in listOf(NavScreens.ModelConfig.route, NavScreens.ModelManager.route, NavScreens.HfExplorer.route, NavScreens.HfRepoDetail.route, NavScreens.Downloads.route)) {
            assertEquals(WorkstationApp.MODELS, WorkstationRoutes.app(route))
            assertFalse(WorkstationRoutes.isRoot(route))
        }
        assertEquals(WorkstationApp.SETTINGS, WorkstationRoutes.app(NavScreens.Storage.route))
        assertNull(WorkstationRoutes.app(NavScreens.TermsConditions.route))
        assertNull(WorkstationRoutes.app(NavScreens.PasswordScreen.route))
    }

    @Test fun settingsChildrenBelongToSettingsWithoutClaimingOnboarding() {
        assertEquals(WorkstationApp.SETTINGS, WorkstationRoutes.app(NavScreens.SettingsPrivacy.route))
        assertEquals(WorkstationApp.SETTINGS, WorkstationRoutes.app(NavScreens.SettingsDiagnostics.route))
        assertNull(WorkstationRoutes.app(NavScreens.SetupAbilities.route))
        assertFalse(WorkstationRoutes.isRoot(NavScreens.SettingsPrivacy.route))
        assertTrue(WorkstationRoutes.isRoot(NavScreens.Settings.route))
    }
}
