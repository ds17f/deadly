package com.grateful.deadly.feature.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.PlatformDimens
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

/**
 * Exact V2 secondary controls component with precise specifications.
 *
 * V2 Exact Features:
 * - Layout: Row with SpaceBetween arrangement
 * - All buttons: 40dp IconButton size
 * - Left: Connect (Cast icon)
 * - Right section: Row containing Share and Queue icons
 * - Colors: onSurfaceVariant tint for all icons
 * - Padding: 24dp horizontal, 12dp vertical
 */
@Composable
fun PlayerSecondaryControls(
    onConnect: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onQueue: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Row with SpaceBetween arrangement, platform-specific horizontal padding, 12dp vertical padding
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PlatformDimens.playerHorizontalPadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // V2 Exact: Connect button (Cast icon) on left - 40dp IconButton
        if (onConnect != null) {
            IconButton(
                onClick = onConnect,
                modifier = Modifier.size(40.dp)
            ) {
                AppIcon.Cast.Render(
                    size = 24.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Spacer to maintain layout when no connect button
            Spacer(modifier = Modifier.size(40.dp))
        }

        // V2 Exact: Right section - Row containing Share and Queue icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Share button - 40dp IconButton
            if (onShare != null) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(40.dp)
                ) {
                    AppIcon.Share.Render(
                        size = 24.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Queue button - 40dp IconButton
            if (onQueue != null) {
                IconButton(
                    onClick = onQueue,
                    modifier = Modifier.size(40.dp)
                ) {
                    AppIcon.QueueMusic.Render(
                        size = 24.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}