package com.teamfund.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.teamfund.manager.core.designsystem.TeamFundTheme
import com.teamfund.manager.core.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as TeamFundApplication).appContainer

        setContent {
            val themeMode by appContainer.userPreferencesRepository.themeModeFlow.collectAsStateWithLifecycle(initialValue = "SYSTEM")
            val isDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            TeamFundTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    appContainer = appContainer
                )
            }
        }
    }
}
