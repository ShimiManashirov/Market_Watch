package com.example.marketwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.marketwatch.auth.AuthViewModel
import com.example.marketwatch.auth.LoginScreen
import com.example.marketwatch.auth.RegistrationScreen
import com.example.marketwatch.main.MainScreen
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
    val authViewModel: AuthViewModel = viewModel()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onNavigateToRegistration = { navController.navigate("registration") },
                onLoginSuccess = { navController.navigate("main") { popUpTo("login") { inclusive = true } } }
            )
        }
        composable("registration") {
            RegistrationScreen(
                onNavigateToLogin = { navController.navigate("login") { popUpTo("login") { inclusive = true } } }
            )
        }
        composable("main") {
            val userName by authViewModel.userName.collectAsState()
            MainScreen(
                userName = userName,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") { popUpTo("main") { inclusive = true } }
                }
            )
        }
    }
}
