package com.example.marketwatch.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.marketwatch.auth.AuthViewModel
import com.example.marketwatch.ui.theme.ThemeViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Portfolio : Screen("portfolio", "Portfolio", Icons.AutoMirrored.Filled.ShowChart)
    object Profile : Screen("profile", "Profile", Icons.Default.AccountCircle)
}

val items = listOf(
    Screen.Search,
    Screen.Portfolio,
    Screen.Profile
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authViewModel: AuthViewModel, 
    themeViewModel: ThemeViewModel
) {
    val navController = rememberNavController()
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Market Watch") },
                actions = {
                    IconButton(onClick = { themeViewModel.toggleTheme() }) {
                        Icon(
                            if (isDarkMode) Icons.Default.WbSunny else Icons.Default.NightsStay,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Portfolio.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.Portfolio.route) { 
                PortfolioScreen(onStockClick = { symbol -> navController.navigate("stockDetail/$symbol") }) 
            }
            composable(Screen.Profile.route) { 
                ProfileScreen(authViewModel = authViewModel)
            }
            composable(
                route = "stockDetail/{stockSymbol}",
                arguments = listOf(navArgument("stockSymbol") { type = NavType.StringType })
            ) {
                val stockSymbol = it.arguments?.getString("stockSymbol") ?: ""
                StockDetailScreen(stockSymbol, onNavigateBack = { navController.navigateUp() })
            }
        }
    }
}
