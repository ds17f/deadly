package com.grateful.deadly.core.design

import androidx.compose.ui.unit.dp

/**
 * Android implementation of PlatformDimens.
 * Maintains the original V2 design specifications.
 */
actual object PlatformDimens {
    /**
     * Android cover art height - maintains original V2 design
     */
    actual val coverArtHeight = 450.dp

    /**
     * Android horizontal padding - maintains original V2 design
     */
    actual val playerHorizontalPadding = 24.dp

    /**
     * Android vertical spacing - standard spacing
     */
    actual val playerVerticalSpacing = 8.dp
}