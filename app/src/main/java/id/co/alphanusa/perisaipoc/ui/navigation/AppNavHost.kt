package id.co.alphanusa.perisaipoc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.co.alphanusa.perisaipoc.ui.screens.home.HomeScreen
import id.co.alphanusa.perisaipoc.ui.screens.setting.SettingsScreen
import id.co.alphanusa.perisaipoc.ui.screens.splash.SplashScreen

/** Graf navigasi aplikasi: splash → home → settings. */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.SPLASH,
        modifier = modifier,
    ) {
        composable(AppRoute.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(AppRoute.HOME) {
                        popUpTo(AppRoute.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(AppRoute.HOME) {
            HomeScreen(
                onNavigateToSettings = { navController.navigate(AppRoute.SETTINGS) },
            )
        }

        composable(AppRoute.SETTINGS) {
            SettingsScreen(onBackPressed = { navController.popBackStack() })
        }
    }
}
