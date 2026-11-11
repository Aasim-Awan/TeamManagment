package com.teamfund.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.teamfund.manager.core.navigation.TeamFundNavigationBar
import com.teamfund.manager.core.navigation.topLevelRoutes
import com.teamfund.manager.core.designsystem.TeamFundTheme
import com.teamfund.manager.core.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as TeamFundApplication).appContainer

        setContent {
            val themeMode by appContainer.userPreferencesRepository.themeModeFlow.collectAsStateWithLifecycle(initialValue = "LIGHT")
            val isDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            SideEffect {
                val barStyle = if (isDarkTheme) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }

            TeamFundTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val entry by navController.currentBackStackEntryAsState()
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (entry?.destination?.route in topLevelRoutes) {
                            TeamFundNavigationBar(entry?.destination?.route) { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        AppNavigation(navController = navController, appContainer = appContainer)
                    }
                }
            }
        }
    }
}
