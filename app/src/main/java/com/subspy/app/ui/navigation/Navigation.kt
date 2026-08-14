package com.subspy.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.subspy.app.ui.screens.AddSubscriptionScreen
import com.subspy.app.ui.screens.AnalyticsScreen
import com.subspy.app.ui.screens.CancelFlowScreen
import com.subspy.app.ui.screens.HomeScreen
import com.subspy.app.ui.screens.LoginScreen
import com.subspy.app.ui.screens.NotificationsScreen
import com.subspy.app.ui.screens.PremiumScreen
import com.subspy.app.ui.screens.RegisterScreen
import com.subspy.app.ui.screens.SourcesScreen
import com.subspy.app.ui.screens.SubscriptionDetailScreen
import com.subspy.app.viewmodel.AuthState
import com.subspy.app.viewmodel.AuthViewModel
import com.subspy.app.viewmodel.SubscriptionViewModel

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object Detail : Screen("detail/{subscriptionId}") {
        fun createRoute(id: String) = "detail/$id"
    }
    data object Cancel : Screen("cancel/{subscriptionId}") {
        fun createRoute(id: String) = "cancel/$id"
    }
    data object Notifications : Screen("notifications")
    data object Analytics : Screen("analytics")
    data object Premium : Screen("premium")
    data object Sources : Screen("sources")
    data object AddManual : Screen("add_manual")
}

@Composable
fun SubSpyNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    subscriptionViewModel: SubscriptionViewModel,
    authState: AuthState
) {
    val startDestination = when (authState) {
        is AuthState.Authenticated -> Screen.Home.route
        else -> Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onSignInSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                subscriptionViewModel = subscriptionViewModel,
                onSubscriptionClick = { subscriptionId ->
                    navController.navigate(Screen.Detail.createRoute(subscriptionId))
                },
                onNotificationsClick = {
                    navController.navigate(Screen.Notifications.route)
                },
                onAnalyticsClick = {
                    navController.navigate(Screen.Analytics.route)
                },
                onPremiumClick = {
                    navController.navigate(Screen.Premium.route)
                },
                onAddSourcesClick = {
                    navController.navigate(Screen.Sources.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("subscriptionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subscriptionId = backStackEntry.arguments?.getString("subscriptionId") ?: ""
            SubscriptionDetailScreen(
                subscriptionId = subscriptionId,
                subscriptionViewModel = subscriptionViewModel,
                onCancelClick = { id ->
                    navController.navigate(Screen.Cancel.createRoute(id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Cancel.route,
            arguments = listOf(navArgument("subscriptionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subscriptionId = backStackEntry.arguments?.getString("subscriptionId") ?: ""
            CancelFlowScreen(
                subscriptionId = subscriptionId,
                subscriptionViewModel = subscriptionViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(
                subscriptionViewModel = subscriptionViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                subscriptionViewModel = subscriptionViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Premium.route) {
            PremiumScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Sources.route) {
            SourcesScreen(
                subscriptionViewModel = subscriptionViewModel,
                onAddManual = { navController.navigate(Screen.AddManual.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddManual.route) {
            AddSubscriptionScreen(
                subscriptionViewModel = subscriptionViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
