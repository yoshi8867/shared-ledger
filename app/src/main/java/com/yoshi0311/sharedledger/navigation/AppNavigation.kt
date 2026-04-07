package com.yoshi0311.sharedledger.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yoshi0311.sharedledger.ui.screens.auth.SignupScreen
import com.yoshi0311.sharedledger.ui.screens.autofill.AutoFillScreen
import com.yoshi0311.sharedledger.ui.screens.category.CategoryManageScreen
import com.yoshi0311.sharedledger.ui.screens.home.HomeScreen
import com.yoshi0311.sharedledger.ui.screens.login.LoginScreen
import com.yoshi0311.sharedledger.ui.screens.settings.SettingsScreen
import com.yoshi0311.sharedledger.ui.screens.shared.SharedLedgerScreen
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
                onNavigateToTransactionEdit = { id, dateMillis ->
                    navController.navigate(Routes.TransactionEdit.createRoute(id, dateMillis))
                },
                onNavigateToSharedLedger = { ledgerId ->
                    navController.navigate(Routes.SharedLedger.createRoute(ledgerId))
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.Settings.route)
                },
                onNavigateToAutoFill = {
                    navController.navigate(Routes.AutoFill.route)
                }
            )
        }

        composable(Routes.AutoFill.route) {
            AutoFillScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.SharedLedger.route,
            arguments = listOf(
                navArgument("ledgerId") { type = NavType.LongType }
            )
        ) {
            SharedLedgerScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToCategoryManage = {
                    navController.navigate(Routes.CategoryManage.route)
                }
            )
        }

        composable(Routes.CategoryManage.route) {
            CategoryManageScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.TransactionEdit.route,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("dateMillis") {
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
