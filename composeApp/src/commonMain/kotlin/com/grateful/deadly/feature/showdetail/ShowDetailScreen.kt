package com.grateful.deadly.feature.showdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.logging.Logger

/**
 * ShowDetail Screen - Main screen for showing Grateful Dead show details and tracks
 *
 * Following V2's playlist screen architecture with LazyColumn layout,
 * floating back button, and progressive loading pattern.
 *
 * Supports dual route navigation:
 * - showdetail/{showId} - Load show with best recording
 * - showdetail/{showId}/{recordingId} - Load specific recording
 */
@Composable
fun ShowDetailScreen(
    showId: String?,
    recordingId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    viewModel: ShowDetailViewModel,
    modifier: Modifier = Modifier
) {
    Logger.d("ShowDetailScreen", "Loading show: $showId, recording: $recordingId")

    // Collect UI state from ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Track loading with LaunchedEffect for parameter changes (V2 pattern)
    LaunchedEffect(showId, recordingId) {
        Logger.d("ShowDetailScreen", "Parameters changed - showId: $showId, recordingId: $recordingId")
        viewModel.loadShow(showId, recordingId)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Main content - LazyColumn matching V2's playlist layout
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Show header with actual data
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (uiState.isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Loading show...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else if (uiState.error != null) {
                            Text(
                                text = "Error: ${uiState.error}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            uiState.showData?.let { showData ->
                                Text(
                                    text = showData.displayTitle,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = "${showData.location.city}, ${showData.location.state}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                if (showData.averageRating != null) {
                                    Text(
                                        text = "★ ${showData.averageRating} (${showData.totalReviews} reviews)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                uiState.currentRecordingId?.let { recordingId ->
                                    Text(
                                        text = "Recording: $recordingId",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Track list section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Tracks",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (uiState.isTrackListLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Loading tracks...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else if (uiState.tracks.isNotEmpty()) {
                            uiState.tracks.forEach { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "${track.number}. ${track.title}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${track.duration} • ${track.format}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Button(
                                        onClick = { onNavigateToPlayer() },
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text("Play")
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No tracks available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // Floating back button (V2 pattern)
        FloatingActionButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text("←", style = MaterialTheme.typography.headlineMedium)
        }
    }
}