package com.grateful.deadly.feature.recording.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
import com.grateful.deadly.domain.models.RecordingSelectionAction
import com.grateful.deadly.domain.models.RecordingSelectionState

/**
 * RecordingSelectionSheet - Modal bottom sheet for recording selection
 *
 * Follows V2's design patterns:
 * - Material3 ModalBottomSheet with skipPartiallyExpanded
 * - Header with close button and show info
 * - Scrollable list of RecordingOptionCard components
 * - Action buttons for "Set as Default" and "Reset to Recommended"
 * - Loading and error states
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingSelectionSheet(
    state: RecordingSelectionState,
    onAction: (RecordingSelectionAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (state.isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header with close button and show info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Choose Recording",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.showTitle.isNotBlank()) {
                            Text(
                                text = state.showTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (state.showDate.isNotBlank()) {
                            Text(
                                text = state.showDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        AppIcon.Close.Render(
                            size = 24.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on state
                when {
                    state.isLoading -> {
                        LoadingContent()
                    }

                    state.errorMessage != null -> {
                        ErrorContent(
                            message = state.errorMessage,
                            onRetry = { /* TODO: Add retry action */ }
                        )
                    }

                    else -> {
                        RecordingListContent(
                            state = state,
                            onAction = onAction
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                ActionButtons(
                    state = state,
                    onAction = onAction
                )

                // Bottom padding for safe area
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading recordings...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Error loading recordings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun RecordingListContent(
    state: RecordingSelectionState,
    onAction: (RecordingSelectionAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = state.allRecordings,
            key = { it.identifier }
        ) { recording ->
            RecordingOptionCard(
                recordingOption = recording,
                onClick = {
                    onAction(RecordingSelectionAction.SelectRecording(recording.identifier))
                }
            )
        }

        // Empty state if no recordings
        if (state.allRecordings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recordings available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    state: RecordingSelectionState,
    onAction: (RecordingSelectionAction) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Reset to Recommended button
        if (state.shouldShowResetToRecommended) {
            OutlinedButton(
                onClick = { onAction(RecordingSelectionAction.ResetToRecommended) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset to Recommended")
            }
        }

        // Set as Default button
        if (state.shouldShowSetAsDefault) {
            val selectedRecording = state.selectedRecording
            if (selectedRecording != null) {
                Button(
                    onClick = {
                        onAction(RecordingSelectionAction.SetAsDefault(selectedRecording.identifier))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Set as Default")
                }
            }
        }

        // If no action buttons are shown, add spacer
        if (!state.shouldShowResetToRecommended && !state.shouldShowSetAsDefault) {
            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}

/**
 * Preview functions for interactive testing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingSelectionSheet_Preview_Loading() {
    MaterialTheme {
        RecordingSelectionSheet(
            state = RecordingSelectionState(
                isVisible = true,
                showTitle = "Grateful Dead Live at Cornell University",
                showDate = "May 8, 1977",
                isLoading = true
            ),
            onAction = {},
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingSelectionSheet_Preview_WithRecordings() {
    MaterialTheme {
        RecordingSelectionSheet(
            state = RecordingSelectionState(
                isVisible = true,
                showTitle = "Grateful Dead Live at Cornell University",
                showDate = "May 8, 1977",
                currentRecording = com.grateful.deadly.domain.models.RecordingOptionViewModel(
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
                alternativeRecordings = listOf(
                    com.grateful.deadly.domain.models.RecordingOptionViewModel(
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
                        isRecommended = false,
                        matchReason = "Popular Choice"
                    ),
                    com.grateful.deadly.domain.models.RecordingOptionViewModel(
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
                    )
                ),
                hasRecommended = true
            ),
            onAction = {},
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingSelectionSheet_Preview_Error() {
    MaterialTheme {
        RecordingSelectionSheet(
            state = RecordingSelectionState(
                isVisible = true,
                showTitle = "Grateful Dead Live at Cornell University",
                showDate = "May 8, 1977",
                errorMessage = "Failed to load recordings from Archive.org"
            ),
            onAction = {},
            onDismiss = {}
        )
    }
}