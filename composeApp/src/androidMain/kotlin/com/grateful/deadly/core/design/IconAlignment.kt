package com.grateful.deadly.core.design

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable

/**
 * Android-specific icon alignment implementation
 *
 * Uses graphicsLayer.translationY instead of offset to shift icons up by 4dp.
 * This prevents clipping issues with small icons while maintaining the visual correction.
 */
actual fun Modifier.universalIconAlignment(): Modifier = this.then(
    Modifier.graphicsLayer {
        translationY = -4.dp.toPx()
    }
)
