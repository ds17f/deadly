package com.grateful.deadly.services.media

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.DeadObjectException
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.grateful.deadly.services.archive.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MediaController repository for Android MediaSession integration.
 *
 * Connects to DeadlyMediaSessionService to enable notifications, Android Auto, and Wear support.
 * Based on V2's proven MediaSession architecture, adapted for KMM.
 */
class MediaControllerRepository(private val context: Context) {

    companion object {
        private const val TAG = "MediaControllerRepository"
    }

    // Connection state
    enum class ConnectionState {
        Disconnected, Connecting, Connected, Failed
    }

    private var mediaController: MediaController? = null
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null

    // Coroutine scope for async operations
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Connection state management
    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Command queue for operations while connecting
    private val pendingCommands = mutableListOf<suspend () -> Unit>()

    // Playback state flows
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentTrack = MutableStateFlow<MediaMetadata?>(null)
    val currentTrack: StateFlow<MediaMetadata?> = _currentTrack.asStateFlow()

    private val _currentMediaItemIndex = MutableStateFlow(-1)
    val currentMediaItemIndex: StateFlow<Int> = _currentMediaItemIndex.asStateFlow()

    init {
        // Start async connection immediately
        connectToService()
    }

    /**
     * Load and play a single track using MediaSession (creates single-item playlist)
     */
    suspend fun loadAndPlay(url: String, track: Track, recordingId: String): Result<Unit> {
        Log.d(TAG, "🎵 [MEDIA] loadAndPlay: ${track.title ?: track.name} from $recordingId")

        executeWhenConnected {
            val controller = mediaController
            if (controller != null) {
                try {
                    // Create MediaItem with metadata for notifications
                    val mediaItem = createMediaItem(url, track, recordingId)

                    Log.d(TAG, "🎵 [URL] Loading single track URL: $url")

                    // Set loading state before MediaController operations
                    updateConnectionState(isLoading = true)

                    controller.setMediaItem(mediaItem)
                    controller.prepare()
                    controller.play()

                    Log.d(TAG, "🎵 [MEDIA] MediaController.play() completed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in loadAndPlay", e)
                    updateConnectionState(isLoading = false)
                    throw e
                }
            } else {
                Log.w(TAG, "MediaController is null, cannot play")
            }
        }

        return Result.success(Unit)
    }

    /**
     * Load and play a playlist of tracks using MediaSession (V2 pattern)
     * This is what V2 does - queue the entire show and play from specific track
     */
    suspend fun loadAndPlayPlaylist(tracks: List<Track>, recordingId: String, startTrackIndex: Int = 0, startPosition: Long = 0L): Result<Unit> {
        Log.d(TAG, "🎵 [MEDIA] loadAndPlayPlaylist: ${tracks.size} tracks from $recordingId, starting at track $startTrackIndex")

        executeWhenConnected {
            val controller = mediaController
            if (controller != null) {
                try {
                    // Convert all tracks to MediaItems (following V2 pattern)
                    val mediaItems = tracks.mapIndexed { index, track ->
                        val url = "https://archive.org/download/$recordingId/${track.name}"
                        createMediaItem(url, track, recordingId, index)
                    }

                    Log.d(TAG, "🎵 [PLAYLIST] Setting ${mediaItems.size} media items to MediaController")
                    mediaItems.forEachIndexed { index, item ->
                        Log.d(TAG, "🎵 [PLAYLIST] Track $index: ${item.localConfiguration?.uri}")
                    }

                    // Set loading state before MediaController operations (V2 pattern)
                    updateConnectionState(isLoading = true)

                    // Set entire playlist and start at specific track/position (V2 pattern)
                    controller.setMediaItems(mediaItems, startTrackIndex, startPosition)
                    controller.prepare()
                    controller.play()

                    Log.d(TAG, "🎵 [PLAYLIST] MediaController playlist loaded and playing")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in loadAndPlayPlaylist", e)
                    updateConnectionState(isLoading = false)
                    throw e
                }
            } else {
                Log.w(TAG, "MediaController is null, cannot play playlist")
            }
        }

        return Result.success(Unit)
    }

    /**
     * Pause playback
     */
    suspend fun pause(): Result<Unit> {
        Log.d(TAG, "🎵 [MEDIA] pause() called")

        executeWhenConnected {
            val controller = mediaController
            if (controller != null) {
                controller.pause()
                Log.d(TAG, "🎵 [MEDIA] MediaController.pause() completed")
            } else {
                Log.w(TAG, "MediaController is null, cannot pause")
            }
        }

        return Result.success(Unit)
    }

    /**
     * Resume playback
     */
    suspend fun resume(): Result<Unit> {
        Log.d(TAG, "🎵 [MEDIA] resume() called")

        executeWhenConnected {
            val controller = mediaController
            if (controller != null) {
                controller.play()
                Log.d(TAG, "🎵 [MEDIA] MediaController.play() completed")
            } else {
                Log.w(TAG, "MediaController is null, cannot resume")
            }
        }

        return Result.success(Unit)
    }

    /**
     * Seek to position
     */
    suspend fun seekTo(positionMs: Long): Result<Unit> {
        Log.d(TAG, "🎵 [MEDIA] seekTo: ${positionMs}ms")

        executeWhenConnected {
            val controller = mediaController
            if (controller != null) {
                controller.seekTo(positionMs)
                Log.d(TAG, "🎵 [MEDIA] MediaController.seekTo() completed")
            } else {
                Log.w(TAG, "MediaController is null, cannot seek")
            }
        }

        return Result.success(Unit)
    }

    /**
     * Skip to next track in playlist
     */
    suspend fun nextTrack(): Result<Unit> {
        Log.d(TAG, "🎵 [MEDIA] nextTrack()")

        executeWhenConnected {
            val controller = mediaController
            if (controller != null) {
                if (controller.hasNextMediaItem()) {
                    controller.seekToNextMediaItem()
                    Log.d(TAG, "🎵 [MEDIA] Skipped to next track")
                } else {
                    Log.d(TAG, "🎵 [MEDIA] No next track available")
                }
            } else {
                Log.w(TAG, "MediaController is null, cannot skip to next")
            }
        }

        return Result.success(Unit)
    }

    /**
     * Skip to previous track in playlist
     */
    suspend fun previousTrack(): Result<Unit> {
        Log.d(TAG, "🎵 [MEDIA] previousTrack()")

        executeWhenConnected {
            val controller = mediaController
            if (controller != null) {
                if (controller.hasPreviousMediaItem()) {
                    controller.seekToPreviousMediaItem()
                    Log.d(TAG, "🎵 [MEDIA] Skipped to previous track")
                } else {
                    Log.d(TAG, "🎵 [MEDIA] No previous track available")
                }
            } else {
                Log.w(TAG, "MediaController is null, cannot skip to previous")
            }
        }

        return Result.success(Unit)
    }

    /**
     * Stop playback
     */
    suspend fun stop(): Result<Unit> {
        Log.d(TAG, "🎵 [MEDIA] stop() called")

        executeWhenConnected {
            val controller = mediaController
            if (controller != null) {
                controller.stop()
                Log.d(TAG, "🎵 [MEDIA] MediaController.stop() completed")
            } else {
                Log.w(TAG, "MediaController is null, cannot stop")
            }
        }

        return Result.success(Unit)
    }

    /**
     * Create MediaItem with rich metadata for notifications and Auto/Wear
     */
    private fun createMediaItem(url: String, track: Track, recordingId: String, trackIndex: Int = 0): MediaItem {
        return MediaItem.Builder()
            .setUri(url)
            .setMediaId("${recordingId}|${trackIndex}|${track.title ?: track.name}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title ?: track.name)
                    .setArtist("Grateful Dead")
                    .setAlbumTitle(recordingId) // Use recording ID as album for context
                    .apply {
                        track.trackNumber?.let { setTrackNumber(it) }
                    }
                    .setExtras(Bundle().apply {
                        putString("recordingId", recordingId)
                        putString("trackUrl", url)
                        putString("duration", track.duration)
                        putInt("trackIndex", trackIndex)
                    })
                    .build()
            )
            .build()
    }

    /**
     * Update loading state for UI feedback
     */
    private fun updateConnectionState(isLoading: Boolean) {
        Log.d(TAG, "🎵 [STATE] Setting loading state: $isLoading")
        _isLoading.value = isLoading
    }

    /**
     * Async connection to MediaSessionService
     */
    private fun connectToService() {
        if (_connectionState.value == ConnectionState.Connecting) {
            Log.d(TAG, "🎵 [MEDIA] Connection already in progress")
            return
        }

        _connectionState.value = ConnectionState.Connecting
        Log.d(TAG, "🎵 [MEDIA] Connecting to MediaSessionService")

        try {
            val sessionToken = SessionToken(
                context,
                ComponentName(context, DeadlyMediaSessionService::class.java)
            )

            val future = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture = future

            // Add async listener - runs on main executor
            future.addListener({
                try {
                    val controller = future.get()
                    mediaController = controller
                    _connectionState.value = ConnectionState.Connected
                    Log.d(TAG, "🎵 [MEDIA] Connected to MediaSessionService successfully")

                    // Set up player state listeners
                    controller.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlaying) {
                                Log.d(TAG, "🎵 [AUDIO] MediaController detected AUDIO STARTED")
                            } else {
                                Log.d(TAG, "🎵 [AUDIO] MediaController detected audio stopped")
                            }
                            _isPlaying.value = isPlaying
                        }

                        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                            _currentTrack.value = mediaMetadata
                            Log.d(TAG, "🎵 [METADATA] Track: ${mediaMetadata.title}")
                        }

                        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                            val newIndex = controller.currentMediaItemIndex
                            _currentMediaItemIndex.value = newIndex
                            Log.d(TAG, "🎵 [NAVIGATION] Media item transition to index: $newIndex (reason: $reason)")
                        }

                        override fun onPositionDiscontinuity(reason: Int) {
                            // Update position when there's a discontinuity (seeking, etc.)
                            _currentPosition.value = controller.currentPosition
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            val stateString = when (playbackState) {
                                Player.STATE_IDLE -> "IDLE"
                                Player.STATE_BUFFERING -> "BUFFERING"
                                Player.STATE_READY -> "READY"
                                Player.STATE_ENDED -> "ENDED"
                                else -> "UNKNOWN($playbackState)"
                            }
                            Log.d(TAG, "🎵 [PLAYER] Playback state: $stateString")

                            // Clear loading state when ready to play
                            if (playbackState == Player.STATE_READY) {
                                _isLoading.value = false
                                _duration.value = controller.duration.coerceAtLeast(0L)
                                _currentPosition.value = controller.currentPosition
                                Log.d(TAG, "🎵 [STATE] Cleared loading state - player ready")
                            }

                            // Set loading state when buffering
                            if (playbackState == Player.STATE_BUFFERING) {
                                _isLoading.value = true
                                Log.d(TAG, "🎵 [STATE] Set loading state - buffering")
                            }
                        }
                    })

                    Log.d(TAG, "🎵 [MEDIA] MediaController connected successfully")

                    // Start position updater
                    startPositionUpdater()

                    // Execute any pending commands
                    repositoryScope.launch {
                        executePendingCommands()
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "🎵 [ERROR] Failed to connect MediaController", e)
                    _connectionState.value = ConnectionState.Failed
                    mediaController = null
                }
            }, ContextCompat.getMainExecutor(context))

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up MediaController connection", e)
            _connectionState.value = ConnectionState.Failed
        }
    }

    /**
     * Execute command when connected, or queue if still connecting
     * CRITICAL: All MediaController operations MUST run on Main thread
     */
    private suspend fun executeWhenConnected(command: suspend () -> Unit) {
        when (_connectionState.value) {
            ConnectionState.Connected -> {
                // CRITICAL: MediaController operations must run on Main thread
                withContext(Dispatchers.Main) {
                    try {
                        Log.d(TAG, "Executing MediaController command on Main thread")
                        command()
                        Log.d(TAG, "MediaController command completed successfully")
                    } catch (e: DeadObjectException) {
                        Log.w(TAG, "MediaController is dead - attempting to reconnect", e)
                        _connectionState.value = ConnectionState.Failed
                        // Queue the command for retry after reconnection
                        synchronized(pendingCommands) {
                            pendingCommands.add(command)
                        }
                        connectToService()
                    } catch (e: Exception) {
                        Log.e(TAG, "MediaController command failed", e)
                        throw e
                    }
                }
            }
            ConnectionState.Connecting -> {
                // Queue command for later execution
                synchronized(pendingCommands) {
                    pendingCommands.add(command)
                }
                Log.d(TAG, "Queued command - waiting for connection")
            }
            ConnectionState.Disconnected, ConnectionState.Failed -> {
                Log.d(TAG, "Attempting to reconnect...")
                connectToService()
                // Queue command for execution after connection
                synchronized(pendingCommands) {
                    pendingCommands.add(command)
                }
            }
        }
    }

    /**
     * Execute all pending commands after connection is established
     * CRITICAL: All MediaController operations MUST run on Main thread
     */
    private suspend fun executePendingCommands() {
        val commandsToExecute = synchronized(pendingCommands) {
            val commands = pendingCommands.toList()
            pendingCommands.clear()
            commands
        }

        Log.d(TAG, "Executing ${commandsToExecute.size} pending commands on Main thread")

        // CRITICAL: Execute pending commands on Main thread
        withContext(Dispatchers.Main) {
            commandsToExecute.forEach { command ->
                try {
                    Log.d(TAG, "Executing pending MediaController command on Main thread")
                    command()
                    Log.d(TAG, "Pending MediaController command completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing pending MediaController command", e)
                }
            }
        }
    }

    /**
     * Start periodic position updates
     */
    private fun startPositionUpdater() {
        repositoryScope.launch {
            while (true) {
                val controller = mediaController
                if (controller != null) {
                    _currentPosition.value = controller.currentPosition
                }
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }
    }

    /**
     * Release resources
     */
    fun release() {
        Log.d(TAG, "release() - Releasing MediaController resources")

        try {
            // Safely release MediaController
            mediaController?.let { controller ->
                try {
                    controller.release()
                    Log.d(TAG, "MediaController released successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing MediaController (likely already dead)", e)
                }
            }
            mediaController = null

            // Cancel any pending connection futures
            controllerFuture?.let { future ->
                try {
                    future.cancel(true)
                    Log.d(TAG, "Controller future cancelled")
                } catch (e: Exception) {
                    Log.w(TAG, "Error cancelling controller future", e)
                }
            }
            controllerFuture = null

            // Update connection state
            _connectionState.value = ConnectionState.Disconnected

        } catch (e: Exception) {
            Log.e(TAG, "Error during MediaControllerRepository cleanup", e)
        } finally {
            Log.d(TAG, "release() - Resources released successfully")
        }
    }
}