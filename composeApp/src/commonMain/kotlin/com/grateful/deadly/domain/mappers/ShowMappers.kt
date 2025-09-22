package com.grateful.deadly.domain.mappers

import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.domain.models.*
import com.grateful.deadly.services.data.models.ShowEntity
import kotlinx.serialization.json.Json

/**
 * ShowMappers - centralized conversion between data and domain models.
 *
 * Handles safe JSON parsing with empty list fallbacks on errors.
 * All conversion logic is isolated here for testability and maintainability.
 */
class ShowMappers(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) {

    companion object {
        private const val TAG = "ShowMappers"
    }

    /**
     * Convert ShowEntity to Show domain model
     */
    fun entityToDomain(entity: ShowEntity): Show {
        return Show(
            id = entity.showId,
            date = entity.date,
            year = entity.year,
            band = entity.band,
            venue = Venue(
                name = entity.venueName,
                city = entity.city,
                state = entity.state,
                country = entity.country
            ),
            location = Location.fromRaw(entity.locationRaw, entity.city, entity.state),
            setlist = parseSetlist(entity.setlistRaw, entity.setlistStatus),
            lineup = parseLineup(entity.lineupRaw, entity.lineupStatus),
            recordingIds = parseRecordingIds(entity.recordingsRaw),
            bestRecordingId = entity.bestRecordingId,
            recordingCount = entity.recordingCount,
            averageRating = entity.averageRating?.toFloat(),
            totalReviews = entity.totalReviews,
            isInLibrary = entity.isInLibrary,
            libraryAddedAt = entity.libraryAddedAt
        )
    }

    /**
     * Convert list of ShowEntity to list of Show domain models
     */
    fun entitiesToDomain(entities: List<ShowEntity>): List<Show> =
        entities.map(::entityToDomain)

    /**
     * Convert Recording entity to Recording domain model (for future use)
     */
    fun recordingEntityToDomain(entity: RecordingEntity): Recording {
        return Recording(
            identifier = entity.identifier,
            showId = entity.showId,
            sourceType = RecordingSourceType.fromString(entity.sourceType),
            rating = entity.rating,
            reviewCount = entity.reviewCount,
            taper = entity.taper,
            source = entity.source,
            lineage = entity.lineage,
            sourceTypeString = entity.sourceTypeString
        )
    }

    /**
     * Convert list of Recording entities to domain models (for future use)
     */
    fun recordingEntitiesToDomain(entities: List<RecordingEntity>): List<Recording> =
        entities.map(::recordingEntityToDomain)

    /**
     * Parse setlist JSON with error handling
     */
    private fun parseSetlist(json: String?, status: String?): Setlist? {
        return try {
            Setlist.parse(json, status)
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to parse setlist: $json", e)
            null
        }
    }

    /**
     * Parse lineup JSON with error handling
     */
    private fun parseLineup(json: String?, status: String?): Lineup? {
        return try {
            Lineup.parse(json, status)
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to parse lineup: $json", e)
            null
        }
    }

    /**
     * Parse recording IDs from JSON array string
     */
    private fun parseRecordingIds(jsonString: String?): List<String> {
        return try {
            if (jsonString.isNullOrBlank()) return emptyList()
            json.decodeFromString<List<String>>(jsonString)
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to parse recording IDs: $jsonString", e)
            emptyList()
        }
    }
}

/**
 * Placeholder for RecordingEntity - will be implemented when we add recordings
 */
data class RecordingEntity(
    val identifier: String,
    val showId: String,
    val sourceType: String?,
    val rating: Double,
    val reviewCount: Int,
    val taper: String?,
    val source: String?,
    val lineage: String?,
    val sourceTypeString: String?
)