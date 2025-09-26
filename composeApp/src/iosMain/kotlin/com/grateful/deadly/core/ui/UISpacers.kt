package com.grateful.deadly.core.ui

import androidx.compose.ui.Modifier

/**
 * iOS-specific UI spacing implementations
 *
 * iOS rendering is already pixel-perfect for our UI components.
 * These implementations are pass-through (no modifications) to maintain
 * the existing perfect iOS appearance while allowing Android to get fixes.
 */

/**
 * iOS icon alignment - no changes needed, iOS renders perfectly
 */
actual fun Modifier.iconAlignment(): Modifier = this

/**
 * iOS play button alignment - no changes needed, iOS renders perfectly
 */
actual fun Modifier.playButtonAlignment(): Modifier = this

/**
 * iOS check icon spacing - no changes needed, iOS renders perfectly
 */
actual fun Modifier.checkIconSpacing(): Modifier = this