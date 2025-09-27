package com.grateful.deadly.core.design

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Android-specific icon alignment implementation
 *
 * Shifts all icons up by 3dp to correct the systematic "low" rendering
 * of Material Symbols icons on Android compared to iOS.
 */
actual fun Modifier.universalIconAlignment(): Modifier = this.then(
    Modifier.offset(y = (-3).dp)
)