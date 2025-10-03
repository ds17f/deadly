package com.grateful.deadly.services.media

import com.grateful.deadly.services.archive.Track
import kotlinx.serialization.Serializable

/**
 * Enriched track data structure containing all V2 metadata fields.
 *
 * This structure carries both the original Track data and all the rich metadata
 * needed for MediaItem creation, Now Playing integration, and RecentShowsService tracking.
 *
 * Based on V2's MediaMetadata extras pattern, this ensures both Android and iOS
 * have access to all the same metadata fields for consistent functionality.
 */
@Serializable
data class EnrichedTrack(
    // Original track data
    val track: Track,
    val trackIndex: Int,  // Position in playlist (0-based)

    // V2 Metadata Fields (matching MediaMetadata extras exactly)
    val showId: String,           // For RecentShowsService tracking
    val recordingId: String,      // For track context identification
    val venue: String?,           // For UI display
    val showDate: String?,        // For UI display
    val location: String?,        // For UI display
    val filename: String,         // track.name - for track identification
    val format: String,           // User-selected format (SBD/AUD/etc)
    val trackUrl: String,         // Archive.org streaming URL

    // Computed Display Fields (for MediaMetadata display)
    val displayTitle: String,     // track.title ?: track.name
    val displayArtist: String,    // "Grateful Dead"
    val displayAlbum: String,     // Formatted: "May 8, 1977 - Barton Hall"
    val mediaId: String           // "${showId}|${recordingId}|${trackIndex}"
) {
    companion object {
        /**
         * Create EnrichedTrack from Track and show context.
         * Handles all the metadata computation and formatting logic.
         */
        fun create(
            track: Track,
            trackIndex: Int,
            showId: String,
            recordingId: String,
            format: String,
            showDate: String?,
            venue: String?,
            location: String?
        ): EnrichedTrack {
            // Build Archive.org streaming URL
            val trackUrl = "https://archive.org/download/${recordingId}/${track.name}"

            // Compute display fields
            val displayTitle = track.title?.takeIf { it.isNotBlank() } ?: track.name
            val displayArtist = "Grateful Dead"
            val displayAlbum = buildDisplayAlbum(showDate, venue)
            val mediaId = "${showId}|${recordingId}|${trackIndex}"

            return EnrichedTrack(
                track = track,
                trackIndex = trackIndex,
                showId = showId,
                recordingId = recordingId,
                venue = venue,
                showDate = showDate,
                location = location,
                filename = track.name,
                format = format,
                trackUrl = trackUrl,
                displayTitle = displayTitle,
                displayArtist = displayArtist,
                displayAlbum = displayAlbum,
                mediaId = mediaId
            )
        }

        /**
         * Build display album string following V2 pattern.
         * Format: "Apr 3, 1990 - The Omni" or just show date if no venue
         */
        private fun buildDisplayAlbum(showDate: String?, venue: String?): String {
            val formattedDate = formatShowDate(showDate)

            return if (!venue.isNullOrBlank() && venue != "Unknown Venue") {
                "$formattedDate - $venue"
            } else {
                formattedDate
            }
        }

        /**
         * Format show date from YYYY-MM-DD to "MMM DD, YYYY" (V2 pattern).
         */
        private fun formatShowDate(dateString: String?): String {
            if (dateString.isNullOrBlank()) return "Unknown Date"

            return try {
                // Simple date formatting for YYYY-MM-DD format
                val parts = dateString.split("-")
                if (parts.size == 3) {
                    val year = parts[0]
                    val month = parts[1].toInt()
                    val day = parts[2].toInt()

                    val monthNames = arrayOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )

                    "${monthNames[month - 1]} $day, $year"
                } else {
                    dateString
                }
            } catch (e: Exception) {
                dateString
            }
        }
    }

    /**
     * Whether this track has complete metadata for proper display.
     */
    val hasCompleteMetadata: Boolean
        get() = showId.isNotBlank() &&
                recordingId.isNotBlank() &&
                displayTitle.isNotBlank() &&
                trackUrl.isNotBlank()

    /**
     * Short identifier for logging/debugging.
     */
    val shortId: String
        get() = "${showId.takeLast(8)}|${trackIndex}"
}