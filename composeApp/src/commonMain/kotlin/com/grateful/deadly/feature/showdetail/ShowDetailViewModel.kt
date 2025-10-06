package com.grateful.deadly.feature.showdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.domain.models.Recording
import com.grateful.deadly.services.archive.Track
import com.grateful.deadly.services.media.MediaService
import com.grateful.deadly.services.media.PlaybackStatus
import com.grateful.deadly.services.library.LibraryService
import com.grateful.deadly.navigation.AppScreen
import com.grateful.deadly.navigation.NavigationEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

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
@OptIn(ExperimentalCoroutinesApi::class)
class ShowDetailViewModel(
    private val showDetailService: ShowDetailService,
    private val mediaService: MediaService,
    private val libraryService: LibraryService
) : ViewModel() {

    companion object {
        private const val TAG = "ShowDetailViewModel"
    }

    // UI State - combines service state flows into single reactive UI state (V2 pattern)
    val uiState: StateFlow<ShowDetailUiState> = combine(
        showDetailService.currentShow,
        showDetailService.currentRecording,
        showDetailService.currentTracks,
        showDetailService.isTracksLoading,
        showDetailService.error
    ) { show, recording, tracks, isTracksLoading, error ->
        Logger.d(TAG, "Base UI state update: show=${show?.displayTitle}, recording=${recording?.identifier}, tracks=${tracks.size}, loading=$isTracksLoading, error=$error")

        ShowDetailUiState(
            showData = show,
            currentRecordingId = recording?.identifier,
            tracks = tracks,
            isLoading = show == null && error == null, // Loading if no show and no error
            isTracksLoading = isTracksLoading,
            error = error,
            isCurrentShowAndRecording = false, // Will be updated by media state combination
            isPlaying = false,
            isMediaLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShowDetailUiState(isLoading = true)
    )

    // Media state for comparison logic (separate StateFlow)
    private val mediaState: StateFlow<MediaState> = combine(
        mediaService.currentShowId,
        mediaService.currentRecordingId,
        mediaService.playbackStatus,
        mediaService.isPlaying
    ) { mediaShowId, mediaRecordingId, playbackStatus, isPlaying ->
        MediaState(
            currentShowId = mediaShowId,
            currentRecordingId = mediaRecordingId,
            playbackStatus = playbackStatus,
            isPlaying = isPlaying
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaState()
    )

    // Library state flow that reacts to current show changes (V2 pattern)
    private val libraryState: StateFlow<Boolean> = uiState
        .map { it.showData?.id }
        .distinctUntilChanged()
        .flatMapLatest { showId ->
            if (showId != null) {
                Logger.d(TAG, "Library state: Observing library status for showId: $showId")
                libraryService.isShowInLibrary(showId)
            } else {
                Logger.d(TAG, "Library state: No show ID, returning false")
                flowOf(false)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Menu state (for modal visibility)
    private val _showMenu = MutableStateFlow(false)

    // Combined UI state with media comparison logic and library state (V2 pattern)
    val enhancedUiState: StateFlow<ShowDetailUiState> = combine(
        uiState,
        mediaState,
        libraryState,
        _showMenu
    ) { baseState, media, isInLibrary, showMenu ->
        // V2 logic: Determine if current show and recording match MediaService
        val playlistShowId = baseState.showData?.id
        val playlistRecordingId = baseState.currentRecordingId
        val isCurrentShowAndRecording = playlistShowId != null &&
                                       playlistRecordingId != null &&
                                       (playlistShowId == media.currentShowId || playlistShowId == media.currentShowId?.replace("-", "")) &&
                                       playlistRecordingId == media.currentRecordingId

        // Update showData with reactive library status (V2 pattern)
        val updatedShowData = baseState.showData?.copy(isInLibrary = isInLibrary)

        Logger.d(TAG, "Enhanced UI state update: isCurrentShowAndRecording=$isCurrentShowAndRecording, mediaShowId=${media.currentShowId}, mediaRecordingId=${media.currentRecordingId}, isInLibrary=$isInLibrary, showId=${baseState.showData?.id}")

        baseState.copy(
            showData = updatedShowData,
            isCurrentShowAndRecording = isCurrentShowAndRecording,
            isPlaying = media.isPlaying,
            isMediaLoading = media.playbackStatus?.isLoading == true || media.playbackStatus?.isBuffering == true,
            showMenu = showMenu
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
                    // Pass null to let it check user preferences
                    loadShow(previousShow.id, null)
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
                    // Pass null to let it check user preferences
                    loadShow(nextShow.id, null)
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
                val allTracks = enhancedUiState.value.tracks
                val recordingId = enhancedUiState.value.currentRecordingId ?: ""
                val showData = enhancedUiState.value.showData

                Logger.d(TAG, "🔴 ShowDetailViewModel.playTrack() - recordingId from UI state: $recordingId")

                // Pass show metadata to MediaService for UI display
                val result = mediaService.playTrack(
                    track = track,
                    recordingId = recordingId,
                    allTracks = allTracks,
                    showId = showData?.id ?: "",
                    format = "SBD", // TODO: Get from user preference or recording metadata
                    showDate = showData?.date,
                    venue = showData?.venue?.name,
                    location = showData?.location?.displayText
                )

                if (result.isSuccess) {
                    Logger.d(TAG, "Track playback started successfully (playlist loaded)")
                    // Stay on ShowDetail screen - don't navigate to player
                } else {
                    Logger.e(TAG, "Failed to start track playback: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error playing track: ${e.message}", e)
            }
        }
    }

    /**
     * Toggle playback of the current show - V2 logic for same show vs different show.
     */
    fun togglePlayback() {
        Logger.d(TAG, "Toggling playback for current show (V2 logic)")

        viewModelScope.launch {
            try {
                val currentState = enhancedUiState.value // Use reactive state with isCurrentShowAndRecording

                if (currentState.isCurrentShowAndRecording && currentState.isPlaying) {
                    // Currently playing this show/recording → pause
                    Logger.d(TAG, "V2 Media: Pausing current playback")
                    mediaService.pause()
                } else {
                    // Either not playing, or different show/recording → start playback
                    Logger.d(TAG, "V2 Media: Starting playback (new or resume)")

                    if (currentState.isCurrentShowAndRecording) {
                        Logger.d(TAG, "V2 Media: Resuming current recording ${currentState.currentRecordingId}")
                        mediaService.resume()
                    } else {
                        Logger.d(TAG, "V2 Media: Play All for new recording ${currentState.currentRecordingId}")
                        // Use MediaService for Play All logic (new show/recording)
                        val tracks = currentState.tracks
                        if (tracks.isNotEmpty()) {
                            val firstTrack = tracks.first()
                            Logger.d(TAG, "Starting playback with first track: ${firstTrack.title ?: firstTrack.name}")

                            Logger.d(TAG, "🔴 ShowDetailViewModel.togglePlayback() - recordingId from UI state: ${currentState.currentRecordingId}")

                            val showData = currentState.showData
                            val result = mediaService.playTrack(
                                track = firstTrack,
                                recordingId = currentState.currentRecordingId ?: "",
                                allTracks = tracks,
                                showId = showData?.id ?: "",
                                format = "SBD", // TODO: Get from user preference or recording metadata
                                showDate = showData?.date,
                                venue = showData?.venue?.name,
                                location = showData?.location?.displayText
                            )
                            if (result.isSuccess) {
                                Logger.d(TAG, "Show playback started successfully (full playlist loaded)")
                                // Stay on ShowDetail screen - don't navigate to player
                            } else {
                                Logger.e(TAG, "Failed to start show playback: ${result.exceptionOrNull()?.message}")
                            }
                        } else {
                            Logger.w(TAG, "No tracks available to play")
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error toggling playback: ${e.message}", e)
            }
        }
    }

    // === Menu Actions ===

    /**
     * Show the menu bottom sheet
     */
    fun showMenu() {
        Logger.d(TAG, "Showing menu")
        _showMenu.value = true
    }

    /**
     * Hide the menu bottom sheet
     */
    fun hideMenu() {
        Logger.d(TAG, "Hiding menu")
        _showMenu.value = false
    }

    /**
     * Share the current show
     */
    fun shareShow() {
        Logger.d(TAG, "Share show")
        // TODO: Implement share functionality
        // For now, just log that it was called
    }

    // === Library Actions (V2 Pattern Integration) ===

    /**
     * Toggle library status for current show
     */
    fun toggleLibraryStatus() {
        val showData = enhancedUiState.value.showData ?: return

        Logger.d(TAG, "toggleLibraryStatus() - Current status: ${showData.isInLibrary}")

        viewModelScope.launch {
            if (showData.isInLibrary) {
                libraryService.removeFromLibrary(showData.id)
            } else {
                libraryService.addToLibrary(showData.id)
            }
        }
    }

}

/**
 * Media state for comparison logic.
 */
private data class MediaState(
    val currentShowId: String? = null,
    val currentRecordingId: String? = null,
    val playbackStatus: PlaybackStatus? = null,
    val isPlaying: Boolean = false
)

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
    val error: String? = null,
    val isCurrentShowAndRecording: Boolean = false,
    val isPlaying: Boolean = false,
    val isMediaLoading: Boolean = false,
    val showMenu: Boolean = false
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