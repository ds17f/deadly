package com.grateful.deadly.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.AppDimens
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
// BarConfiguration will be available when navigation is properly wired
import com.grateful.deadly.services.media.MediaPlaybackState
import com.grateful.deadly.services.media.MediaService
import com.grateful.deadly.services.archive.Track
import kotlinx.coroutines.launch

/**
 * Full-screen player following V2's proven patterns.
 *
 * Features:
 * - Large track info display with show metadata
 * - Progress slider with time scrubbing
 * - Full playback controls (previous, play/pause, next)
 * - Track queue with current track highlighting
 * - Reactive state management via MediaService
 */
@Composable
fun PlayerScreen(
    mediaService: MediaService,
    onNavigateBack: () -> Unit
) {
    val playbackState by mediaService.playbackState.collectAsState(
        initial = MediaPlaybackState(
            currentTrack = null,
            isPlaying = false,
            currentPositionMs = 0L,
            durationMs = 0L,
            isLoading = false,
            isBuffering = false,
            error = null,
            hasNext = false,
            hasPrevious = false,
            playlistPosition = 0,
            playlistSize = 0
        )
    )
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(AppDimens.M)
    ) {
        // Track info section
        PlayerTrackInfo(
            playbackState = playbackState,
            modifier = Modifier.weight(1f)
        )

        // Progress section
        PlayerProgressSection(
            playbackState = playbackState,
            onSeek = { positionMs ->
                coroutineScope.launch {
                    mediaService.seekTo(positionMs)
                }
            },
            modifier = Modifier.padding(vertical = AppDimens.L)
        )

        // Control buttons
        PlayerControls(
            playbackState = playbackState,
            onPrevious = {
                coroutineScope.launch {
                    mediaService.previousTrack()
                }
            },
            onPlayPause = {
                coroutineScope.launch {
                    if (playbackState.isPlaying) {
                        mediaService.pause()
                    } else {
                        mediaService.resume()
                    }
                }
            },
            onNext = {
                coroutineScope.launch {
                    mediaService.nextTrack()
                }
            },
            modifier = Modifier.padding(vertical = AppDimens.L)
        )

        // Queue info (simplified for now)
        if (playbackState.playlistSize > 1) {
            PlayerQueueInfo(
                playbackState = playbackState,
                modifier = Modifier.padding(top = AppDimens.M)
            )
        }
    }
}

/**
 * Track info section with large display following V2 patterns.
 */
@Composable
private fun PlayerTrackInfo(
    playbackState: MediaPlaybackState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Track title
        Text(
            text = playbackState.currentTrack?.title ?: "No Track",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(AppDimens.S))

        // Artist (always Grateful Dead for now)
        Text(
            text = "Grateful Dead",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // TODO: Add album/show info when available
        // This would come from MediaService context in future iterations
    }
}

/**
 * Progress section with scrubbing support following V2 patterns.
 */
@Composable
private fun PlayerProgressSection(
    playbackState: MediaPlaybackState,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Progress slider
        Slider(
            value = playbackState.progress,
            onValueChange = { progress ->
                val newPosition = (playbackState.durationMs * progress).toLong()
                onSeek(newPosition)
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
        )

        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = playbackState.formattedPosition,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = playbackState.formattedDuration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Main playback controls following V2's button layout.
 */
@Composable
private fun PlayerControls(
    playbackState: MediaPlaybackState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        PlayerControlButton(
            icon = AppIcon.ArrowLeft, // Using available icon
            enabled = playbackState.hasPrevious,
            onClick = onPrevious
        )

        // Main play/pause button (larger)
        PlayerMainButton(
            isPlaying = playbackState.isPlaying,
            isLoading = playbackState.isLoading,
            onClick = onPlayPause
        )

        // Next button
        PlayerControlButton(
            icon = AppIcon.ArrowRight, // Using available icon
            enabled = playbackState.hasNext,
            onClick = onNext
        )
    }
}

/**
 * Standard control button for previous/next.
 */
@Composable
private fun PlayerControlButton(
    icon: AppIcon,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                if (enabled) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                }
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        icon.Render(
            size = AppDimens.IconSize.Large,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            }
        )
    }
}

/**
 * Large main play/pause button following V2 patterns.
 */
@Composable
private fun PlayerMainButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            isPlaying -> {
                AppIcon.PauseCircleFilled.Render(
                    size = AppDimens.IconSize.XLarge,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            else -> {
                AppIcon.PlayCircleFilled.Render(
                    size = AppDimens.IconSize.XLarge,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * Queue information display.
 */
@Composable
private fun PlayerQueueInfo(
    playbackState: MediaPlaybackState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Track ${playbackState.playlistPosition} of ${playbackState.playlistSize}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Note: BarConfiguration will be handled by the navigation system