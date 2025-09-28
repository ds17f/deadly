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
import platform.MediaPlayer.*
import platform.darwin.NSObject
import platform.AudioToolbox.*

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

    // Track current index for iOS manual navigation
    private val _currentTrackIndex = MutableStateFlow(-1)
    actual val currentTrackIndex: Flow<Int> = _currentTrackIndex.asStateFlow()

    init {
        setupPlayerObservers()
        startPositionUpdates()
    }


    // Current track metadata for MPNowPlayingInfoCenter
    private var currentTrack: com.grateful.deadly.services.archive.Track? = null
    private var currentRecordingId: String? = null

    // Manual playlist management for iOS (no native playlist like Android MediaController)
    private var currentPlaylist: List<com.grateful.deadly.services.archive.Track> = emptyList()
    private var currentPlaylistIndex: Int = -1
    private var currentPlaylistRecordingId: String? = null

    /**
     * Set track metadata for rich platform integrations.
     * iOS implementation updates MPNowPlayingInfoCenter for CarPlay/Apple Watch support.
     */
    actual suspend fun setTrackMetadata(track: com.grateful.deadly.services.archive.Track, recordingId: String) {
        currentTrack = track
        currentRecordingId = recordingId

        // Update MPNowPlayingInfoCenter for CarPlay/Apple Watch
        updateNowPlayingInfo()
    }

    /**
     * Load and play a playlist of tracks.
     * iOS implementation: falls back to playing first track (no native playlist queue like Android MediaController)
     */
    actual suspend fun loadAndPlayPlaylist(tracks: List<com.grateful.deadly.services.archive.Track>, recordingId: String, startIndex: Int): Result<Unit> {
        return try {
            if (tracks.isEmpty() || startIndex !in tracks.indices) {
                return Result.failure(Exception("Invalid playlist or start index"))
            }

            // Store playlist for manual navigation
            currentPlaylist = tracks
            currentPlaylistIndex = startIndex
            currentPlaylistRecordingId = recordingId

            // Update track index flow
            _currentTrackIndex.value = startIndex

            // For iOS, set metadata and load the starting track
            val startTrack = tracks[startIndex]
            setTrackMetadata(startTrack, recordingId)

            val url = "https://archive.org/download/$recordingId/${startTrack.name}"
            loadAndPlay(url)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Skip to next track (iOS manual navigation)
     */
    actual suspend fun nextTrack(): Result<Unit> {
        return try {
            val playlist = currentPlaylist
            val currentIndex = currentPlaylistIndex
            val recordingId = currentPlaylistRecordingId

            if (playlist.isEmpty() || recordingId == null) {
                return Result.failure(Exception("No playlist loaded"))
            }

            if (currentIndex >= playlist.size - 1) {
                return Result.failure(Exception("No next track available"))
            }

            val nextIndex = currentIndex + 1
            val nextTrack = playlist[nextIndex]

            currentPlaylistIndex = nextIndex
            _currentTrackIndex.value = nextIndex
            setTrackMetadata(nextTrack, recordingId)

            val url = "https://archive.org/download/$recordingId/${nextTrack.name}"
            loadAndPlay(url)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Skip to previous track (iOS manual navigation)
     */
    actual suspend fun previousTrack(): Result<Unit> {
        return try {
            val playlist = currentPlaylist
            val currentIndex = currentPlaylistIndex
            val recordingId = currentPlaylistRecordingId

            if (playlist.isEmpty() || recordingId == null) {
                return Result.failure(Exception("No playlist loaded"))
            }

            if (currentIndex <= 0) {
                return Result.failure(Exception("No previous track available"))
            }

            val prevIndex = currentIndex - 1
            val prevTrack = playlist[prevIndex]

            currentPlaylistIndex = prevIndex
            _currentTrackIndex.value = prevIndex
            setTrackMetadata(prevTrack, recordingId)

            val url = "https://archive.org/download/$recordingId/${prevTrack.name}"
            loadAndPlay(url)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load and play an audio URL using AVPlayer.
     */
    actual suspend fun loadAndPlay(url: String): Result<Unit> = withContext(Dispatchers.Main) {
        try {
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

            // Update MPNowPlayingInfoCenter
            updateNowPlayingInfo()

            Result.success(Unit)

        } catch (e: Exception) {
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
            avPlayer.pause()

            // Update MPNowPlayingInfoCenter
            updateNowPlayingInfo()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun resume(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            avPlayer.play()

            // Update MPNowPlayingInfoCenter
            updateNowPlayingInfo()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun seekTo(positionMs: Long): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val timeValue = positionMs / 1000.0 // Convert to seconds
            val cmTime = CMTimeMakeWithSeconds(timeValue, 1000)

            avPlayer.seekToTime(cmTime)

            // Update MPNowPlayingInfoCenter with new position
            updateNowPlayingInfo()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun stop(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
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
            Result.failure(e)
        }
    }

    actual fun release() {
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

        playerScope.launch {
            delay(delayMs)

            try {
                val currentPositionMs = (CMTimeGetSeconds(avPlayer.currentTime()) * 1000).toLong()
                val wasPlaying = avPlayer.rate > 0.0f

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
                // Retry failed, give up
            }
        }
    }

    /**
     * Update MPNowPlayingInfoCenter for CarPlay and Apple Watch integration.
     * Called whenever track metadata or playback state changes.
     */
    private fun updateNowPlayingInfo() {
        val track = currentTrack ?: return
        val recordingId = currentRecordingId ?: return

        try {
            // Create now playing info dictionary
            val nowPlayingInfo = mutableMapOf<String, Any?>()

            // Basic track information
            nowPlayingInfo[MPMediaItemPropertyTitle] = track.title ?: track.name
            nowPlayingInfo[MPMediaItemPropertyArtist] = "Grateful Dead"
            nowPlayingInfo[MPMediaItemPropertyAlbumTitle] = recordingId

            // Track number if available
            track.trackNumber?.let { trackNumber ->
                nowPlayingInfo[MPMediaItemPropertyAlbumTrackNumber] = trackNumber
            }

            // Duration if available (convert from string to seconds)
            track.duration?.let { durationString ->
                val durationSeconds = parseDurationToSeconds(durationString)
                if (durationSeconds > 0) {
                    nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = durationSeconds
                }
            }

            // Current playback info
            val currentTime = avPlayer.currentTime()
            val currentSeconds = CMTimeGetSeconds(currentTime)
            if (currentSeconds.isFinite() && currentSeconds >= 0) {
                nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentSeconds
            }

            // Playback rate (1.0 = playing, 0.0 = paused)
            val playbackRate = if (avPlayer.rate > 0.0f) 1.0 else 0.0
            nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = playbackRate

            // Set the now playing info
            MPNowPlayingInfoCenter.defaultCenter().setNowPlayingInfo(nowPlayingInfo as Map<Any?, *>)

            // Set playback state
            val playbackState = if (avPlayer.rate > 0.0f) {
                MPNowPlayingPlaybackStatePlaying
            } else {
                MPNowPlayingPlaybackStatePaused
            }
            MPNowPlayingInfoCenter.defaultCenter().setPlaybackState(playbackState)

        } catch (e: Exception) {
            // Failed to update MPNowPlayingInfoCenter
        }
    }

    /**
     * Parse duration string (e.g., "4:32" or "1:23:45") to seconds.
     */
    private fun parseDurationToSeconds(durationString: String): Double {
        return try {
            val parts = durationString.split(":")
            when (parts.size) {
                2 -> { // mm:ss format
                    val minutes = parts[0].toDouble()
                    val seconds = parts[1].toDouble()
                    minutes * 60 + seconds
                }
                3 -> { // hh:mm:ss format
                    val hours = parts[0].toDouble()
                    val minutes = parts[1].toDouble()
                    val seconds = parts[2].toDouble()
                    hours * 3600 + minutes * 60 + seconds
                }
                else -> 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    private fun updatePlaybackState(update: PlatformPlaybackState.() -> PlatformPlaybackState) {
        _playbackState.value = _playbackState.value.update()
    }
}