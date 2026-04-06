package com.yoshi0311.sharedledger.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yoshi0311.sharedledger.ui.screens.auth.SignupScreen
import com.yoshi0311.sharedledger.ui.screens.home.HomeScreen
import com.yoshi0311.sharedledger.ui.screens.login.LoginScreen
import com.yoshi0311.sharedledger.ui.screens.splash.SplashScreen
import com.yoshi0311.sharedledger.ui.screens.transaction.TransactionEditScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Splash.route
    ) {
        composable(Routes.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Routes.Signup.route)
                }
            )
        }

        composable(Routes.Signup.route) {
            SignupScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignupSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Home.route) {
            HomeScreen(
                onNavigateToTransactionEdit = { id ->
                    navController.navigate(Routes.TransactionEdit.createRoute(id))
                }
            )
        }

        composable(
            route = Routes.TransactionEdit.route,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            TransactionEditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
