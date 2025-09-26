package com.grateful.deadly.core.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Android-specific UI spacing implementations
 *
 * These implementations provide specific offsets and padding adjustments
 * to compensate for Android's different font rendering and icon positioning
 * compared to iOS, ensuring visual consistency across platforms.
 */

/**
 * Android-specific icon alignment fix
 * Material Symbols render with different baseline on Android, requiring slight Y offset
 */
actual fun Modifier.iconAlignment(): Modifier = this.then(
    Modifier.offset(y = (-1).dp)
)

/**
 * Android-specific play button alignment fix
 * Surface/Circle rendering has different positioning on Android, requiring top padding
 */
actual fun Modifier.playButtonAlignment(): Modifier = this.then(
    Modifier.padding(top = 2.dp)
)

/**
 * Android-specific check icon spacing fix
 * CheckCircle icons have excessive top spacing on Android, requiring Y offset
 */
actual fun Modifier.checkIconSpacing(): Modifier = this.then(
    Modifier.offset(y = (-2).dp)
)