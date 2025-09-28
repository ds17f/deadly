package com.grateful.deadly.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs

/**
 * Exact V2 recording-based gradient background system with Grateful Dead color palette.
 *
 * Replicates the precise V2 gradient algorithm:
 * - 5 Grateful Dead colors with hash-based selection
 * - lerp blending with background at 0.8f, 0.4f, 0.1f alpha levels
 * - 5-stop gradient: strong → medium → faint → background → background
 * - Vertical gradient from top to bottom
 * - Falls back to theme colors when no recording ID
 */
@Composable
fun PlayerGradientBackground(
    recordingId: String?,
    modifier: Modifier = Modifier
) {
    val gradient = if (recordingId != null) {
        createRecordingGradient(recordingId)
    } else {
        createDefaultGradient()
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
    )
}

/**
 * Creates the exact V2 recording gradient using lerp blending.
 * Returns a Brush that can be applied to any container.
 */
@Composable
fun createRecordingGradient(recordingId: String): Brush {
    val backgroundColor = MaterialTheme.colorScheme.background
    val deadColors = getV2DeadlyColors()

    // Use V2's hash algorithm
    val hash = abs(recordingId.hashCode())
    val colorIndex = hash % deadColors.size
    val baseColor = deadColors[colorIndex]

    // V2's exact lerp blending with background
    val strongColor = lerp(backgroundColor, baseColor, 0.8f)
    val mediumColor = lerp(backgroundColor, baseColor, 0.4f)
    val faintColor = lerp(backgroundColor, baseColor, 0.1f)

    // 5-stop gradient matching V2 exactly
    return Brush.verticalGradient(
        colorStops = arrayOf(
            0f to strongColor,      // Top: strong blend
            0.3f to mediumColor,    // Upper: medium blend
            0.6f to faintColor,     // Middle: faint blend
            0.8f to backgroundColor, // Lower: background
            1f to backgroundColor   // Bottom: background
        )
    )
}


/**
 * Creates default gradient using Material 3 theme colors (V2 fallback behavior).
 */
@Composable
private fun createDefaultGradient(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

/**
 * Exact V2 Grateful Dead color palette (5 colors matching V2 implementation).
 */
private fun getV2DeadlyColors(): List<Color> {
    return listOf(
        Color(0xFFDC143C),  // DeadRed - Crimson red
        Color(0xFFFFD700),  // DeadGold - Golden yellow
        Color(0xFF228B22),  // DeadGreen - Forest green
        Color(0xFF4169E1),  // DeadBlue - Royal blue
        Color(0xFF8A2BE2)   // DeadPurple - Blue violet
    )
}