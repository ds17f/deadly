package com.grateful.deadly.services.data

import com.grateful.deadly.core.util.Logger
import com.grateful.deadly.database.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * JSON import service for parsing and importing show data.
 * Handles parsing individual show JSON files and batch importing to database.
 */
class JsonImportService(
    private val database: Database,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {

    companion object {
        private const val TAG = "JsonImportService"
    }

    data class ImportProgress(
        val currentFile: String = "",
        val importedFiles: Int = 0,
        val totalFiles: Int = 0,
        val isCompleted: Boolean = false,
        val error: String? = null
    ) {
        val progressPercent: Float get() = if (totalFiles > 0) importedFiles.toFloat() / totalFiles else 0f
    }

    sealed class ImportResult {
        data class Success(val importedCount: Int) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    /**
     * Raw show data structure from JSON files
     */
    @Serializable
    data class ShowJsonData(
        @SerialName("identifier") val identifier: String,
        @SerialName("date") val date: String,
        @SerialName("venue") val venue: String? = null,
        @SerialName("coverage") val coverage: String? = null,
        @SerialName("lineage") val lineage: String? = null,
        @SerialName("taper") val taper: String? = null,
        @SerialName("transferer") val transferer: String? = null,
        @SerialName("source") val source: String? = null,
        @SerialName("avg_rating") val avgRating: Double? = null,
        @SerialName("reviews") val reviews: Int? = null,
        @SerialName("downloads") val downloads: Int? = null
    )

    /**
     * Import JSON files from extraction directory
     */
    suspend fun importJsonFiles(
        extractedFiles: List<String>,
        progressCallback: ((ImportProgress) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            Logger.d(TAG, "Starting import of ${extractedFiles.size} JSON files")

            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            var importedCount = 0
            val totalFiles = extractedFiles.size

            // Initial progress
            progressCallback?.invoke(
                ImportProgress(
                    currentFile = "",
                    importedFiles = 0,
                    totalFiles = totalFiles,
                    isCompleted = false
                )
            )

            // Process files in batches for better performance
            database.transaction {
                extractedFiles.forEachIndexed { index, filePath ->
                    try {
                        val fileName = filePath.substringAfterLast('/')

                        // Progress update
                        progressCallback?.invoke(
                            ImportProgress(
                                currentFile = fileName,
                                importedFiles = index,
                                totalFiles = totalFiles,
                                isCompleted = false
                            )
                        )

                        // Read and parse JSON file
                        val jsonContent = fileSystem.read(filePath.toPath()) {
                            readUtf8()
                        }

                        val showData = json.decodeFromString<ShowJsonData>(jsonContent)

                        // Convert to database format and insert
                        importShowToDatabase(showData)
                        importedCount++

                        Logger.d(TAG, "Imported show ${index + 1}/$totalFiles: ${showData.identifier}")

                    } catch (e: Exception) {
                        Logger.e(TAG, "Failed to import file: $filePath", e)
                        // Continue with other files even if one fails
                    }
                }
            }

            // Final progress
            progressCallback?.invoke(
                ImportProgress(
                    currentFile = "Import complete",
                    importedFiles = importedCount,
                    totalFiles = totalFiles,
                    isCompleted = true
                )
            )

            Logger.d(TAG, "Import completed: $importedCount/$totalFiles shows imported")

            ImportResult.Success(importedCount)

        } catch (e: Exception) {
            Logger.e(TAG, "Import failed", e)

            progressCallback?.invoke(
                ImportProgress(
                    error = e.message ?: "Import failed"
                )
            )

            ImportResult.Error("Import failed: ${e.message}")
        }
    }

    /**
     * Convert JSON show data to database format and insert
     */
    private fun importShowToDatabase(showData: ShowJsonData) {
        // Parse show details from identifier and date
        val showId = showData.identifier
        val date = showData.date
        val year = extractYear(date)
        val month = extractMonth(date)
        val yearMonth = "$year-${month.toString().padStart(2, '0')}"

        // Extract band name from identifier (e.g., "gd1977-05-08" -> "Grateful Dead")
        val band = when {
            showId.startsWith("gd") -> "Grateful Dead"
            showId.startsWith("jgb") -> "Jerry Garcia Band"
            showId.startsWith("ratdog") -> "RatDog"
            else -> "Grateful Dead" // Default
        }

        // Parse venue and location from venue field
        val venueName = showData.venue ?: "Unknown Venue"
        val (city, state) = parseLocation(venueName)

        database.showQueries.insertShow(
            showId = showId,
            date = date,
            year = year.toLong(),
            month = month.toLong(),
            yearMonth = yearMonth,
            band = band,
            url = null, // TODO: Extract from metadata if available
            venueName = venueName,
            city = city,
            state = state,
            country = "USA", // Default, could be enhanced with location parsing
            locationRaw = venueName,
            setlistStatus = null,
            setlistRaw = null,
            songList = null,
            lineupStatus = null,
            lineupRaw = null,
            memberList = null,
            recordingCount = (showData.downloads ?: 0).toLong(),
            bestRecordingId = null, // TODO: Extract from metadata if available
            averageRating = showData.avgRating,
            totalReviews = (showData.reviews ?: 0).toLong(),
            isInLibrary = 0L,
            libraryAddedAt = null,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
    }

    /**
     * Extract year from date string (e.g., "1977-05-08" -> 1977)
     */
    private fun extractYear(date: String): Int {
        return try {
            date.split("-")[0].toInt()
        } catch (e: Exception) {
            1970 // Default year if parsing fails
        }
    }

    /**
     * Extract month from date string (e.g., "1977-05-08" -> 5)
     */
    private fun extractMonth(date: String): Int {
        return try {
            date.split("-")[1].toInt()
        } catch (e: Exception) {
            1 // Default month if parsing fails
        }
    }

    /**
     * Parse city and state from venue string
     * Basic implementation - could be enhanced with more sophisticated parsing
     */
    private fun parseLocation(venue: String): Pair<String?, String?> {
        return try {
            // Look for common patterns like "Venue Name, City, ST"
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
                else -> Pair(null, null)
            }
        } catch (e: Exception) {
            Pair(null, null)
        }
    }
}