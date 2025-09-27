package com.grateful.deadly.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.AppDimens
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
import com.grateful.deadly.services.media.MediaPlaybackState
import com.grateful.deadly.services.media.MediaService

/**
 * Mini player component that appears at the bottom of the app when audio is playing.
 *
 * Follows V2's proven mini player patterns with track info, progress, and controls.
 * Designed to be embedded in AppScaffold for global access across all screens.
 */
@Composable
fun MiniPlayer(
    mediaService: MediaService,
    onPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
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

    // Only show when there's a current track
    if (playbackState.currentTrack != null) {
        MiniPlayerContent(
            playbackState = playbackState,
            onPlayPause = {
                // Use MediaService's reactive play/pause logic
                if (playbackState.isPlaying) {
                    mediaService::pause
                } else {
                    mediaService::resume
                }
            },
            onPlayerClick = onPlayerClick,
            modifier = modifier
        )
    }
}

/**
 * Mini player content layout following V2's design patterns.
 */
@Composable
private fun MiniPlayerContent(
    playbackState: MediaPlaybackState,
    onPlayPause: suspend () -> Unit,
    onPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onPlayerClick() }
    ) {
        // Progress bar (following V2's thin progress indicator)
        LinearProgressIndicator(
            progress = { playbackState.progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            gapSize = 0.dp,
            drawStopIndicator = {}
        )

        // Main content row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.S),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track info (expandable)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playbackState.currentTrack?.title ?: "Unknown Track",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Grateful Dead", // Following V2 pattern
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(AppDimens.S))

            // Time display (current/total)
            Text(
                text = "${playbackState.formattedPosition} / ${playbackState.formattedDuration}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(AppDimens.S))

            // Play/pause button
            MiniPlayerPlayButton(
                isPlaying = playbackState.isPlaying,
                isLoading = playbackState.isLoading,
                onPlayPause = onPlayPause
            )
        }
    }
}

/**
 * Play/pause button for mini player following V2's design patterns.
 */
@Composable
private fun MiniPlayerPlayButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPause: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
            .clickable {
                // TODO: Launch coroutine for suspend function
                // For now, using simple click handling
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            isPlaying -> {
                AppIcon.PauseCircleFilled.Render(
                    size = AppDimens.IconSize.Medium,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            else -> {
                AppIcon.PlayCircleFilled.Render(
                    size = AppDimens.IconSize.Medium,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}