package com.example.marketwatch

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.marketwatch.auth.AuthViewModel
import com.example.marketwatch.auth.LoginScreen
import com.example.marketwatch.auth.RegistrationScreen
import com.example.marketwatch.main.MainScreen
import com.example.marketwatch.ui.theme.MarketWatchTheme
import com.example.marketwatch.ui.theme.ThemeViewModel
import com.example.marketwatch.util.CurrencyConverter
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fetch latest currency rates on app startup
        lifecycleScope.launch {
            CurrencyConverter.updateRates()
        }

        setContent {
            val themeViewModel: ThemeViewModel by viewModels()
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()

            MarketWatchTheme(darkTheme = isDarkMode) {
                AppNavigation(themeViewModel = themeViewModel, authViewModel = authViewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(themeViewModel: ThemeViewModel, authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()

    if (currentUser == null) {
        NavHost(navController = navController, startDestination = "login") {
            composable("login") {
                LoginScreen(
                    onNavigateToRegistration = { navController.navigate("registration") },
                    authViewModel = authViewModel
                )
            }
            composable("registration") {
                RegistrationScreen(
                    onNavigateToLogin = { navController.navigate("login") { popUpTo("login") { inclusive = true } } },
                    authViewModel = authViewModel
                )
            }
        }
    } else {
        MainScreen(
            themeViewModel = themeViewModel,
            authViewModel = authViewModel
        )
    }
}
