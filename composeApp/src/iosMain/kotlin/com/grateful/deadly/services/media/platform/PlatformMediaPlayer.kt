package com.grateful.deadly.services.media.platform

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
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.Foundation.*
import platform.darwin.NSObject

/**
 * iOS implementation of PlatformMediaPlayer using AVPlayer.
 *
 * Real implementation using AVFoundation AVPlayer for iOS audio playback.
 * Handles generic media operations with proper error handling.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformMediaPlayer {

    companion object {
        private const val POSITION_UPDATE_INTERVAL_MS = 1000L
        private const val MAX_RETRIES = 3
    }

    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionUpdateJob: Job? = null
    private var retryCount = 0
    private var currentUrl: String? = null

    // AVPlayer instance
    private val avPlayer = AVPlayer()
    private var timeObserver: Any? = null

    private val _playbackState = MutableStateFlow(PlatformPlaybackState())
    actual val playbackState: Flow<PlatformPlaybackState> = _playbackState.asStateFlow()

    init {
        setupPlayerObservers()
        startPositionUpdates()
    }

    /**
     * Load and play an audio URL using AVPlayer.
     */
    actual suspend fun loadAndPlay(url: String): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            println("PlatformMediaPlayer: Loading and playing URL: $url")

            updatePlaybackState {
                copy(isLoading = true, error = null, isBuffering = false)
            }

            currentUrl = url

            // Create AVPlayerItem from URL
            val nsUrl = NSURL.URLWithString(url)
            if (nsUrl == null) {
                throw Exception("Invalid URL: $url")
            }

            val playerItem = AVPlayerItem.playerItemWithURL(nsUrl)

            // Replace current item and play
            avPlayer.replaceCurrentItemWithPlayerItem(playerItem)
            avPlayer.play()

            // Reset retry count on new load
            retryCount = 0

            println("PlatformMediaPlayer: AVPlayer.play() called for URL: $url")
            Result.success(Unit)

        } catch (e: Exception) {
            println("PlatformMediaPlayer: Failed to load and play URL: $url, error: ${e.message}")
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
            println("PlatformMediaPlayer: Pausing playback")
            avPlayer.pause()
            Result.success(Unit)
        } catch (e: Exception) {
            println("PlatformMediaPlayer: Failed to pause: ${e.message}")
            Result.failure(e)
        }
    }

    actual suspend fun resume(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            println("PlatformMediaPlayer: Resuming playback")
            avPlayer.play()
            Result.success(Unit)
        } catch (e: Exception) {
            println("PlatformMediaPlayer: Failed to resume: ${e.message}")
            Result.failure(e)
        }
    }

    actual suspend fun seekTo(positionMs: Long): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            println("PlatformMediaPlayer: Seeking to position: ${positionMs}ms")

            val timeValue = positionMs / 1000.0 // Convert to seconds
            val cmTime = CMTimeMakeWithSeconds(timeValue, 1000)

            avPlayer.seekToTime(cmTime)
            Result.success(Unit)
        } catch (e: Exception) {
            println("PlatformMediaPlayer: Failed to seek to position: ${positionMs}ms, error: ${e.message}")
            Result.failure(e)
        }
    }

    actual suspend fun stop(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            println("PlatformMediaPlayer: Stopping playback")
            avPlayer.pause()
            avPlayer.replaceCurrentItemWithPlayerItem(null)

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
            println("PlatformMediaPlayer: Failed to stop: ${e.message}")
            Result.failure(e)
        }
    }

    actual fun release() {
        println("PlatformMediaPlayer: Releasing AVPlayer resources")
        positionUpdateJob?.cancel()

        playerScope.launch {
            timeObserver?.let { observer ->
                avPlayer.removeTimeObserver(observer)
                timeObserver = null
            }
        }
    }

    /**
     * Set up AVPlayer observers for state changes.
     */
    private fun setupPlayerObservers() {
        // Set up KVO observers for rate (playing state)
        playerScope.launch {
            // This is a simplified approach - in a production app you'd want proper KVO
            // For now, we'll rely on periodic status checks
        }
    }

    /**
     * Start periodic position updates and state monitoring.
     */
    private fun startPositionUpdates() {
        positionUpdateJob = playerScope.launch {
            while (true) {
                try {
                    val currentItem = avPlayer.currentItem
                    if (currentItem != null) {
                        // Get current position
                        val currentTime = avPlayer.currentTime()
                        val currentPositionMs = (CMTimeGetSeconds(currentTime) * 1000).toLong()

                        // Get duration
                        val duration = currentItem.duration
                        val durationSeconds = CMTimeGetSeconds(duration)
                        val durationMs = if (durationSeconds.isFinite() && durationSeconds > 0) {
                            (durationSeconds * 1000).toLong()
                        } else 0L

                        // Get playback rate to determine if playing
                        val isPlaying = avPlayer.rate > 0.0f

                        // Get player status for loading/buffering states
                        val status = currentItem.status
                        val isLoading = status == AVPlayerItemStatusUnknown
                        val isBuffering = status == AVPlayerItemStatusReadyToPlay && !isPlaying && avPlayer.rate == 0.0f

                        updatePlaybackState {
                            copy(
                                isPlaying = isPlaying,
                                currentPositionMs = currentPositionMs,
                                durationMs = durationMs,
                                isLoading = isLoading,
                                isBuffering = isBuffering
                            )
                        }

                        // Reset retry count on successful playback
                        if (isPlaying) {
                            retryCount = 0
                        }

                        // Check for errors
                        val error = currentItem.error
                        if (error != null) {
                            handlePlayerError(error)
                        }
                    }

                    delay(POSITION_UPDATE_INTERVAL_MS)
                } catch (e: Exception) {
                    println("PlatformMediaPlayer: Error updating position: ${e.message}")
                    delay(POSITION_UPDATE_INTERVAL_MS)
                }
            }
        }
    }

    /**
     * Handle player errors with retry logic.
     */
    private fun handlePlayerError(error: NSError) {
        val errorMessage = error.localizedDescription
        println("PlatformMediaPlayer: AVPlayer error: $errorMessage")

        // Simple retry logic for network errors
        val isRetryable = error.domain == NSURLErrorDomain &&
                (error.code == NSURLErrorTimedOut.toLong() ||
                 error.code == NSURLErrorNotConnectedToInternet.toLong() ||
                 error.code == NSURLErrorNetworkConnectionLost.toLong())

        if (!isRetryable || retryCount >= MAX_RETRIES) {
            val finalErrorMessage = if (retryCount >= MAX_RETRIES) {
                "Network error after $MAX_RETRIES retries: $errorMessage"
            } else {
                "Playback error: $errorMessage"
            }

            println("PlatformMediaPlayer: $finalErrorMessage")
            updatePlaybackState {
                copy(
                    isLoading = false,
                    isBuffering = false,
                    error = finalErrorMessage
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
        println("PlatformMediaPlayer: Retrying playback (attempt $retryCount/$MAX_RETRIES) in ${delayMs}ms")

        playerScope.launch {
            delay(delayMs)

            try {
                val currentPositionMs = (CMTimeGetSeconds(avPlayer.currentTime()) * 1000).toLong()
                val wasPlaying = avPlayer.rate > 0.0f

                println("PlatformMediaPlayer: Retry attempt $retryCount: position=${currentPositionMs}ms, wasPlaying=$wasPlaying")

                // Retry by reloading the current URL
                currentUrl?.let { url ->
                    val nsUrl = NSURL.URLWithString(url)
                    if (nsUrl != null) {
                        val playerItem = AVPlayerItem.playerItemWithURL(nsUrl)
                        avPlayer.replaceCurrentItemWithPlayerItem(playerItem)

                        // Seek to previous position if it was significant
                        if (currentPositionMs > 0) {
                            val timeValue = currentPositionMs / 1000.0
                            val cmTime = CMTimeMakeWithSeconds(timeValue, 1000)
                            avPlayer.seekToTime(cmTime)
                        }

                        if (wasPlaying) {
                            avPlayer.play()
                        }
                    }
                }

            } catch (retryError: Exception) {
                println("PlatformMediaPlayer: Retry attempt $retryCount failed: ${retryError.message}")
            }
        }
    }

    private fun updatePlaybackState(update: PlatformPlaybackState.() -> PlatformPlaybackState) {
        _playbackState.value = _playbackState.value.update()
    }
}