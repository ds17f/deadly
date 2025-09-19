package com.grateful.deadly.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import com.grateful.deadly.core.logging.Logger

/**
 * Android implementation of NavigationController using Jetpack Navigation
 */
actual class NavigationController(
    internal val navController: NavHostController
) {
    private var _currentScreen by mutableStateOf<AppScreen?>(null)
    actual val currentScreen: AppScreen? get() = _currentScreen

    actual fun navigate(screen: AppScreen) {
        _currentScreen = screen
        navController.navigate(screen.route())
    }

    actual fun navigateUp() {
        navController.navigateUp()
    }

    internal fun setCurrentScreen(screen: AppScreen) {
        _currentScreen = screen
    }
}

/**
 * Android implementation of DeadlyNavGraphBuilder using NavGraphBuilder
 */
actual class DeadlyNavGraphBuilder {
    private val routes = mutableMapOf<AppScreen, @Composable () -> Unit>()
    private val parameterizedRoutes = mutableMapOf<String, @Composable (Map<String, String>) -> Unit>()
    
    actual fun composable(
        screen: AppScreen,
        content: @Composable () -> Unit
    ) {
        routes[screen] = content
    }
    
    actual fun composable(
        route: String,
        content: @Composable (args: Map<String, String>) -> Unit
    ) {
        parameterizedRoutes[route] = content
    }
    
    fun build(): androidx.navigation.NavGraphBuilder.() -> Unit = {
        // Build standard routes
        routes.forEach { (screen, content) ->
            composable(screen.route()) { content() }
        }
        
        // Build parameterized routes with arguments
        parameterizedRoutes.forEach { (route, content) ->
            when {
                route.contains("{showId}") && route.contains("{recordingId}") -> {
                    composable(
                        route,
                        arguments = listOf(
                            navArgument("showId") { type = NavType.StringType },
                            navArgument("recordingId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val args = mapOf(
                            "showId" to (backStackEntry.arguments?.getString("showId") ?: ""),
                            "recordingId" to (backStackEntry.arguments?.getString("recordingId") ?: "")
                        )
                        content(args)
                    }
                }
                route.contains("{showId}") -> {
                    composable(
                        route,
                        arguments = listOf(navArgument("showId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val args = mapOf(
                            "showId" to (backStackEntry.arguments?.getString("showId") ?: "")
                        )
                        content(args)
                    }
                }
                else -> {
                    composable(route) { backStackEntry ->
                        val args = backStackEntry.arguments?.let { bundle ->
                            bundle.keySet().associateWith { key -> bundle.getString(key) ?: "" }
                        } ?: emptyMap()
                        content(args)
                    }
                }
            }
        }
    }
}

/**
 * Creates Android NavigationController using rememberNavController
 */
@Composable
actual fun rememberNavigationController(): NavigationController {
    val navController = rememberNavController()
    return NavigationController(navController)
}


/**
 * Android implementation of DeadlyNavHost using Jetpack Navigation NavHost
 */
@Composable
actual fun DeadlyNavHost(
    navigationController: NavigationController,
    startDestination: AppScreen,
    modifier: Modifier,
    onScreenChanged: (AppScreen) -> Unit,
    content: DeadlyNavGraphBuilder.() -> Unit
) {
    val builder = DeadlyNavGraphBuilder()
    builder.content()

    // Track current destination and update both callback and NavigationController
    val currentBackStackEntry by navigationController.navController.currentBackStackEntryAsState()
    LaunchedEffect(currentBackStackEntry) {
        currentBackStackEntry?.destination?.route?.let { route ->
            val screen = route.toAppScreen()
            screen?.let {
                Logger.d("AndroidNavigation", "Screen changed to: $it")
                navigationController.setCurrentScreen(it)
                onScreenChanged(it)
            }
        }
    }

    // Set initial screen
    LaunchedEffect(Unit) {
        navigationController.setCurrentScreen(startDestination)
        onScreenChanged(startDestination)
    }

    NavHost(
        navController = navigationController.navController,
        startDestination = startDestination.route(),
        modifier = modifier,
        builder = builder.build()
    )
}