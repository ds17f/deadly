# Recordings Import Implementation Plan

**Date**: 2025-01-20
**Status**: Ready for implementation using V2-proven patterns
**Architecture**: Universal Service + Platform Tool pattern, matching V2's single-repository approach

## Data Structure Analysis

### GitHub Data Overview
- **Shows**: ~2,313 JSON files with `recordings` array containing recording identifiers
- **Recordings**: ~17,854 JSON files with detailed recording metadata
- **Relationship**: Shows reference recordings via identifier strings in `recordings` array
- **Data Location**: `/data-sample/recordings/*.json` and `/data-sample/shows/*.json`

### Example Data Structure

**Show→Recording Relationship:**
```json
// 1977-02-26-swing-auditorium-san-bernardino-ca-usa.json
{
  "show_id": "1977-02-26-swing-auditorium-san-bernardino-ca-usa",
  "recordings": [
    "gd1977-02-26.sbd.cantor.deibert.83283.flac16",
    "gd77-02-26.sbd.owen.23808.sbeok.shnf",
    "gd77-02-26.sbd.alphadog.9752.sbeok.shnf",
    // ... 8 more recordings
  ],
  "best_recording": "gd1977-02-26.sbd.cantor.deibert.83283.flac16",
  "avg_rating": 4.722537375415285,
  "recording_count": 11
}
```

**Recording Detail:**
```json
// gd1977-02-26.sbd.cantor.deibert.83283.flac16.json
{
  "identifier": "gd1977-02-26.sbd.cantor.deibert.83283.flac16",
  "title": "Grateful Dead Live at Swing Auditorium on 1977-02-26",
  "date": "1977-02-26",
  "venue": "Swing Auditorium",
  "location": "San Bernardino, CA",
  "source_type": "SBD",
  "lineage": "7\" Betty Board Reel @ 7 1/2 ips > PCM501ES > ...",
  "taper": "Betty Cantor",
  "source": "Betty Soundboard",
  "rating": 4.9523809523809526,
  "review_count": 22,
  "confidence": 1.0,
  "raw_rating": 4.9523809523809526,
  "high_ratings": 21,
  "low_ratings": 0
}
```

## V2 Architecture Pattern Analysis

### V2's Proven Single-Repository Pattern

**Key Discovery**: V2 does NOT have separate RecordingRepository. It uses **single ShowRepository** that handles both shows AND recordings.

**V2 Repository Interface:**
```kotlin
interface ShowRepository {
    // Show queries
    suspend fun getShowById(showId: String): Show?
    suspend fun getAllShows(): List<Show>

    // Recording queries (in SAME interface!)
    suspend fun getRecordingsForShow(showId: String): List<Recording>
    suspend fun getBestRecordingForShow(showId: String): Recording?
    suspend fun getRecordingById(identifier: String): Recording?
    suspend fun getTopRatedRecordings(minRating: Double, minReviews: Int, limit: Int): List<Recording>
}
```

**V2 Implementation Pattern:**
```kotlin
class ShowRepositoryImpl @Inject constructor(
    private val showDao: ShowDao,
    private val recordingDao: RecordingDao,  // Both DAOs in same repository!
    private val showMappers: ShowMappers     // Single mapper for both
) : ShowRepository {

    override suspend fun getRecordingsForShow(showId: String): List<Recording> {
        return showMappers.recordingEntitiesToDomain(recordingDao.getRecordingsForShow(showId))
    }
}
```

**V2 ShowMappers handles both:**
```kotlin
class ShowMappers {
    fun entityToDomain(entity: ShowEntity): Show { /*...*/ }
    fun recordingEntityToDomain(entity: RecordingEntity): Recording { /*...*/ }
    fun recordingEntitiesToDomain(entities: List<RecordingEntity>): List<Recording> { /*...*/ }
}
```

## Implementation Plan (Following V2 Pattern)

### 1. Recording Database Schema (SQLDelight)

**Create: `Recording.sq`** - Exact match to V2's proven schema

```sql
-- Recording table with foreign key to Show
CREATE TABLE Recording (
    identifier TEXT PRIMARY KEY NOT NULL,  -- Archive.org identifier
    showId TEXT NOT NULL,                   -- Foreign key to Show.showId

    -- Source information
    sourceType TEXT,                        -- "SBD", "AUD", "FM", "MATRIX", "REMASTER"
    taper TEXT,                            -- Person who recorded
    source TEXT,                           -- Equipment info
    lineage TEXT,                          -- Digital transfer chain
    sourceTypeString TEXT,                 -- Raw source string from data

    -- Quality metrics
    rating REAL NOT NULL DEFAULT 0.0,      -- Weighted rating (0.0-5.0)
    rawRating REAL NOT NULL DEFAULT 0.0,   -- Simple average (0.0-5.0)
    reviewCount INTEGER NOT NULL DEFAULT 0,
    confidence REAL NOT NULL DEFAULT 0.0,  -- Rating confidence (0.0-1.0)
    highRatings INTEGER NOT NULL DEFAULT 0, -- Count of 4-5★ reviews
    lowRatings INTEGER NOT NULL DEFAULT 0,  -- Count of 1-2★ reviews

    -- Metadata
    collectionTimestamp INTEGER NOT NULL DEFAULT 0,

    -- Foreign key constraint
    FOREIGN KEY(showId) REFERENCES Show(showId) ON DELETE CASCADE
);

-- Performance indexes (from V2)
CREATE INDEX idx_recording_show_id ON Recording(showId);
CREATE INDEX idx_recording_source_type ON Recording(sourceType);
CREATE INDEX idx_recording_rating ON Recording(rating);
CREATE INDEX idx_recording_show_rating ON Recording(showId, rating);

-- Recording queries
selectRecordingById:
SELECT * FROM Recording WHERE identifier = ?;

selectRecordingsForShow:
SELECT * FROM Recording WHERE showId = ? ORDER BY rating DESC;

selectBestRecordingForShow:
SELECT * FROM Recording WHERE showId = ? ORDER BY rating DESC LIMIT 1;

selectRecordingsBySourceType:
SELECT * FROM Recording WHERE sourceType = ? ORDER BY rating DESC;

selectTopRatedRecordings:
SELECT * FROM Recording
WHERE rating > ? AND reviewCount >= ?
ORDER BY rating DESC, reviewCount DESC
LIMIT ?;

getRecordingCount:
SELECT COUNT(*) FROM Recording;

insertRecording:
INSERT OR REPLACE INTO Recording (
    identifier, showId, sourceType, taper, source, lineage, sourceTypeString,
    rating, rawRating, reviewCount, confidence, highRatings, lowRatings,
    collectionTimestamp
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

deleteAllRecordings:
DELETE FROM Recording;

deleteRecordingsForShow:
DELETE FROM Recording WHERE showId = ?;
```

### 2. Recording Models & Domain Objects

**Extend: `ImportModels.kt`** - Add recording-specific models

```kotlin
// JSON schema matching GitHub recording files
@Serializable
data class RecordingJsonSchema(
    @SerialName("identifier") val identifier: String,
    @SerialName("title") val title: String? = null,
    @SerialName("date") val date: String,
    @SerialName("venue") val venue: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("source_type") val sourceType: String? = null,
    @SerialName("lineage") val lineage: String? = null,
    @SerialName("taper") val taper: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("runtime") val runtime: String? = null,
    @SerialName("rating") val rating: Double = 0.0,
    @SerialName("review_count") val reviewCount: Int = 0,
    @SerialName("confidence") val confidence: Double = 0.0,
    @SerialName("raw_rating") val rawRating: Double = 0.0,
    @SerialName("high_ratings") val highRatings: Int = 0,
    @SerialName("low_ratings") val lowRatings: Int = 0
)

// Database entity for SQLDelight
data class RecordingEntity(
    val identifier: String,
    val showId: String,
    val sourceType: String?,
    val taper: String?,
    val source: String?,
    val lineage: String?,
    val sourceTypeString: String?,
    val rating: Double,
    val rawRating: Double,
    val reviewCount: Int,
    val confidence: Double,
    val highRatings: Int,
    val lowRatings: Int,
    val collectionTimestamp: Long
)

// Domain model (rich business object)
data class Recording(
    val identifier: String,
    val showId: String,
    val sourceType: RecordingSourceType,
    val rating: Double,
    val reviewCount: Int,
    val taper: String? = null,
    val source: String? = null,
    val lineage: String? = null,
    val sourceTypeString: String? = null
) {
    val hasRating: Boolean get() = rating > 0.0 && reviewCount > 0
    val displayRating: String get() = if (hasRating) "%.1f/5.0 (%d reviews)".format(rating, reviewCount) else "Not Rated"
    val displayTitle: String get() = "${sourceType.displayName} • $displayRating"
}

// Source type enum with safe parsing (from V2)
@Serializable
enum class RecordingSourceType(val displayName: String) {
    SOUNDBOARD("SBD"),
    AUDIENCE("AUD"),
    FM("FM"),
    MATRIX("Matrix"),
    REMASTER("Remaster"),
    UNKNOWN("Unknown");

    companion object {
        fun fromString(value: String?): RecordingSourceType {
            return when (value?.uppercase()) {
                "SBD", "SOUNDBOARD" -> SOUNDBOARD
                "AUD", "AUDIENCE" -> AUDIENCE
                "FM" -> FM
                "MATRIX", "MTX" -> MATRIX
                "REMASTER" -> REMASTER
                else -> UNKNOWN
            }
        }
    }
}
```

### 3. Enhanced ShowRepository (Following V2 Single-Repository Pattern)

**Extend: `ShowRepository.kt`** - Add recording operations to existing repository

```kotlin
expect class ShowRepository(database: Database) {
    // Existing show operations
    suspend fun insertShow(show: ShowEntity)
    suspend fun getShowById(showId: String): ShowEntity?
    suspend fun getShowCount(): Long
    suspend fun deleteAllShows()
    suspend fun insertShows(shows: List<ShowEntity>)

    // NEW: Recording operations (following V2 pattern)
    suspend fun insertRecording(recording: RecordingEntity)
    suspend fun insertRecordings(recordings: List<RecordingEntity>)
    suspend fun getRecordingById(identifier: String): RecordingEntity?
    suspend fun getRecordingsForShow(showId: String): List<RecordingEntity>
    suspend fun getBestRecordingForShow(showId: String): RecordingEntity?
    suspend fun getRecordingsBySourceType(sourceType: String): List<RecordingEntity>
    suspend fun getTopRatedRecordings(minRating: Double, minReviews: Int, limit: Int): List<RecordingEntity>
    suspend fun getRecordingCount(): Long
    suspend fun deleteAllRecordings()
    suspend fun deleteRecordingsForShow(showId: String)
}
```

### 4. ShowMappers (Universal Service)

**Create: `ShowMappers.kt`** - Handle both Show and Recording entity↔domain conversion

```kotlin
class ShowMappers(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    companion object {
        private const val TAG = "ShowMappers"
    }

    // Show entity → domain conversion
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

    fun entitiesToDomain(entities: List<ShowEntity>): List<Show> = entities.map(::entityToDomain)

    // Recording entity → domain conversion
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

    fun recordingEntitiesToDomain(entities: List<RecordingEntity>): List<Recording> =
        entities.map(::recordingEntityToDomain)

    // Private parsing helpers
    private fun parseSetlist(json: String?, status: String?): Setlist? {
        // TODO: Implement setlist parsing
        return null
    }

    private fun parseLineup(json: String?, status: String?): Lineup? {
        // TODO: Implement lineup parsing
        return null
    }

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
```

### 5. Enhanced DataImportService (Universal Service)

**Extend: `DataImportService.kt`** - Add recording import workflow

```kotlin
class DataImportService(
    private val showRepository: ShowRepository,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun importData(
        extractedFiles: List<ExtractedFile>,
        progressCallback: ((ImportProgress) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            Logger.d(TAG, "Starting import of ${extractedFiles.size} files")

            // Phase 1: Import shows (existing functionality)
            val showFiles = filterShowFiles(extractedFiles)
            val showResult = importShowData(showFiles, progressCallback)

            if (showResult is ImportResult.Error) {
                return@withContext showResult
            }

            // Phase 2: Import recordings (NEW)
            val recordingFiles = filterRecordingFiles(extractedFiles)
            val recordingResult = importRecordingData(recordingFiles, progressCallback)

            when {
                showResult is ImportResult.Success && recordingResult is ImportResult.Success -> {
                    ImportResult.Success(showResult.importedCount + recordingResult.importedCount)
                }
                recordingResult is ImportResult.Error -> recordingResult
                else -> ImportResult.Error("Unknown import error")
            }

        } catch (e: Exception) {
            Logger.e(TAG, "Import failed", e)
            ImportResult.Error("Import failed: ${e.message}")
        }
    }

    // NEW: Recording import functionality
    private suspend fun importRecordingData(
        recordingFiles: List<ExtractedFile>,
        progressCallback: ((ImportProgress) -> Unit)? = null
    ): ImportResult {

        Logger.d(TAG, "Starting recording import: ${recordingFiles.size} files")

        if (recordingFiles.isEmpty()) {
            return ImportResult.Success(0)
        }

        // Get all show IDs to filter recordings
        val showIds = getAllShowIds()
        var importedCount = 0
        val totalFiles = recordingFiles.size

        // Process recordings in batches
        recordingFiles.chunked(BATCH_SIZE).forEachIndexed { batchIndex, batch ->
            val recordingEntities = mutableListOf<RecordingEntity>()

            batch.forEachIndexed { fileIndex, file ->
                val globalIndex = batchIndex * BATCH_SIZE + fileIndex

                progressCallback?.invoke(
                    ImportProgress.Processing(
                        current = globalIndex,
                        total = totalFiles,
                        currentFile = file.relativePath
                    )
                )

                try {
                    // Parse recording JSON
                    val jsonContent = readFileContent(file.path)
                    val recordingData = json.decodeFromString<RecordingJsonSchema>(jsonContent)

                    // Find which show(s) reference this recording
                    val referencingShows = findShowsReferencingRecording(recordingData.identifier, showIds)

                    // Only import recordings referenced by imported shows
                    referencingShows.forEach { showId ->
                        val recordingEntity = mapRecordingJsonToEntity(recordingData, showId)
                        recordingEntities.add(recordingEntity)
                    }

                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to process recording: ${file.relativePath}", e)
                }
            }

            // Batch insert recordings
            if (recordingEntities.isNotEmpty()) {
                showRepository.insertRecordings(recordingEntities)
                importedCount += recordingEntities.size
                Logger.d(TAG, "Imported recording batch ${batchIndex + 1}: ${recordingEntities.size} recordings")
            }
        }

        Logger.d(TAG, "Recording import completed: $importedCount recordings imported")
        return ImportResult.Success(importedCount)
    }

    private fun filterRecordingFiles(extractedFiles: List<ExtractedFile>): List<ExtractedFile> {
        return extractedFiles.filter { file ->
            !file.isDirectory &&
            file.relativePath.contains("recordings/") &&
            file.relativePath.endsWith(".json", ignoreCase = true)
        }
    }

    private suspend fun getAllShowIds(): Set<String> {
        // Get all imported show IDs to filter recordings
        // Implementation depends on available ShowRepository methods
        return emptySet() // TODO: Implement based on ShowRepository interface
    }

    private suspend fun findShowsReferencingRecording(recordingId: String, showIds: Set<String>): List<String> {
        // Implementation: check which shows have this recording in their recordings array
        // This requires either querying shows or maintaining a mapping during show import
        return emptyList() // TODO: Implement recording→show mapping
    }

    private fun mapRecordingJsonToEntity(json: RecordingJsonSchema, showId: String): RecordingEntity {
        val now = Clock.System.now().toEpochMilliseconds()

        return RecordingEntity(
            identifier = json.identifier,
            showId = showId,
            sourceType = json.sourceType,
            taper = json.taper,
            source = json.source,
            lineage = json.lineage,
            sourceTypeString = json.sourceType,
            rating = json.rating,
            rawRating = json.rawRating,
            reviewCount = json.reviewCount,
            confidence = json.confidence,
            highRatings = json.highRatings,
            lowRatings = json.lowRatings,
            collectionTimestamp = now
        )
    }
}
```

### 6. Enhanced DataSyncOrchestrator

**Extend: `DataSyncOrchestrator.kt`** - Update workflow for recordings

```kotlin
class DataSyncOrchestrator(
    private val downloadService: DownloadService,
    private val fileDiscoveryService: FileDiscoveryService,
    private val fileExtractionService: FileExtractionService,
    private val dataImportService: DataImportService,
    private val getAppFilesDir: () -> String
) {

    suspend fun syncData(): SyncResult {
        return try {
            // Check if data already exists
            val showCount = dataImportService.getShowCount()
            val recordingCount = dataImportService.getRecordingCount() // NEW

            if (showCount > 0 && recordingCount > 0) {
                Logger.d(TAG, "Data already exists ($showCount shows, $recordingCount recordings), skipping sync")
                return SyncResult.AlreadyExists(showCount.toInt())
            }

            // Existing workflow: download → extract → import (now includes recordings)
            // ... existing implementation remains the same
            // DataImportService now handles both shows and recordings automatically

        } catch (e: Exception) {
            Logger.e(TAG, "Sync failed", e)
            SyncResult.Error(e.message ?: "Unknown sync error")
        }
    }
}
```

### 7. DI Integration

**Extend: `CommonModule.kt`** - Add ShowMappers

```kotlin
val commonModule = module {

    // Existing services...

    // NEW: ShowMappers for entity↔domain conversion
    single<ShowMappers> {
        Logger.d("CommonModule", "Creating ShowMappers")
        ShowMappers()
    }

    // Update DataImportService to use enhanced version
    single<DataImportService> {
        Logger.d("CommonModule", "Creating DataImportService (with recordings)")
        DataImportService(
            showRepository = get(),
            fileSystem = get()
        )
    }
}
```

## Implementation Timeline

### Phase 1: Core Recording Infrastructure (4-6 hours)
1. **Recording.sq** - Database schema and queries
2. **Recording models** - JSON schema, entity, domain objects
3. **Enhanced ShowRepository** - Add recording operations
4. **ShowMappers** - Entity↔domain conversion

### Phase 2: Recording Import Logic (4-6 hours)
1. **Enhanced DataImportService** - Recording import workflow
2. **Recording filtering** - Only import recordings referenced by shows
3. **Show↔Recording mapping** - Link recordings to imported shows
4. **Progress tracking** - Update for recording import phase

### Phase 3: Integration & Testing (2-4 hours)
1. **DI integration** - Update CommonModule
2. **Cross-platform testing** - Verify on Android and iOS
3. **Performance validation** - Ensure import handles ~17k recording files
4. **Data verification** - Confirm recording counts and relationships

### Phase 4: Enhanced Repository Interface (Future)
1. **Domain-layer ShowRepository** - Rich query interface returning domain models
2. **Recording queries** - By source type, quality, show, etc.
3. **Service layer** - RecordingService for business operations

## Success Metrics

✅ **Import Performance**: Handle ~17,854 recording files efficiently
✅ **Data Integrity**: Recordings correctly linked to shows via identifier arrays
✅ **Architecture Consistency**: Follow V2's single-repository pattern exactly
✅ **Cross-platform**: Work identically on Android and iOS
✅ **Future-ready**: Foundation prepared for media player integration

## Technical Notes

### Efficient Recording Import Strategy
- **Filter by show references**: Only import recordings that are referenced in show.recordings arrays (~90% reduction)
- **Batch processing**: Process recordings in chunks for memory efficiency
- **Error resilience**: Continue import even if individual recordings fail to parse

### Show↔Recording Relationship
- **Shows drive recordings**: Import shows first, then filter recordings by show references
- **Foreign key integrity**: Recording.showId → Show.showId with CASCADE delete
- **Multiple shows per recording**: Some recordings may be referenced by multiple shows

### V2 Compatibility
- **Exact schema match**: Recording table matches V2's RecordingEntity exactly
- **Single repository**: ShowRepository handles both shows and recordings (not separate repos)
- **Domain model conversion**: ShowMappers handles both Show and Recording entity↔domain mapping

This implementation plan provides a complete roadmap for adding recording import to our existing show import system while maintaining architectural consistency with V2's proven patterns.