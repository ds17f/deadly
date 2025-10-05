package com.grateful.deadly.feature.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.PlatformDimens
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

/**
 * Player cover art component with platform-optimized specifications.
 *
 * Features:
 * - Container height: Platform-specific (450dp Android, 380dp iOS)
 * - Horizontal padding: Platform-specific (24dp Android, 16dp iOS)
 * - Shape: Card with RoundedCornerShape(16.dp)
 * - Aspect ratio: 1:1 (square) within the platform-specific container
 * - Colors: primaryContainer background, onPrimaryContainer icon tint
 * - Icon: AlbumArt icon at 160dp size
 * - Layout: Box with Center alignment containing Card
 */
@Composable
fun PlayerCoverArt(
    modifier: Modifier = Modifier
) {
    // Platform-specific height container with platform-specific horizontal padding
    Box(
        modifier = modifier
            .height(PlatformDimens.coverArtHeight)
            .fillMaxWidth()
            .padding(horizontal = PlatformDimens.playerHorizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        // V2 Exact: Card with 16dp corners, square aspect ratio
        Card(
            modifier = Modifier.aspectRatio(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // V2 Exact: AlbumArt icon at 160dp size with onPrimaryContainer tint
                AppIcon.AlbumArt.Render(
                    size = 160.dp,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}