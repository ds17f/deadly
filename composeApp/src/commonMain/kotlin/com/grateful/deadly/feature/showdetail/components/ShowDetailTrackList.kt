package com.grateful.deadly.feature.showdetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grateful.deadly.services.archive.Track
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

/**
 * ShowDetailTrackList - Professional track list component
 *
 * Matches V2's PlaylistTrackList with:
 * - Track number, title, duration, format
 * - Individual play and download buttons per track
 * - Proper spacing and typography
 * - Professional layout matching streaming services
 */
fun LazyListScope.ShowDetailTrackList(
    tracks: List<Track>,
    onPlayClick: (Track) -> Unit,
    onDownloadClick: (Track) -> Unit
) {
    if (tracks.isNotEmpty()) {
        // Track list header
        item {
            Text(
                text = "Tracks (${tracks.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }

        // Individual tracks
        itemsIndexed(tracks) { index, track ->
            ShowDetailTrackItem(
                track = track,
                trackIndex = index + 1,
                onPlayClick = { onPlayClick(track) },
                onDownloadClick = { onDownloadClick(track) }
            )
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    } else {
        // Empty state
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tracks available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShowDetailTrackItem(
    track: Track,
    trackIndex: Int,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Track info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Track number
                Text(
                    text = "${track.trackNumber ?: trackIndex}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                // Track title
                Text(
                    text = track.title ?: track.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Duration and format
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                track.duration?.let { duration ->
                    Text(
                        text = duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = track.format,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Show file size if available
                track.size?.let { size ->
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = formatFileSize(size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Right: Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Download button
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(32.dp)
            ) {
                AppIcon.FileDownload.Render(
                    size = 18.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Play button
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(32.dp)
            ) {
                AppIcon.PlayArrow.Render(
                    size = 18.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Format file size string (e.g., "5.2 MB")
 */
private fun formatFileSize(sizeString: String): String {
    return try {
        val sizeBytes: Long = sizeString.toLongOrNull() ?: return sizeString
        when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            sizeBytes < 1024 * 1024 * 1024 -> "${sizeBytes / (1024 * 1024)} MB"
            else -> "${sizeBytes / (1024 * 1024 * 1024)} GB"
        }
    } catch (e: Exception) {
        sizeString
    }
}