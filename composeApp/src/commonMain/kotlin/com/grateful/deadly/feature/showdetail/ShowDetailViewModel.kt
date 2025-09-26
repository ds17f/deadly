package com.grateful.deadly.feature.showdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.domain.models.Recording
import com.grateful.deadly.services.archive.Track
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
    private val showDetailService: ShowDetailService
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
     */
    fun navigateToPreviousShow() {
        Logger.d(TAG, "Navigating to previous show")

        viewModelScope.launch {
            try {
                val adjacentShows = showDetailService.getAdjacentShows()
                val previousShow = adjacentShows.previousShow

                if (previousShow != null) {
                    Logger.d(TAG, "Navigating to previous show: ${previousShow.displayTitle}")

                    // Emit navigation event
                    _navigation.emit(NavigationEvent(
                        AppScreen.ShowDetail(previousShow.id, previousShow.bestRecordingId)
                    ))
                } else {
                    Logger.d(TAG, "No previous show available")
                }

            } catch (e: Exception) {
                Logger.e(TAG, "Error navigating to previous show", e)
            }
        }
    }

    /**
     * Navigate to next show chronologically.
     */
    fun navigateToNextShow() {
        Logger.d(TAG, "Navigating to next show")

        viewModelScope.launch {
            try {
                val adjacentShows = showDetailService.getAdjacentShows()
                val nextShow = adjacentShows.nextShow

                if (nextShow != null) {
                    Logger.d(TAG, "Navigating to next show: ${nextShow.displayTitle}")

                    // Emit navigation event
                    _navigation.emit(NavigationEvent(
                        AppScreen.ShowDetail(nextShow.id, nextShow.bestRecordingId)
                    ))
                } else {
                    Logger.d(TAG, "No next show available")
                }

            } catch (e: Exception) {
                Logger.e(TAG, "Error navigating to next show", e)
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
     * Play a track (placeholder for Phase 5 media integration).
     */
    fun playTrack(track: Track) {
        Logger.d(TAG, "Playing track: ${track.title ?: track.name}")

        viewModelScope.launch {
            // TODO Phase 5: Integrate with MediaService
            Logger.d(TAG, "Track playback not yet implemented - needs MediaService integration")
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