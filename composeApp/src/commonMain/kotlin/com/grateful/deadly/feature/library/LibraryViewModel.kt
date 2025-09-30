package com.grateful.deadly.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.domain.models.LibraryShow
import com.grateful.deadly.domain.models.LibraryShowViewModel
import com.grateful.deadly.domain.models.LibraryDisplayMode
import com.grateful.deadly.domain.models.LibrarySortDirection
import com.grateful.deadly.domain.models.LibrarySortOption
import com.grateful.deadly.domain.models.LibraryUiState
import com.grateful.deadly.services.library.LibraryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * LibraryViewModel (V2 pattern)
 *
 * Transforms service StateFlows into UI state with sorting/filtering.
 * Handles user actions and maintains UI preferences.
 * All UI logic is reactive - follows V2's proven ViewModel pattern.
 */
class LibraryViewModel(
    private val libraryService: LibraryService
) : ViewModel() {

    companion object {
        private const val TAG = "LibraryViewModel"
    }

    // === UI State Management ===

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    // === UI Preferences (V2 pattern) ===

    private val _selectedSortOption = MutableStateFlow(LibrarySortOption.DATE_ADDED)
    val selectedSortOption: StateFlow<LibrarySortOption> = _selectedSortOption.asStateFlow()

    private val _selectedSortDirection = MutableStateFlow(LibrarySortDirection.DESCENDING)
    val selectedSortDirection: StateFlow<LibrarySortDirection> = _selectedSortDirection.asStateFlow()

    private val _displayMode = MutableStateFlow(LibraryDisplayMode.LIST)
    val displayMode: StateFlow<LibraryDisplayMode> = _displayMode.asStateFlow()

    init {
        Logger.d(TAG, "LibraryViewModel initialized - setting up reactive flows")
        loadLibrary()
    }

    // === Reactive State Loading (V2 pattern) ===

    /**
     * Load library shows and create reactive UI state (V2 pattern)
     * Combines service flows with UI preferences for complete reactive state
     */
    private fun loadLibrary() {
        viewModelScope.launch {
            try {
                Logger.d(TAG, "loadLibrary() - Setting up reactive state flows")

                // Create reactive UI state from service flows + UI preferences (V2 pattern)
                combine(
                    libraryService.getCurrentShows(),
                    libraryService.getLibraryStats(),
                    _selectedSortOption,
                    _selectedSortDirection,
                    _displayMode
                ) { shows, stats, sortOption, sortDirection, displayMode ->

                    // Apply sorting with pin priority (V2 pattern: pinned shows always first)
                    val sortedShows = applySortingWithPinPriority(shows, sortOption, sortDirection)

                    // Convert to view models for UI display
                    val showViewModels = sortedShows.map { libraryShow ->
                        LibraryShowViewModel(
                            showId = libraryShow.showId,
                            date = libraryShow.date,
                            displayDate = libraryShow.displayDate,
                            venue = libraryShow.venue,
                            location = libraryShow.location,
                            rating = libraryShow.averageRating,
                            reviewCount = libraryShow.totalReviews,
                            addedToLibraryAt = libraryShow.addedToLibraryAt,
                            isPinned = libraryShow.isPinned,
                            downloadStatus = libraryShow.downloadStatus,
                            isDownloaded = libraryShow.isDownloaded,
                            isDownloading = libraryShow.isDownloading,
                            libraryStatusDescription = libraryShow.libraryStatusDescription
                        )
                    }

                    LibraryUiState(
                        isLoading = false,
                        error = null,
                        shows = showViewModels,
                        stats = stats,
                        selectedSortOption = sortOption,
                        selectedSortDirection = sortDirection,
                        displayMode = displayMode
                    )
                }.collect { newState ->
                    _uiState.value = newState
                    Logger.d(TAG, "UI state updated: ${newState.shows.size} shows, ${newState.stats.totalPinned} pinned")
                }

            } catch (e: Exception) {
                Logger.e(TAG, "Error loading library", e)
                _uiState.value = LibraryUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load library"
                )
            }
        }
    }

    /**
     * Apply sorting with pin priority (V2 pattern: pinned shows always first)
     * This matches V2's exact sorting behavior
     */
    private fun applySortingWithPinPriority(
        shows: List<LibraryShow>,
        sortOption: LibrarySortOption,
        sortDirection: LibrarySortDirection
    ): List<LibraryShow> {
        Logger.d(TAG, "applySortingWithPinPriority() - Sorting ${shows.size} shows by $sortOption $sortDirection")

        return when (sortOption) {
            LibrarySortOption.DATE_OF_SHOW -> {
                if (sortDirection == LibrarySortDirection.ASCENDING) {
                    shows.sortedWith(compareBy<LibraryShow> { !it.isPinned }.thenBy { it.date })
                } else {
                    shows.sortedWith(compareBy<LibraryShow> { !it.isPinned }.thenByDescending { it.date })
                }
            }
            LibrarySortOption.DATE_ADDED -> {
                if (sortDirection == LibrarySortDirection.ASCENDING) {
                    shows.sortedWith(compareBy<LibraryShow> { !it.isPinned }.thenBy { it.addedToLibraryAt })
                } else {
                    shows.sortedWith(compareBy<LibraryShow> { !it.isPinned }.thenByDescending { it.addedToLibraryAt })
                }
            }
            LibrarySortOption.VENUE -> {
                if (sortDirection == LibrarySortDirection.ASCENDING) {
                    shows.sortedWith(compareBy<LibraryShow> { !it.isPinned }.thenBy { it.venue })
                } else {
                    shows.sortedWith(compareBy<LibraryShow> { !it.isPinned }.thenByDescending { it.venue })
                }
            }
            LibrarySortOption.RATING -> {
                if (sortDirection == LibrarySortDirection.ASCENDING) {
                    shows.sortedWith(compareBy<LibraryShow> { !it.isPinned }.thenBy { it.averageRating ?: 0f })
                } else {
                    shows.sortedWith(compareBy<LibraryShow> { !it.isPinned }.thenByDescending { it.averageRating ?: 0f })
                }
            }
        }
    }

    // === User Actions (V2 pattern) ===

    /**
     * Add show to library (delegates to service)
     */
    fun addToLibrary(showId: String) {
        Logger.d(TAG, "addToLibrary() - User action: adding show '$showId'")
        viewModelScope.launch {
            libraryService.addToLibrary(showId)
        }
    }

    /**
     * Remove show from library (delegates to service)
     */
    fun removeFromLibrary(showId: String) {
        Logger.d(TAG, "removeFromLibrary() - User action: removing show '$showId'")
        viewModelScope.launch {
            libraryService.removeFromLibrary(showId)
        }
    }

    /**
     * Pin show for priority display (delegates to service)
     */
    fun pinShow(showId: String) {
        Logger.d(TAG, "pinShow() - User action: pinning show '$showId'")
        viewModelScope.launch {
            libraryService.pinShow(showId)
        }
    }

    /**
     * Unpin show (delegates to service)
     */
    fun unpinShow(showId: String) {
        Logger.d(TAG, "unpinShow() - User action: unpinning show '$showId'")
        viewModelScope.launch {
            libraryService.unpinShow(showId)
        }
    }

    /**
     * Update library notes (delegates to service)
     */
    fun updateLibraryNotes(showId: String, notes: String?) {
        Logger.d(TAG, "updateLibraryNotes() - User action: updating notes for show '$showId'")
        viewModelScope.launch {
            libraryService.updateLibraryNotes(showId, notes)
        }
    }

    /**
     * Share show (delegates to service)
     */
    fun shareShow(showId: String) {
        Logger.d(TAG, "shareShow() - User action: sharing show '$showId'")
        viewModelScope.launch {
            libraryService.shareShow(showId)
        }
    }

    /**
     * Clear entire library (delegates to service)
     */
    fun clearLibrary() {
        Logger.d(TAG, "clearLibrary() - User action: clearing entire library")
        viewModelScope.launch {
            libraryService.clearLibrary()
        }
    }

    /**
     * Unpin all shows (delegates to service)
     */
    fun unpinAllShows() {
        Logger.d(TAG, "unpinAllShows() - User action: unpinning all shows")
        viewModelScope.launch {
            libraryService.unpinAllShows()
        }
    }

    // === UI Preference Updates (V2 pattern) ===

    /**
     * Update sort option and trigger re-sort
     */
    fun updateSortOption(sortOption: LibrarySortOption) {
        Logger.d(TAG, "updateSortOption() - User changed sort to: $sortOption")
        _selectedSortOption.value = sortOption
    }

    /**
     * Update sort direction and trigger re-sort
     */
    fun updateSortDirection(sortDirection: LibrarySortDirection) {
        Logger.d(TAG, "updateSortDirection() - User changed sort direction to: $sortDirection")
        _selectedSortDirection.value = sortDirection
    }

    /**
     * Toggle between list and grid display modes
     */
    fun toggleDisplayMode() {
        val newMode = if (_displayMode.value == LibraryDisplayMode.LIST) {
            LibraryDisplayMode.GRID
        } else {
            LibraryDisplayMode.LIST
        }
        Logger.d(TAG, "toggleDisplayMode() - User toggled display mode to: $newMode")
        _displayMode.value = newMode
    }

    /**
     * Set specific display mode
     */
    fun setDisplayMode(displayMode: LibraryDisplayMode) {
        Logger.d(TAG, "setDisplayMode() - User set display mode to: $displayMode")
        _displayMode.value = displayMode
    }

    // === Reactive Queries for Individual Shows (V2 pattern) ===

    /**
     * Get reactive StateFlow for show library status
     * Used by ShowDetail and other screens for real-time updates
     */
    fun isShowInLibrary(showId: String): StateFlow<Boolean> {
        Logger.d(TAG, "isShowInLibrary() - Creating reactive flow for show '$showId'")
        return libraryService.isShowInLibrary(showId)
    }

    /**
     * Get reactive StateFlow for show pin status
     * Used by UI components for real-time pin indicators
     */
    fun isShowPinned(showId: String): StateFlow<Boolean> {
        Logger.d(TAG, "isShowPinned() - Creating reactive flow for show '$showId'")
        return libraryService.isShowPinned(showId)
    }

    // === Error Handling ===

    /**
     * Retry loading library after error
     */
    fun retry() {
        Logger.d(TAG, "retry() - User requested retry")
        _uiState.value = LibraryUiState(isLoading = true)
        loadLibrary()
    }

    /**
     * Refresh library data (same as retry, for LibraryScreen compatibility)
     */
    fun refreshLibrary() {
        Logger.d(TAG, "refreshLibrary() - User requested refresh")
        retry()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        Logger.d(TAG, "clearError() - User dismissed error")
        if (_uiState.value.error != null) {
            _uiState.value = _uiState.value.copy(error = null)
        }
    }

    // === Development and Testing Methods ===

    /**
     * Populate library with test data for development
     */
    fun populateTestData() {
        Logger.d(TAG, "populateTestData() - Populating library with test data")
        viewModelScope.launch {
            libraryService.populateTestData()
        }
    }

    // === Download Management (Phase 5 - Placeholders for now) ===

    /**
     * Download show for offline access
     * TODO Phase 5: Implement actual download functionality
     */
    fun downloadShow(showId: String) {
        Logger.d(TAG, "downloadShow() - User requested download for show '$showId' (Phase 5: Coming soon)")
        // TODO Phase 5: Implement download service integration
    }

    /**
     * Cancel show download
     * TODO Phase 5: Implement actual download cancellation
     */
    fun cancelDownload(showId: String) {
        Logger.d(TAG, "cancelDownload() - User requested cancel download for show '$showId' (Phase 5: Coming soon)")
        // TODO Phase 5: Implement download service integration
    }
}