package com.grateful.deadly.feature.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.AppDimens
import com.grateful.deadly.services.media.MediaPlaybackState

/**
 * Exact V2 progress control component with precise specifications.
 *
 * V2 Exact Features:
 * - Layout: Column with 2dp vertical spacing
 * - Slider: Full width with 2dp vertical padding
 * - Colors: Primary thumb/active track, onSurface.copy(alpha = 0.24f) inactive
 * - Time labels: Row with SpaceBetween, bodySmall typography, onSurfaceVariant color
 * - Horizontal padding: 24dp
 */
@Composable
fun PlayerProgressControl(
    playbackState: MediaPlaybackState,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }

    // Use drag progress when actively scrubbing, otherwise use actual progress
    val displayProgress = if (isDragging) dragProgress else playbackState.progress
    val hasTrack = playbackState.currentTrack != null

    // V2 Exact: Column with 2dp vertical spacing, 24dp horizontal padding
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // V2 Exact: Slider with 2dp vertical padding
        Slider(
            value = displayProgress,
            onValueChange = { progress ->
                isDragging = true
                dragProgress = progress
            },
            onValueChangeFinished = {
                isDragging = false
                val newPosition = (playbackState.durationMs * dragProgress).toLong()
                onSeek(newPosition)
            },
            valueRange = 0f..1f,
            enabled = hasTrack && playbackState.durationMs > 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            colors = SliderDefaults.colors(
                // V2 Exact: Primary thumb and active track
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                // V2 Exact: onSurface.copy(alpha = 0.24f) for inactive
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledInactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        )

        // V2 Exact: Time labels - Row with SpaceBetween, bodySmall, onSurfaceVariant
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Current position
            Text(
                text = if (hasTrack) playbackState.formattedPosition else "--:--",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Duration
            Text(
                text = if (hasTrack) playbackState.formattedDuration else "--:--",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}