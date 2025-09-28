package com.grateful.deadly.feature.home

import com.grateful.deadly.core.design.topbar.TopBarMode
import com.grateful.deadly.navigation.BarConfiguration
import com.grateful.deadly.navigation.TopBarConfig
import com.grateful.deadly.navigation.BottomBarConfig

/**
 * HomeBarConfiguration - Bar configuration for Home feature
 *
 * Defines how the navigation bars should appear for Home screens.
 * Colocated with Home feature to keep related UI settings together.
 */
object HomeBarConfiguration {

    /**
     * Configuration for main Home screen
     *
     * Simple home title with standard bottom navigation
     */
    fun getHomeBarConfig(): BarConfiguration = BarConfiguration(
        topBar = TopBarConfig(
            title = "Home",
            mode = TopBarMode.SOLID,
            visible = true
        ),
        bottomBar = BottomBarConfig(visible = true)
    )
}