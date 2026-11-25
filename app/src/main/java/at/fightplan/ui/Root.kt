package at.fightplan.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Heute", Icons.Filled.Today),
    WEEK("week", "Woche", Icons.Filled.CalendarViewWeek),
    WEIGHT("weight", "Gewicht", Icons.Filled.MonitorWeight),
    CHECKIN("checkin", "Check-in", Icons.Filled.FactCheck),
    SETTINGS("settings", "Mehr", Icons.Filled.Settings),
}

private const val ROUTE_PERMISSIONS = "permissions"

private val LightColors = lightColorScheme(
    primary = Color(0xFFB71C1C),
    secondary = Color(0xFF455A64),
    tertiary = Color(0xFF2E7D32),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A80),
    secondary = Color(0xFFB0BEC5),
    tertiary = Color(0xFFA5D6A7),
)

@Composable
fun FightPlanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}

@Composable
fun FightPlanRoot(requestedTab: String?, onTabConsumed: () -> Unit) {
    FightPlanTheme {
        val vm: MainViewModel = viewModel()
        val state by vm.state.collectAsStateWithLifecycle()
        val nav = rememberNavController()
        val snackbar = remember { SnackbarHostState() }

        LifecycleResumeEffect(Unit) {
            vm.refreshDate()
            vm.rescheduleAlarms()
            onPauseOrDispose { }
        }
        LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
        LaunchedEffect(requestedTab) {
            val tab = Tab.entries.firstOrNull { it.route == requestedTab } ?: return@LaunchedEffect
            nav.navigate(tab.route) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true }
            onTabConsumed()
        }

        val backStack by nav.currentBackStackEntryAsState()
        val current = backStack?.destination?.route

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
        ) { padding ->
            val s = state
            if (s == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }
            NavHost(nav, startDestination = Tab.TODAY.route, modifier = Modifier.padding(padding)) {
                composable(Tab.TODAY.route) {
                    TodayScreen(s, vm, onOpenPermissions = { nav.navigate(ROUTE_PERMISSIONS) })
                }
                composable(Tab.WEEK.route) { WeekScreen(s) }
                composable(Tab.WEIGHT.route) { WeightScreen(s, vm) }
                composable(Tab.CHECKIN.route) { CheckInScreen(s, vm) }
                composable(Tab.SETTINGS.route) {
                    SettingsScreen(s, vm, onOpenPermissions = { nav.navigate(ROUTE_PERMISSIONS) })
                }
                composable(ROUTE_PERMISSIONS) { PermissionsScreen(onBack = { nav.popBackStack() }) }
            }
        }
    }
}
