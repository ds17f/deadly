package com.grateful.deadly.feature.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
import com.grateful.deadly.services.media.MediaPlaybackState
import com.grateful.deadly.services.media.MediaService
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState

/**
 * Exact V2 Mini Player component that appears when scrolling past media controls.
 *
 * V2 Exact Features:
 * - 72dp height Card with 8dp elevation, 0dp corner radius
 * - Uses medium color from recording's color stack (index 1: alpha 0.4f blend)
 * - Track info (title + album) with white text and click to expand
 * - Play/Pause IconButton (48dp) with 24dp icons in white
 * - LinearProgressIndicator at bottom (3dp height, no thumb)
 * - White progress color, 0.3f alpha track color
 * - 16dp horizontal, 8dp vertical padding for main content
 */
@Composable
fun PlayerMiniPlayer(
    mediaService: MediaService,
    onPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Get playback state
    val playbackState = mediaService.playbackState.collectAsState(
        initial = com.grateful.deadly.services.media.MediaPlaybackState(
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
    ).value

    // V2 Exact: Use medium color from recording's color stack (index 1)
    val backgroundColor = getRecordingColorStack(playbackState.currentRecordingId)[1]

    Card(
        modifier = modifier
            .height(72.dp)
            .clickable { onPlayerClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column {
            // V2 Exact: Main content row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // V2 Exact: Track info (clickable area for expand)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPlayerClick() }
                ) {
                    Text(
                        text = playbackState.currentTrack?.title ?: "No Track",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = playbackState.currentTrack?.name ?: "Unknown Track",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // V2 Exact: Play/Pause button (NOT clickable for expansion) - 48dp IconButton
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            if (playbackState.isPlaying) {
                                mediaService.pause()
                            } else {
                                mediaService.resume()
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    if (playbackState.isPlaying) {
                        AppIcon.Pause.Render(
                            size = 24.dp,
                            tint = Color.White
                        )
                    } else {
                        AppIcon.PlayArrow.Render(
                            size = 24.dp,
                            tint = Color.White
                        )
                    }
                }
            }

            // V2 Exact: Progress bar at bottom (3dp height, without thumb)
            val progress = if (playbackState.durationMs > 0) {
                (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * V2 Exact: Get the complete color stack for a recording.
 * Returns list of solid colors using color blending instead of alpha transparency.
 * Uses the same Grateful Dead color system as the main gradient.
 */
@Composable
private fun getRecordingColorStack(recordingId: String?): List<Color> {
    val baseColor = recordingIdToColor(recordingId)
    val background = MaterialTheme.colorScheme.background

    return listOf(
        lerp(background, baseColor, 0.8f),  // Index 0: Strong blend
        lerp(background, baseColor, 0.4f),  // Index 1: Medium blend
        lerp(background, baseColor, 0.1f),  // Index 2: Faint blend
        background,                         // Index 3: Background
        background                          // Index 4: Background
    )
}

/**
 * V2 Exact: Convert recordingId to a consistent base color using hash function.
 * Uses the same Grateful Dead color palette as the main gradient system.
 */
private fun recordingIdToColor(recordingId: String?): Color {
    if (recordingId.isNullOrEmpty()) return DeadRed

    val hash = recordingId.hashCode()
    val index = kotlin.math.abs(hash) % GradientColors.size
    return GradientColors[index]
}

// V2 Exact: Grateful Dead inspired color palette (matching main gradient system)
private val DeadRed = Color(0xFFDC143C)      // Crimson red
private val DeadGold = Color(0xFFFFD700)     // Golden yellow
private val DeadGreen = Color(0xFF228B22)    // Forest green
private val DeadBlue = Color(0xFF4169E1)     // Royal blue
private val DeadPurple = Color(0xFF8A2BE2)   // Blue violet

private val GradientColors = listOf(DeadGreen, DeadGold, DeadRed, DeadBlue, DeadPurple)