package com.example.marketwatch.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.marketwatch.R
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
    val userName by authViewModel.userName.collectAsState()
    val userProfileImageUrl by authViewModel.userProfileImageUrl.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = userProfileImageUrl,
                            contentDescription = "User Profile Image",
                            placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                            error = painterResource(id = R.drawable.ic_launcher_foreground),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Hello, $userName")
                    }
                },
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
            composable(Screen.Search.route) { 
                SearchScreen(onStockClick = { symbol -> navController.navigate("stockDetail/$symbol") })
            }
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
