package com.grateful.deadly.feature.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grateful.deadly.services.media.MediaPlaybackState

/**
 * Exact V2 Material3 panels for extended content.
 *
 * V2 Exact Features:
 * - Always-expanded Material3 panels
 * - ElevatedCard with 16dp corners, surfaceContainer color, 2dp elevation
 * - 20dp internal padding with 12dp vertical spacing
 * - titleMedium Bold + bodyMedium with increased line height
 * - Four panels: About the Venue, Lyrics, Similar Shows, Credits
 * - 16dp vertical spacing between panels
 */
@Composable
fun PlayerMaterialPanels(
    playbackState: MediaPlaybackState,
    modifier: Modifier = Modifier
) {
    // V2 Exact: Column with 16dp vertical spacing between panels
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // About the Venue Panel - only show when we have venue data
        if (playbackState.currentTrack != null) {
            MaterialPanel(
                title = "About the Venue",
                content = "Venue information will be displayed when show metadata is available."
            )

            // Lyrics Panel - only show when we have track data
            MaterialPanel(
                title = "Lyrics",
                content = "Lyrics will be displayed when available for ${playbackState.currentTrack?.title ?: "this track"}."
            )

            // Similar Shows Panel
            MaterialPanel(
                title = "Similar Shows",
                content = "Similar shows will be displayed when show data is loaded."
            )

            // Credits Panel
            MaterialPanel(
                title = "Credits",
                content = "Performance credits will be displayed when available."
            )
        }
    }
}

/**
 * V2 Exact: Beautiful Material3 panel component
 * - ElevatedCard with surfaceContainer color, 2dp elevation
 * - 16dp rounded corners, 20dp internal padding
 * - titleMedium Bold title + bodyMedium content with 1.2x line height
 * - 12dp vertical spacing between title and content
 */
@Composable
private fun MaterialPanel(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
            )
        }
    }
}