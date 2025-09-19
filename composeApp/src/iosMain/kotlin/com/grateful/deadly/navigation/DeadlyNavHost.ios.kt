package com.grateful.deadly.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * iOS implementation of NavigationController
 * TODO: Integrate with SwiftUI NavigationStack in future
 */
actual class NavigationController {
    private var _currentScreen = mutableStateOf<AppScreen?>(null)
    actual val currentScreen: AppScreen? get() = _currentScreen.value

    actual fun navigate(screen: AppScreen) {
        _currentScreen.value = screen
        // TODO: Integrate with iOS NavigationStack
    }

    actual fun navigateUp() {
        // Simple back navigation - go to Search screen
        // TODO: Implement proper navigation stack with SwiftUI NavigationStack
        _currentScreen.value = AppScreen.Search
    }

    internal fun getCurrentScreenState() = _currentScreen
}

/**
 * iOS implementation of DeadlyNavGraphBuilder
 * TODO: Integrate with SwiftUI NavigationStack in future
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
    
    internal fun getRoutes() = routes
    internal fun getParameterizedRoutes() = parameterizedRoutes
}

/**
 * Creates iOS NavigationController
 */
@Composable
actual fun rememberNavigationController(): NavigationController {
    return remember { NavigationController() }
}

/**
 * iOS implementation of DeadlyNavHost
 * TODO: Replace with SwiftUI NavigationStack integration
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
    
    // Set initial current screen and observe state
    val currentScreenState = navigationController.getCurrentScreenState()
    val currentScreen by currentScreenState
    val actualCurrentScreen = currentScreen ?: run {
        navigationController.navigate(startDestination)
        startDestination
    }

    // Notify callback when screen changes
    LaunchedEffect(actualCurrentScreen) {
        onScreenChanged(actualCurrentScreen)
    }

    val routes = builder.getRoutes()

    // Simple placeholder implementation - just show current screen content
    // TODO: Replace with proper SwiftUI NavigationStack integration
    Box(modifier = modifier) {
        routes[actualCurrentScreen]?.invoke()
    }
}