package com.grateful.deadly.core.design

import androidx.compose.ui.Modifier

/**
 * iOS-specific icon alignment implementation
 *
 * iOS renders icons perfectly, so no adjustment is needed.
 * This is a pass-through to maintain the existing perfect iOS behavior.
 */
actual fun Modifier.universalIconAlignment(): Modifier = this