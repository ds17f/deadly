package com.grateful.deadly.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grateful.deadly.services.media.MediaPlaybackState
import com.grateful.deadly.services.media.MediaService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine

/**
 * PlayerViewModel for reactive state management following V2 patterns.
 *
 * Provides a clean interface between PlayerScreen and MediaService.
 * All business logic is delegated to MediaService (Universal Service pattern).
 *
 * Follows V2's architecture where ViewModel combines service flows into unified UI state
 * with navigation information for proper show/recording context.
 */
class PlayerViewModel(
    private val mediaService: MediaService
) : ViewModel() {

    /**
     * Unified UI state combining playback state with navigation info, following V2 pattern.
     * Exposes current show/recording IDs for navigation while maintaining reactive updates.
     */
    val uiState: StateFlow<PlayerUiState> = combine(
        mediaService.playbackState,
        mediaService.currentShowId,
        mediaService.currentRecordingId
    ) { playbackState, showId, recordingId ->
        PlayerUiState(
            playbackState = playbackState,
            navigationInfo = NavigationInfo(
                showId = showId,
                recordingId = recordingId
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PlayerUiState(
            playbackState = MediaPlaybackState(
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
            ),
            navigationInfo = NavigationInfo(
                showId = null,
                recordingId = null
            )
        )
    )

    /**
     * Play or pause the current track.
     */
    suspend fun togglePlayPause() {
        val currentState = uiState.value.playbackState
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

/**
 * Unified UI state for PlayerScreen, following V2's PlayerUiState pattern.
 * Combines playback state with navigation information for proper show/recording context.
 */
data class PlayerUiState(
    val playbackState: MediaPlaybackState,
    val navigationInfo: NavigationInfo
)

/**
 * Navigation information for the current track, following V2's NavigationInfo pattern.
 * Provides show and recording IDs for navigation to ShowDetail screen.
 */
data class NavigationInfo(
    val showId: String?,
    val recordingId: String?
)