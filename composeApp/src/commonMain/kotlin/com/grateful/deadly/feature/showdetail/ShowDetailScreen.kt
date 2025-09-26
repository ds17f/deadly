package com.grateful.deadly.feature.showdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.feature.showdetail.components.ShowDetailAlbumArt
import com.grateful.deadly.feature.showdetail.components.ShowDetailShowInfo
import com.grateful.deadly.feature.showdetail.components.ShowDetailActionRow
import com.grateful.deadly.feature.showdetail.components.ShowDetailTrackList

/**
 * ShowDetailScreen - V2-level rich UI implementation
 *
 * Matches V2's PlaylistScreen architecture with:
 * - Album art at top
 * - Rich show info with navigation
 * - Action buttons row (library, download, setlist, menu, play)
 * - Professional track list
 * - Progressive loading (show immediate, tracks async)
 * - Modal sheets for rich interactions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(
    showId: String?,
    recordingId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    viewModel: ShowDetailViewModel,
    modifier: Modifier = Modifier
) {
    Logger.d("ShowDetailScreen", "=== SHOW DETAIL SCREEN LOADED === showId: $showId, recordingId: $recordingId")

    val uiState by viewModel.uiState.collectAsState()

    // Load show data when screen opens
    LaunchedEffect(showId, recordingId) {
        Logger.d("ShowDetailScreen", "Parameters changed - showId: $showId, recordingId: $recordingId")
        viewModel.loadShow(showId, recordingId)
    }

    Box(modifier = modifier.fillMaxSize()) {

        // Back arrow overlay at the top (V2 pattern)
        FloatingActionButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(48.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Text("←", style = MaterialTheme.typography.headlineSmall)
        }

        // Main content - V2's Spotify-style LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Loading show...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                uiState.error != null -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Error: ${uiState.error}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(onClick = {
                                    viewModel.clearError()
                                    viewModel.loadShow(showId, recordingId)
                                }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Album cover art - fixed size at top (V2 pattern)
                    item {
                        ShowDetailAlbumArt()
                    }

                    // Show info section with navigation buttons (V2 pattern)
                    uiState.showData?.let { showData ->
                        item {
                            ShowDetailShowInfo(
                                showData = showData,
                                onPreviousShow = viewModel::navigateToPreviousShow,
                                onNextShow = viewModel::navigateToNextShow
                            )
                        }

                        // Action buttons row (V2 pattern)
                        item {
                            ShowDetailActionRow(
                                showData = showData,
                                onLibraryAction = {
                                    // TODO Phase 5: Implement library actions
                                    Logger.d("ShowDetailScreen", "Library action")
                                },
                                onDownload = {
                                    // TODO Phase 5: Implement download
                                    Logger.d("ShowDetailScreen", "Download show")
                                },
                                onShowSetlist = {
                                    // TODO Phase 5: Show setlist modal
                                    Logger.d("ShowDetailScreen", "Show setlist")
                                },
                                onShowMenu = {
                                    // TODO Phase 5: Show menu modal
                                    Logger.d("ShowDetailScreen", "Show menu")
                                },
                                onTogglePlayback = {
                                    // TODO Phase 5: Toggle playback
                                    Logger.d("ShowDetailScreen", "Toggle playback")
                                }
                            )
                        }
                    }

                    // Track list with progressive loading (V2 pattern)
                    if (uiState.isTracksLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Loading tracks...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        ShowDetailTrackList(
                            tracks = uiState.tracks,
                            onPlayClick = { track ->
                                Logger.d("ShowDetailScreen", "Play track: ${track.title ?: track.name}")
                                viewModel.playTrack(track)
                            },
                            onDownloadClick = { track ->
                                // TODO Phase 5: Implement individual track download
                                Logger.d("ShowDetailScreen", "Download track: ${track.title ?: track.name}")
                            }
                        )
                    }
                }
            }
        }
    }

    // TODO Phase 5: Add modal sheets
    // - Reviews modal
    // - Menu modal
    // - Recording selection modal
    // - Setlist modal
}