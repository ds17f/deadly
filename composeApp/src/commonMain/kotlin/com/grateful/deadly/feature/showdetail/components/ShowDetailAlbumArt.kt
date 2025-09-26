package com.grateful.deadly.feature.showdetail.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import deadly.composeapp.generated.resources.Res
import deadly.composeapp.generated.resources.deadly_logo
import org.jetbrains.compose.resources.painterResource

/**
 * ShowDetailAlbumArt - Album art component for show detail screen
 *
 * Matches V2's PlaylistAlbumArt exactly:
 * - 220.dp size (not 280.dp)
 * - 8.dp rounded corners (not 16.dp)
 * - Real deadly_logo image
 * - ContentScale.Crop
 * - Centered placement with proper padding
 */
@Composable
fun ShowDetailAlbumArt(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.deadly_logo),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}