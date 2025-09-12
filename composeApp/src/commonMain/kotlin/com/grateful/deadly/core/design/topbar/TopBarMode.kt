package com.grateful.deadly.core.design.topbar

/**
 * TopBar display modes for different screen types
 * 
 * SOLID: Content padded below status bar with solid background
 * IMMERSIVE: Content behind status bar with transparent/scrim background
 */
sealed class TopBarMode {
    object SOLID : TopBarMode()      // Content padded below status bar
    object IMMERSIVE : TopBarMode()  // Content behind status bar with scrim
}