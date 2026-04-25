package com.awper.lightscore.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.awper.lightscore.settings.SettingsStore
import com.awper.lightscore.ui.game.GameScreen
import com.awper.lightscore.ui.home.HomeScreen
import com.awper.lightscore.ui.settings.SettingsScreen
import com.awper.lightscore.ui.settings.SettingsViewModel
import com.awper.lightscore.ui.settings.SettingsViewModelFactory
import com.awper.lightscore.ui.settings.TeamPickerScreen
import com.awper.lightscore.ui.settings.TeamPickerViewModel
import com.awper.lightscore.ui.settings.TeamPickerViewModelFactory
import com.awper.lightscore.ui.stats.StatsScreen

@Composable
fun LightscoreApp(settingsStore: SettingsStore) {
    val navController = rememberNavController()
    val keepScreenAwake by settingsStore.keepScreenAwake.collectAsState(initial = false)
    val lowDataMode by settingsStore.lowDataMode.collectAsState(initial = false)
    val pinFavorites by settingsStore.pinFavorites.collectAsState(initial = true)
    val favoriteTeamIds by settingsStore.favorites.collectAsState(initial = emptyList())
    LocalView.current.keepScreenOn = keepScreenAwake
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                lowDataMode = lowDataMode,
                favoriteTeamIds = favoriteTeamIds.toSet(),
                pinFavorites = pinFavorites,
                onNavigateToGame = { gamePk ->
                    navController.navigate("game/$gamePk") { launchSingleTop = true }
                },
                onSwipeToStats = {
                    navController.navigate("stats") { launchSingleTop = true }
                },
                onSwipeToSettings = {
                    navController.navigate("settings") { launchSingleTop = true }
                }
            )
        }
        composable("stats") {
            StatsScreen(onBack = {
                if (!navController.popBackStack("home", false)) {
                    navController.navigate("home") { launchSingleTop = true }
                }
            })
        }
        composable("game/{gamePk}", arguments = listOf(navArgument("gamePk") { type = NavType.IntType })) {
            val gamePk = it.arguments?.getInt("gamePk") ?: 0
            GameScreen(gamePk = gamePk, lowDataMode = lowDataMode, onBack = {
                if (!navController.popBackStack("home", false)) {
                    navController.navigate("home") { launchSingleTop = true }
                }
            })
        }
        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(settingsStore)
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = {
                    if (!navController.popBackStack("home", false)) {
                        navController.navigate("home") { launchSingleTop = true }
                    }
                },
                onNavigateToTeamPicker = {
                    navController.navigate("teamPicker") { launchSingleTop = true }
                }
            )
        }
        composable("teamPicker") {
            val teamPickerViewModel: TeamPickerViewModel = viewModel(
                factory = TeamPickerViewModelFactory(settingsStore)
            )
            TeamPickerScreen(viewModel = teamPickerViewModel, onBack = {
                if (!navController.popBackStack("settings", false)) {
                    navController.navigate("settings") { launchSingleTop = true }
                }
            })
        }
    }
}
