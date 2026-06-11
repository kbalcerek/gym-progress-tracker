package com.example.gymprogresstracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gymprogresstracker.GymApplication
import com.example.gymprogresstracker.R
import com.example.gymprogresstracker.ui.bodyweight.BodyweightScreen
import com.example.gymprogresstracker.ui.exercise.ExerciseScreen
import com.example.gymprogresstracker.ui.log.WorkoutLogScreen
import com.example.gymprogresstracker.ui.progress.ProgressScreen
import com.example.gymprogresstracker.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Log : Screen("log")
    object Bodyweight : Screen("bodyweight")
    object Progress : Screen("progress")
    object Exercises : Screen("exercises")
    object Settings : Screen("settings")
}

@Composable
fun GymNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val app = LocalContext.current.applicationContext as GymApplication

    val bottomItems = listOf(Screen.Log, Screen.Bodyweight, Screen.Progress, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            when (screen) {
                                Screen.Log -> Icon(Icons.Default.FitnessCenter, contentDescription = null)
                                Screen.Bodyweight -> Icon(Icons.Default.MonitorWeight, contentDescription = null)
                                Screen.Progress -> Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null)
                                Screen.Settings -> Icon(Icons.Default.Settings, contentDescription = null)
                                else -> {}
                            }
                        },
                        label = {
                            when (screen) {
                                Screen.Log -> Text(stringResource(R.string.nav_log))
                                Screen.Bodyweight -> Text(stringResource(R.string.nav_bodyweight))
                                Screen.Progress -> Text(stringResource(R.string.nav_progress))
                                Screen.Settings -> Text(stringResource(R.string.nav_settings))
                                else -> {}
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Log.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Log.route) {
                WorkoutLogScreen(
                    repository = app.repository,
                    onManageExercises = { navController.navigate(Screen.Exercises.route) }
                )
            }
            composable(Screen.Bodyweight.route) {
                BodyweightScreen(repository = app.repository)
            }
            composable(Screen.Progress.route) {
                ProgressScreen(repository = app.repository)
            }
            composable(Screen.Exercises.route) {
                ExerciseScreen(
                    repository = app.repository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(backupManager = app.backupManager)
            }
        }
    }
}
