package com.grateful.deadly.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grateful.deadly.feature.player.components.*
import com.grateful.deadly.services.media.MediaPlaybackState
import com.grateful.deadly.services.media.MediaService
import com.grateful.deadly.core.design.AppDimens
import kotlinx.coroutines.launch

/**
 * Exact V2 full-screen player with precise LazyColumn structure.
 *
 * V2 Exact Features:
 * - LazyColumn with scroll state for mini player detection
 * - Full-screen immersive design (no topBar, bottomBar, miniPlayer)
 * - Gradient background applied to main content section
 * - Scroll-based Mini Player shows when scrolled past item index 0 with offset > 1200dp
 * - PlayerTopBar, PlayerCoverArt (450dp), PlayerTrackInfoRow, PlayerProgressControl, PlayerEnhancedControls
 * - PlayerSecondaryControls and PlayerMaterialPanels as separate items
 * - Exact V2 spacing and layout structure
 */
@Composable
fun PlayerScreen(
    mediaService: MediaService,
    onNavigateBack: () -> Unit
) {
    val playbackState by mediaService.playbackState.collectAsState(
        initial = MediaPlaybackState(
            currentTrack = null,
            currentRecordingId = null,
            showDate = null,
            venue = null,
            location = null,
            isPlaying = false,
            currentPositionMs = 0L,
            durationMs = 0L,
            isLoading = false,
            isBuffering = false,
            error = null,
            hasNext = false,
            hasPrevious = false,
            playlistPosition = 0,
            playlistSize = 0
        )
    )
    val coroutineScope = rememberCoroutineScope()

    // V2 Exact: LazyColumn with scroll state for mini player detection
    val lazyListState = rememberLazyListState()

    // V2 Exact: Show mini player when scrolled past item index 0 with offset > 1200dp
    val showMiniPlayer by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
            (lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset > 1200)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // V2 Exact: LazyColumn structure
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            // V2 Exact: Single gradient item containing all main UI
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(createRecordingGradient(playbackState.currentRecordingId ?: "default"))
                ) {
                    Column {
                        // V2 Exact: PlayerTopBar with 8dp vertical padding
                        PlayerTopBar(
                            playbackState = playbackState,
                            onNavigateBack = onNavigateBack,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // V2 Exact: PlayerCoverArt (450dp height, 24dp horizontal padding)
                        PlayerCoverArt()

                        // V2 Exact: PlayerTrackInfoRow (24dp horizontal padding)
                        PlayerTrackInfoRow(
                            playbackState = playbackState,
                            onAddToPlaylist = {
                                // TODO: Implement add to playlist
                            }
                        )

                        // V2 Exact: PlayerProgressControl (24dp horizontal padding)
                        PlayerProgressControl(
                            playbackState = playbackState,
                            onSeek = { positionMs ->
                                coroutineScope.launch {
                                    mediaService.seekTo(positionMs)
                                }
                            }
                        )

                        // V2 Exact: PlayerEnhancedControls (24dp horizontal padding)
                        PlayerEnhancedControls(
                            playbackState = playbackState,
                            onPrevious = {
                                coroutineScope.launch {
                                    mediaService.previousTrack()
                                }
                            },
                            onPlayPause = {
                                coroutineScope.launch {
                                    if (playbackState.isPlaying) {
                                        mediaService.pause()
                                    } else {
                                        mediaService.resume()
                                    }
                                }
                            },
                            onNext = {
                                coroutineScope.launch {
                                    mediaService.nextTrack()
                                }
                            },
                            onShuffle = {
                                // TODO: Implement shuffle
                            },
                            onRepeat = {
                                // TODO: Implement repeat
                            }
                        )
                    }
                }
            }

            // V2 Exact: PlayerSecondaryControls (24dp horizontal, 12dp vertical)
            item {
                PlayerSecondaryControls(
                    onConnect = {
                        // TODO: Implement connect
                    },
                    onShare = {
                        // TODO: Implement share
                    },
                    onQueue = {
                        // TODO: Implement queue
                    }
                )
            }

            // V2 Exact: PlayerMaterialPanels (16dp horizontal, 6dp vertical)
            item {
                PlayerMaterialPanels(
                    playbackState = playbackState,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // V2 Exact: Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // V2 Exact: Overlay Mini Player when scrolled
        if (showMiniPlayer && playbackState.currentTrack != null) {
            PlayerMiniPlayer(
                mediaService = mediaService,
                onPlayerClick = {
                    // Scroll back to top when mini player is clicked
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}


/**
 * Queue information display following V2 patterns.
 */
@Composable
private fun PlayerQueueInfo(
    playbackState: MediaPlaybackState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.M),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Track ${playbackState.playlistPosition} of ${playbackState.playlistSize}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}