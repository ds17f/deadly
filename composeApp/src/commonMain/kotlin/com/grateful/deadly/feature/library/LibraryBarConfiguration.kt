package com.grateful.deadly.feature.library

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
import com.grateful.deadly.navigation.BarConfiguration
import com.grateful.deadly.navigation.BottomBarConfig
import com.grateful.deadly.navigation.TopBarConfig

/**
 * LibraryBarConfiguration - Bar configuration for Library feature
 *
 * Defines how the navigation bars should appear for Library screens.
 * Colocated with Library feature to keep related UI settings together.
 */
object LibraryBarConfiguration {

    /**
     * Configuration for main Library screen
     *
     * Library title with search and add actions for library management
     */
    fun getLibraryBarConfig(): BarConfiguration = BarConfiguration(
        topBar = TopBarConfig(
            title = "Your Library",
            showBackButton = false, // No back button in bottom nav context
            actions = { LibraryTopBarActions() }
        ),
        bottomBar = BottomBarConfig(visible = true)
        // Note: MiniPlayer is handled globally in AppScaffold
    )
}

/**
 * Top bar actions for main Library screen
 */
@Composable
private fun LibraryTopBarActions() {
    // Search action (Phase 5: TODO - implement library search)
    IconButton(onClick = { /* TODO Phase 5: Implement library search */ }) {
        AppIcon.Search.Render()
    }

    // Add action (Phase 5: TODO - implement add to library)
    IconButton(onClick = { /* TODO Phase 5: Implement add to library */ }) {
        AppIcon.Add.Render()
    }
}