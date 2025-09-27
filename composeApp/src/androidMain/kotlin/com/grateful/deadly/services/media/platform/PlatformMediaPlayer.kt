package com.grateful.deadly.services.media.platform

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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

/**
 * Android implementation of PlatformMediaPlayer using ExoPlayer.
 *
 * Real implementation using Media3 ExoPlayer following V2's proven architecture.
 * Handles generic media operations with proper error handling and retry logic.
 */
@UnstableApi
actual class PlatformMediaPlayer(
    private val context: Context
) {

    companion object {
        private const val TAG = "PlatformMediaPlayer"
        private const val POSITION_UPDATE_INTERVAL_MS = 1000L
        private const val MAX_RETRIES = 3
    }

    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionUpdateJob: Job? = null
    private var retryCount = 0

    // ExoPlayer instance with V2's proven configuration
    private val exoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _playbackState = MutableStateFlow(PlatformPlaybackState())
    actual val playbackState: Flow<PlatformPlaybackState> = _playbackState.asStateFlow()

    init {
        setupPlayerListeners()
        startPositionUpdates()
    }

    /**
     * Load and play an audio URL using ExoPlayer.
     * Follows V2's proven Archive.org streaming patterns.
     */
    actual suspend fun loadAndPlay(url: String): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "Loading and playing URL: $url")

            updatePlaybackState {
                copy(isLoading = true, error = null, isBuffering = false)
            }

            // Create MediaItem for the URL
            val mediaItem = MediaItem.fromUri(url)

            // Set the media item and prepare
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()

            // Reset retry count on new load
            retryCount = 0

            Log.d(TAG, "ExoPlayer.play() called for URL: $url")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load and play URL: $url", e)
            updatePlaybackState {
                copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load media"
                )
            }
            Result.failure(e)
        }
    }

    actual suspend fun pause(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "Pausing playback")
            exoPlayer.pause()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause", e)
            Result.failure(e)
        }
    }

    actual suspend fun resume(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "Resuming playback")
            exoPlayer.play()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume", e)
            Result.failure(e)
        }
    }

    actual suspend fun seekTo(positionMs: Long): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "Seeking to position: ${positionMs}ms")
            exoPlayer.seekTo(positionMs)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seek to position: ${positionMs}ms", e)
            Result.failure(e)
        }
    }

    actual suspend fun stop(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "Stopping playback")
            exoPlayer.stop()
            exoPlayer.clearMediaItems()

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
            Log.e(TAG, "Failed to stop", e)
            Result.failure(e)
        }
    }

    actual fun release() {
        Log.d(TAG, "Releasing ExoPlayer resources")
        positionUpdateJob?.cancel()
        playerScope.launch {
            exoPlayer.release()
        }
    }

    /**
     * Set up ExoPlayer listeners following V2's proven patterns.
     */
    private fun setupPlayerListeners() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "ExoPlayer isPlaying changed: $isPlaying")
                updatePlaybackState { copy(isPlaying = isPlaying) }

                if (isPlaying) {
                    // Reset retry count on successful playback
                    retryCount = 0
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateString = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN($playbackState)"
                }
                Log.d(TAG, "ExoPlayer state changed: $stateString")

                updatePlaybackState {
                    copy(
                        isLoading = playbackState == Player.STATE_BUFFERING,
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        durationMs = if (playbackState == Player.STATE_READY) {
                            exoPlayer.duration.coerceAtLeast(0L)
                        } else durationMs
                    )
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "ExoPlayer error: ${error.message}", error)
                handlePlayerError(error)
            }
        })
    }

    /**
     * Handle player errors with retry logic following V2's patterns.
     */
    private fun handlePlayerError(error: androidx.media3.common.PlaybackException) {
        // Check if this is a retryable network error
        val isRetryable = error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                error.message?.contains("Source error") == true

        if (!isRetryable || retryCount >= MAX_RETRIES) {
            val errorMessage = if (retryCount >= MAX_RETRIES) {
                "Network error after $MAX_RETRIES retries: ${error.message}"
            } else {
                "Playback error: ${error.message}"
            }

            Log.e(TAG, errorMessage)
            updatePlaybackState {
                copy(
                    isLoading = false,
                    isBuffering = false,
                    error = errorMessage
                )
            }
            retryCount = 0
            return
        }

        // Retry with exponential backoff
        val delayMs = when (retryCount) {
            0 -> 0L      // Immediate retry
            1 -> 1000L   // 1 second
            2 -> 2000L   // 2 seconds
            else -> 3000L
        }

        retryCount++
        Log.w(TAG, "Retrying playback (attempt $retryCount/$MAX_RETRIES) in ${delayMs}ms")

        playerScope.launch {
            delay(delayMs)

            try {
                val currentPosition = exoPlayer.currentPosition
                val wasPlaying = exoPlayer.playWhenReady

                Log.d(TAG, "Retry attempt $retryCount: position=${currentPosition}ms, wasPlaying=$wasPlaying")

                // Retry by seeking to current position and resuming playback state
                exoPlayer.seekTo(maxOf(0L, currentPosition))
                if (wasPlaying) {
                    exoPlayer.play()
                }
                exoPlayer.prepare()

            } catch (retryError: Exception) {
                Log.e(TAG, "Retry attempt $retryCount failed", retryError)
            }
        }
    }

    /**
     * Start periodic position updates for progress tracking.
     */
    private fun startPositionUpdates() {
        positionUpdateJob = playerScope.launch {
            while (true) {
                try {
                    val currentPosition = exoPlayer.currentPosition
                    val duration = exoPlayer.duration.coerceAtLeast(0L)

                    updatePlaybackState {
                        copy(
                            currentPositionMs = currentPosition,
                            durationMs = duration
                        )
                    }

                    delay(POSITION_UPDATE_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating position", e)
                    delay(POSITION_UPDATE_INTERVAL_MS)
                }
            }
        }
    }

    private fun updatePlaybackState(update: PlatformPlaybackState.() -> PlatformPlaybackState) {
        _playbackState.value = _playbackState.value.update()
    }
}