package com.grateful.deadly.services.data

import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.services.data.models.*
import com.grateful.deadly.services.data.platform.ShowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * Universal data import service using the Universal Service + Platform Tool pattern.
 *
 * This service handles all universal concerns:
 * - JSON parsing and validation
 * - Field mapping from JSON to database entities
 * - Business logic and data transformation
 * - Progress tracking and error handling
 * - Batch processing coordination
 *
 * Platform-specific database operations are delegated to the ShowRepository platform tool.
 */
class DataImportService(
    private val showRepository: ShowRepository,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) {

    companion object {
        private const val TAG = "DataImportService"
        private const val BATCH_SIZE = 50 // Process shows in batches for performance
    }

    /**
     * Import show data from extracted JSON files.
     *
     * This method demonstrates the Universal Service pattern:
     * - Universal JSON parsing, mapping, and business logic
     * - Platform-specific database operations delegated to ShowRepository
     * - Standardized progress tracking and error handling
     */
    suspend fun importShowData(
        extractedFiles: List<ExtractedFile>,
        progressCallback: ((ImportProgress) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            Logger.d(TAG, "Starting import of ${extractedFiles.size} files")

            // Universal file filtering - only process show JSON files
            val showFiles = filterShowFiles(extractedFiles)
            Logger.d(TAG, "Found ${showFiles.size} show files to import")

            if (showFiles.isEmpty()) {
                return@withContext ImportResult.Error("No show files found to import")
            }

            // Universal progress tracking
            progressCallback?.invoke(ImportProgress.Started)

            var importedCount = 0
            val totalFiles = showFiles.size

            // Process files in batches for better performance
            showFiles.chunked(BATCH_SIZE).forEachIndexed { batchIndex, batch ->
                val showEntities = mutableListOf<ShowEntity>()

                // Universal JSON parsing and mapping for each file in batch
                batch.forEachIndexed { fileIndex, file ->
                    val globalIndex = batchIndex * BATCH_SIZE + fileIndex

                    try {
                        progressCallback?.invoke(
                            ImportProgress.Processing(
                                current = globalIndex,
                                total = totalFiles,
                                currentFile = file.relativePath
                            )
                        )

                        // Universal JSON parsing
                        val jsonContent = readFileContent(file.path)
                        val showData = json.decodeFromString<ShowJsonSchema>(jsonContent)

                        // Universal field mapping and business logic
                        val showEntity = mapJsonToEntity(showData)

                        // Universal validation
                        if (validateShowEntity(showEntity)) {
                            showEntities.add(showEntity)
                        } else {
                            Logger.w(TAG, "Invalid show entity skipped: ${showEntity.showId}")
                        }

                    } catch (e: Exception) {
                        Logger.e(TAG, "Failed to process file: ${file.relativePath}", e)
                        // Continue with other files even if one fails
                    }
                }

                // Delegate batch database operations to platform tool
                if (showEntities.isNotEmpty()) {
                    showRepository.insertShows(showEntities)
                    importedCount += showEntities.size
                    Logger.d(TAG, "Imported batch ${batchIndex + 1}: ${showEntities.size} shows")
                }
            }

            // Universal completion tracking
            progressCallback?.invoke(ImportProgress.Completed)

            Logger.d(TAG, "Import completed: $importedCount/$totalFiles shows imported")
            ImportResult.Success(importedCount)

        } catch (e: Exception) {
            Logger.e(TAG, "Import failed", e)
            ImportResult.Error("Import failed: ${e.message}")
        }
    }

    /**
     * Get the current show count from the database.
     */
    suspend fun getShowCount(): Long {
        return showRepository.getShowCount()
    }

    /**
     * Clear all show data from the database.
     */
    suspend fun clearAllShows(): Boolean {
        return try {
            showRepository.deleteAllShows()
            Logger.d(TAG, "All shows cleared from database")
            true
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to clear shows", e)
            false
        }
    }

    /**
     * Delete the entire database file to resolve schema mismatches.
     */
    suspend fun deleteDatabaseFile(): Boolean {
        return try {
            val success = showRepository.deleteDatabaseFile()
            if (success) {
                Logger.d(TAG, "Database file deleted and recreated")
            } else {
                Logger.w(TAG, "Failed to delete database file")
            }
            success
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to delete database file", e)
            false
        }
    }

    /**
     * Universal file filtering - identify show JSON files.
     */
    private fun filterShowFiles(extractedFiles: List<ExtractedFile>): List<ExtractedFile> {
        return extractedFiles.filter { file ->
            !file.isDirectory &&
            file.relativePath.contains("shows/") &&
            file.relativePath.endsWith(".json", ignoreCase = true)
        }
    }

    /**
     * Universal file reading using cross-platform FileSystem.
     */
    private suspend fun readFileContent(filePath: String): String {
        return fileSystem.read(filePath.toPath()) {
            readUtf8()
        }
    }

    /**
     * Universal field mapping from JSON to database entity.
     * This is the core business logic that gets written once and works everywhere.
     */
    private fun mapJsonToEntity(json: ShowJsonSchema): ShowEntity {
        val showId = json.showId
        val date = json.date
        val year = extractYear(date)
        val month = extractMonth(date)
        val yearMonth = formatYearMonth(year, month)

        // Use band from JSON or determine from showId
        val band = json.band ?: determineBand(showId)

        // Process venue and location data
        val venueName = json.venue ?: "Unknown Venue"
        val city = json.city
        val state = json.state

        // Process setlist data
        val (setlistStatus, setlistRaw, songList) = processSetlistData(json.setlist, json.setlistStatus)

        // Process lineup data
        val (lineupStatus, lineupRaw, memberList) = processLineupData(json.lineup, json.lineupStatus)

        // Process recording data
        val recordingCount = json.recordings?.size ?: 0
        val recordingsRaw = if (!json.recordings.isNullOrEmpty()) {
            json.recordings.joinToString(",", "[", "]") { "\"$it\"" }
        } else {
            null
        }

        val now = Clock.System.now().toEpochMilliseconds()

        return ShowEntity(
            showId = showId,
            date = date,
            year = year,
            month = month,
            yearMonth = yearMonth,
            band = band,
            url = json.url,
            venueName = venueName,
            city = city,
            state = state,
            country = json.country ?: "USA",
            locationRaw = json.locationRaw,
            setlistStatus = setlistStatus,
            setlistRaw = setlistRaw,
            songList = songList,
            lineupStatus = lineupStatus,
            lineupRaw = lineupRaw,
            memberList = memberList,
            showSequence = 1, // Default to 1, will be updated if needed
            recordingsRaw = recordingsRaw,
            recordingCount = recordingCount,
            bestRecordingId = json.recordings?.firstOrNull(),
            averageRating = json.averageRating,
            totalReviews = json.totalReviews ?: 0,
            isInLibrary = false,
            libraryAddedAt = null,
            createdAt = now,
            updatedAt = now
        )
    }

    /**
     * Universal business logic helpers - written once, work everywhere.
     */
    private fun extractYear(date: String): Int {
        return try {
            date.split("-")[0].toInt()
        } catch (e: Exception) {
            1970 // Default year if parsing fails
        }
    }

    private fun extractMonth(date: String): Int {
        return try {
            date.split("-")[1].toInt()
        } catch (e: Exception) {
            1 // Default month if parsing fails
        }
    }

    private fun formatYearMonth(year: Int, month: Int): String {
        return "$year-${month.toString().padStart(2, '0')}"
    }

    private fun determineBand(identifier: String): String {
        return when {
            identifier.startsWith("gd") -> "Grateful Dead"
            identifier.startsWith("jgb") -> "Jerry Garcia Band"
            identifier.startsWith("ratdog") -> "RatDog"
            else -> "Grateful Dead" // Default
        }
    }

    private fun parseLocationFromVenue(
        venue: String,
        cityFromJson: String?,
        stateFromJson: String?
    ): Pair<String?, String?> {
        // Use explicit city/state from JSON if available
        if (!cityFromJson.isNullOrBlank() && !stateFromJson.isNullOrBlank()) {
            return Pair(cityFromJson, stateFromJson)
        }

        // Otherwise, try to parse from venue string
        return try {
            val parts = venue.split(",").map { it.trim() }
            when {
                parts.size >= 3 -> {
                    val city = parts[parts.size - 2]
                    val state = parts[parts.size - 1]
                    Pair(city, state)
                }
                parts.size == 2 -> {
                    val city = parts[0]
                    val state = parts[1]
                    Pair(city, state)
                }
                else -> Pair(cityFromJson, stateFromJson)
            }
        } catch (e: Exception) {
            Pair(cityFromJson, stateFromJson)
        }
    }

    private fun processSetlistData(setlist: List<SetJsonSchema>?, setlistStatus: String?): Triple<String?, String?, String?> {
        if (setlist.isNullOrEmpty() || setlistStatus != "found") {
            return Triple(setlistStatus, null, null)
        }

        try {
            // Extract song names for search
            val songs = setlist.flatMap { set ->
                set.songs?.map { it.name } ?: emptyList()
            }
            val songList = songs.joinToString(",")

            // Convert setlist to simple JSON string for storage
            val setlistRaw = songs.joinToString(",", "[", "]") { "\"$it\"" }

            return Triple("found", setlistRaw, songList)
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to process setlist data", e)
            return Triple("error", null, null)
        }
    }

    private fun processLineupData(lineup: List<LineupMemberJsonSchema>?, lineupStatus: String?): Triple<String?, String?, String?> {
        if (lineup.isNullOrEmpty() || lineupStatus != "found") {
            return Triple(lineupStatus, null, null)
        }

        try {
            // Extract member names for search
            val memberNames = lineup.map { it.name }
            val memberList = memberNames.joinToString(",")

            // Convert lineup to simple JSON string for storage
            val lineupRaw = memberNames.joinToString(",", "[", "]") { "\"$it\"" }

            return Triple("found", lineupRaw, memberList)
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to process lineup data", e)
            return Triple("error", null, null)
        }
    }

    private fun validateShowEntity(show: ShowEntity): Boolean {
        return show.showId.isNotBlank() &&
               show.date.isNotBlank() &&
               show.year > 1960 &&
               show.year < 2030
    }
}