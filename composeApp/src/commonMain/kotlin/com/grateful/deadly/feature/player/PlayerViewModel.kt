package com.grateful.deadly.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grateful.deadly.services.media.MediaPlaybackState
import com.grateful.deadly.services.media.MediaService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

/**
 * PlayerViewModel for reactive state management following V2 patterns.
 *
 * Provides a clean interface between PlayerScreen and MediaService.
 * All business logic is delegated to MediaService (Universal Service pattern).
 */
class PlayerViewModel(
    private val mediaService: MediaService
) : ViewModel() {

    /**
     * Current playback state from MediaService.
     * Exposed as StateFlow for reactive UI updates.
     */
    val playbackState: StateFlow<MediaPlaybackState> = mediaService.playbackState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = MediaPlaybackState(
                currentTrack = null,
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

    /**
     * Play or pause the current track.
     */
    suspend fun togglePlayPause() {
        val currentState = playbackState.value
        if (currentState.isPlaying) {
            mediaService.pause()
        } else {
            mediaService.resume()
        }
    }

    /**
     * Seek to the previous track in the playlist.
     */
    suspend fun previousTrack() {
        mediaService.previousTrack()
    }

    /**
     * Seek to the next track in the playlist.
     */
    suspend fun nextTrack() {
        mediaService.nextTrack()
    }

    /**
     * Seek to a specific position in the current track.
     */
    suspend fun seekTo(positionMs: Long) {
        mediaService.seekTo(positionMs)
    }

    /**
     * Seek forward by the default amount (15 seconds).
     */
    suspend fun seekForward() {
        mediaService.seekForward()
    }

    /**
     * Seek backward by the default amount (15 seconds).
     */
    suspend fun seekBackward() {
        mediaService.seekBackward()
    }

    /**
     * Stop playback and clear the current track.
     */
    suspend fun stop() {
        mediaService.stop()
    }
}