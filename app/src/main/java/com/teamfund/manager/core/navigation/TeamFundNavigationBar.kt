package com.teamfund.manager.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

val topLevelRoutes = setOf(
    Screen.Dashboard.route, Screen.TeamRoster.route, Screen.Matches.route, Screen.AllExpenses.route, Screen.Reports.route
)

@Composable
fun TeamFundNavigationBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val destinations = listOf(
        Triple(Screen.Dashboard.route, "Home", Icons.Outlined.Home),
        Triple(Screen.TeamRoster.route, "Players", Icons.Outlined.Group),
        Triple(Screen.Matches.route, "Matches", Icons.Outlined.SportsScore),
        Triple(Screen.AllExpenses.route, "Expenses", Icons.Outlined.ReceiptLong),
        Triple(Screen.Reports.route, "Reports", Icons.Outlined.BarChart)
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        destinations.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_$label")
            )
        }
    }
}
