package com.grateful.deadly.services.data

import com.grateful.deadly.database.Database
import com.grateful.deadly.core.util.Logger
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okio.FileSystem
import okio.Path

/**
 * Data import service following V2 DatabaseManager patterns.
 * Handles download → extract → parse → import flow with progress tracking.
 */
class DataImportService(
    private val database: Database,
    private val httpClient: HttpClient,
    private val settings: Settings,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {

    companion object {
        private const val TAG = "DataImportService"
        // TODO: Replace with actual data source URL
        private const val REMOTE_DATA_URL = "https://github.com/your-org/deadly-data/archive/main.zip"
        private const val DATA_IMPORTED_KEY = "has_imported_data"
        private const val IMPORT_TIMESTAMP_KEY = "import_timestamp"
    }

    private val _progress = MutableStateFlow<ImportProgress>(ImportProgress.Idle)
    val progress: Flow<ImportProgress> = _progress.asStateFlow()

    /**
     * Initialize data if needed - called at app startup (V2 pattern)
     */
    suspend fun initializeDataIfNeeded(): ImportResult {
        return try {
            Logger.d(TAG, "Checking if data initialization is needed...")

            // SQLDelight automatically creates tables on first database access
            val showCount = database.showQueries.getShowCount().executeAsOne()
            Logger.d(TAG, "Current show count: $showCount")

            if (showCount > 0) {
                Logger.d(TAG, "Data already exists, skipping import")
                return ImportResult.AlreadyExists(showCount.toInt())
            }

            Logger.d(TAG, "No data found, starting import process...")
            // No data - download and import
            downloadAndImportData()

        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize data", e)
            ImportResult.Error("Failed to initialize data: ${e.message}")
        }
    }

    /**
     * Force refresh data - called from settings screen
     */
    suspend fun forceRefreshData(): ImportResult {
        return try {
            Logger.d(TAG, "Force refreshing data...")

            // Clear existing data
            clearAllData()

            // Download fresh data
            downloadAndImportData()

        } catch (e: Exception) {
            Logger.e(TAG, "Failed to refresh data", e)
            ImportResult.Error("Failed to refresh data: ${e.message}")
        }
    }

    /**
     * Clear all data - called from settings screen
     */
    suspend fun clearAllData(): ImportResult {
        return try {
            Logger.d(TAG, "Clearing all data...")
            _progress.value = ImportProgress.Clearing

            // Clear database
            database.showQueries.deleteAllShows()

            val remainingCount = database.showQueries.getShowCount().executeAsOne()
            Logger.d(TAG, "Remaining shows after clear: $remainingCount")

            // Clear settings
            settings.remove(DATA_IMPORTED_KEY)
            settings.remove(IMPORT_TIMESTAMP_KEY)

            _progress.value = ImportProgress.Idle
            Logger.d(TAG, "Data cleared successfully")
            ImportResult.Cleared

        } catch (e: Exception) {
            Logger.e(TAG, "Failed to clear data", e)
            _progress.value = ImportProgress.Idle
            ImportResult.Error("Failed to clear data: ${e.message}")
        }
    }

    /**
     * Import mock data for testing (temporary solution)
     */
    suspend fun importMockData(): ImportResult {
        return try {
            Logger.d(TAG, "Importing mock data for testing...")
            _progress.value = ImportProgress.Parsing(0, 0)

            // Get mock shows from our existing SearchServiceStub
            val mockShows = getMockShowData()

            _progress.value = ImportProgress.Importing(0, mockShows.size)
            importShowsToDatabase(mockShows)

            // Mark as completed
            settings.putBoolean(DATA_IMPORTED_KEY, true)
            settings.putLong(IMPORT_TIMESTAMP_KEY, Clock.System.now().toEpochMilliseconds())

            _progress.value = ImportProgress.Idle

            val finalCount = database.showQueries.getShowCount().executeAsOne()
            Logger.d(TAG, "Mock data import completed: $finalCount shows")
            ImportResult.Success(finalCount.toInt())

        } catch (e: Exception) {
            Logger.e(TAG, "Failed to import mock data", e)
            _progress.value = ImportProgress.Idle
            ImportResult.Error("Mock import failed: ${e.message}")
        }
    }

    private suspend fun downloadAndImportData(): ImportResult = withContext(Dispatchers.IO) {
        try {
            // For now, use mock data until we have real data source
            // TODO: Implement real download when data source is available
            Logger.d(TAG, "Using mock data (real download not yet implemented)")
            return@withContext importMockData()

            /*
            // Real implementation for future:
            // 1. Download ZIP file
            _progress.value = ImportProgress.Downloading
            val zipBytes = httpClient.get(REMOTE_DATA_URL).body<ByteArray>()

            // 2. Extract ZIP to memory (using Okio)
            _progress.value = ImportProgress.Extracting
            val showFiles = extractShowFilesFromZip(zipBytes)

            // 3. Parse JSON files
            _progress.value = ImportProgress.Parsing(0, showFiles.size)
            val shows = parseShowFiles(showFiles)

            // 4. Import to database
            _progress.value = ImportProgress.Importing(0, shows.size)
            importShowsToDatabase(shows)

            // 5. Mark as completed
            settings.putBoolean(DATA_IMPORTED_KEY, true)
            settings.putLong(IMPORT_TIMESTAMP_KEY, Clock.System.now().toEpochMilliseconds())

            _progress.value = ImportProgress.Idle

            val finalCount = database.showQueries.getShowCount().executeAsOne()
            ImportResult.Success(finalCount.toInt())
            */

        } catch (e: Exception) {
            Logger.e(TAG, "Download and import failed", e)
            _progress.value = ImportProgress.Idle
            ImportResult.Error("Import failed: ${e.message}")
        }
    }

    private fun getMockShowData(): List<ShowImportData> {
        // Extract mock data from existing SearchServiceStub patterns
        return listOf(
            ShowImportData(
                showId = "gd1977-05-08",
                band = "Grateful Dead",
                venue = "Barton Hall, Cornell University",
                city = "Ithaca",
                state = "NY",
                country = "USA",
                date = "1977-05-08",
                locationRaw = "Ithaca, NY",
                recordingCount = 1,
                bestRecording = "gd1977-05-08.sbd.hicks.4982.sbeok.shnf",
                avgRating = 4.8
            ),
            ShowImportData(
                showId = "gd1972-05-03",
                band = "Grateful Dead",
                venue = "Olympia Theatre",
                city = "Paris",
                state = null,
                country = "France",
                date = "1972-05-03",
                locationRaw = "Paris, France",
                recordingCount = 1,
                bestRecording = "gd72-05-03.sbd.unknown.30057.sbeok.shnf",
                avgRating = 4.6
            ),
            ShowImportData(
                showId = "gd1969-08-16",
                band = "Grateful Dead",
                venue = "Woodstock Music & Art Fair",
                city = "Bethel",
                state = "NY",
                country = "USA",
                date = "1969-08-16",
                locationRaw = "Bethel, NY",
                recordingCount = 1,
                bestRecording = "gd69-08-16.aud.vernon.16793.sbeok.shnf",
                avgRating = 4.2
            ),
            ShowImportData(
                showId = "gd1973-06-10",
                band = "Grateful Dead",
                venue = "RFK Stadium",
                city = "Washington",
                state = "DC",
                country = "USA",
                date = "1973-06-10",
                locationRaw = "Washington, DC",
                recordingCount = 1,
                bestRecording = "dp12",
                avgRating = 4.7
            ),
            ShowImportData(
                showId = "gd1995-07-09",
                band = "Grateful Dead",
                venue = "Soldier Field",
                city = "Chicago",
                state = "IL",
                country = "USA",
                date = "1995-07-09",
                locationRaw = "Chicago, IL",
                recordingCount = 1,
                bestRecording = "gd95-07-09.sbd.miller.97483.flac1644",
                avgRating = 4.1
            ),
            ShowImportData(
                showId = "gd1970-02-14",
                band = "Grateful Dead",
                venue = "Fillmore East",
                city = "New York",
                state = "NY",
                country = "USA",
                date = "1970-02-14",
                locationRaw = "New York, NY",
                recordingCount = 2,
                bestRecording = "gd70-02-14.sbd.miller.97484.flac1644",
                avgRating = 4.5
            ),
            ShowImportData(
                showId = "gd1977-10-29",
                band = "Grateful Dead",
                venue = "Winterland Arena",
                city = "San Francisco",
                state = "CA",
                country = "USA",
                date = "1977-10-29",
                locationRaw = "San Francisco, CA",
                recordingCount = 3,
                bestRecording = "gd77-10-29.sbd.cotsman.6823.sbeok.shnf",
                avgRating = 4.3
            ),
            ShowImportData(
                showId = "gd1971-11-07",
                band = "Grateful Dead",
                venue = "Capitol Theatre",
                city = "Port Chester",
                state = "NY",
                country = "USA",
                date = "1971-11-07",
                locationRaw = "Port Chester, NY",
                recordingCount = 1,
                bestRecording = "gd71-11-07.sbd.miller.97485.flac1644",
                avgRating = 4.4
            ),
            ShowImportData(
                showId = "gd1989-07-17",
                band = "Grateful Dead",
                venue = "Alpine Valley Music Theatre",
                city = "East Troy",
                state = "WI",
                country = "USA",
                date = "1989-07-17",
                locationRaw = "East Troy, WI",
                recordingCount = 2,
                bestRecording = "gd89-07-17.sbd.miller.97486.flac1644",
                avgRating = 4.0
            ),
            ShowImportData(
                showId = "gd1994-10-19",
                band = "Grateful Dead",
                venue = "Madison Square Garden",
                city = "New York",
                state = "NY",
                country = "USA",
                date = "1994-10-19",
                locationRaw = "New York, NY",
                recordingCount = 1,
                bestRecording = "gd94-10-19.sbd.miller.97487.flac1644",
                avgRating = 3.9
            )
        )
    }

    private suspend fun importShowsToDatabase(shows: List<ShowImportData>) {
        database.transaction {
            shows.forEachIndexed { index, show ->
                _progress.value = ImportProgress.Importing(index + 1, shows.size)

                // Map import data to database format (following V2 patterns)
                database.showQueries.insertShow(
                    showId = show.showId,
                    date = show.date,
                    year = extractYear(show.date).toLong(),
                    month = extractMonth(show.date).toLong(),
                    yearMonth = "${extractYear(show.date)}-${extractMonth(show.date).toString().padStart(2, '0')}",
                    band = show.band,
                    url = show.url,
                    venueName = show.venue,
                    city = show.city,
                    state = show.state,
                    country = show.country ?: "USA",
                    locationRaw = show.locationRaw,
                    setlistStatus = show.setlistStatus,
                    setlistRaw = null, // TODO: Add setlist JSON when available
                    songList = null, // TODO: Extract comma-separated song list
                    lineupStatus = show.lineupStatus,
                    lineupRaw = null, // TODO: Add lineup JSON when available
                    memberList = null, // TODO: Extract comma-separated member list
                    recordingCount = show.recordingCount.toLong(),
                    bestRecordingId = show.bestRecording,
                    averageRating = show.avgRating,
                    totalReviews = 0L, // TODO: Add to import data when available
                    isInLibrary = 0L,
                    libraryAddedAt = null,
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                    updatedAt = Clock.System.now().toEpochMilliseconds()
                )

                Logger.d(TAG, "Imported show ${index + 1}/${shows.size}: ${show.showId}")
            }
        }
    }

    private fun extractYear(date: String): Int = date.split("-")[0].toInt()
    private fun extractMonth(date: String): Int = date.split("-")[1].toInt()

    // TODO: Implement when real data source is available
    private fun extractShowFilesFromZip(zipBytes: ByteArray): Map<String, String> {
        // Extract show JSON files from ZIP archive using Okio
        // Return map of filename -> JSON content
        return emptyMap()
    }

    private fun parseShowFiles(showFiles: Map<String, String>): List<ShowImportData> {
        val json = Json { ignoreUnknownKeys = true }
        val shows = mutableListOf<ShowImportData>()

        showFiles.forEach { (filename, content) ->
            try {
                val show = json.decodeFromString<ShowImportData>(content)
                shows.add(show)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to parse $filename", e)
            }
        }

        return shows
    }
}

@Serializable
data class ShowImportData(
    @SerialName("show_id") val showId: String,
    val band: String,
    val venue: String,
    @SerialName("location_raw") val locationRaw: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = "USA",
    val date: String,
    val url: String? = null,
    @SerialName("setlist_status") val setlistStatus: String? = null,
    @SerialName("lineup_status") val lineupStatus: String? = null,
    @SerialName("recording_count") val recordingCount: Int = 0,
    @SerialName("best_recording") val bestRecording: String? = null,
    @SerialName("avg_rating") val avgRating: Double = 0.0
)

sealed class ImportResult {
    data class Success(val showCount: Int) : ImportResult()
    data class AlreadyExists(val showCount: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
    object Cleared : ImportResult()
}

sealed class ImportProgress {
    object Idle : ImportProgress()
    object Downloading : ImportProgress()
    object Extracting : ImportProgress()
    object Clearing : ImportProgress()
    data class Parsing(val current: Int, val total: Int) : ImportProgress()
    data class Importing(val current: Int, val total: Int) : ImportProgress()
}