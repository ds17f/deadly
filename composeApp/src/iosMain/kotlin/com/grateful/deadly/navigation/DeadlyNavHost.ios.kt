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
    private var _onScreenChanged: ((AppScreen) -> Unit)? = null

    actual val currentScreen: AppScreen? get() = _currentScreen.value

    /**
     * Set the callback that should be notified immediately when navigation occurs
     * This ensures synchronous state propagation to prevent UI glitches
     */
    internal fun setOnScreenChanged(callback: (AppScreen) -> Unit) {
        _onScreenChanged = callback
    }

    actual fun navigate(screen: AppScreen) {
        // Add to navigation stack if it's a new screen
        if (_navigationStack.isEmpty() || _navigationStack.last() != screen) {
            _navigationStack.add(screen)
        }

        // Update current screen state
        _currentScreen.value = screen

        // Immediately notify callback for synchronous state propagation
        _onScreenChanged?.invoke(screen)
    }

    actual fun navigateUp() {
        if (_navigationStack.size > 1) {
            // Remove current screen from stack
            _navigationStack.removeLastOrNull()

            // Navigate to previous screen
            val previousScreen = _navigationStack.lastOrNull() ?: AppScreen.Search
            _currentScreen.value = previousScreen
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
            _navigationStack.add(startDestination)
            _currentScreen.value = startDestination
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

    val routes = builder.getRoutes()

    // Render current screen content
    // State changes are now synchronous, so TopBar updates happen immediately
    Box(modifier = modifier) {
        routes[actualCurrentScreen]?.invoke()
    }
}