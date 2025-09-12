package com.grateful.deadly.navigation

/**
 * Navigation event that can be emitted from ViewModels to trigger navigation
 * in a platform-agnostic way using the AppScreen sealed class
 */
data class NavigationEvent(
    val screen: AppScreen,
    val clearBackStack: Boolean = false
)