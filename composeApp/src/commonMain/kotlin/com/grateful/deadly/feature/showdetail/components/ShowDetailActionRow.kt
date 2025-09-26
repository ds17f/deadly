package com.grateful.deadly.feature.showdetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

/**
 * ShowDetailActionRow - Action buttons row
 *
 * Matches V2's PlaylistActionRow with:
 * - Left: Grouped action buttons (Library, Download, Setlist, Menu)
 * - Right: Large play/pause button
 * - Proper icon states and colors
 * - Loading states for downloads/playback
 */
@Composable
fun ShowDetailActionRow(
    showData: Show,
    onLibraryAction: () -> Unit,
    onDownload: () -> Unit,
    onShowSetlist: () -> Unit,
    onShowMenu: () -> Unit,
    onTogglePlayback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Grouped action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Library button
            IconButton(
                onClick = onLibraryAction,
                modifier = Modifier.size(40.dp)
            ) {
                AppIcon.LibraryMusic.Render(
                    size = 24.dp,
                    tint = if (showData.isInLibrary) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Download button
            IconButton(
                onClick = onDownload,
                modifier = Modifier.size(40.dp)
            ) {
                // TODO Phase 5: Show download progress states
                AppIcon.FileDownload.Render(
                    size = 24.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Setlist button
            IconButton(
                onClick = onShowSetlist,
                modifier = Modifier.size(40.dp)
            ) {
                AppIcon.FormatListBulleted.Render(
                    size = 24.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Menu button
            IconButton(
                onClick = onShowMenu,
                modifier = Modifier.size(40.dp)
            ) {
                AppIcon.MoreVertical.Render(
                    size = 24.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Right side: Large Play/Pause button
        IconButton(
            onClick = onTogglePlayback,
            modifier = Modifier.size(56.dp)
        ) {
            // TODO Phase 5: Show proper play/pause/loading states
            AppIcon.PlayCircleFilled.Render(
                size = 56.dp,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}