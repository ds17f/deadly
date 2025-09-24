package com.grateful.deadly.services.archive

import com.grateful.deadly.services.archive.platform.NetworkClient
import com.grateful.deadly.services.archive.platform.CacheManager
import com.grateful.deadly.services.archive.platform.CacheType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Universal ArchiveService implementing Archive.org business logic.
 *
 * This is the Universal Service in the Universal Service + Platform Tool pattern.
 * It contains ALL Archive.org domain knowledge and business rules while remaining
 * platform-agnostic by delegating to platform tools.
 *
 * Responsibilities:
 * - Archive.org URL construction and API knowledge
 * - Business logic for metadata, tracks, and reviews
 * - Smart caching strategies with expiry management
 * - Error handling and retry logic
 * - Data format parsing and domain model mapping
 *
 * Platform tools handle:
 * - Generic HTTP requests (NetworkClient)
 * - Generic file I/O operations (CacheManager)
 * - Generic media playback (PlatformMediaPlayer via MediaService)
 */
class ArchiveService(
    private val networkClient: NetworkClient,
    private val cacheManager: CacheManager
) {

    companion object {
        // Archive.org API endpoints
        private const val ARCHIVE_METADATA_BASE = "https://archive.org/metadata"
        private const val ARCHIVE_DOWNLOAD_BASE = "https://archive.org/download"

        // V2 compatibility patterns
        private const val METADATA_SUFFIX = "_meta.json"
        private const val REVIEWS_SUFFIX = "_reviews.json"

        // Business logic constants
        private const val MIN_TRACK_DURATION_SEC = 30 // Skip very short files
        private val PREFERRED_FORMATS = listOf("mp3", "flac", "ogg") // In preference order
    }

    /**
     * Get show metadata with smart caching.
     *
     * Uses V2's caching pattern: {recordingId}.metadata.json
     * Handles Archive.org metadata API response format and parsing.
     */
    suspend fun getShowMetadata(recordingId: String): Result<ShowMetadata> {
        return try {
            // Check cache first
            val cacheKey = recordingId
            val cached = cacheManager.get(cacheKey, CacheType.METADATA)

            if (cached != null && !cacheManager.isExpired(cacheKey, CacheType.METADATA)) {
                // Parse cached data
                val metadata = parseMetadataJson(cached)
                return Result.success(metadata)
            }

            // Fetch from Archive.org
            val url = "$ARCHIVE_METADATA_BASE/$recordingId"
            val response = networkClient.getJson(url)

            response.fold(
                onSuccess = { jsonData ->
                    // Cache the raw response
                    cacheManager.put(cacheKey, CacheType.METADATA, jsonData)

                    // Parse and return
                    val metadata = parseMetadataJson(jsonData)
                    Result.success(metadata)
                },
                onFailure = { error ->
                    Result.failure(Exception("Failed to fetch metadata for $recordingId: ${error.message}", error))
                }
            )
        } catch (e: Exception) {
            Result.failure(Exception("ArchiveService metadata error for $recordingId", e))
        }
    }

    /**
     * Get show tracks with smart format selection.
     *
     * Implements V2's track discovery and format preference logic.
     * Filters out very short files and prioritizes high-quality formats.
     */
    suspend fun getShowTracks(recordingId: String): Result<List<ShowTrack>> {
        return try {
            // Check cache first
            val cacheKey = recordingId
            val cached = cacheManager.get(cacheKey, CacheType.TRACKS)

            if (cached != null && !cacheManager.isExpired(cacheKey, CacheType.TRACKS)) {
                val tracks = parseTracksJson(cached, recordingId)
                return Result.success(tracks)
            }

            // Get metadata to extract file information
            val metadataResult = getShowMetadata(recordingId)
            metadataResult.fold(
                onSuccess = { metadata ->
                    // Extract tracks from metadata
                    val tracks = extractTracksFromMetadata(metadata, recordingId)

                    // Cache the processed tracks
                    val tracksJson = serializeTracksToJson(tracks)
                    cacheManager.put(cacheKey, CacheType.TRACKS, tracksJson)

                    Result.success(tracks)
                },
                onFailure = { error ->
                    Result.failure(Exception("Failed to get tracks for $recordingId: ${error.message}", error))
                }
            )
        } catch (e: Exception) {
            Result.failure(Exception("ArchiveService tracks error for $recordingId", e))
        }
    }

    /**
     * Get show reviews with V2 compatibility.
     *
     * Archive.org reviews API integration following V2 patterns.
     */
    suspend fun getShowReviews(recordingId: String): Result<List<ShowReview>> {
        return try {
            // Check cache first
            val cacheKey = recordingId
            val cached = cacheManager.get(cacheKey, CacheType.REVIEWS)

            if (cached != null && !cacheManager.isExpired(cacheKey, CacheType.REVIEWS)) {
                val reviews = parseReviewsJson(cached)
                return Result.success(reviews)
            }

            // Fetch reviews from Archive.org
            val url = "$ARCHIVE_METADATA_BASE/$recordingId$REVIEWS_SUFFIX"
            val response = networkClient.getJson(url)

            response.fold(
                onSuccess = { jsonData ->
                    // Cache the raw response
                    cacheManager.put(cacheKey, CacheType.REVIEWS, jsonData)

                    // Parse and return
                    val reviews = parseReviewsJson(jsonData)
                    Result.success(reviews)
                },
                onFailure = { error ->
                    // Reviews are optional - return empty list if not found
                    Result.success(emptyList())
                }
            )
        } catch (e: Exception) {
            // Reviews are optional - return empty list on error
            Result.success(emptyList())
        }
    }

    /**
     * Clear cache for a specific show (all types).
     * Useful for forced refresh or cache management.
     */
    suspend fun clearShowCache(recordingId: String) {
        try {
            cacheManager.clear(recordingId, null) // Clear all types for this recording
        } catch (e: Exception) {
            // Cache clearing is not critical
        }
    }

    /**
     * Clear all Archive.org cache.
     * Useful for cache reset or storage management.
     */
    suspend fun clearAllCache() {
        try {
            cacheManager.clear(null, null) // Clear everything
        } catch (e: Exception) {
            // Cache clearing is not critical
        }
    }

    // Private parsing methods implementing Archive.org format knowledge

    private fun parseMetadataJson(jsonData: String): ShowMetadata {
        val json = Json.parseToJsonElement(jsonData)
        val metadata = json.jsonObject["metadata"]?.jsonObject
            ?: throw Exception("Invalid metadata format - missing metadata object")

        return ShowMetadata(
            identifier = metadata["identifier"]?.jsonPrimitive?.content ?: "",
            title = metadata["title"]?.jsonPrimitive?.content ?: "",
            creator = metadata["creator"]?.jsonPrimitive?.content ?: "",
            date = metadata["date"]?.jsonPrimitive?.content ?: "",
            venue = metadata["venue"]?.jsonPrimitive?.content ?: "",
            source = metadata["source"]?.jsonPrimitive?.content ?: "",
            description = metadata["description"]?.jsonPrimitive?.content ?: ""
        )
    }

    private fun extractTracksFromMetadata(metadata: ShowMetadata, recordingId: String): List<ShowTrack> {
        // This is a stub implementation for Phase 3
        // In Phase 6, this will parse the files array from metadata JSON
        // and implement smart format selection and track discovery
        return emptyList()
    }

    private fun parseTracksJson(jsonData: String, recordingId: String): List<ShowTrack> {
        // This is a stub implementation for Phase 3
        // Will parse cached tracks JSON format
        return emptyList()
    }

    private fun parseReviewsJson(jsonData: String): List<ShowReview> {
        // This is a stub implementation for Phase 3
        // Will parse Archive.org reviews API format
        return emptyList()
    }

    private fun serializeTracksToJson(tracks: List<ShowTrack>): String {
        // This is a stub implementation for Phase 3
        // Will serialize tracks to JSON for caching
        return "[]"
    }
}

/**
 * Domain models for Archive.org data.
 *
 * These represent the business domain, not raw API responses.
 * Universal services parse API data into these clean domain models.
 */

data class ShowMetadata(
    val identifier: String,
    val title: String,
    val creator: String,
    val date: String,
    val venue: String,
    val source: String,
    val description: String
)

data class ShowTrack(
    val recordingId: String,
    val fileName: String,
    val title: String,
    val trackNumber: Int,
    val durationSeconds: Int,
    val format: String,
    val size: Long,
    val downloadUrl: String
)

data class ShowReview(
    val reviewer: String,
    val title: String,
    val reviewText: String,
    val stars: Int,
    val reviewDate: String
)