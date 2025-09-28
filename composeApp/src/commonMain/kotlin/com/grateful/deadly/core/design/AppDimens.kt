package com.grateful.deadly.core.design

import androidx.compose.ui.unit.dp

/**
 * AppDimens - Shared design system spacing constants
 *
 * Ensures consistent spacing across Android and iOS platforms by providing
 * a single source of truth for all spacing values. No raw dp values should
 * be used directly in UI components.
 *
 * Based on 8dp grid system with semantic naming.
 */
object AppDimens {
    // Base spacing units (8dp grid system)
    val XXS = 2.dp    // 2dp - Minimal spacing
    val XS = 4.dp     // 4dp - Tight spacing
    val S = 8.dp      // 8dp - Small spacing
    val M = 12.dp     // 12dp - Medium spacing (card padding)
    val L = 16.dp     // 16dp - Large spacing
    val XL = 20.dp    // 20dp - Extra large spacing
    val XXL = 24.dp   // 24dp - Maximum spacing

    // Icon sizes (Material Design standards)
    object IconSize {
        val Small = 16.dp    // Small icons in UI
        val Medium = 24.dp   // Standard icons (V2 star size)
        val Large = 32.dp    // Large interactive icons
        val XLarge = 48.dp   // Play button circles
    }

    // Card and component spacing
    object Card {
        val Padding = M          // 12dp - Internal card padding
        val Radius = S           // 8dp - Card corner radius
        val Elevation = XS       // 4dp - Card elevation
        val Spacing = S          // 8dp - Space between cards
    }

    // Interactive component sizing
    object Interactive {
        val MinTouchTarget = 48.dp  // Minimum touch target (Material)
        val ButtonHeight = 40.dp    // Standard button height
        val IconButtonSize = 48.dp  // Standard icon button size
    }

    // Text and baseline spacing
    object Text {
        val LineSpacing = XS         // 4dp - Extra line spacing
        val ParagraphSpacing = L     // 16dp - Between paragraphs
        val SectionSpacing = XXL     // 24dp - Between sections
    }

    // Corner radius constants
    object CornerRadius {
        val Small = XS      // 4dp - Small components
        val Medium = S      // 8dp - Standard components
        val Large = M       // 12dp - Large components like cover art
    }
}