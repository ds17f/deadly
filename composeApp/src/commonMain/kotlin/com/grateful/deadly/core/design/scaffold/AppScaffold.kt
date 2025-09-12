package com.grateful.deadly.core.design.scaffold

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grateful.deadly.core.design.component.topbar.TopBar
import com.grateful.deadly.navigation.AppScreen
import com.grateful.deadly.navigation.BottomBarConfig
import com.grateful.deadly.navigation.TopBarConfig

/**
 * AppScaffold - Main layout manager for the Deadly app
 * 
 * Provides unified layout with TopBar, BottomBar, and MiniPlayer coordination.
 * Follows Spotify-style layering where MiniPlayer appears above bottom navigation.
 */
@Composable
fun AppScaffold(
    topBarConfig: TopBarConfig?,
    bottomBarConfig: BottomBarConfig,
    currentScreen: AppScreen,
    onNavigateBack: () -> Unit,
    onNavigateToTab: (AppScreen) -> Unit = {},
    miniPlayerContent: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            topBarConfig?.let { config ->
                TopBar(
                    config = config,
                    onNavigateBack = onNavigateBack
                )
            }
        },
        bottomBar = {
            if (bottomBarConfig.visible) {
                // Spotify-style layering: MiniPlayer above BottomNav
                Column {
                    miniPlayerContent?.invoke()
                    
                    // TODO: Implement BottomNavigationBar
                    // BottomNavigationBar(
                    //     currentScreen = currentScreen,
                    //     onNavigateToTab = onNavigateToTab
                    // )
                }
            }
        }
    ) { padding ->
        content(padding)
    }
}