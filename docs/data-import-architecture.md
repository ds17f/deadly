# Data Import Architecture

## Overview

This document defines the architecture for the data import system in the Deadly KMM app. The system handles downloading, extracting, and importing Grateful Dead show data from GitHub releases.

## Current Problems

### Platform Inconsistency
- **Android ZIP Extractor**: Uses `java.util.zip.ZipInputStream`, properly extracts individual JSON files
- **iOS ZIP Extractor**: Placeholder implementation that writes entire ZIP as single file
- **Result**: Android works, iOS fails with JSON parsing errors

### Mixed Responsibilities
- Services doing too many things at once
- JSON schemas scattered across different files
- No clear separation between platform-specific and shared code

### Poor Phase Management
- Download → Extract → Parse → Import phases not clearly separated
- Error handling inconsistent between phases
- No way to resume partial imports

## Proposed Architecture

### 1. Common Interface Layer (`commonMain`)

Define clear contracts that both platforms must implement:

```kotlin
interface FileDownloader {
    suspend fun download(url: String, destination: String): DownloadResult
}

interface FileExtractor {
    suspend fun extractAll(zipPath: String, extractionDir: String): ExtractionResult
}

interface DataImporter {
    suspend fun importShows(showFiles: List<String>): ImportResult
    suspend fun importRecordings(recordingFiles: List<String>): ImportResult
    suspend fun importCollections(collectionFile: String): ImportResult
}
```

### 2. Shared Data Models (`commonMain`)

Single source of truth for JSON schemas:

```kotlin
@Serializable
data class ShowJsonSchema(
    val identifier: String,          // "1977-05-08-barton-hall-cornell-u-ithaca-ny-usa"
    val date: String,                // "1977-05-08"
    val venue: String?,              // "Barton Hall, Cornell University"
    val city: String?,               // "Ithaca"
    val state: String?,              // "NY"
    val country: String?,            // "USA"
    val setlist: List<SetJsonSchema>?,
    val lineup: List<String>?,
    val recordings: List<String>?,   // List of recording IDs
    val averageRating: Double?,
    val totalReviews: Int?
)

@Serializable
data class RecordingJsonSchema(
    val identifier: String,
    val showId: String,
    val source: String?,
    val taper: String?,
    val transferer: String?,
    val lineage: String?,
    val averageRating: Double?,
    val downloads: Int?
)

@Serializable
data class CollectionJsonSchema(
    val collections: List<Collection>
) {
    @Serializable
    data class Collection(
        val id: String,
        val name: String,
        val description: String?,
        val showIds: List<String>
    )
}
```

### 3. Common Business Logic (`commonMain`)

#### Data Sync Orchestrator
```kotlin
class DataSyncOrchestrator(
    private val downloader: FileDownloader,
    private val extractor: FileExtractor,
    private val importer: DataImporter,
    private val classifier: FileTypeClassifier
) {
    suspend fun syncData(): SyncResult {
        // 1. Check if import needed
        val status = checkDataStatus()
        if (status == DataStatus.UP_TO_DATE) return SyncResult.NoUpdateNeeded

        // 2. Download if necessary
        val downloadResult = downloader.download(GITHUB_RELEASES_URL, LOCAL_ZIP_PATH)
        if (downloadResult is DownloadResult.Error) return SyncResult.DownloadFailed

        // 3. Extract all files
        val extractionResult = extractor.extractAll(LOCAL_ZIP_PATH, EXTRACTION_DIR)
        if (extractionResult is ExtractionResult.Error) return SyncResult.ExtractionFailed

        // 4. Classify files by type
        val classifiedFiles = classifier.classifyExtractedFiles(extractionResult.extractedFiles)

        // 5. Import in phases
        val showResult = importer.importShows(classifiedFiles.shows)
        val recordingResult = importer.importRecordings(classifiedFiles.recordings)
        val collectionResult = importer.importCollections(classifiedFiles.collections.first())

        // 6. Cleanup
        cleanupExtractionDirectory()

        return SyncResult.Success(showResult.count + recordingResult.count)
    }
}
```

#### File Type Classifier
```kotlin
class FileTypeClassifier {
    fun classifyExtractedFiles(allFiles: List<String>): ClassifiedFiles {
        return ClassifiedFiles(
            shows = allFiles.filter { it.contains("/shows/") && it.endsWith(".json") },
            recordings = allFiles.filter { it.contains("/recordings/") && it.endsWith(".json") },
            collections = allFiles.filter { it.endsWith("collections.json") },
            manifest = allFiles.filter { it.endsWith("manifest.json") }
        )
    }
}

data class ClassifiedFiles(
    val shows: List<String>,
    val recordings: List<String>,
    val collections: List<String>,
    val manifest: List<String>
)
```

### 4. Platform-Specific Implementations

#### Android Implementation
```kotlin
// androidMain
actual class AndroidFileExtractor : FileExtractor {
    actual suspend fun extractAll(zipPath: String, extractionDir: String): ExtractionResult {
        // Use java.util.zip.ZipInputStream
        // Extract ALL files (shows/, recordings/, collections.json, manifest.json)
        // Preserve directory structure
        return ExtractionResult.Success(extractedFiles)
    }
}
```

#### iOS Implementation
```kotlin
// iosMain
actual class IosFileExtractor : FileExtractor {
    actual suspend fun extractAll(zipPath: String, extractionDir: String): ExtractionResult {
        // Use Foundation APIs or cross-platform library
        // Must extract ALL files identically to Android
        // Preserve directory structure
        return ExtractionResult.Success(extractedFiles)
    }
}
```

## Workflow Design

### Phase 1: App Launch Check
```kotlin
class AppStartupManager {
    suspend fun checkDataStatus(): DataStatus {
        val hasLocalData = database.showQueries.getShowCount().executeAsOne() > 0
        val hasLocalZip = fileSystem.exists(LOCAL_ZIP_PATH)
        val remoteVersion = getRemoteDataVersion()
        val localVersion = getLocalDataVersion()

        return when {
            !hasLocalData -> DataStatus.NEEDS_INITIAL_IMPORT
            remoteVersion > localVersion -> DataStatus.NEEDS_UPDATE
            else -> DataStatus.UP_TO_DATE
        }
    }
}
```

### Phase 2: Download Management
```kotlin
class DownloadManager : FileDownloader {
    override suspend fun download(url: String, destination: String): DownloadResult {
        // Download data.zip from GitHub releases
        // Verify file integrity
        // Provide progress callbacks
        // Handle network errors gracefully
    }
}
```

### Phase 3: Extraction Management
```kotlin
class ExtractionManager : FileExtractor {
    override suspend fun extractAll(zipPath: String, extractionDir: String): ExtractionResult {
        // Extract ALL files from ZIP
        // Preserve directory structure: shows/, recordings/, collections.json, manifest.json
        // Provide progress callbacks
        // Handle corrupted ZIP files
    }
}
```

### Phase 4: Import Management
```kotlin
class ImportManager : DataImporter {
    override suspend fun importShows(showFiles: List<String>): ImportResult {
        // Parse show JSON files using ShowJsonSchema
        // Convert to database entities
        // Batch insert for performance
        // Handle parsing errors gracefully
    }

    override suspend fun importRecordings(recordingFiles: List<String>): ImportResult {
        // Parse recording JSON files using RecordingJsonSchema
        // Link recordings to existing shows
        // Batch insert for performance
    }

    override suspend fun importCollections(collectionFile: String): ImportResult {
        // Parse collections.json using CollectionJsonSchema
        // Create collection entities
        // Link collections to shows
    }
}
```

## Data Structure Analysis

Based on the actual GitHub data.zip file structure:

```
data.zip
├── manifest.json              # Package metadata
├── collections.json           # Collection definitions
├── shows/                     # Show data (PHASE 1)
│   ├── 1977-05-08-barton-hall-cornell-u-ithaca-ny-usa.json
│   ├── 1979-12-03-uptown-theater-chicago-il-usa.json
│   └── ... (~3000+ show files)
└── recordings/                # Recording data (PHASE 2)
    ├── gd1980-11-29.157406.fob.nak700.cm.wagner.miller.t.flac1644.json
    ├── gd1987-04-07.134861.bltz.flac2496.json
    └── ... (~10000+ recording files)
```

## Implementation Priorities

### Phase 1: Shows Import (Current)
1. Fix ZIP extraction to work identically on both platforms
2. Update JsonImportService to only process `shows/` files
3. Ensure ShowJsonSchema matches actual JSON structure
4. Test end-to-end flow: download → extract → import shows

### Phase 2: Recordings Import (Next)
1. Create RecordingJsonSchema based on actual recording files
2. Build recording import service
3. Link recordings to existing shows
4. Handle large volume (~10k files) efficiently

### Phase 3: Collections Import (Future)
1. Parse collections.json
2. Create collection entities
3. Build collection-show relationships

## Error Handling Strategy

### Download Errors
- Network connectivity issues
- GitHub API rate limiting
- Corrupted downloads
- Insufficient storage space

### Extraction Errors
- Corrupted ZIP files
- Insufficient disk space
- Permission issues
- Platform-specific ZIP format issues

### Import Errors
- JSON parsing failures
- Database constraint violations
- Schema mismatches
- Partial import recovery

## Testing Strategy

### Unit Tests
- JSON schema parsing with real data samples
- File classification logic
- Error handling scenarios
- Database import logic

### Integration Tests
- End-to-end workflow testing
- Platform-specific ZIP extraction
- Large dataset performance
- Network failure scenarios

### Manual Testing
- Fresh app install → initial import
- Update scenarios with newer data
- Memory usage during large imports
- UI responsiveness during import

## Migration Plan

### Step 1: Create Common Interfaces
- Define FileDownloader, FileExtractor, DataImporter interfaces
- Create shared JSON schemas
- Build file classification system

### Step 2: Fix Platform Consistency
- Implement proper iOS ZIP extraction
- Ensure Android and iOS extract identically
- Add comprehensive logging

### Step 3: Implement Orchestrator
- Build DataSyncOrchestrator
- Integrate with existing UI progress indicators
- Handle phase-by-phase import

### Step 4: Update Existing Services
- Migrate current services to use new interfaces
- Maintain backward compatibility during transition
- Update dependency injection

### Step 5: Add Advanced Features
- Resume partial imports
- Incremental updates
- Background sync
- Offline mode support

## Critical Implementation Notes

### Current State Analysis
- **iOS ZIP Extractor**: Currently broken - writes entire ZIP as single JSON file instead of extracting individual files
- **Android ZIP Extractor**: Works correctly - extracts individual JSON files properly
- **JsonImportService**: Tries to parse all extracted files as show data, but needs to filter only `shows/` directory
- **Logger Import**: Some services use `core.util.Logger` (println), others use `core.logging.Logger` (platform-specific) - must be consistent

### File Paths and Structure
```
GitHub URL: https://github.com/ds17f/dead-metadata/releases/download/v2.1.3/data.zip
Local extraction should create:
  extracted_data/
  ├── manifest.json
  ├── collections.json
  ├── shows/
  │   ├── 1977-05-08-barton-hall-cornell-u-ithaca-ny-usa.json
  │   └── ... (~3000 files)
  └── recordings/
      ├── gd1980-11-29.157406.fob.nak700.cm.wagner.miller.t.flac1644.json
      └── ... (~10000 files)
```

### V2 Reference Schema (Shows)
Based on `/home/damian/Developer/dead/v2/core/database/src/main/java/com/deadly/v2/core/database/entities/ShowEntity.kt`:
```kotlin
data class ShowEntity(
    val showId: String,           // "1977-05-08-barton-hall-cornell-u-ithaca-ny-usa"
    val date: String,             // "1977-05-08"
    val year: Int,                // 1977
    val month: Int,               // 5
    val yearMonth: String,        // "1977-05"
    val band: String,             // "Grateful Dead"
    val url: String?,             // Jerry Garcia URL
    val venueName: String,        // "Barton Hall, Cornell University"
    val city: String?,            // "Ithaca"
    val state: String?,           // "NY"
    val country: String = "USA",
    val locationRaw: String?,     // "Ithaca, NY"
    val setlistStatus: String?,
    val setlistRaw: String?,      // JSON string
    val songList: String?,        // Comma-separated for FTS
    val lineupStatus: String?,
    val lineupRaw: String?,       // JSON string
    val memberList: String?,      // Comma-separated for FTS
    val showSequence: Int = 1,
    val recordingsRaw: String?,   // JSON array string
    val recordingCount: Int = 0,
    val bestRecordingId: String?,
    val averageRating: Float?,
    val totalReviews: Int = 0,
    val isInLibrary: Boolean = false,
    val libraryAddedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
```

### Database Schema (SQLDelight)
The KMM app uses SQLDelight, not Room. Check `composeApp/src/commonMain/sqldelight/` for current schema.

### Key Service Locations
- `composeApp/src/commonMain/kotlin/com/grateful/deadly/services/data/DataImportService.kt` - Main orchestrator
- `composeApp/src/commonMain/kotlin/com/grateful/deadly/services/data/JsonImportService.kt` - JSON parsing/import
- `composeApp/src/commonMain/kotlin/com/grateful/deadly/services/data/ZipExtractionService.kt` - ZIP extraction wrapper
- `composeApp/src/androidMain/kotlin/com/grateful/deadly/services/data/ZipExtractor.kt` - Android implementation (working)
- `composeApp/src/iosMain/kotlin/com/grateful/deadly/services/data/ZipExtractor.kt` - iOS implementation (broken)

### Immediate Fixes Needed
1. **Fix iOS ZIP extraction** to extract individual files like Android
2. **Filter JsonImportService** to only process files from `shows/` directory
3. **Standardize logging** - all services should use `core.logging.Logger`
4. **Update JSON schema** to match actual show file structure (not recording structure)

### Testing Commands
```bash
# Remote iOS testing
make run-ios-remotesim
./scripts/remote-logs ios data

# Remote Android testing
make run-android-remote-emu
./scripts/remote-logs android data

# Check extracted files manually
cd /tmp && unzip -l data.zip | grep "shows/"
```

### Data Import Flow (Current)
1. App launches → `DataImportService.initializeDataIfNeeded()`
2. Check show count in database
3. If zero, download data.zip from GitHub releases
4. Extract files using platform-specific `ZipExtractor`
5. Import extracted files using `JsonImportService`
6. Problem: iOS extracts incorrectly, JsonImportService processes all files instead of just shows

### Next Steps Priority
1. **Phase 1**: Fix current show import to work on both platforms
2. **Phase 2**: Add recording import (separate service)
3. **Phase 3**: Add collection import
4. **Phase 4**: Implement full architecture with interfaces

## Future Considerations

### Additional Data Types
- Setlist data (songs, timing, notes)
- Venue information (capacity, location details)
- Artist/member data (lineup changes over time)
- Review and rating data

### Performance Optimizations
- Streaming JSON parsing for large files
- Database indexing strategies
- Memory-efficient processing
- Background processing

### User Experience
- Import progress visualization
- Estimated time remaining
- Pause/resume capability
- Manual refresh triggers