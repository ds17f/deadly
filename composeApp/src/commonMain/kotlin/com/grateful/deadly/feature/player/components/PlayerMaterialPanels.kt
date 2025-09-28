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
        // V2 Exact: About the Venue Panel
        MaterialPanel(
            title = "About the Venue",
            content = "Barton Hall at Cornell University in Ithaca, New York, is legendary among Deadheads for hosting one of the greatest Grateful Dead concerts of all time on May 8, 1977. The show is often cited as the pinnacle of the band's creative peak during their spring 1977 tour."
        )

        // V2 Exact: Lyrics Panel
        MaterialPanel(
            title = "Lyrics",
            content = "Scarlet begonias tucked into her curls\nI knew right away she was not like other girls\nOther girls\nWell I ain't often right but I've never been wrong\nSeldom turns out the way it does in a song\nOnce in a while you get shown the light\nIn the strangest of places if you look at it right"
        )

        // V2 Exact: Similar Shows Panel
        MaterialPanel(
            title = "Similar Shows",
            content = "Other standout shows from Spring 1977 include Boston Music Hall (May 7), Buffalo Memorial Auditorium (May 9), and Hartford Civic Center (May 28). This tour is considered the creative peak of the Grateful Dead."
        )

        // V2 Exact: Credits Panel
        MaterialPanel(
            title = "Credits",
            content = "Jerry Garcia - Lead Guitar, Vocals\nBob Weir - Rhythm Guitar, Vocals\nPhil Lesh - Bass, Vocals\nBill Kreutzmann - Drums\nMickey Hart - Drums\nKeith Godchaux - Piano\nDonna Jean Godchaux - Vocals"
        )
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