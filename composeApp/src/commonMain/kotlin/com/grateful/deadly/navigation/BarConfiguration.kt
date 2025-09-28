package com.grateful.deadly.navigation

import com.grateful.deadly.core.design.topbar.TopBarMode
import com.grateful.deadly.feature.search.SearchBarConfiguration
import com.grateful.deadly.feature.home.HomeBarConfiguration

/**
 * Configuration for the top bar
 */
data class TopBarConfig(
    val visible: Boolean = true,
    val title: String = "",
    val mode: TopBarMode = TopBarMode.SOLID,
    val showBackButton: Boolean = false
)

/**
 * Configuration for the bottom bar
 */
data class BottomBarConfig(
    val visible: Boolean = true
)

/**
 * Combined bar configuration for a screen
 */
data class BarConfiguration(
    val topBar: TopBarConfig? = null,
    val bottomBar: BottomBarConfig = BottomBarConfig()
)

/**
 * Central route mapping to feature bar configurations
 * 
 * This delegates to feature-specific configuration objects,
 * keeping the actual configurations colocated with their features.
 */
object NavigationBarConfig {
    
    /**
     * Get bar configuration for a specific screen
     * Delegates to feature-specific configuration objects
     */
    fun getBarConfig(screen: AppScreen): BarConfiguration = when (screen) {
        // Home routes - delegate to HomeBarConfiguration
        AppScreen.Home -> HomeBarConfiguration.getHomeBarConfig()

        // Search routes - delegate to SearchBarConfiguration
        AppScreen.Search -> SearchBarConfiguration.getSearchBarConfig()
        is AppScreen.SearchResults -> SearchBarConfiguration.getSearchResultsBarConfig()

        // Placeholder configurations for other features
        // TODO: Move these to feature-specific configuration objects
        
        AppScreen.Library -> BarConfiguration(
            topBar = TopBarConfig(title = "Library"),
            bottomBar = BottomBarConfig(visible = true)
        )
        
        AppScreen.Collections -> BarConfiguration(
            topBar = TopBarConfig(title = "Collections"),
            bottomBar = BottomBarConfig(visible = true)
        )
        
        AppScreen.Settings -> BarConfiguration(
            topBar = TopBarConfig(title = "Settings"),
            bottomBar = BottomBarConfig(visible = true)
        )
        
        is AppScreen.ShowDetail -> BarConfiguration(
            topBar = null, // No TopBar for ShowDetail - uses its own header overlay
            bottomBar = BottomBarConfig(visible = true)
        )
        
        AppScreen.Player -> BarConfiguration(
            topBar = null, // No TopBar for Player - using custom PlayerTopBar component
            bottomBar = BottomBarConfig(visible = false) // Hide bottom bar for immersive experience
        )
    }
}