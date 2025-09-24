package com.grateful.deadly.feature.showdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.navigation.AppScreen
import com.grateful.deadly.navigation.NavigationEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * ShowDetailViewModel - State coordination for ShowDetail screen
 *
 * Following V2's PlaylistViewModel architecture pattern:
 * 1. Reactive state management with StateFlow
 * 2. Progressive loading (show immediate, tracks async)
 * 3. Navigation events via SharedFlow
 * 4. Clean separation between UI state and business logic
 *
 * This is a placeholder implementation for Phase 1 navigation testing.
 * Full implementation will come in Phase 5 with universal services.
 */
class ShowDetailViewModel : ViewModel() {

    companion object {
        private const val TAG = "ShowDetailViewModel"
    }

    // UI State
    private val _uiState = MutableStateFlow(ShowDetailUiState())
    val uiState: StateFlow<ShowDetailUiState> = _uiState.asStateFlow()

    // Navigation event flow for reactive navigation
    private val _navigation = MutableSharedFlow<NavigationEvent>()
    val navigation: SharedFlow<NavigationEvent> = _navigation

    /**
     * Load show data for the given showId and optional recordingId
     * Following V2's dual route pattern
     */
    fun loadShow(showId: String?, recordingId: String?) {
        Logger.d(TAG, "Loading show: $showId, recording: $recordingId")

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

                // TODO Phase 3: Load from ShowDetailService
                // For now, create placeholder data to test navigation
                val placeholderShow = Show(
                    id = showId ?: "unknown",
                    date = "1977-05-08", // Cornell '77 as default
                    year = 1977,
                    band = "Grateful Dead",
                    venue = com.grateful.deadly.domain.models.Venue(
                        name = "Barton Hall",
                        city = "Ithaca",
                        state = "NY",
                        country = "USA"
                    ),
                    location = com.grateful.deadly.domain.models.Location(
                        displayText = "Ithaca, NY",
                        city = "Ithaca",
                        state = "NY"
                    ),
                    setlist = null,
                    lineup = null,
                    recordingIds = listOf(recordingId ?: "gd1977-05-08.sbd.example"),
                    bestRecordingId = recordingId ?: "gd1977-05-08.sbd.example",
                    recordingCount = 1,
                    averageRating = 4.8f,
                    totalReviews = 150,
                    isInLibrary = false,
                    libraryAddedAt = null
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showData = placeholderShow,
                    currentRecordingId = recordingId ?: placeholderShow.bestRecordingId,
                    isTrackListLoading = true // Start loading tracks async
                )

                // Simulate async track loading
                loadTrackListAsync()

            } catch (e: Exception) {
                Logger.e(TAG, "Error loading show: $showId", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load show"
                )
            }
        }
    }

    private fun loadTrackListAsync() {
        viewModelScope.launch {
            try {
                // TODO Phase 3: Load from ArchiveService
                // Simulate network delay
                kotlinx.coroutines.delay(2000)

                val placeholderTracks = listOf(
                    TrackInfo(
                        number = 1,
                        title = "Sugar Magnolia",
                        duration = "3:45",
                        format = "VBR MP3"
                    ),
                    TrackInfo(
                        number = 2,
                        title = "Fire on the Mountain",
                        duration = "12:30",
                        format = "VBR MP3"
                    ),
                    TrackInfo(
                        number = 3,
                        title = "Scarlet Begonias",
                        duration = "8:15",
                        format = "VBR MP3"
                    )
                )

                _uiState.value = _uiState.value.copy(
                    isTrackListLoading = false,
                    tracks = placeholderTracks
                )

            } catch (e: Exception) {
                Logger.e(TAG, "Error loading tracks", e)
                _uiState.value = _uiState.value.copy(
                    isTrackListLoading = false,
                    error = e.message ?: "Failed to load tracks"
                )
            }
        }
    }

    /**
     * Navigate to player with track context
     */
    fun onNavigateToPlayer(trackNumber: Int) {
        viewModelScope.launch {
            Logger.d(TAG, "Navigating to player for track: $trackNumber")
            _navigation.emit(NavigationEvent(AppScreen.Player))
        }
    }

    /**
     * Navigate back from show detail
     */
    fun onNavigateBack() {
        viewModelScope.launch {
            Logger.d(TAG, "Navigating back from show detail")
            _navigation.emit(NavigationEvent(AppScreen.Search))
        }
    }

    /**
     * Refresh current show data
     */
    fun refreshShow() {
        val currentShow = _uiState.value.showData
        val currentRecording = _uiState.value.currentRecordingId
        if (currentShow != null) {
            loadShow(currentShow.id, currentRecording)
        }
    }
}

/**
 * UI state for ShowDetail screen
 * Based on V2's PlaylistUiState with progressive loading support
 */
data class ShowDetailUiState(
    val isLoading: Boolean = false,
    val isTrackListLoading: Boolean = false,
    val error: String? = null,
    val showData: Show? = null,
    val currentRecordingId: String? = null,
    val tracks: List<TrackInfo> = emptyList(),
    val isPlaying: Boolean = false,
    val showReviewSheet: Boolean = false,
    val showMenuSheet: Boolean = false,
    val showSetlistSheet: Boolean = false
)

/**
 * Track information for UI display
 * Simplified model for Phase 1, will expand in Phase 3
 */
data class TrackInfo(
    val number: Int,
    val title: String,
    val duration: String,
    val format: String
)