package com.grateful.deadly.feature.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

/**
 * Exact V2 Player cover art component with precise specifications.
 *
 * V2 Exact Features:
 * - Container height: 450dp (V1 exact height)
 * - Horizontal padding: 24dp
 * - Shape: Card with RoundedCornerShape(16.dp)
 * - Aspect ratio: 1:1 (square) within the 450dp container
 * - Colors: primaryContainer background, onPrimaryContainer icon tint
 * - Icon: AlbumArt icon at 160dp size
 * - Layout: Box with Center alignment containing Card
 */
@Composable
fun PlayerCoverArt(
    modifier: Modifier = Modifier
) {
    // V2 Exact: 450dp height container with 24dp horizontal padding
    Box(
        modifier = modifier
            .height(450.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
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