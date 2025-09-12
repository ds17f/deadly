package com.grateful.deadly.feature.search

import androidx.compose.runtime.Composable
import com.grateful.deadly.core.design.topbar.TopBarMode
import com.grateful.deadly.navigation.BarConfiguration
import com.grateful.deadly.navigation.TopBarConfig
import com.grateful.deadly.navigation.BottomBarConfig

/**
 * SearchBarConfiguration - Bar configuration for Search feature
 * 
 * Defines how the navigation bars should appear for Search screens.
 * Colocated with Search feature to keep related UI settings together.
 */
object SearchBarConfiguration {
    
    /**
     * Configuration for main Search screen
     * 
     * Includes search title with future support for custom actions
     */
    fun getSearchBarConfig(): BarConfiguration = BarConfiguration(
        topBar = TopBarConfig(
            title = "Search",
            mode = TopBarMode.SOLID,
            visible = true
        ),
        bottomBar = BottomBarConfig(visible = true)
    )
    
    /**
     * Configuration for Search Results screen
     * 
     * Shows back navigation and search context
     */
    fun getSearchResultsBarConfig(): BarConfiguration = BarConfiguration(
        topBar = TopBarConfig(
            title = "Search Results", 
            mode = TopBarMode.SOLID,
            visible = true,
            showBackButton = true
        ),
        bottomBar = BottomBarConfig(visible = true)
    )
}