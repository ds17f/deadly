package com.grateful.deadly.services.archive

import com.grateful.deadly.services.archive.platform.NetworkClient
import com.grateful.deadly.services.archive.platform.CacheManager
import com.grateful.deadly.services.archive.platform.CacheType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Universal ArchiveService implementing Archive.org business logic using V2's proven patterns.
 *
 * This implementation directly adopts V2's battle-tested approaches:
 * - Conservative data handling (keep strings, don't parse)
 * - Simple audio file detection by extension
 * - Flexible serialization for Archive.org's inconsistent API
 * - V2's exact domain models and mapping logic
 */
class ArchiveService(
    private val networkClient: NetworkClient,
    private val cacheManager: CacheManager
) {

    companion object {
        // Archive.org API endpoints (same as V2)
        private const val ARCHIVE_METADATA_BASE = "https://archive.org/metadata"

        // V2's proven audio file extensions
        private val AUDIO_EXTENSIONS = setOf("mp3", "flac", "ogg", "m4a", "wav", "aac", "wma")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Get recording metadata using V2's exact approach.
     */
    suspend fun getRecordingMetadata(recordingId: String): Result<RecordingMetadata> {
        return try {
            // Check cache first
            val cacheKey = recordingId
            val cached = cacheManager.get(cacheKey, CacheType.METADATA)

            if (cached != null && !cacheManager.isExpired(cacheKey, CacheType.METADATA)) {
                // V2 pattern: cache contains final domain model, not raw API response
                val metadata = json.decodeFromString<RecordingMetadata>(cached)
                return Result.success(metadata)
            }

            // Fetch from Archive.org
            val url = "$ARCHIVE_METADATA_BASE/$recordingId"
            val networkResult = networkClient.getJson(url)

            networkResult.fold(
                onSuccess = { jsonData ->
                    // Parse API response to domain model
                    val response = json.decodeFromString<ArchiveMetadataResponse>(jsonData)
                    val metadata = mapToRecordingMetadata(response)

                    // Cache the final domain model (V2 pattern)
                    val metadataJson = json.encodeToString(metadata)
                    cacheManager.put(cacheKey, CacheType.METADATA, metadataJson)

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
     * Get recording tracks using V2's exact approach.
     */
    suspend fun getRecordingTracks(recordingId: String): Result<List<Track>> {
        return try {
            // Check cache first
            val cacheKey = recordingId
            val cached = cacheManager.get(cacheKey, CacheType.TRACKS)

            if (cached != null && !cacheManager.isExpired(cacheKey, CacheType.TRACKS)) {
                // V2 pattern: cache contains final domain model
                val tracks = json.decodeFromString<List<Track>>(cached)
                return Result.success(tracks)
            }

            // Fetch from Archive.org
            val url = "$ARCHIVE_METADATA_BASE/$recordingId"
            val networkResult = networkClient.getJson(url)

            networkResult.fold(
                onSuccess = { jsonData ->
                    // Parse API response to domain models
                    val response = json.decodeFromString<ArchiveMetadataResponse>(jsonData)
                    val tracks = mapToTracks(response)

                    // Cache the final domain models (V2 pattern)
                    val tracksJson = json.encodeToString(tracks)
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
     * Get recording reviews using V2's exact approach.
     */
    suspend fun getRecordingReviews(recordingId: String): Result<List<Review>> {
        return try {
            // Check cache first
            val cacheKey = recordingId
            val cached = cacheManager.get(cacheKey, CacheType.REVIEWS)

            if (cached != null && !cacheManager.isExpired(cacheKey, CacheType.REVIEWS)) {
                // V2 pattern: cache contains final domain models
                val reviews = json.decodeFromString<List<Review>>(cached)
                return Result.success(reviews)
            }

            // Fetch from Archive.org
            val url = "$ARCHIVE_METADATA_BASE/$recordingId"
            val networkResult = networkClient.getJson(url)

            networkResult.fold(
                onSuccess = { jsonData ->
                    // Parse API response to domain models
                    val response = json.decodeFromString<ArchiveMetadataResponse>(jsonData)
                    val reviews = mapToReviews(response)

                    // Cache the final domain models (V2 pattern)
                    val reviewsJson = json.encodeToString(reviews)
                    cacheManager.put(cacheKey, CacheType.REVIEWS, reviewsJson)

                    Result.success(reviews)
                },
                onFailure = { error ->
                    // Reviews are optional - return empty list if not found (V2 pattern)
                    Result.success(emptyList())
                }
            )
        } catch (e: Exception) {
            // Reviews are optional - return empty list on error (V2 pattern)
            Result.success(emptyList())
        }
    }

    /**
     * Clear cache for a specific recording (all types).
     */
    suspend fun clearCache(recordingId: String): Result<Unit> {
        return try {
            cacheManager.clear(recordingId, null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear all Archive.org cache.
     */
    suspend fun clearAllCache(): Result<Unit> {
        return try {
            cacheManager.clear(null, null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // V2's exact mapping logic

    /**
     * Convert Archive API response to RecordingMetadata domain model (V2's exact logic).
     */
    private fun mapToRecordingMetadata(response: ArchiveMetadataResponse): RecordingMetadata {
        val metadata = response.metadata
        val audioTracks = response.files.filter { isAudioFile(it.name) }

        return RecordingMetadata(
            identifier = metadata?.identifier ?: "",
            title = metadata?.title ?: "",
            date = metadata?.date,
            venue = metadata?.venue,
            description = metadata?.description,
            setlist = metadata?.setlist,
            source = metadata?.source,
            taper = metadata?.taper,
            transferer = metadata?.transferer,
            lineage = metadata?.lineage,
            totalTracks = audioTracks.size,
            totalReviews = response.reviews?.size ?: 0
        )
    }

    /**
     * Convert Archive API response to list of Track domain models.
     *
     * Sorts by filename (not trackNumber) because:
     * - Archive.org track metadata is often missing or incorrect
     * - Filenames encode disc/track info (e.g., d1t01, d2t15)
     * - Lexicographic filename sort respects natural playback order
     */
    private fun mapToTracks(response: ArchiveMetadataResponse): List<Track> {
        return response.files
            .filter { isAudioFile(it.name) }
            .mapIndexed { index, file ->
                Track(
                    name = file.name,
                    title = file.title ?: extractTitleFromFilename(file.name),
                    trackNumber = file.track?.toIntOrNull() ?: (index + 1),
                    duration = file.length,
                    format = file.format,
                    size = file.size,
                    bitrate = file.bitrate,
                    sampleRate = file.sampleRate,
                    isAudio = true
                )
            }
            .sortedBy { it.name } // Sort by filename for reliable ordering
    }

    /**
     * Convert Archive API response to list of Review domain models (V2's exact logic).
     */
    private fun mapToReviews(response: ArchiveMetadataResponse): List<Review> {
        return response.reviews?.map { review ->
            Review(
                reviewer = review.reviewer,
                title = review.title,
                body = review.body,
                rating = review.stars,
                reviewDate = review.reviewDate
            )
        } ?: emptyList()
    }

    /**
     * Check if a file is an audio file based on extension (V2's exact logic).
     */
    private fun isAudioFile(filename: String): Boolean {
        val extension = filename.lowercase().substringAfterLast(".", "")
        return extension in AUDIO_EXTENSIONS
    }

    /**
     * Extract song title from filename for tracks without title metadata (V2's exact logic).
     */
    private fun extractTitleFromFilename(filename: String): String {
        return filename
            .substringBeforeLast(".")
            .removePrefix("gd")
            .removePrefix("grateful_dead")
            .replace(Regex("^\\d{4}-\\d{2}-\\d{2}"), "") // Remove date prefix
            .replace(Regex("^d\\dt\\d+\\."), "") // Remove disc/track prefix
            .replace("_", " ")
            .trim()
            .takeIf { it.isNotBlank() } ?: filename
    }
}

// V2's exact Archive.org API response models with flexible serialization

@Serializable
data class ArchiveMetadataResponse(
    @SerialName("files")
    val files: List<ArchiveFile> = emptyList(),

    @SerialName("metadata")
    val metadata: ArchiveMetadata? = null,

    @SerialName("reviews")
    val reviews: List<ArchiveReview>? = null
)

@Serializable
data class ArchiveFile(
    @SerialName("name")
    val name: String,

    @SerialName("format")
    val format: String,

    @SerialName("size")
    val size: String? = null,

    @SerialName("length")
    val length: String? = null,

    @SerialName("title")
    val title: String? = null,

    @SerialName("track")
    val track: String? = null,

    @SerialName("bitrate")
    val bitrate: String? = null,

    @SerialName("sample_rate")
    val sampleRate: String? = null
)

@Serializable
data class ArchiveMetadata(
    @SerialName("identifier")
    val identifier: String,

    @SerialName("title")
    val title: String,

    @SerialName("date")
    val date: String? = null,

    @SerialName("venue")
    @Serializable(with = FlexibleStringSerializer::class)
    val venue: String? = null,

    @SerialName("creator")
    @Serializable(with = FlexibleStringSerializer::class)
    val creator: String? = null,

    @SerialName("description")
    @Serializable(with = FlexibleStringListSerializer::class)
    val description: String? = null,

    @SerialName("setlist")
    @Serializable(with = FlexibleStringListSerializer::class)
    val setlist: String? = null,

    @SerialName("source")
    @Serializable(with = FlexibleStringListSerializer::class)
    val source: String? = null,

    @SerialName("taper")
    @Serializable(with = FlexibleStringSerializer::class)
    val taper: String? = null,

    @SerialName("transferer")
    @Serializable(with = FlexibleStringSerializer::class)
    val transferer: String? = null,

    @SerialName("lineage")
    @Serializable(with = FlexibleStringListSerializer::class)
    val lineage: String? = null
)

@Serializable
data class ArchiveReview(
    @SerialName("reviewtitle")
    val title: String? = null,

    @SerialName("reviewbody")
    val body: String? = null,

    @SerialName("reviewer")
    val reviewer: String? = null,

    @SerialName("reviewdate")
    val reviewDate: String? = null,

    @SerialName("stars")
    val stars: Int? = null
)

// V2's exact domain models

@Serializable
data class Track(
    val name: String,
    val title: String? = null,
    val trackNumber: Int? = null,
    val duration: String? = null,
    val format: String,
    val size: String? = null,
    val bitrate: String? = null,
    val sampleRate: String? = null,
    val isAudio: Boolean = true
)

@Serializable
data class Review(
    val reviewer: String?,
    val title: String? = null,
    val body: String? = null,
    val rating: Int? = null,
    val reviewDate: String? = null
)

@Serializable
data class RecordingMetadata(
    val identifier: String,
    val title: String,
    val date: String? = null,
    val venue: String? = null,
    val description: String? = null,
    val setlist: String? = null,
    val source: String? = null,
    val taper: String? = null,
    val transferer: String? = null,
    val lineage: String? = null,
    val totalTracks: Int = 0,
    val totalReviews: Int = 0
)

// V2's flexible serializers for handling Archive.org's inconsistent API responses

/**
 * Custom serializer that can handle fields that may be either String or Array<String>
 * Returns the first string if it's an array, or the string itself if it's a string
 */
object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        encoder.encodeString(value ?: "")
    }

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This serializer only works with JSON")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    else -> element.content // Handle numbers as strings
                }
            }
            is JsonArray -> {
                // If it's an array, take the first non-empty element
                element.firstOrNull { it is JsonPrimitive && !it.content.isBlank() }
                    ?.let { (it as JsonPrimitive).content }
            }
            is JsonNull -> null
            else -> null
        }
    }
}

/**
 * Custom serializer for fields that can be String or Array<String>
 * Returns all strings joined with newlines
 */
object FlexibleStringListSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleStringList", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        encoder.encodeString(value ?: "")
    }

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This serializer only works with JSON")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                if (element.isString) element.content else null
            }
            is JsonArray -> {
                element.filterIsInstance<JsonPrimitive>()
                    .map { it.content }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                    .takeIf { it.isNotBlank() }
            }
            is JsonNull -> null
            else -> null
        }
    }
}