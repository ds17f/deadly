package com.grateful.deadly.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.AppDimens
import com.grateful.deadly.core.design.PlatformDimens
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
import com.grateful.deadly.services.media.MediaPlaybackState

/**
 * Exact V2 enhanced playback controls with precise specifications.
 *
 * V2 Exact Features:
 * - Layout: Row with full width, center vertical alignment
 * - Shuffle/Repeat: 40dp IconButton size on far left/right
 * - Previous/Next: 56dp IconButton size with 36dp icon size
 * - Play/Pause: FloatingActionButton 72dp size with 36dp icon or 24dp CircularProgressIndicator
 * - Spacing: 16dp between center controls, Spacer(weight = 1f) for outer positioning
 * - Colors: Primary for enabled states, onSurfaceVariant for secondary, onSurface.copy(alpha = 0.38f) for disabled
 */
@Composable
fun PlayerEnhancedControls(
    playbackState: MediaPlaybackState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: (() -> Unit)? = null,
    onRepeat: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Row with full width, center vertical alignment, platform-specific padding
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PlatformDimens.playerHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // V2 Exact: Shuffle button (40dp) on far left
        if (onShuffle != null) {
            IconButton(
                onClick = onShuffle,
                modifier = Modifier.size(40.dp)
            ) {
                AppIcon.Shuffle.Render(
                    size = 24.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // V2 Exact: Spacer(weight = 1f) for positioning
        Spacer(modifier = Modifier.weight(1f))

        // V2 Exact: Previous button (56dp with 36dp icon)
        IconButton(
            onClick = onPrevious,
            enabled = playbackState.hasPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            AppIcon.SkipPrevious.Render(
                size = 36.dp,
                tint = if (playbackState.hasPrevious) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }

        // V2 Exact: 16dp spacing between center controls
        Spacer(modifier = Modifier.width(16.dp))

        // V2 Exact: FloatingActionButton (72dp) with 36dp icon or 24dp loading
        FloatingActionButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            when {
                playbackState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                playbackState.isPlaying -> {
                    AppIcon.Pause.Render(
                        size = 36.dp,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                else -> {
                    AppIcon.PlayArrow.Render(
                        size = 36.dp,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // V2 Exact: 16dp spacing between center controls
        Spacer(modifier = Modifier.width(16.dp))

        // V2 Exact: Next button (56dp with 36dp icon)
        IconButton(
            onClick = onNext,
            enabled = playbackState.hasNext,
            modifier = Modifier.size(56.dp)
        ) {
            AppIcon.SkipNext.Render(
                size = 36.dp,
                tint = if (playbackState.hasNext) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }

        // V2 Exact: Spacer(weight = 1f) for positioning
        Spacer(modifier = Modifier.weight(1f))

        // V2 Exact: Repeat button (40dp) on far right
        if (onRepeat != null) {
            IconButton(
                onClick = onRepeat,
                modifier = Modifier.size(40.dp)
            ) {
                AppIcon.Repeat.Render(
                    size = 24.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


