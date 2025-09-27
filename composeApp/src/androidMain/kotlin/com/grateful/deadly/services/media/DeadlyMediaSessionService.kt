package com.grateful.deadly.services.media

import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MediaSessionService for cross-platform media player.
 *
 * Provides Android notifications, Wear OS support, and Android Auto compatibility.
 * Based on V2's proven MediaSession architecture with simplified implementation.
 */
@UnstableApi
class DeadlyMediaSessionService : MediaSessionService() {

    companion object {
        private const val TAG = "DeadlyMediaSessionService"
    }

    private lateinit var exoPlayer: ExoPlayer
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Retry logic state
    private var retryCount = 0
    private val maxRetries = 3 // immediate, 1s, 2s

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🎵 [MEDIA] DeadlyMediaSessionService onCreate started at ${System.currentTimeMillis()}")

        // Initialize ExoPlayer with audio attributes for proper audio focus handling
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // Handle audio focus automatically
            )
            .setHandleAudioBecomingNoisy(true) // Pause when headphones disconnected
            .build()

        // Add player listeners for state tracking
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateString = when (playbackState) {
                    Player.STATE_IDLE -> {
                        Log.d(TAG, "🎵 [PLAYER] ExoPlayer state: IDLE at ${System.currentTimeMillis()}")
                        "IDLE"
                    }
                    Player.STATE_BUFFERING -> {
                        Log.d(TAG, "🎵 [PLAYER] ExoPlayer state: BUFFERING (loading URL) at ${System.currentTimeMillis()}")
                        "BUFFERING"
                    }
                    Player.STATE_READY -> {
                        Log.d(TAG, "🎵 [PLAYER] ExoPlayer state: READY (URL loaded, can play) at ${System.currentTimeMillis()}")
                        resetRetryCount() // Reset retry count on successful recovery
                        "READY"
                    }
                    Player.STATE_ENDED -> {
                        Log.d(TAG, "🎵 [PLAYER] ExoPlayer state: ENDED at ${System.currentTimeMillis()}")
                        "ENDED"
                    }
                    else -> {
                        Log.d(TAG, "🎵 [PLAYER] ExoPlayer state: UNKNOWN($playbackState) at ${System.currentTimeMillis()}")
                        "UNKNOWN"
                    }
                }
                Log.d(TAG, "🎵 [PLAYER] Playback state changed to: $stateString")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    Log.d(TAG, "🎵 [AUDIO] AUDIO PLAYBACK STARTED at ${System.currentTimeMillis()}")
                } else {
                    Log.d(TAG, "🎵 [AUDIO] Audio playback stopped/paused at ${System.currentTimeMillis()}")
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "🎵 [ERROR] Player error at ${System.currentTimeMillis()}: ${error.message}", error)
                handlePlayerError(error)
            }
        })

        // Create MediaSession with session ID
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setId("DeadlySession")
            .setCallback(MediaSessionCallback())
            .build()

        Log.d(TAG, "🎵 [MEDIA] DeadlyMediaSessionService onCreate completed at ${System.currentTimeMillis()}")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.d(TAG, "🎵 [MEDIA] Client requesting session: ${controllerInfo.packageName} at ${System.currentTimeMillis()}")
        return mediaSession
    }

    override fun onDestroy() {
        Log.d(TAG, "🎵 [MEDIA] Service onDestroy()")
        mediaSession?.run {
            release()
            mediaSession = null
        }
        exoPlayer.release()
        super.onDestroy()
    }

    /**
     * Handle player errors with exponential backoff retry logic
     * Retry pattern: immediate → 1s → 2s → fail
     */
    private fun handlePlayerError(error: androidx.media3.common.PlaybackException) {
        // Check if this is a retryable error (network/source issues)
        val isRetryable = error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                error.message?.contains("Source error") == true

        if (!isRetryable) {
            Log.e(TAG, "Non-retryable error: ${error.errorCode} - ${error.message}")
            retryCount = 0 // Reset for next error
            return
        }

        if (retryCount >= maxRetries) {
            Log.e(TAG, "Max retries ($maxRetries) exceeded - giving up")
            retryCount = 0 // Reset for next error
            return
        }

        // Calculate delay: 0s, 1s, 2s
        val delayMs = when (retryCount) {
            0 -> 0L      // Immediate retry
            1 -> 1000L   // 1 second
            2 -> 2000L   // 2 seconds
            else -> 0L
        }

        retryCount++
        Log.w(TAG, "Retryable error detected (attempt $retryCount/$maxRetries) - retrying in ${delayMs}ms")

        serviceScope.launch {
            delay(delayMs)

            try {
                val currentIndex = exoPlayer.currentMediaItemIndex
                val wasPlaying = exoPlayer.playWhenReady
                val currentPosition = exoPlayer.currentPosition

                Log.d(TAG, "Retry attempt $retryCount: index=$currentIndex, position=${currentPosition}ms, wasPlaying=$wasPlaying")

                // Retry by seeking to current item and resuming playback state
                exoPlayer.seekTo(currentIndex, maxOf(0L, currentPosition))
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
     * Reset retry count on successful playback state changes
     */
    private fun resetRetryCount() {
        if (retryCount > 0) {
            Log.d(TAG, "Playback recovered - resetting retry count")
            retryCount = 0
        }
    }

    /**
     * MediaSession callback for standard commands
     */
    private inner class MediaSessionCallback : MediaSession.Callback {
        // MediaSession.Callback methods will handle player commands automatically
        // No need to override unless we need custom behavior
    }
}