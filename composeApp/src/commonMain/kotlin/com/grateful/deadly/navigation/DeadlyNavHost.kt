package com.grateful.deadly.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Cross-platform navigation controller abstraction
 * Wraps platform-specific navigation controllers to provide unified API
 */
expect class NavigationController {
    fun navigate(screen: AppScreen)
    fun navigateUp()
    val currentScreen: AppScreen?
}

/**
 * Cross-platform navigation graph builder
 * Abstracts the differences between Android NavHost and iOS NavigationStack
 */
expect class DeadlyNavGraphBuilder {
    fun composable(
        screen: AppScreen,
        content: @Composable () -> Unit
    )
    
    fun composable(
        route: String,
        content: @Composable (args: Map<String, String>) -> Unit
    )
}

/**
 * Creates a platform-specific navigation controller
 */
@Composable
expect fun rememberNavigationController(): NavigationController

/**
 * Cross-platform navigation host composable
 * Uses Android NavHost on Android, SwiftUI NavigationStack on iOS
 */
@Composable
expect fun DeadlyNavHost(
    navigationController: NavigationController,
    startDestination: AppScreen,
    modifier: Modifier = Modifier,
    onScreenChanged: (AppScreen) -> Unit = {},
    content: DeadlyNavGraphBuilder.() -> Unit
)