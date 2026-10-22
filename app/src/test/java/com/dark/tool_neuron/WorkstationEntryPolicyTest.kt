package com.dark.tool_neuron

import com.dark.tool_neuron.model.NavScreens
import com.dark.tool_neuron.model.WorkstationEntryPolicy
import org.junit.Assert.*
import org.junit.Test

class WorkstationEntryPolicyTest {
    @Test fun aConfiguredLockAlwaysRequiresAuthenticationBeforeEntry() {
        for (complete in listOf(false, true)) {
            val progress = WorkstationEntryPolicy(complete, complete, complete, complete)
            assertEquals(NavScreens.PasswordScreen.route, progress.destination(requiresUnlock = true))
        }
    }
    @Test fun unlockingResumesUnfinishedSetupInsteadOfSkippingAbilities() {
        val unfinished = WorkstationEntryPolicy(true, true, true, false)
        assertEquals(NavScreens.ModelSetup.route, unfinished.destination(requiresUnlock = false))
        assertFalse(unfinished.workstationReady)
        val complete = unfinished.copy(modelSetupDone = true)
        assertEquals(NavScreens.Desktop.route, complete.destination(false))
        assertTrue(complete.workstationReady)
    }
    @Test fun setupRoutesFollowPersistedProgressInOrder() {
        assertEquals(NavScreens.TermsConditions.route, WorkstationEntryPolicy(false, false, false, false).destination(false))
        assertEquals(NavScreens.DevNotes.route, WorkstationEntryPolicy(true, false, false, false).destination(false))
        assertEquals(NavScreens.SetupScreen.route, WorkstationEntryPolicy(true, true, false, false).destination(false))
    }
}
