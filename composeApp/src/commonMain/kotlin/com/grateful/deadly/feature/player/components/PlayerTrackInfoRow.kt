package com.grateful.deadly.feature.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.AppDimens
import com.grateful.deadly.core.design.PlatformDimens
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
import com.grateful.deadly.services.media.MediaPlaybackState

/**
 * V2 track info row component with platform-optimized specifications.
 *
 * Features:
 * - Layout: Row with SpaceBetween arrangement
 * - Spacing: Column with 4dp vertical spacing
 * - Title: headlineSmall, FontWeight.Bold, single line with marquee scrolling
 * - Show date: bodyLarge, onSurfaceVariant color
 * - Venue: bodyMedium, onSurfaceVariant color, 2 maxLines, ellipsis
 * - Add button: IconButton 36dp size with AddCircle icon, primary color tint
 * - Horizontal padding: Platform-specific (24dp Android, 16dp iOS)
 */
@Composable
fun PlayerTrackInfoRow(
    playbackState: MediaPlaybackState,
    onAddToPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Row with SpaceBetween arrangement, platform-specific horizontal padding
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PlatformDimens.playerHorizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // V2 Exact: Column with 4dp vertical spacing for track info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Track title - headlineSmall, FontWeight.Bold, single line with marquee scrolling
            Text(
                text = playbackState.currentTrack?.title ?: "No Track Selected",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )

            // V2 Exact: Show date - bodyLarge, onSurfaceVariant color
            Text(
                text = playbackState.showDate ?: "Show date loading...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // V2 Exact: Venue - bodyMedium, onSurfaceVariant color, 2 maxLines
            Text(
                text = when {
                    !playbackState.venue.isNullOrBlank() -> playbackState.venue!!
                    !playbackState.location.isNullOrBlank() -> playbackState.location!!
                    playbackState.currentTrack != null -> "Venue loading..."
                    else -> "No venue information"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // V2 Exact: Add button - IconButton 36dp size with primary color
        if (onAddToPlaylist != null) {
            IconButton(
                onClick = onAddToPlaylist,
                modifier = Modifier.size(36.dp)
            ) {
                AppIcon.LibraryAdd.Render(
                    size = 24.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

