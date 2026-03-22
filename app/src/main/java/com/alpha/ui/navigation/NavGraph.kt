package com.alpha.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alpha.features.budget.BudgetScreen
import com.alpha.features.calculator.CalculatorScreen
import com.alpha.features.settings.SettingsScreen
import com.alpha.features.websearch.WebSearchScreen
import com.alpha.features.sbrcontrol.SbrControlScreen
import com.alpha.ui.home.HomeScreen

@Composable
fun AlphaNavGraph(isDarkMode: Boolean, onThemeToggle: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                isDarkMode = isDarkMode,
                onThemeToggle = onThemeToggle,
                onModuleClick = { navController.navigate(it) },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable("web_search") {
            WebSearchScreen(onBack = { navController.popBackStack() })
        }
        composable("calculator") {
            CalculatorScreen(onBack = { navController.popBackStack() })
        }
        composable("sbr_control") {
            SbrControlScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("budget") {
            BudgetScreen(onBack = { navController.popBackStack() })
        }
    }
}
