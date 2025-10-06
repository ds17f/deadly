package com.grateful.deadly.domain.models

import kotlinx.serialization.Serializable

/**
 * Show domain model - rich business object with computed properties.
 *
 * Represents a Grateful Dead concert as a pure domain entity with business logic
 * and computed properties. Contains show-level metadata and references to recordings.
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

    // User state (V2 hybrid pattern: denormalized in Show table for fast queries)
    val isInLibrary: Boolean,
    val libraryAddedAt: Long?,

    // Recent play tracking (only populated for recently played shows)
    val lastPlayedRecordingId: String? = null
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
        get() = averageRating?.let { "${it}★" } ?: "Not Rated"

    /**
     * Whether this show has multiple recordings
     */
    val hasMultipleRecordings: Boolean
        get() = recordingCount > 1

    /**
     * Short date display (MM/DD/YY format)
     */
    val shortDate: String
        get() = try {
            val parts = date.split("-")
            "${parts[1]}/${parts[2]}/${parts[0].takeLast(2)}"
        } catch (e: Exception) {
            date
        }

    /**
     * Whether this show has setlist information
     */
    val hasSetlist: Boolean
        get() = setlist != null && setlist.sets.isNotEmpty()

    /**
     * Whether this show has lineup information
     */
    val hasLineup: Boolean
        get() = lineup != null && lineup.members.isNotEmpty()
}