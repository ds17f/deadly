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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.AppDimens
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
import com.grateful.deadly.services.media.MediaPlaybackState
import com.grateful.deadly.services.media.MediaService
import kotlinx.coroutines.launch

/**
 * Global MiniPlayer component following exact V2 design.
 *
 * V2 Exact Features:
 * - 68dp height Card with 8dp elevation and top rounded corners only
 * - Dark red-brown color (0xFF2D1B1B) for V2 theme consistency
 * - Track title and show date/venue subtitle
 * - Play/pause button with loading states (40dp)
 * - Progress bar at bottom (2dp height) with primary color
 * - 10dp padding for compact V2 layout
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
            currentRecordingId = null,
            showDate = null,
            venue = null,
            location = null,
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
            .height(68.dp)
            .clickable { onPlayerClick() },
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D1B1B) // V2 Exact: Dark red-brown
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column {
            // V2 Exact: Main content row with 10dp padding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // V2 Exact: Track information (no cover art)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // V2 Exact: Track title (songTitle)
                    Text(
                        text = playbackState.currentTrack?.title ?: "Unknown Track",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White // V2 Exact: White text on dark background
                    )
                    // V2 Exact: Show subtitle (displaySubtitle - showDate - venue)
                    Text(
                        text = playbackState.displaySubtitle.ifBlank { "Grateful Dead" },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White.copy(alpha = 0.7f) // V2 Exact: Semi-transparent white
                    )
                }

                // V2 Exact: Play/pause button (40dp IconButton)
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(40.dp)
                ) {
                    if (playbackState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        if (playbackState.isPlaying) {
                            AppIcon.Pause.Render(
                                size = 18.dp,
                                tint = Color.White
                            )
                        } else {
                            AppIcon.PlayArrow.Render(
                                size = 18.dp,
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // V2 Exact: Progress bar at bottom (2dp height)
            LinearProgressIndicator(
                progress = { playbackState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        }
    }
}

// MiniPlayerPlayButton component removed - now using inline IconButton to match V2 exactly