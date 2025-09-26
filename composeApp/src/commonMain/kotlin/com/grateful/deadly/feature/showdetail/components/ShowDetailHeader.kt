package com.grateful.deadly.feature.showdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

/**
 * ShowDetailHeader - Floating back button overlay
 *
 * Matches V2's PlaylistHeader exactly:
 * - Circular IconButton (40.dp size)
 * - Semi-transparent surface background (alpha = 0.9f)
 * - ArrowBack icon with proper tint
 * - zIndex(1f) for overlay positioning
 */
@Composable
fun ShowDetailHeader(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onNavigateBack,
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                CircleShape
            )
            .zIndex(1f)
    ) {
        AppIcon.ArrowBack.Render(
            size = 24.dp,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}