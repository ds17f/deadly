package com.grateful.deadly.feature.showdetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

/**
 * ShowDetailShowInfo - Show information with navigation
 *
 * Matches V2's PlaylistShowInfo with:
 * - Left: Show date, venue, location
 * - Right: Previous/Next navigation arrows
 * - Proper typography and spacing
 * - Navigation arrows for browsing through shows (doesn't affect back stack)
 */
@Composable
fun ShowDetailShowInfo(
    showData: Show,
    onPreviousShow: () -> Unit,
    onNextShow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left side: Show info
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            // Show Date (main title) - matches V2 exactly
            Text(
                text = showData.date,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Venue and Location - matches V2 format exactly
            val venueLine = "${showData.venue.name}, ${showData.location.displayText}"

            Text(
                text = venueLine,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right side: Navigation buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous show button
            IconButton(
                onClick = onPreviousShow,
                // TODO Phase 5: Enable/disable based on adjacent shows
                enabled = true,
                modifier = Modifier.size(40.dp)
            ) {
                AppIcon.ArrowLeft.Render(
                    size = 24.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Next show button
            IconButton(
                onClick = onNextShow,
                // TODO Phase 5: Enable/disable based on adjacent shows
                enabled = true,
                modifier = Modifier.size(40.dp)
            ) {
                AppIcon.ArrowRight.Render(
                    size = 24.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}