package com.grateful.deadly.services.media.platform

import com.grateful.deadly.core.util.Logger
import com.grateful.deadly.services.media.EnrichedTrack
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.darwin.NSObject

/**
 * iOS implementation of PlatformMediaPlayer using SmartQueuePlayer wrapper.
 *
 * This implementation uses a custom SmartQueuePlayer Swift class that encapsulates
 * all AVQueuePlayer complexity and provides simple callbacks for track changes.
 * Includes production-ready features like Now Playing Info Center and remote controls.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformMediaPlayer {

    companion object {
        private const val TAG = "PlatformMediaPlayer"
        private const val POSITION_UPDATE_INTERVAL_MS = 1000L
    }

    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionUpdateJob: Job? = null
    private var playerId: String? = null

    private val _playbackState = MutableStateFlow(PlatformPlaybackState())
    actual val playbackState: Flow<PlatformPlaybackState> = _playbackState.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(-1)
    actual val currentTrackIndex: Flow<Int> = _currentTrackIndex.asStateFlow()

    // Current track metadata
    private var currentTrack: com.grateful.deadly.services.archive.Track? = null
    private var currentRecordingId: String? = null

    // Enriched track metadata for extraction
    private var currentEnrichedTracks: List<EnrichedTrack> = emptyList()
    private var currentEnrichedTrackIndex: Int = -1

    init {
        startPositionUpdates()
    }

    /**
     * Set track metadata for platform integrations.
     * SmartQueuePlayer handles MPNowPlayingInfoCenter automatically.
     */
    actual suspend fun setTrackMetadata(track: com.grateful.deadly.services.archive.Track, recordingId: String) {
        currentTrack = track
        currentRecordingId = recordingId
        // SmartQueuePlayer handles Now Playing Info Center updates automatically
    }

    /**
     * Load and play a playlist of enriched tracks.
     * Creates SmartQueuePlayer instance and sets up callbacks.
     */
    actual suspend fun loadAndPlayPlaylist(enrichedTracks: List<EnrichedTrack>, startIndex: Int): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            if (enrichedTracks.isEmpty() || startIndex !in enrichedTracks.indices) {
                return@withContext Result.failure(Exception("Invalid enriched playlist or start index"))
            }

            // Store enriched tracks for metadata extraction
            currentEnrichedTracks = enrichedTracks
            currentEnrichedTrackIndex = startIndex

            // Extract URLs from enriched tracks
            val urls = enrichedTracks.map { it.trackUrl }

            // Create SmartQueuePlayer with URLs
            playerId = SmartQueuePlayerBridge.createPlayer(urls, startIndex)

            // Set up callbacks
            playerId?.let { id ->
                SmartQueuePlayerBridge.setTrackChangedCallback(id, "trackChanged_$id")
                SmartQueuePlayerBridge.setPlaylistEndedCallback(id, "playlistEnded_$id")
            }

            // Update initial state
            currentEnrichedTrackIndex = startIndex
            _currentTrackIndex.value = startIndex

            // Set initial metadata
            val startTrack = enrichedTracks[startIndex]
            setTrackMetadata(startTrack.track, startTrack.recordingId)

            Logger.d(TAG, "🎵 [PLAYLIST] Loaded ${enrichedTracks.size} tracks starting at index $startIndex")

            // Start playback
            playerId?.let { SmartQueuePlayerBridge.play(it) }

            Result.success(Unit)

        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load playlist", e)
            Result.failure(e)
        }
    }

    /**
     * Skip to next track.
     * SmartQueuePlayer handles queue management automatically.
     */
    actual suspend fun nextTrack(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            if (currentEnrichedTracks.isEmpty()) {
                return@withContext Result.failure(Exception("No playlist loaded"))
            }

            val success = playerId?.let { SmartQueuePlayerBridge.playNext(it) } ?: false
            if (success) {
                Logger.d(TAG, "🎵 [NEXT] Skipped to next track")
                Result.success(Unit)
            } else {
                Logger.d(TAG, "🎵 [NEXT] No next track available")
                Result.failure(Exception("No next track available"))
            }

        } catch (e: Exception) {
            Logger.e(TAG, "Failed to skip to next track", e)
            Result.failure(e)
        }
    }

    /**
     * Skip to previous track or restart current track.
     * SmartQueuePlayer handles the >3 second restart logic automatically.
     */
    actual suspend fun previousTrack(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            if (currentEnrichedTracks.isEmpty()) {
                return@withContext Result.failure(Exception("No playlist loaded"))
            }

            val success = playerId?.let { SmartQueuePlayerBridge.playPrevious(it) } ?: false
            if (success) {
                Logger.d(TAG, "🎵 [PREV] Previous track action completed")
                Result.success(Unit)
            } else {
                Logger.d(TAG, "🎵 [PREV] Previous track action failed")
                Result.failure(Exception("Previous track action failed"))
            }

        } catch (e: Exception) {
            Logger.e(TAG, "Failed to go to previous track", e)
            Result.failure(e)
        }
    }

    actual suspend fun pause(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            playerId?.let { SmartQueuePlayerBridge.pause(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun resume(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            playerId?.let { SmartQueuePlayerBridge.play(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun seekTo(positionMs: Long): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            playerId?.let { SmartQueuePlayerBridge.seekTo(it, positionMs) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun stop(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            playerId?.let {
                SmartQueuePlayerBridge.pause(it)
                SmartQueuePlayerBridge.releasePlayer(it)
            }
            playerId = null

            updatePlaybackState {
                copy(
                    isPlaying = false,
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    isLoading = false,
                    isBuffering = false,
                    error = null
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual fun release() {
        positionUpdateJob?.cancel()
        playerId?.let { SmartQueuePlayerBridge.releasePlayer(it) }
        playerId = null
    }

    /**
     * Extract showId from currently playing item.
     */
    actual fun extractShowIdFromCurrentItem(): String? {
        val currentIndex = currentEnrichedTrackIndex
        return if (currentIndex >= 0 && currentIndex < currentEnrichedTracks.size) {
            currentEnrichedTracks[currentIndex].showId
        } else {
            null
        }
    }

    /**
     * Extract recordingId from currently playing item.
     */
    actual fun extractRecordingIdFromCurrentItem(): String? {
        val currentIndex = currentEnrichedTrackIndex
        return if (currentIndex >= 0 && currentIndex < currentEnrichedTracks.size) {
            currentEnrichedTracks[currentIndex].recordingId
        } else {
            null
        }
    }

    /**
     * Extract complete enriched track metadata from currently playing item.
     */
    actual fun extractCurrentEnrichedTrack(): EnrichedTrack? {
        val currentIndex = currentEnrichedTrackIndex
        return if (currentIndex >= 0 && currentIndex < currentEnrichedTracks.size) {
            currentEnrichedTracks[currentIndex]
        } else {
            null
        }
    }

    /**
     * Handle track changed callback from SmartQueuePlayer.
     */
    private fun handleTrackChanged(newIndex: Int) {
        playerScope.launch {
            try {
                currentEnrichedTrackIndex = newIndex
                _currentTrackIndex.value = newIndex

                // Update metadata
                if (newIndex >= 0 && newIndex < currentEnrichedTracks.size) {
                    val track = currentEnrichedTracks[newIndex]
                    setTrackMetadata(track.track, track.recordingId)
                    Logger.d(TAG, "🎵 [TRACK_CHANGED] Advanced to track $newIndex: ${track.displayTitle}")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to handle track change", e)
            }
        }
    }

    /**
     * Handle playlist ended callback from SmartQueuePlayer.
     */
    private fun handlePlaylistEnded() {
        playerScope.launch {
            Logger.d(TAG, "🎵 [PLAYLIST_ENDED] Playlist complete")
            updatePlaybackState { copy(isPlaying = false) }
        }
    }

    /**
     * Start periodic position updates and state monitoring.
     */
    private fun startPositionUpdates() {
        positionUpdateJob = playerScope.launch {
            while (true) {
                try {
                    val id = playerId
                    if (id != null) {
                        // Get current playback state
                        val state = SmartQueuePlayerBridge.getPlaybackState(id)
                        val currentPositionMs = (state.currentTime * 1000).toLong()
                        val durationMs = (state.duration * 1000).toLong()
                        val isPlaying = state.isPlaying

                        // Check for track changes (simple polling approach)
                        if (state.trackIndex != currentEnrichedTrackIndex) {
                            handleTrackChanged(state.trackIndex)
                        }

                        updatePlaybackState {
                            copy(
                                isPlaying = isPlaying,
                                currentPositionMs = currentPositionMs,
                                durationMs = if (durationMs > 0) durationMs else 0L,
                                isLoading = false,
                                isBuffering = false,
                                error = null
                            )
                        }
                    }

                    delay(POSITION_UPDATE_INTERVAL_MS)
                } catch (e: Exception) {
                    delay(POSITION_UPDATE_INTERVAL_MS)
                }
            }
        }
    }

    /**
     * Save current playback state for restoration after app restart.
     */
    fun saveCurrentPlaybackState() {
        if (currentEnrichedTracks.isEmpty() || currentEnrichedTrackIndex < 0) {
            return
        }

        val positionMs = playerId?.let {
            (SmartQueuePlayerBridge.getPlaybackState(it).currentTime * 1000).toLong()
        } ?: 0L

        val currentTrack = currentEnrichedTracks.getOrNull(currentEnrichedTrackIndex)
        if (currentTrack != null) {
            PlaybackStatePersistenceBridge.saveState(
                enrichedTracks = currentEnrichedTracks,
                trackIndex = currentEnrichedTrackIndex,
                positionMs = positionMs,
                showId = currentTrack.showId,
                recordingId = currentTrack.recordingId,
                format = currentTrack.format
            )
        }
    }

    /**
     * Restore saved playback state from UserDefaults.
     */
    fun restoreSavedPlaybackState(): SavedPlaybackState? {
        return PlaybackStatePersistenceBridge.restoreState()
    }

    /**
     * Clear saved playback state.
     */
    fun clearSavedPlaybackState() {
        PlaybackStatePersistenceBridge.clearState()
    }

    private fun updatePlaybackState(update: PlatformPlaybackState.() -> PlatformPlaybackState) {
        _playbackState.value = _playbackState.value.update()
    }
}

// SmartQueuePlayer implementation uses AppPlatform handler pattern