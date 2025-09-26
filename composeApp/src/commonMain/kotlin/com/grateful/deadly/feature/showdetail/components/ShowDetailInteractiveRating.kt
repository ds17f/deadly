package com.grateful.deadly.feature.showdetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

/**
 * ShowDetailInteractiveRating - Interactive rating display card
 *
 * Matches V2's PlaylistInteractiveRating exactly:
 * - Card with surfaceVariant background at 30% alpha
 * - Left: Star rating and numerical score
 * - Right: Review count with chevron arrow
 * - Clickable to show reviews
 * - 8.dp rounded corners, 12.dp padding
 */
@Composable
fun ShowDetailInteractiveRating(
    showData: Show,
    onShowReviews: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onShowReviews() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Star rating and numerical score
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Star rating display
                CompactStarRating(
                    rating = if (showData.averageRating != null && showData.averageRating!! > 0) {
                        showData.averageRating!!.toFloat()
                    } else null
                )

                // Numerical rating
                Text(
                    text = if (showData.averageRating != null && showData.averageRating!! > 0) {
                        val rounded = (showData.averageRating!! * 10).toInt() / 10.0
                        "$rounded"
                    } else {
                        "N/A"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (showData.averageRating != null && showData.averageRating!! > 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Right: Review count and indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val reviewCount = showData.totalReviews ?: 0
                Text(
                    text = if (reviewCount > 0) {
                        "($reviewCount)"
                    } else {
                        "No reviews"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AppIcon.ChevronRight.Render(
                    size = 20.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact star rating display - shows only star icons without text
 * Matches V2's CompactStarRating behavior
 */
@Composable
private fun CompactStarRating(
    rating: Float?,
    maxRating: Int = 5,
    modifier: Modifier = Modifier
) {
    val safeRating = rating?.coerceIn(0f, maxRating.toFloat()) ?: 0f

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(maxRating) { index ->
            val starRating = safeRating - index

            val iconToUse = when {
                starRating >= 1f -> AppIcon.Star
                starRating >= 0.5f -> AppIcon.StarHalf
                else -> AppIcon.StarBorder
            }

            iconToUse.Render(
                size = 16.dp, // MEDIUM size matching V2
                tint = if (starRating > 0f) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            )
        }
    }
}