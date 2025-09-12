package com.grateful.deadly.navigation

/**
 * Route conversion extensions for AppScreen sealed interface
 * 
 * These extensions provide bidirectional conversion between AppScreen objects
 * and string routes for use with navigation frameworks.
 */

/**
 * Convert AppScreen to string route for navigation
 */
fun AppScreen.route(): String = when (this) {
    AppScreen.Home -> "home"
    AppScreen.Search -> "search"
    AppScreen.Library -> "library"
    AppScreen.Collections -> "collections"
    AppScreen.Settings -> "settings"
    AppScreen.SearchResults -> "search-results"
    is AppScreen.ShowDetail -> "show/${this.id}"
}

/**
 * Convert string route back to AppScreen object
 * Returns null if route is not recognized
 */
fun String.toAppScreen(): AppScreen? = when {
    this == "home" -> AppScreen.Home
    this == "search" -> AppScreen.Search
    this == "library" -> AppScreen.Library
    this == "collections" -> AppScreen.Collections
    this == "settings" -> AppScreen.Settings
    this == "search-results" -> AppScreen.SearchResults
    this.startsWith("show/") -> AppScreen.ShowDetail(this.removePrefix("show/"))
    else -> null
}