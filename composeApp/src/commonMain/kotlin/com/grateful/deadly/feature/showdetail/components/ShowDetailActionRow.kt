package com.grateful.deadly.feature.showdetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
import com.grateful.deadly.core.ui.playButtonAlignment

/**
 * ShowDetailActionRow - Action buttons row matching V2's PlaylistActionRow exactly
 *
 * Features from V2:
 * - Left: Grouped action buttons (Library, Download, Setlist, Collections, Menu)
 * - Right: Large play/pause button with loading states
 * - Proper icon states and colors based on data state
 * - Progress indicators for downloads and playback loading
 */
@Composable
fun ShowDetailActionRow(
    showData: Show,
    isPlaying: Boolean = false,
    isLoading: Boolean = false,
    isCurrentShowAndRecording: Boolean = false,
    downloadProgress: Float? = null, // null = not downloaded, 0.0-1.0 = progress, >=1.0 = complete
    showCollections: List<String> = emptyList(), // Collection IDs for this show
    onLibraryAction: () -> Unit,
    onDownload: () -> Unit,
    onShowSetlist: () -> Unit,
    onShowCollections: () -> Unit,
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
            // Library button - shows different states
            IconButton(
                onClick = onLibraryAction,
                modifier = Modifier.size(40.dp)
            ) {
                val iconToUse = if (showData.isInLibrary) {
                    AppIcon.LibraryAddCheck
                } else {
                    AppIcon.LibraryAdd
                }

                iconToUse.Render(
                    size = 24.dp,
                    tint = if (showData.isInLibrary) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Download button with progress states
            IconButton(
                onClick = onDownload,
                modifier = Modifier.size(40.dp)
            ) {
                when {
                    downloadProgress == null -> {
                        // Not downloaded
                        AppIcon.FileDownload.Render(
                            size = 24.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    downloadProgress < 1.0f -> {
                        // Downloading - show progress
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(24.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                            )
                            AppIcon.FileDownload.Render(
                                size = 12.dp,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    else -> {
                        // Downloaded - show completed state
                        AppIcon.CheckCircle.Render(
                            size = 24.dp,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
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

            // Collections button - shows active state when show is in collections
            IconButton(
                onClick = onShowCollections,
                modifier = Modifier.size(40.dp)
            ) {
                AppIcon.Collections.Render(
                    size = 24.dp,
                    tint = if (showCollections.isNotEmpty()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
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

        // Right side: Play/Pause button (large) with loading state and circle background
        IconButton(
            onClick = onTogglePlayback,
            modifier = Modifier
                .size(56.dp)
                .playButtonAlignment()
        ) {
            if (isLoading) {
                // Show loading spinner when any track is loading
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp)
                ) {
                    // Circle background for loading state
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {}
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            } else {
                // Show pause icon only if currently playing this exact show/recording
                val showPauseIcon = isCurrentShowAndRecording && isPlaying

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp)
                ) {
                    // Circle background matching V2
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {}

                    // Play/Pause icon
                    val iconToUse = if (showPauseIcon) {
                        AppIcon.PauseCircleFilled
                    } else {
                        AppIcon.PlayCircleFilled
                    }

                    iconToUse.Render(
                        size = 32.dp, // Smaller icon inside the circle
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}