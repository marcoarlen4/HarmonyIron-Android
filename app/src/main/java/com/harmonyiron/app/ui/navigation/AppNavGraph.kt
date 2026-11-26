package com.harmonyiron.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.harmonyiron.app.ui.screen.CoachScreen
import com.harmonyiron.app.ui.screen.PlansScreen
import com.harmonyiron.app.ui.screen.SettingsScreen
import com.harmonyiron.app.ui.screen.StatsScreen
import com.harmonyiron.app.ui.screen.TodayScreen
import com.harmonyiron.app.ui.screen.WorkoutSessionScreen
import com.harmonyiron.app.ui.theme.IronColors
import java.net.URLDecoder
import java.net.URLEncoder

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val tabs = listOf(
        BottomTab("today", "今日训练", Icons.Default.FitnessCenter),
        BottomTab("plans", "计划库", Icons.Default.ListAlt),
        BottomTab("stats", "数据分析", Icons.Default.BarChart),
        BottomTab("coach", "AI 教练", Icons.Default.Psychology)
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        containerColor = IronColors.Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = IronColors.Surface) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo("today") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = IronColors.PrimaryOrange,
                                selectedTextColor = IronColors.PrimaryOrange,
                                unselectedIconColor = IronColors.TextSecondary,
                                unselectedTextColor = IronColors.TextSecondary,
                                indicatorColor = IronColors.SurfaceElevated
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "today",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("today") {
                TodayScreen(
                    onStartWorkout = { dayId, dayTitle ->
                        val encoded = URLEncoder.encode(dayTitle, "UTF-8")
                        navController.navigate("session/$dayId/$encoded")
                    }
                )
            }
            composable("plans") { PlansScreen() }
            composable("stats") { StatsScreen() }
            composable("coach") {
                CoachScreen(onOpenSettings = { navController.navigate("settings") })
            }
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "session/{dayId}/{dayTitle}",
                arguments = listOf(
                    navArgument("dayId") { type = NavType.LongType },
                    navArgument("dayTitle") { type = NavType.StringType }
                )
            ) { entry ->
                val dayId = entry.arguments?.getLong("dayId") ?: 1L
                val encodedTitle = entry.arguments?.getString("dayTitle") ?: ""
                val title = URLDecoder.decode(encodedTitle, "UTF-8")
                WorkoutSessionScreen(
                    planDayId = dayId,
                    dayTitle = title,
                    onFinishSession = { navController.popBackStack() }
                )
            }
        }
    }
}
