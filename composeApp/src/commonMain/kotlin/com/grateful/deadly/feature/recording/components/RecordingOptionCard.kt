package com.grateful.deadly.feature.recording.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
import com.grateful.deadly.core.design.components.CompactStarRating
import com.grateful.deadly.domain.models.RecordingOptionViewModel

/**
 * RecordingOptionCard - Individual recording display with V2-style selection states
 *
 * Follows V2's design patterns:
 * - Card with conditional border (2dp primary border when selected)
 * - Background color coding (selected, recommended, normal)
 * - Check icon for selected recordings
 * - 4-line layout: source type, taper, technical details, identifier
 * - Compact star rating (12dp) without text
 */
@Composable
fun RecordingOptionCard(
    recordingOption: RecordingOptionViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                recordingOption.isSelected -> MaterialTheme.colorScheme.primaryContainer
                recordingOption.isRecommended -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (recordingOption.isSelected) {
            BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary)
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Main content column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Line 1: Source type (bold) + Star rating (compact) - V2 exact
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recordingOption.sourceType,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Compact star rating like V2
                    recordingOption.rating?.let { rating ->
                        CompactStarRating(
                            rating = rating,
                            starSize = 12.dp
                        )
                    }
                }

                // Line 2: Taper info (only when we have actual taper name data) - V2 logic
                recordingOption.taperInfo?.let { taper ->
                    // Check if we have actual content after "Taper: "
                    val taperName = if (taper.startsWith("Taper: ")) {
                        taper.substring(7).trim()
                    } else {
                        taper.trim()
                    }

                    val hasValidTaper = taperName.isNotBlank() &&
                                       !taperName.equals("unknown", ignoreCase = true) &&
                                       !taperName.equals("n/a", ignoreCase = true) &&
                                       taperName.length > 0

                    if (hasValidTaper) {
                        Text(
                            text = taper,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Line 3: Technical details (equipment, quality) - V2 logic
                recordingOption.technicalDetails?.let { details ->
                    val cleanDetails = details
                        .replace(Regex("<[^>]*>"), "") // Strip HTML tags
                        .replace(Regex("\\s+"), " ") // Normalize whitespace
                        .trim()

                    if (cleanDetails.isNotBlank()) {
                        Text(
                            text = cleanDetails,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Line 4: Archive ID (red-tinted, muted - V2 exact)
                Text(
                    text = recordingOption.identifier,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Match reason badge (if available)
                recordingOption.matchReason?.let { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            recordingOption.isRecommended -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Selected check icon (V2 exact: 24.dp size)
            if (recordingOption.isSelected) {
                AppIcon.CheckCircle.Render(
                    size = 24.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Preview functions for visual testing
 */
@Composable
fun RecordingOptionCard_Preview_Selected() {
    MaterialTheme {
        RecordingOptionCard(
            recordingOption = RecordingOptionViewModel(
                identifier = "gd1977-05-08.sbd.hicks.4982.sbeok.shnf",
                sourceType = "SBD",
                taperInfo = "Taper: Betty Cantor",
                technicalDetails = "Soundboard, DAT → CD → FLAC",
                rating = 4.8f,
                reviewCount = 23,
                rawSource = "Betty Soundboard",
                rawLineage = "SBD > DAT > CD > FLAC",
                isSelected = true,
                isCurrent = true,
                isRecommended = true,
                matchReason = "Recommended"
            ),
            onClick = {}
        )
    }
}

@Composable
fun RecordingOptionCard_Preview_Recommended() {
    MaterialTheme {
        RecordingOptionCard(
            recordingOption = RecordingOptionViewModel(
                identifier = "gd1977-05-08.aud.miller.23456.flac16",
                sourceType = "AUD",
                taperInfo = "Taper: Mike Miller",
                technicalDetails = "Audience, Nak 700 → Cassette → DAT",
                rating = 4.2f,
                reviewCount = 12,
                rawSource = "Audience",
                rawLineage = "AUD > Cassette > DAT > FLAC",
                isSelected = false,
                isCurrent = false,
                isRecommended = true,
                matchReason = "Recommended"
            ),
            onClick = {}
        )
    }
}

@Composable
fun RecordingOptionCard_Preview_Normal() {
    MaterialTheme {
        RecordingOptionCard(
            recordingOption = RecordingOptionViewModel(
                identifier = "gd1977-05-08.aud.smith.98765.flac24",
                sourceType = "AUD",
                taperInfo = "Taper: John Smith",
                technicalDetails = null,
                rating = 3.8f,
                reviewCount = 5,
                rawSource = "Audience Recording",
                rawLineage = null,
                isSelected = false,
                isCurrent = false,
                isRecommended = false,
                matchReason = "Popular Choice"
            ),
            onClick = {}
        )
    }
}

@Composable
fun RecordingOptionCard_Preview_NoRating() {
    MaterialTheme {
        RecordingOptionCard(
            recordingOption = RecordingOptionViewModel(
                identifier = "gd1977-05-08.mtx.jones.11111",
                sourceType = "Matrix",
                taperInfo = null,
                technicalDetails = "Matrix blend of SBD + AUD",
                rating = null,
                reviewCount = null,
                rawSource = null,
                rawLineage = null,
                isSelected = false,
                isCurrent = false,
                isRecommended = false,
                matchReason = null
            ),
            onClick = {}
        )
    }
}