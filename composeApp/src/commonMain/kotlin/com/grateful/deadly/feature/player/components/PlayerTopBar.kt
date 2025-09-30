package com.grateful.deadly.feature.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.AppDimens
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
import com.grateful.deadly.services.media.MediaPlaybackState

/**
 * Player top bar component following V2's design with navigation and context.
 *
 * Features:
 * - Back navigation button
 * - Context text showing "Playing from..."
 * - Options menu for additional actions
 * - Proper spacing and typography
 * - Material 3 theming
 */
@Composable
fun PlayerTopBar(
    playbackState: MediaPlaybackState,
    onNavigateBack: () -> Unit,
    onContextClick: (() -> Unit)? = null,
    onOptionsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Down chevron (V2 exact: KeyboardArrowDown, 34dp)
        IconButton(onClick = onNavigateBack) {
            AppIcon.KeyboardArrowDown.Render(
                size = 34.dp,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // V2 Exact: Context text (clickable to navigate to playlist)
        Text(
            text = "Playing from Show",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = if (onContextClick != null) {
                Modifier.clickable { onContextClick() }
            } else {
                Modifier
            }
        )

        // 3-dot menu (V2 exact: MoreVertical, 28dp)
        IconButton(onClick = onOptionsClick ?: {}) {
            AppIcon.MoreVertical.Render(
                size = 28.dp,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}