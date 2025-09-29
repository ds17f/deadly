package com.grateful.deadly.feature.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            .padding(horizontal = AppDimens.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.size(48.dp)
        ) {
            AppIcon.ArrowBack.Render(
                size = AppDimens.IconSize.Medium,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(AppDimens.S))

        // V2 Exact: Simple "Playing from Show" context text (clickable like V2)
        Text(
            text = "Playing from Show",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onContextClick != null) {
                        Modifier.clickable { onContextClick() }
                    } else {
                        Modifier
                    }
                )
        )

        // Options menu button (if callback provided)
        onOptionsClick?.let { callback ->
            Spacer(modifier = Modifier.width(AppDimens.S))

            IconButton(
                onClick = callback,
                modifier = Modifier.size(48.dp)
            ) {
                AppIcon.MoreVertical.Render(
                    size = AppDimens.IconSize.Medium,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}