package com.grateful.deadly.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.AppDimens
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
import com.grateful.deadly.services.media.MediaPlaybackState
import com.grateful.deadly.services.media.MediaService
import kotlinx.coroutines.launch

/**
 * Mini player component following V2's card-based design with progress bar.
 *
 * Features:
 * - Card-based layout with elevation and rounded corners
 * - Thin progress bar at top following V2 patterns
 * - Compact track info with proper typography hierarchy
 * - Play/pause button with loading states
 * - Dark themed design matching V2 mini player
 * - Proper spacing and Material 3 theming
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
    val coroutineScope = rememberCoroutineScope()

    // Only show when there's a current track
    if (playbackState.currentTrack != null) {
        MiniPlayerContent(
            playbackState = playbackState,
            onPlayPause = {
                coroutineScope.launch {
                    if (playbackState.isPlaying) {
                        mediaService.pause()
                    } else {
                        mediaService.resume()
                    }
                }
            },
            onPlayerClick = onPlayerClick,
            modifier = modifier
        )
    }
}

/**
 * Mini player content following V2's card-based design.
 */
@Composable
private fun MiniPlayerContent(
    playbackState: MediaPlaybackState,
    onPlayPause: () -> Unit,
    onPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.S, vertical = AppDimens.XS)
            .clickable { onPlayerClick() },
        shape = RoundedCornerShape(AppDimens.CornerRadius.Medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column {
            // Thin progress bar at top (V2 style)
            LinearProgressIndicator(
                progress = { playbackState.progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent, // Invisible track for cleaner look
                strokeCap = StrokeCap.Round
            )

            // Main content row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.M),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Small cover art placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(AppDimens.CornerRadius.Small))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon.MusicNote.Render(
                        size = 20.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.width(AppDimens.M))

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
                        text = "Grateful Dead • ${playbackState.formattedPosition}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(AppDimens.M))

                // Play/pause button
                MiniPlayerPlayButton(
                    isPlaying = playbackState.isPlaying,
                    isLoading = playbackState.isLoading,
                    onPlayPause = onPlayPause
                )
            }
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
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable { onPlayPause() },
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = if (isPlaying) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            isPlaying -> {
                AppIcon.Pause.Render(
                    size = AppDimens.IconSize.Small,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            else -> {
                AppIcon.PlayArrow.Render(
                    size = AppDimens.IconSize.Small,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}