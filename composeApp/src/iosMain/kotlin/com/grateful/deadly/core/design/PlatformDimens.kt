package com.grateful.deadly.core.design

import androidx.compose.ui.unit.dp

/**
 * iOS implementation of PlatformDimens.
 * Optimized for iOS screen real estate and layout behavior.
 */
actual object PlatformDimens {
    /**
     * iOS cover art height - reduced from 450dp to better fit iOS screens
     * and ensure lower content (share button) remains visible
     */
    actual val coverArtHeight = 380.dp

    /**
     * iOS horizontal padding - reduced for more compact layout
     * and better space utilization on iOS
     */
    actual val playerHorizontalPadding = 16.dp

    /**
     * iOS vertical spacing - tighter spacing for compact layout
     */
    actual val playerVerticalSpacing = 6.dp
}