package com.dark.tool_neuron.ui.screens.desktop

import com.dark.tool_neuron.laptop.WorkstationApp
import com.dark.tool_neuron.model.NavScreens
import androidx.navigation.navOptions

object WorkstationRoutes {
    private val routes = mapOf(
        WorkstationApp.VIBE to NavScreens.Vibe.route,
        WorkstationApp.CIPHER to NavScreens.Cipher.route,
        WorkstationApp.CHAT to NavScreens.HomeScreen.route,
        WorkstationApp.PROJECTS to NavScreens.Laptop.route,
        WorkstationApp.TERMINAL to NavScreens.Terminal.route,
        WorkstationApp.MODELS to NavScreens.ModelStore.route,
        WorkstationApp.ABILITIES to NavScreens.Abilities.route,
        WorkstationApp.KNOWLEDGE to NavScreens.Knowledge.route,
        WorkstationApp.FILES to NavScreens.Files.route,
        WorkstationApp.EVIDENCE to NavScreens.Evidence.route,
        WorkstationApp.SETTINGS to NavScreens.Settings.route,
    )

    fun route(app: WorkstationApp): String = routes.getValue(app)
    fun isRoot(route: String?): Boolean = route in routes.values
    fun navigationOptions() = navOptions {
        popUpTo(NavScreens.Desktop.route) { saveState = false }
        launchSingleTop = true
        restoreState = false
    }
    private val modelChildren = setOf(NavScreens.ModelLab.route, NavScreens.ModelConfig.route, NavScreens.ModelManager.route,
        NavScreens.HfExplorer.route, NavScreens.HfRepoDetail.route, NavScreens.Downloads.route)
    private val settingsChildren = setOf(NavScreens.Storage.route, NavScreens.AppGuide.route,
        NavScreens.GuideChat.route, NavScreens.GuideModels.route, NavScreens.GuideRag.route,
        NavScreens.GuideVlm.route, NavScreens.GuideVoice.route, NavScreens.GuideSecurity.route,
        NavScreens.GuideThemes.route, NavScreens.GuideServer.route, NavScreens.GuidePlugins.route,
        NavScreens.GuideImages.route)
    fun app(route: String?): WorkstationApp? = routes.entries.firstOrNull { it.value == route }?.key
        ?: when {
            route in modelChildren -> WorkstationApp.MODELS
            route in settingsChildren || route?.startsWith("settings_") == true -> WorkstationApp.SETTINGS
            else -> null
        }
}
