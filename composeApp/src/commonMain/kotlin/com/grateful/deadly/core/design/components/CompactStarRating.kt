package com.grateful.deadly.core.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
import kotlin.math.floor
import kotlin.math.round

/**
 * CompactStarRating - V2-style compact star rating display
 *
 * Matches the V2 CompactStarRating component exactly:
 * - Uses AppIcon.Star for filled stars
 * - Uses AppIcon.StarHalf for half stars
 * - Uses AppIcon.StarBorder for empty stars
 * - Configurable star size (default 12.dp like V2)
 * - Displays rating out of 5 stars
 */
@Composable
fun CompactStarRating(
    rating: Float,
    modifier: Modifier = Modifier,
    starSize: Dp = 12.dp,
    starColor: Color = MaterialTheme.colorScheme.primary,
    emptyStarColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
) {
    val maxStars = 5
    val fullStars = floor(rating).toInt()
    val hasHalfStar = (rating - fullStars) >= 0.5f
    val emptyStars = maxStars - fullStars - if (hasHalfStar) 1 else 0

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Full stars
        repeat(fullStars) {
            AppIcon.Star.Render(
                size = starSize,
                tint = starColor
            )
        }

        // Half star
        if (hasHalfStar) {
            AppIcon.StarHalf.Render(
                size = starSize,
                tint = starColor
            )
        }

        // Empty stars
        repeat(emptyStars) {
            AppIcon.StarBorder.Render(
                size = starSize,
                tint = emptyStarColor
            )
        }
    }
}

/**
 * CompactStarRatingWithCount - V2-style rating with review count
 *
 * Displays stars followed by formatted review count like V2
 */
@Composable
fun CompactStarRatingWithCount(
    rating: Float,
    reviewCount: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 12.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactStarRating(
            rating = rating,
            starSize = starSize
        )

        androidx.compose.material3.Text(
            text = "($reviewCount)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}