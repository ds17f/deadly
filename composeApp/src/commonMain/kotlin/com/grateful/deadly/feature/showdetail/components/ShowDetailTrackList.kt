package com.grateful.deadly.feature.showdetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grateful.deadly.services.archive.Track
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

/**
 * ShowDetailTrackList - Simple track list matching V2's PlaylistTrackList exactly
 *
 * V2 pattern: Clean clickable rows with no cards, no buttons - just content
 * - Simple two-line layout: title, then "format • duration"
 * - Clickable rows for play action
 * - Optional download indicator for downloaded tracks
 * - Clean padding: horizontal 24dp, vertical 12dp
 */
fun LazyListScope.ShowDetailTrackList(
    tracks: List<Track>,
    onPlayClick: (Track) -> Unit,
    onDownloadClick: (Track) -> Unit
) {
    // Section header - matches V2 exactly
    item {
        Text(
            text = "Tracks (${tracks.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }

    // Track items - simple clickable rows like V2
    items(
        items = tracks,
        key = { track -> track.name }
    ) { track ->
        ShowDetailTrackItem(
            track = track,
            onPlayClick = onPlayClick,
            onDownloadClick = onDownloadClick
        )
    }

    // Bottom spacing
    item {
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * ShowDetailTrackItem - Individual track row matching V2's PlaylistTrackItem exactly
 *
 * Simple, clean design focused on browsing tracks with minimal visual complexity.
 * Matches V2's visual layout and behavior exactly.
 */
@Composable
private fun ShowDetailTrackItem(
    track: Track,
    onPlayClick: (Track) -> Unit,
    onDownloadClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlayClick(track) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track info - matches V2 layout exactly
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            // Track title
            Text(
                text = track.title ?: track.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start
            )

            // Format and duration line - matches V2 format exactly
            Text(
                text = "${track.format} • ${track.duration ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }

        // Download indicator - only shown if track is downloaded (TODO: track download state)
        // For now, we don't show download indicator since we don't have download state tracking yet
        // if (track.isDownloaded) {
        //     AppIcon.CheckCircle.Render(
        //         size = 16.dp,
        //         tint = MaterialTheme.colorScheme.primary
        //     )
        // }
    }
}