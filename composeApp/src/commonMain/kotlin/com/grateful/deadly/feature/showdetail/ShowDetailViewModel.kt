package com.grateful.deadly.feature.showdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.domain.models.Recording
import com.grateful.deadly.services.archive.Track
import com.grateful.deadly.services.media.MediaService
import com.grateful.deadly.navigation.AppScreen
import com.grateful.deadly.navigation.NavigationEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ShowDetailViewModel - Real implementation with ShowDetailService integration
 *
 * Following V2's PlaylistViewModel architecture pattern:
 * 1. Reactive state management combining service StateFlows
 * 2. Progressive loading (show immediate, tracks async)
 * 3. Navigation events via SharedFlow
 * 4. Clean separation between UI state and business logic
 *
 * Uses ShowDetailService for V2's database-first loading patterns.
 */
class ShowDetailViewModel(
    private val showDetailService: ShowDetailService,
    private val mediaService: MediaService
) : ViewModel() {

    companion object {
        private const val TAG = "ShowDetailViewModel"
    }

    // UI State - combines service state flows into single reactive UI state
    val uiState: StateFlow<ShowDetailUiState> = combine(
        showDetailService.currentShow,
        showDetailService.currentRecording,
        showDetailService.currentTracks,
        showDetailService.isTracksLoading,
        showDetailService.error
    ) { show, recording, tracks, isTracksLoading, error ->
        Logger.d(TAG, "UI state update: show=${show?.displayTitle}, recording=${recording?.identifier}, tracks=${tracks.size}, loading=$isTracksLoading, error=$error")

        ShowDetailUiState(
            showData = show,
            currentRecordingId = recording?.identifier,
            tracks = tracks,
            isLoading = show == null && error == null, // Loading if no show and no error
            isTracksLoading = isTracksLoading,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShowDetailUiState(isLoading = true)
    )

    // Navigation event flow for reactive navigation
    private val _navigation = MutableSharedFlow<NavigationEvent>()
    val navigation: SharedFlow<NavigationEvent> = _navigation

    /**
     * Load show data using ShowDetailService with V2's database-first approach.
     * Database data loads immediately, Archive.org tracks load asynchronously.
     */
    fun loadShow(showId: String?, recordingId: String?) {
        if (showId == null) {
            Logger.w(TAG, "Cannot load show: showId is null")
            return
        }

        Logger.d(TAG, "Loading show: $showId, recording: $recordingId")

        viewModelScope.launch {
            try {
                // Clear any previous error state
                showDetailService.clearError()

                // Load show with V2's database-first pattern
                showDetailService.loadShow(showId, recordingId)

                Logger.d(TAG, "Show loading initiated successfully")

            } catch (e: Exception) {
                Logger.e(TAG, "Error loading show: $showId", e)
            }
        }
    }

    /**
     * Select a different recording for the current show.
     * Triggers track reload from Archive.org.
     */
    fun selectRecording(recordingId: String) {
        Logger.d(TAG, "Selecting recording: $recordingId")

        viewModelScope.launch {
            try {
                showDetailService.selectRecording(recordingId)
                Logger.d(TAG, "Recording selection successful")

            } catch (e: Exception) {
                Logger.e(TAG, "Error selecting recording: $recordingId", e)
            }
        }
    }

    /**
     * Refresh current show data.
     * Clears Archive.org cache and reloads tracks.
     */
    fun refreshShow() {
        Logger.d(TAG, "Refreshing current show")

        viewModelScope.launch {
            try {
                showDetailService.refreshCurrentShow()
                Logger.d(TAG, "Show refresh successful")

            } catch (e: Exception) {
                Logger.e(TAG, "Error refreshing show", e)
            }
        }
    }

    /**
     * Navigate to previous show chronologically.
     * Loads new show data in place without affecting navigation back stack.
     */
    fun navigateToPreviousShow() {
        Logger.d(TAG, "Browsing to previous show")

        viewModelScope.launch {
            try {
                val adjacentShows = showDetailService.getAdjacentShows()
                val previousShow = adjacentShows.previousShow

                if (previousShow != null) {
                    Logger.d(TAG, "Loading previous show: ${previousShow.displayTitle}")

                    // Load new show data in place (doesn't add to back stack)
                    loadShow(previousShow.id, previousShow.bestRecordingId)
                } else {
                    Logger.d(TAG, "No previous show available")
                }

            } catch (e: Exception) {
                Logger.e(TAG, "Error browsing to previous show", e)
            }
        }
    }

    /**
     * Navigate to next show chronologically.
     * Loads new show data in place without affecting navigation back stack.
     */
    fun navigateToNextShow() {
        Logger.d(TAG, "Browsing to next show")

        viewModelScope.launch {
            try {
                val adjacentShows = showDetailService.getAdjacentShows()
                val nextShow = adjacentShows.nextShow

                if (nextShow != null) {
                    Logger.d(TAG, "Loading next show: ${nextShow.displayTitle}")

                    // Load new show data in place (doesn't add to back stack)
                    loadShow(nextShow.id, nextShow.bestRecordingId)
                } else {
                    Logger.d(TAG, "No next show available")
                }

            } catch (e: Exception) {
                Logger.e(TAG, "Error browsing to next show", e)
            }
        }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        Logger.d(TAG, "Clearing error state")
        showDetailService.clearError()
    }

    /**
     * Play a track using MediaService.
     */
    fun playTrack(track: Track) {
        Logger.d(TAG, "Playing track: ${track.title ?: track.name}")

        viewModelScope.launch {
            try {
                // V2 pattern: Load entire show playlist and start at clicked track
                val allTracks = uiState.value.tracks
                val recordingId = uiState.value.currentRecordingId ?: ""
                val showData = uiState.value.showData

                // Pass show metadata to MediaService for UI display
                val result = mediaService.playTrack(
                    track = track,
                    recordingId = recordingId,
                    allTracks = allTracks,
                    showDate = showData?.date,
                    venue = showData?.venue?.name,
                    location = showData?.location?.displayText
                )

                if (result.isSuccess) {
                    Logger.d(TAG, "Track playback started successfully (playlist loaded)")
                    // Navigate to the player screen to show full player UI
                    _navigation.emit(NavigationEvent(AppScreen.Player))
                } else {
                    Logger.e(TAG, "Failed to start track playback: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error playing track: ${e.message}", e)
            }
        }
    }

    /**
     * Toggle playback of the current show - play first track if none playing.
     */
    fun togglePlayback() {
        Logger.d(TAG, "Toggling playback for current show")

        viewModelScope.launch {
            try {
                val currentPlaybackState = mediaService.playbackState.first()

                if (currentPlaybackState.currentTrack != null && currentPlaybackState.isPlaying) {
                    // If something is playing, pause it
                    mediaService.pause()
                    Logger.d(TAG, "Paused current track")
                } else if (currentPlaybackState.currentTrack != null) {
                    // If something is paused, resume it
                    mediaService.resume()
                    Logger.d(TAG, "Resumed current track")
                } else {
                    // If nothing is playing, play the first track of the current show
                    val tracks = uiState.value.tracks
                    if (tracks.isNotEmpty()) {
                        val firstTrack = tracks.first()
                        Logger.d(TAG, "Starting playback with first track: ${firstTrack.title ?: firstTrack.name}")

                        val showData = uiState.value.showData
                        val result = mediaService.playTrack(
                            track = firstTrack,
                            recordingId = uiState.value.currentRecordingId ?: "",
                            allTracks = tracks,
                            showDate = showData?.date,
                            venue = showData?.venue?.name,
                            location = showData?.location?.displayText
                        )
                        if (result.isSuccess) {
                            Logger.d(TAG, "Show playback started successfully (full playlist loaded)")
                            // Navigate to the player screen
                            _navigation.emit(NavigationEvent(AppScreen.Player))
                        } else {
                            Logger.e(TAG, "Failed to start show playback: ${result.exceptionOrNull()?.message}")
                        }
                    } else {
                        Logger.w(TAG, "No tracks available to play")
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error toggling playback: ${e.message}", e)
            }
        }
    }
}

/**
 * UI state for ShowDetail screen combining all service state.
 * Follows V2's pattern of cohesive UI state objects.
 */
data class ShowDetailUiState(
    val showData: Show? = null,
    val currentRecordingId: String? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val isTracksLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * Whether we have show data loaded.
     */
    val hasShow: Boolean get() = showData != null

    /**
     * Whether we have tracks loaded.
     */
    val hasTracks: Boolean get() = tracks.isNotEmpty()

    /**
     * Whether we're in any loading state.
     */
    val isAnyLoading: Boolean get() = isLoading || isTracksLoading

    /**
     * Whether we have an error to display.
     */
    val hasError: Boolean get() = error != null

    /**
     * Display title for the current show.
     */
    val displayTitle: String get() = showData?.displayTitle ?: "Loading..."
}