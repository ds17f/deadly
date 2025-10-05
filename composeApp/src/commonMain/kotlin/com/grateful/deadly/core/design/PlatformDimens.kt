package com.grateful.deadly.core.design

import androidx.compose.ui.unit.Dp

/**
 * Platform-specific UI dimensions for optimal display on Android and iOS.
 *
 * This system allows us to fine-tune UI elements for each platform while
 * maintaining a single codebase. Use this for dimensions that need platform
 * optimization rather than creating separate components.
 */
expect object PlatformDimens {
    /**
     * Height for the player cover art container.
     * Android: 450dp (original design)
     * iOS: 380dp (optimized for smaller screens)
     */
    val coverArtHeight: Dp

    /**
     * Horizontal padding for player components.
     * Android: 24dp (original design)
     * iOS: 16dp (more compact for better space utilization)
     */
    val playerHorizontalPadding: Dp

    /**
     * Vertical spacing between player components.
     * Future use for fine-tuning component spacing.
     */
    val playerVerticalSpacing: Dp
}