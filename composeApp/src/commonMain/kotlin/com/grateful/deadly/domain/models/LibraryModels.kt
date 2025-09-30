package com.grateful.deadly.domain.models

import kotlinx.serialization.Serializable

/**
 * LibraryShow - Domain model combining Show with library metadata (V2 pattern)
 *
 * Wraps a Show with library-specific metadata from the LibraryShow table.
 * Provides convenience accessors and computed properties for UI display.
 */
@Serializable
data class LibraryShow(
    val show: Show,
    val addedToLibraryAt: Long,
    val isPinned: Boolean = false,
    val libraryNotes: String? = null,
    val customRating: Float? = null,
    val lastAccessedAt: Long? = null,
    val tags: List<String>? = null,
    val downloadStatus: LibraryDownloadStatus = LibraryDownloadStatus.NOT_DOWNLOADED
) {
    // Convenience accessors from wrapped Show
    val showId: String get() = show.id
    val date: String get() = show.date
    val venue: String get() = show.venue.name
    val location: String get() = show.location.displayText
    val displayTitle: String get() = show.displayTitle
    val displayDate: String get() = formatDisplayDate(show.date)
    val displayVenue: String get() = show.venue.name
    val displayLocation: String get() = show.location.displayText
    val recordingCount: Int get() = show.recordingCount
    val averageRating: Float? get() = show.averageRating
    val totalReviews: Int get() = show.totalReviews
    val isInLibrary: Boolean get() = true // Always true for LibraryShow

    // Library-specific computed properties (V2 pattern)
    val isPinnedAndDownloaded: Boolean
        get() = isPinned && downloadStatus == LibraryDownloadStatus.COMPLETED

    val libraryAge: Long
        get() = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - addedToLibraryAt

    val isDownloaded: Boolean
        get() = downloadStatus == LibraryDownloadStatus.COMPLETED

    val isDownloading: Boolean
        get() = downloadStatus == LibraryDownloadStatus.DOWNLOADING

    val libraryStatusDescription: String get() = when {
        isPinned && isDownloaded -> "Pinned & Downloaded"
        isPinned -> "Pinned"
        isDownloaded -> "Downloaded"
        isDownloading -> "Downloading..."
        else -> "In Library"
    }

    // Sortable properties (V2 pattern)
    val sortableAddedDate: Long get() = addedToLibraryAt
    val sortableShowDate: String get() = show.date
    val sortablePinStatus: Int get() = if (isPinned) 0 else 1 // 0 = pinned first

    /**
     * Format date for display (V2 pattern)
     */
    private fun formatDisplayDate(date: String): String {
        return try {
            val parts = date.split("-")
            if (parts.size != 3) return date

            val year = parts[0]
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            val monthNames = arrayOf(
                "", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )

            if (month < 1 || month > 12) return date

            "${monthNames[month]} $day, $year"
        } catch (e: Exception) {
            date
        }
    }
}

/**
 * Download status for library shows (V2 pattern)
 */
@Serializable
enum class LibraryDownloadStatus {
    NOT_DOWNLOADED,
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Library statistics (V2 pattern)
 */
@Serializable
data class LibraryStats(
    val totalShows: Int,
    val totalPinned: Int,
    val totalDownloaded: Int = 0,
    val totalStorageUsed: Long = 0L
)

/**
 * Sort options for library display (V2 pattern)
 */
enum class LibrarySortOption(val displayName: String) {
    DATE_OF_SHOW("Show Date"),
    DATE_ADDED("Date Added"),
    VENUE("Venue"),
    RATING("Rating")
}

/**
 * Sort directions (V2 pattern)
 */
enum class LibrarySortDirection(val displayName: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
}

/**
 * Display modes for library (V2 pattern)
 */
enum class LibraryDisplayMode {
    LIST,
    GRID
}

/**
 * UI state for Library screens (V2 pattern)
 */
data class LibraryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val shows: List<LibraryShowViewModel> = emptyList(),
    val stats: LibraryStats = LibraryStats(0, 0),
    val selectedSortOption: LibrarySortOption = LibrarySortOption.DATE_ADDED,
    val selectedSortDirection: LibrarySortDirection = LibrarySortDirection.DESCENDING,
    val displayMode: LibraryDisplayMode = LibraryDisplayMode.LIST
)

/**
 * UI-specific ViewModel for LibraryShow display (V2 pattern)
 *
 * Lightweight view model for rendering library shows in lists/grids.
 * Flattens nested data for simpler UI binding.
 */
@Serializable
data class LibraryShowViewModel(
    val showId: String,
    val date: String,
    val displayDate: String,
    val venue: String,
    val location: String,
    val rating: Float?,
    val reviewCount: Int,
    val addedToLibraryAt: Long,
    val isPinned: Boolean,
    val downloadStatus: LibraryDownloadStatus,
    val isDownloaded: Boolean,
    val isDownloading: Boolean,
    val libraryStatusDescription: String
)