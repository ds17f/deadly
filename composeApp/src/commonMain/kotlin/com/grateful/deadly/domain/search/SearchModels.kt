package com.grateful.deadly.domain.search

import kotlinx.serialization.Serializable

/**
 * Search specific data models for the KMM search interface
 * Ported from V2 search models
 */

@Serializable
data class SearchResultShow(
    val show: Show,
    val relevanceScore: Float = 1.0f,
    val matchType: SearchMatchType = SearchMatchType.TITLE,
    val hasDownloads: Boolean = false,
    val highlightedFields: List<String> = emptyList()
)

@Serializable
data class RecentSearch(
    val query: String,
    val timestamp: Long
)

@Serializable
data class SuggestedSearch(
    val query: String,
    val resultCount: Int = 0,
    val type: SuggestionType = SuggestionType.GENERAL
)

enum class SearchMatchType(val displayName: String) {
    TITLE("Title"),
    VENUE("Venue"), 
    YEAR("Year"),
    SETLIST("Setlist"),
    LOCATION("Location"),
    GENERAL("General")
}

enum class SuggestionType {
    GENERAL,
    VENUE,
    YEAR,
    SONG,
    LOCATION
}

enum class SearchStatus {
    IDLE,
    SEARCHING,
    SUCCESS,
    ERROR,
    NO_RESULTS
}

@Serializable
data class SearchStats(
    val totalResults: Int,
    val searchDuration: Long,
    val appliedFilters: List<String> = emptyList()
)

/**
 * UI State for SearchScreen
 * 
 * Comprehensive state model discovered through UI-first development
 * and coordinated with SearchService reactive flows.
 */
data class SearchUiState(
    val searchQuery: String = "",
    val searchResults: List<SearchResultShow> = emptyList(),
    val searchStatus: SearchStatus = SearchStatus.IDLE,
    val recentSearches: List<RecentSearch> = emptyList(),
    val suggestedSearches: List<SuggestedSearch> = emptyList(),
    val searchStats: SearchStats = SearchStats(0, 0),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * KMM Show domain model
 * 
 * Represents a Grateful Dead concert as a pure domain entity.
 * Contains show-level metadata and references to recordings,
 * but not the full recording objects themselves.
 */
@Serializable
data class Show(
    val id: String,
    val date: String,
    val year: Int,
    val band: String,
    val venue: Venue,
    val location: Location,
    val setlist: Setlist?,
    val lineup: Lineup?,
    
    // Recording references
    val recordingIds: List<String>,
    val bestRecordingId: String?,
    
    // Show-level stats (precomputed from recordings)
    val recordingCount: Int,
    val averageRating: Float?,
    val totalReviews: Int,
    
    // User state  
    val isInLibrary: Boolean,
    val libraryAddedAt: Long?
) {
    /**
     * Display title for the show
     */
    val displayTitle: String
        get() = "${venue.name} - $date"
    
    /**
     * Whether this show has ratings
     */
    val hasRating: Boolean
        get() = averageRating != null && averageRating > 0f
    
    /**
     * Formatted rating display
     */
    val displayRating: String
        get() = averageRating?.let { 
            val rounded = kotlin.math.round(it * 10) / 10.0
            "$rounded★"
        } ?: "Not Rated"
    
    /**
     * Whether this show has multiple recordings
     */
    val hasMultipleRecordings: Boolean
        get() = recordingCount > 1
}

@Serializable
data class Venue(
    val name: String,
    val city: String?,
    val state: String?,
    val country: String
) {
    val displayLocation: String
        get() = listOfNotNull(city, state, country.takeIf { it != "USA" })
            .joinToString(", ")
}

@Serializable
data class Location(
    val displayText: String,
    val city: String?,
    val state: String?
) {
    companion object {
        fun fromRaw(raw: String?, city: String?, state: String?): Location {
            val display = raw ?: listOfNotNull(city, state).joinToString(", ").ifEmpty { "Unknown Location" }
            return Location(display, city, state)
        }
    }
}

@Serializable
data class Setlist(
    val status: String,
    val sets: List<SetlistSet>,
    val raw: String?,
    val date: String? = null,
    val venue: String? = null
)

@Serializable
data class SetlistSet(
    val name: String,
    val songs: List<SetlistSong>
)

@Serializable
data class SetlistSong(
    val name: String,
    val position: Int,
    val hasSegue: Boolean = false,
    val segueSymbol: String? = null
) {
    val displayName: String
        get() = if (hasSegue && segueSymbol != null) {
            "$name $segueSymbol"
        } else {
            name
        }
}

@Serializable
data class Lineup(
    val status: String,
    val members: List<LineupMember>,
    val raw: String?
)

@Serializable
data class LineupMember(
    val name: String,
    val instruments: String
)

enum class SearchFilter(val displayName: String) {
    VENUE("Venue"),
    YEAR("Year"),
    LOCATION("Location"),
    HAS_DOWNLOADS("Has Downloads"),
    RECENT("Recent"),
    POPULAR("Popular"),
    SOUNDBOARD("Soundboard"),
    AUDIENCE("Audience")
}