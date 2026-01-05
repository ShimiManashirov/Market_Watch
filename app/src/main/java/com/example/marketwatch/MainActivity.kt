package com.example.marketwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.marketwatch.auth.LoginScreen
import com.example.marketwatch.auth.RegistrationScreen
import com.example.marketwatch.ui.theme.MarketWatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MarketWatchTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onNavigateToRegistration = { navController.navigate("registration") }
            )
        }
        composable("registration") {
            RegistrationScreen(
                onNavigateToLogin = { navController.navigate("login") }
            )
        }
    }
}
