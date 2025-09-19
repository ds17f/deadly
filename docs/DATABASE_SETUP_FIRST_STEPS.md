# Database Setup First Steps

This document outlines the exact steps to implement real data import and persistence, replacing the SearchServiceStub with a SQLDelight-backed implementation that follows V2's proven patterns.

## 🎯 Overview

**Goal**: Get data import working with minimal tables, following V2's exact execution flow:
1. App startup → Check if data exists
2. If no data → Download ZIP from remote source → Extract → Parse JSON → Import to SQLDelight
3. If data exists → Skip download, use existing database
4. Settings screen → Manual controls for refresh/clear

**V2 Pattern Analysis**:
- V2 uses `DatabaseManager.initializeV2DataIfNeeded()` called at app startup
- Downloads from GitHub, extracts ZIP, processes JSON files
- Uses Room with comprehensive ShowEntity (denormalized for search performance)
- Settings screen has clear database management UI

## 📋 Implementation Steps

### Step 1: Create SQLDelight Schema (Minimal - Shows Only)

**File**: `composeApp/src/commonMain/sqldelight/com/grateful/deadly/database/Show.sq`

```sql
-- Based on V2 ShowEntity - exact same structure, proven performance
CREATE TABLE Show (
    showId TEXT PRIMARY KEY NOT NULL,

    -- Date components for flexible searching (V2 pattern)
    date TEXT NOT NULL,                    -- "1977-05-08"
    year INTEGER NOT NULL,                 -- 1977
    month INTEGER NOT NULL,                -- 5
    yearMonth TEXT NOT NULL,               -- "1977-05"

    -- Show metadata
    band TEXT NOT NULL,                    -- "Grateful Dead"
    url TEXT,                              -- Jerry Garcia URL

    -- Venue data (denormalized for fast search - V2 pattern)
    venueName TEXT NOT NULL,               -- "Barton Hall, Cornell University"
    city TEXT,                             -- "Ithaca"
    state TEXT,                            -- "NY"
    country TEXT NOT NULL DEFAULT 'USA',
    locationRaw TEXT,                      -- "Ithaca, NY"

    -- Setlist data (JSON strings - V2 pattern)
    setlistStatus TEXT,                    -- "found", "not_found"
    setlistRaw TEXT,                       -- JSON string for UI display
    songList TEXT,                         -- "Scarlet Begonias,Fire on the Mountain" (comma-separated for search)

    -- Lineup data (JSON strings - V2 pattern)
    lineupStatus TEXT,                     -- "found", "missing"
    lineupRaw TEXT,                        -- JSON string for UI display
    memberList TEXT,                       -- "Jerry Garcia,Bob Weir,Phil Lesh" (comma-separated for search)

    -- Recording data
    recordingCount INTEGER NOT NULL DEFAULT 0,
    bestRecordingId TEXT,
    averageRating REAL,
    totalReviews INTEGER NOT NULL DEFAULT 0,

    -- Library status (for future V2 features)
    isInLibrary INTEGER NOT NULL DEFAULT 0,
    libraryAddedAt INTEGER,

    -- Timestamps
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);

-- Indexes for fast search (based on V2 ShowEntity indexes)
CREATE INDEX idx_shows_date ON Show(date);
CREATE INDEX idx_shows_year ON Show(year);
CREATE INDEX idx_shows_yearMonth ON Show(yearMonth);
CREATE INDEX idx_shows_venueName ON Show(venueName);
CREATE INDEX idx_shows_city ON Show(city);
CREATE INDEX idx_shows_state ON Show(state);

-- Basic queries
selectAllShows:
SELECT * FROM Show ORDER BY date DESC;

selectShowById:
SELECT * FROM Show WHERE showId = ?;

getShowCount:
SELECT COUNT(*) FROM Show;

-- Search query (simplified version of V2 complex search)
searchShows:
SELECT * FROM Show
WHERE date LIKE '%' || ? || '%'
   OR venueName LIKE '%' || ? || '%'
   OR city LIKE '%' || ? || '%'
   OR state LIKE '%' || ? || '%'
   OR songList LIKE '%' || ? || '%'
   OR memberList LIKE '%' || ? || '%'
ORDER BY
  CASE
    WHEN venueName LIKE ? || '%' THEN 1
    WHEN city LIKE ? || '%' THEN 2
    WHEN date LIKE ? || '%' THEN 3
    ELSE 4
  END,
  averageRating DESC,
  date DESC;

-- Data management queries
insertShow:
INSERT OR REPLACE INTO Show (
    showId, date, year, month, yearMonth, band, url,
    venueName, city, state, country, locationRaw,
    setlistStatus, setlistRaw, songList,
    lineupStatus, lineupRaw, memberList,
    recordingCount, bestRecordingId, averageRating, totalReviews,
    isInLibrary, libraryAddedAt, createdAt, updatedAt
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

deleteAllShows:
DELETE FROM Show;
```

### Step 2: Create DataImportService

**File**: `composeApp/src/commonMain/kotlin/com/grateful/deadly/services/data/DataImportService.kt`

```kotlin
package com.grateful.deadly.services.data

import com.grateful.deadly.database.Database
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

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
        private const val REMOTE_DATA_URL = "https://github.com/your-org/deadly-data/archive/main.zip"
        private const val DATA_IMPORTED_KEY = "has_imported_data"
        private const val IMPORT_TIMESTAMP_KEY = "import_timestamp"
    }

    private val _progress = MutableStateFlow(ImportProgress.Idle)
    val progress: Flow<ImportProgress> = _progress.asStateFlow()

    /**
     * Initialize data if needed - called at app startup (V2 pattern)
     */
    suspend fun initializeDataIfNeeded(): ImportResult {
        return try {
            // SQLDelight automatically creates tables on first database access
            val showCount = database.showQueries.getShowCount().executeAsOne()

            if (showCount > 0) {
                return ImportResult.AlreadyExists(showCount.toInt())
            }

            // No data - download and import
            downloadAndImportData()

        } catch (e: Exception) {
            ImportResult.Error("Failed to initialize data: ${e.message}")
        }
    }

    /**
     * Force refresh data - called from settings screen
     */
    suspend fun forceRefreshData(): ImportResult {
        return try {
            // Clear existing data
            clearAllData()

            // Download fresh data
            downloadAndImportData()

        } catch (e: Exception) {
            ImportResult.Error("Failed to refresh data: ${e.message}")
        }
    }

    /**
     * Clear all data - called from settings screen
     */
    suspend fun clearAllData(): ImportResult {
        return try {
            _progress.value = ImportProgress.Clearing

            // Clear database
            database.showQueries.deleteAllShows()

            // Clear settings
            settings.remove(DATA_IMPORTED_KEY)
            settings.remove(IMPORT_TIMESTAMP_KEY)

            _progress.value = ImportProgress.Idle
            ImportResult.Cleared

        } catch (e: Exception) {
            ImportResult.Error("Failed to clear data: ${e.message}")
        }
    }

    private suspend fun downloadAndImportData(): ImportResult = withContext(Dispatchers.IO) {
        try {
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

        } catch (e: Exception) {
            _progress.value = ImportProgress.Idle
            ImportResult.Error("Import failed: ${e.message}")
        }
    }

    private fun extractShowFilesFromZip(zipBytes: ByteArray): Map<String, String> {
        // TODO: Implement ZIP extraction using Okio
        // Extract show JSON files from ZIP archive
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
                // Log error but continue with other files
                println("Failed to parse $filename: ${e.message}")
            }
        }

        return shows
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
                    setlistRaw = null, // TODO: Serialize setlist JSON
                    songList = null, // TODO: Extract comma-separated song list
                    lineupStatus = show.lineupStatus,
                    lineupRaw = null, // TODO: Serialize lineup JSON
                    memberList = null, // TODO: Extract comma-separated member list
                    recordingCount = show.recordingCount.toLong(),
                    bestRecordingId = show.bestRecording,
                    averageRating = show.avgRating,
                    totalReviews = 0L, // TODO: Add to import data
                    isInLibrary = 0L,
                    libraryAddedAt = null,
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                    updatedAt = Clock.System.now().toEpochMilliseconds()
                )
            }
        }
    }

    private fun extractYear(date: String): Int = date.split("-")[0].toInt()
    private fun extractMonth(date: String): Int = date.split("-")[1].toInt()
}

@Serializable
data class ShowImportData(
    val showId: String,
    val band: String,
    val venue: String,
    val locationRaw: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = "USA",
    val date: String,
    val url: String? = null,
    val setlistStatus: String? = null,
    val lineupStatus: String? = null,
    val recordingCount: Int = 0,
    val bestRecording: String? = null,
    val avgRating: Double = 0.0
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
```

### Step 3: App Startup Integration

**File**: `composeApp/src/commonMain/kotlin/com/grateful/deadly/App.kt`

```kotlin
// Add to existing App.kt
@Composable
fun App() {
    val dataImportService: DataImportService = koinInject()

    // Initialize data on app startup (V2 pattern)
    LaunchedEffect(Unit) {
        dataImportService.initializeDataIfNeeded()
    }

    // Rest of existing app code...
}
```

### Step 4: Settings Screen Integration

**File**: `composeApp/src/commonMain/kotlin/com/grateful/deadly/feature/settings/SettingsScreen.kt`

```kotlin
@Composable
fun DatabaseManagementSection() {
    val dataImportService: DataImportService = koinInject()
    val database: Database = koinInject()
    val scope = rememberCoroutineScope()

    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    val progress by dataImportService.progress.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Database Management",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Current status
        val showCount by remember {
            derivedStateOf {
                try {
                    database.showQueries.getShowCount().executeAsOne().toInt()
                } catch (e: Exception) {
                    0
                }
            }
        }

        Text("Current shows in database: $showCount")

        Spacer(modifier = Modifier.height(16.dp))

        // Progress indicator
        when (progress) {
            is ImportProgress.Downloading -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Downloading data...")
            }
            is ImportProgress.Extracting -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Extracting files...")
            }
            is ImportProgress.Parsing -> {
                LinearProgressIndicator(
                    progress = progress.current.toFloat() / progress.total.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Parsing files: ${progress.current}/${progress.total}")
            }
            is ImportProgress.Importing -> {
                LinearProgressIndicator(
                    progress = progress.current.toFloat() / progress.total.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Importing shows: ${progress.current}/${progress.total}")
            }
            is ImportProgress.Clearing -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Clearing data...")
            }
            else -> {
                // Show buttons when not in progress
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                importResult = dataImportService.forceRefreshData()
                            }
                        }
                    ) {
                        Text("Refresh Data")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                importResult = dataImportService.clearAllData()
                            }
                        }
                    ) {
                        Text("Clear Data")
                    }
                }
            }
        }

        // Show result
        importResult?.let { result ->
            Spacer(modifier = Modifier.height(8.dp))
            when (result) {
                is ImportResult.Success -> Text("✅ Successfully imported ${result.showCount} shows")
                is ImportResult.AlreadyExists -> Text("ℹ️ Data already exists (${result.showCount} shows)")
                is ImportResult.Error -> Text("❌ Error: ${result.message}")
                is ImportResult.Cleared -> Text("✅ Data cleared successfully")
            }
        }
    }
}
```

## 🔄 Execution Flow

### Automatic Initialization (App Startup)
```
App.kt LaunchedEffect → DataImportService.initializeDataIfNeeded()
                    ↓
            Check database.showQueries.getShowCount()
                    ↓
    If count > 0 → Return AlreadyExists (skip download)
    If count = 0 → Download ZIP → Extract → Parse → Import → Mark complete
```

### Manual Controls (Settings Screen)
```
Settings "Refresh Data" → DataImportService.forceRefreshData()
                       → Clear existing → Download → Import

Settings "Clear Data" → DataImportService.clearAllData()
                     → Delete all shows → Clear settings
```

### SQLDelight Table Creation
```
First database access → SQLDelight.Schema.create() → Runs CREATE TABLE statements
                     → Tables exist and ready for data
```

## ✅ Key Implementation Notes

1. **SQLDelight handles table creation automatically** - No manual SQL execution needed
2. **Follow V2 ShowEntity exactly** - Proven structure, don't get creative
3. **Denormalized venue data** - V2 pattern for fast search without JOINs
4. **JSON strings for complex data** - setlistRaw, lineupRaw as JSON for UI display
5. **Comma-separated lists** - songList, memberList for simple search
6. **Progress tracking** - UI feedback during long operations
7. **Error handling** - Graceful failures, continue with partial data

## 🚀 Next Steps

1. **Implement Step 1** - Create Show.sq with exact V2 structure
2. **Implement Step 2** - Create DataImportService with download logic
3. **Test on both platforms** - Verify table creation and data import
4. **Add to Settings screen** - Manual database controls
5. **Create SearchRepository** - Use SQLDelight queries instead of mock data
6. **Update SearchServiceImpl** - Use repository instead of mockShows

This gives us the foundation for real data import following V2's proven patterns, with minimal scope focusing on the Show table only.