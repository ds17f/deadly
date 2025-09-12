package com.grateful.deadly.navigation

/**
 * AppScreen - Sealed interface representing all navigable screens in the app
 * 
 * This serves as the single source of truth for navigation across Android and iOS,
 * providing type-safe navigation with compile-time route validation.
 * 
 * Each screen can carry data parameters as properties of the data class.
 */
sealed interface AppScreen {
    object Home : AppScreen
    object Search : AppScreen
    data class ShowDetail(val id: String) : AppScreen
    object Library : AppScreen
    object Collections : AppScreen
    object Settings : AppScreen
    object SearchResults : AppScreen
}