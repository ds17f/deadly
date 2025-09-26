package com.grateful.deadly.core.ui

import androidx.compose.ui.Modifier

/**
 * UISpacers - Platform-specific UI spacing and alignment utilities
 *
 * Provides expect/actual modifier extensions to handle platform differences
 * in font rendering, icon alignment, and spacing between Android and iOS.
 *
 * Usage:
 * - iconAlignment(): Fixes Material Symbols baseline alignment on Android
 * - playButtonAlignment(): Fixes Surface/Circle positioning on Android
 * - checkIconSpacing(): Fixes CheckCircle top spacing on Android
 *
 * iOS implementations are pass-through (no changes needed).
 * Android implementations provide specific offsets/padding to match iOS appearance.
 */

/**
 * Platform-specific alignment for icons, particularly Material Symbols
 * that render with different baselines on Android vs iOS
 */
expect fun Modifier.iconAlignment(): Modifier

/**
 * Platform-specific alignment for large play/pause buttons with circle backgrounds
 * to handle Surface shape rendering differences
 */
expect fun Modifier.playButtonAlignment(): Modifier

/**
 * Platform-specific spacing for check/circle icons that have excessive
 * top spacing on Android compared to iOS
 */
expect fun Modifier.checkIconSpacing(): Modifier