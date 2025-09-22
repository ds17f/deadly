package com.grateful.deadly.services.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON schema for show data from GitHub releases.
 * This matches the actual structure of show files in the data.zip.
 */
@Serializable
data class ShowJsonSchema(
    @SerialName("show_id") val showId: String,
    @SerialName("date") val date: String,
    @SerialName("band") val band: String? = null,
    @SerialName("venue") val venue: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("country") val country: String? = null,
    @SerialName("location_raw") val locationRaw: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("setlist_status") val setlistStatus: String? = null,
    @SerialName("setlist") val setlist: List<SetJsonSchema>? = null,
    @SerialName("lineup_status") val lineupStatus: String? = null,
    @SerialName("lineup") val lineup: List<LineupMemberJsonSchema>? = null,
    @SerialName("recordings") val recordings: List<String>? = null,
    @SerialName("avg_rating") val averageRating: Double? = null,
    @SerialName("reviews") val totalReviews: Int? = null
)

@Serializable
data class SetJsonSchema(
    @SerialName("set_name") val setName: String? = null,
    @SerialName("songs") val songs: List<SongJsonSchema>? = null
)

@Serializable
data class SongJsonSchema(
    @SerialName("name") val name: String,
    @SerialName("url") val url: String? = null,
    @SerialName("segue_into_next") val segueIntoNext: Boolean? = null
)

@Serializable
data class LineupMemberJsonSchema(
    @SerialName("name") val name: String,
    @SerialName("instruments") val instruments: String? = null,
    @SerialName("image_url") val imageUrl: String? = null
)

/**
 * Show entity for database storage.
 * This matches the SQLDelight schema structure.
 */
data class ShowEntity(
    val showId: String,
    val date: String,
    val year: Int,
    val month: Int,
    val yearMonth: String,
    val band: String,
    val url: String?,
    val venueName: String,
    val city: String?,
    val state: String?,
    val country: String,
    val locationRaw: String?,
    val setlistStatus: String?,
    val setlistRaw: String?,
    val songList: String?,
    val lineupStatus: String?,
    val lineupRaw: String?,
    val memberList: String?,
    val showSequence: Int,
    val recordingsRaw: String?,
    val recordingCount: Int,
    val bestRecordingId: String?,
    val averageRating: Double?,
    val totalReviews: Int,
    val isInLibrary: Boolean,
    val libraryAddedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Progress tracking for data import operations.
 */
sealed class ImportProgress {
    object Started : ImportProgress()
    data class Processing(val current: Int, val total: Int, val currentFile: String) : ImportProgress() {
        val percent: Float get() = if (total > 0) current.toFloat() / total else 0f
    }
    object Completed : ImportProgress()
}

/**
 * Result of a data import operation.
 */
sealed class ImportResult {
    data class Success(val importedCount: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}