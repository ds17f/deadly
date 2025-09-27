package com.grateful.deadly.core.design

import androidx.compose.ui.Modifier

/**
 * IconAlignment - Universal icon alignment fix for cross-platform consistency
 *
 * Addresses the systematic issue where Material Symbols icons render "low"
 * on Android compared to iOS. This provides a clean, universal solution
 * that can be applied to all icon usage.
 *
 * Based on user feedback: Android icons appear ~3dp too low across the board.
 */

/**
 * Platform-specific icon vertical alignment correction
 *
 * - Android: Shifts icons up by 3dp to fix baseline alignment
 * - iOS: No adjustment needed (renders perfectly)
 */
expect fun Modifier.universalIconAlignment(): Modifier