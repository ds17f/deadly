package com.grateful.deadly.feature.library.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grateful.deadly.domain.models.LibraryShowViewModel
import com.grateful.deadly.domain.models.LibraryDownloadStatus
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

/**
 * KMM Library Show List Item Component
 *
 * Focused component for displaying library shows in list view.
 * Shows date, venue, location, pin status, and download status.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryShowListItem(
    show: LibraryShowViewModel,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album cover placeholder
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                AppIcon.AlbumArt.Render(
                    size = 30.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Show info - 2 line layout
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Line 1: Date • City, State with status icons
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pin indicator
                    if (show.isPinned) {
                        AppIcon.PushPin.Render(
                            size = 12.dp,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Download indicator
                    if (show.downloadStatus == LibraryDownloadStatus.COMPLETED) {
                        AppIcon.CheckCircle.Render(
                            size = 12.dp,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = "${show.displayDate} • ${show.location}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Line 2: Venue
                Text(
                    text = show.venue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Line 3: Rating if available
                show.rating?.let { rating ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        AppIcon.Star.Render(
                            size = 10.dp,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${(rating * 10).toInt() / 10.0f}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (show.reviewCount > 0) {
                            Text(
                                text = " (${show.reviewCount})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * KMM Library Show Grid Item Component
 *
 * Focused component for displaying library shows in grid view.
 * Compact layout showing essential information.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryShowGridItem(
    show: LibraryShowViewModel,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Album cover - square aspect ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                AppIcon.AlbumArt.Render(
                    size = 32.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Text section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Line 1: Date with status icons
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pin indicator
                    if (show.isPinned) {
                        AppIcon.PushPin.Render(
                            size = 8.dp,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    // Download indicator
                    if (show.downloadStatus == LibraryDownloadStatus.COMPLETED) {
                        AppIcon.CheckCircle.Render(
                            size = 8.dp,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    Text(
                        text = show.date,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Line 2: Venue
                Text(
                    text = show.venue,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Line 3: Location
                Text(
                    text = show.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}