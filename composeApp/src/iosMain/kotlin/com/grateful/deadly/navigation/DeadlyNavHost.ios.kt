package com.grateful.deadly.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * iOS implementation of NavigationController with proper stack management
 * Provides immediate state synchronization to prevent UI timing issues
 */
actual class NavigationController {
    private val _navigationStack = mutableListOf<AppScreen>()
    private var _currentScreen = mutableStateOf<AppScreen?>(null)
    private var _currentRoute = mutableStateOf<String?>(null)
    private var _onScreenChanged: ((AppScreen) -> Unit)? = null

    actual val currentScreen: AppScreen? get() = _currentScreen.value
    internal val currentRoute: String? get() = _currentRoute.value

    /**
     * Set the callback that should be notified immediately when navigation occurs
     * This ensures synchronous state propagation to prevent UI glitches
     */
    internal fun setOnScreenChanged(callback: (AppScreen) -> Unit) {
        _onScreenChanged = callback
    }

    actual fun navigate(screen: AppScreen) {
        // SPECIAL CASE: Intercept ShowDetail navigation and present SwiftUI view
        if (screen is AppScreen.ShowDetail) {
            ShowDetailBridgeHelper.showDetail(screen.showId, screen.recordingId)
            return
        }

        // SPECIAL CASE: Intercept Player navigation and present SwiftUI view
        if (screen is AppScreen.Player) {
            PlayerBridgeHelper.showPlayer { showId, recordingId ->
                // Navigate to ShowDetail from Player
                navigate(AppScreen.ShowDetail(showId, recordingId))
            }
            return
        }

        // Convert AppScreen to route string (like Android does)
        val routeString = screen.route()

        // Add to navigation stack if it's a new screen
        if (_navigationStack.isEmpty() || _navigationStack.last() != screen) {
            _navigationStack.add(screen)
        }

        // Update current screen and route state
        _currentScreen.value = screen
        _currentRoute.value = routeString

        // Immediately notify callback for synchronous state propagation
        _onScreenChanged?.invoke(screen)
    }

    actual fun navigateUp() {
        if (_navigationStack.size > 1) {
            // Remove current screen from stack
            _navigationStack.removeLastOrNull()

            // Navigate to previous screen
            val previousScreen = _navigationStack.lastOrNull() ?: AppScreen.Search
            val routeString = previousScreen.route()
            _currentScreen.value = previousScreen
            _currentRoute.value = routeString
            _onScreenChanged?.invoke(previousScreen)
        } else {
            // Fallback to Search if stack is empty
            navigate(AppScreen.Search)
        }
    }

    /**
     * Initialize the navigation stack with a start destination
     * This ensures proper initial state setup like Android implementation
     */
    internal fun initialize(startDestination: AppScreen) {
        if (_navigationStack.isEmpty()) {
            val routeString = startDestination.route()
            _navigationStack.add(startDestination)
            _currentScreen.value = startDestination
            _currentRoute.value = routeString
            _onScreenChanged?.invoke(startDestination)
        }
    }

    internal fun getCurrentScreenState() = _currentScreen

    /**
     * Get current navigation stack (for debugging)
     */
    internal fun getNavigationStack(): List<AppScreen> = _navigationStack.toList()
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
 * Helper functions for route matching (iOS implementation of Android's route matching)
 */
private fun routeMatches(actualRoute: String, pattern: String): Boolean {
    // Convert pattern like "showDetail/{showId}/{recordingId}" to regex
    val regex = pattern
        .replace("{showId}", "([^/]+)")
        .replace("{recordingId}", "([^/]+)")
        .toRegex()

    return regex.matches(actualRoute)
}

private fun extractRouteArgs(actualRoute: String, pattern: String): Map<String, String> {
    val args = mutableMapOf<String, String>()

    // Extract parameter names from pattern
    val paramNames = mutableListOf<String>()
    val paramRegex = "\\{([^}]+)\\}".toRegex()
    paramRegex.findAll(pattern).forEach { match ->
        paramNames.add(match.groupValues[1])
    }

    // Create regex to extract values
    val valueRegex = pattern
        .replace("{showId}", "([^/]+)")
        .replace("{recordingId}", "([^/]+)")
        .toRegex()

    // Extract values and map to parameter names
    val matchResult = valueRegex.find(actualRoute)
    if (matchResult != null) {
        paramNames.forEachIndexed { index, paramName ->
            if (index + 1 < matchResult.groupValues.size) {
                args[paramName] = matchResult.groupValues[index + 1]
            }
        }
    }

    return args
}

/**
 * Creates iOS NavigationController
 */
@Composable
actual fun rememberNavigationController(): NavigationController {
    return remember { NavigationController() }
}

/**
 * iOS implementation of DeadlyNavHost with immediate state synchronization
 * Eliminates timing issues by using synchronous state propagation
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

    // Set up immediate callback registration - this must happen first
    LaunchedEffect(navigationController) {
        navigationController.setOnScreenChanged(onScreenChanged)
    }

    // Initialize navigation stack with start destination (like Android does)
    LaunchedEffect(Unit) {
        navigationController.initialize(startDestination)
    }

    // Get current screen state for rendering
    val currentScreenState = navigationController.getCurrentScreenState()
    val currentScreen by currentScreenState
    val actualCurrentScreen = currentScreen ?: startDestination
    val currentRouteString = navigationController.currentRoute ?: startDestination.route()

    val routes = builder.getRoutes()
    val parameterizedRoutes = builder.getParameterizedRoutes()

    // Render current screen content (iOS matching Android's behavior)
    Box(modifier = modifier) {
        var routeHandled = false

        // First try exact screen match
        routes[actualCurrentScreen]?.let { composable ->
            composable()
            routeHandled = true
        }

        // If no exact match, try parameterized routes (like Android does)
        if (!routeHandled && parameterizedRoutes.isNotEmpty()) {
            for ((pattern, composable) in parameterizedRoutes) {
                if (routeMatches(currentRouteString, pattern)) {
                    val args = extractRouteArgs(currentRouteString, pattern)
                    composable(args)
                    routeHandled = true
                    break
                }
            }
        }

        // If still no match, show a debug message instead of falling back
        if (!routeHandled) {
            androidx.compose.material3.Text(
                text = "Route not found: $currentRouteString\nScreen: $actualCurrentScreen",
                color = androidx.compose.material3.MaterialTheme.colorScheme.error
            )
        }
    }
}