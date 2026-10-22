package com.dark.tool_neuron.model

data class WorkstationEntryPolicy(
    val termsAccepted: Boolean,
    val onboardingComplete: Boolean,
    val securitySetupDone: Boolean,
    val modelSetupDone: Boolean,
) {
    val workstationReady: Boolean get() = termsAccepted && onboardingComplete && securitySetupDone && modelSetupDone
    fun destination(requiresUnlock: Boolean): String = when {
        requiresUnlock -> NavScreens.PasswordScreen.route
        !termsAccepted -> NavScreens.TermsConditions.route
        !onboardingComplete -> NavScreens.DevNotes.route
        !securitySetupDone -> NavScreens.SetupScreen.route
        !modelSetupDone -> NavScreens.ModelSetup.route
        else -> NavScreens.Desktop.route
    }
}
