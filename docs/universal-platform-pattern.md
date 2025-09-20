# Universal Service + Platform Tool Pattern

## Overview

This document defines the architectural pattern used in the Deadly KMM app for writing maximum shared code while handling platform-specific operations. The pattern separates universal business logic from platform-specific implementation details.

## Core Principle

**Write code universally. Delegate platform-specific operations to platform tools.**

When you need to:
- Extract files from a ZIP
- Read/write from filesystem
- Interact with databases
- Parse JSON and map to entities

You want **as much of that code to be universal as possible**. Business logic, field mapping, error handling, and workflow coordination should be written once in `commonMain` and work on both Android and iOS.

## Pattern Structure

### Universal Service (commonMain)
- Contains all business logic, workflow coordination, and data transformation
- Handles progress tracking, error handling, result formatting
- Uses platform tools via dependency injection
- Returns standardized results that work across platforms

### Platform Tool (expect/actual)
- Handles only the low-level, platform-specific operations
- Android implementation uses Android-specific APIs
- iOS implementation uses iOS-specific APIs
- Both implementations return identical data structures to the universal service

## Example 1: File Extraction

### Problem
You need to extract a ZIP file and process the contents. The ZIP extraction mechanism differs between platforms, but the workflow, progress tracking, and result processing should be identical.

### Solution

**Universal Service (commonMain):**
```kotlin
// File: commonMain/.../services/FileExtractionService.kt
class FileExtractionService(
    private val zipExtractor: ZipExtractor,
    private val fileSystem: FileSystem
) {
    suspend fun extractFiles(
        zipPath: String,
        outputDir: String,
        progressCallback: ((ExtractionProgress) -> Unit)? = null
    ): ExtractionResult {
        return try {
            // Universal validation logic
            if (!fileSystem.exists(zipPath.toPath())) {
                return ExtractionResult.Error("ZIP file does not exist")
            }

            // Universal progress tracking
            progressCallback?.invoke(ExtractionProgress.Started)

            // Delegate platform-specific extraction to tool
            val extractedFiles = zipExtractor.extractAll(zipPath, outputDir) { current, total ->
                progressCallback?.invoke(ExtractionProgress.Progress(current, total))
            }

            // Universal result processing
            progressCallback?.invoke(ExtractionProgress.Completed)
            ExtractionResult.Success(extractedFiles)

        } catch (e: Exception) {
            // Universal error handling
            ExtractionResult.Error("Extraction failed: ${e.message}")
        }
    }
}

// Shared result types
sealed class ExtractionResult {
    data class Success(val extractedFiles: List<ExtractedFile>) : ExtractionResult()
    data class Error(val message: String) : ExtractionResult()
}

data class ExtractedFile(
    val path: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long
)
```

**Platform Tool (expect/actual):**
```kotlin
// File: commonMain/.../platform/ZipExtractor.kt
expect class ZipExtractor {
    suspend fun extractAll(
        zipPath: String,
        outputDir: String,
        progressCallback: ((current: Int, total: Int) -> Unit)? = null
    ): List<ExtractedFile>
}

// File: androidMain/.../platform/ZipExtractor.kt
actual class ZipExtractor {
    actual suspend fun extractAll(
        zipPath: String,
        outputDir: String,
        progressCallback: ((current: Int, total: Int) -> Unit)?
    ): List<ExtractedFile> {
        // Android-specific implementation using java.util.zip
        val extractedFiles = mutableListOf<ExtractedFile>()

        ZipInputStream(FileInputStream(zipPath)).use { zis ->
            var entry = zis.nextEntry
            var currentEntry = 0
            val totalEntries = countZipEntries(zipPath) // Helper function

            while (entry != null) {
                progressCallback?.invoke(currentEntry, totalEntries)

                if (!entry.isDirectory) {
                    val outputFile = File(outputDir, entry.name)
                    outputFile.parentFile?.mkdirs()

                    outputFile.outputStream().use { output ->
                        zis.copyTo(output)
                    }

                    extractedFiles.add(ExtractedFile(
                        path = outputFile.absolutePath,
                        relativePath = entry.name,
                        isDirectory = false,
                        sizeBytes = entry.size
                    ))
                }

                currentEntry++
                entry = zis.nextEntry
            }
        }

        return extractedFiles
    }
}

// File: iosMain/.../platform/ZipExtractor.kt
actual class ZipExtractor {
    actual suspend fun extractAll(
        zipPath: String,
        outputDir: String,
        progressCallback: ((current: Int, total: Int) -> Unit)?
    ): List<ExtractedFile> {
        // iOS-specific implementation using Foundation APIs
        val extractedFiles = mutableListOf<ExtractedFile>()

        // Use NSFileManager and Foundation ZIP extraction
        // Implementation details vary, but returns same ExtractedFile structure

        return extractedFiles
    }
}
```

## Example 2: Database Operations

### Problem
You need to parse JSON show data and save it to the database. The JSON parsing, field mapping, and business logic should be universal, but the actual database operations are platform-specific.

### Solution

**Universal Service (commonMain):**
```kotlin
// File: commonMain/.../services/ShowImportService.kt
class ShowImportService(
    private val showRepository: ShowRepository,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun importShow(jsonContent: String): ImportResult {
        return try {
            // Universal JSON parsing
            val showData = json.decodeFromString<ShowJsonSchema>(jsonContent)

            // Universal field mapping and business logic
            val showEntity = mapJsonToEntity(showData)

            // Universal validation
            if (showEntity.showId.isBlank()) {
                return ImportResult.Error("Invalid show ID")
            }

            // Delegate to platform-specific database operations
            showRepository.insertShow(showEntity)

            ImportResult.Success(showEntity.showId)

        } catch (e: Exception) {
            ImportResult.Error("Import failed: ${e.message}")
        }
    }

    // Universal mapping logic - written once, works everywhere
    private fun mapJsonToEntity(json: ShowJsonSchema): ShowEntity {
        val (city, state) = parseLocation(json.venue ?: "")

        return ShowEntity(
            showId = json.identifier,
            date = json.date,
            year = extractYear(json.date),
            month = extractMonth(json.date),
            yearMonth = formatYearMonth(json.date),
            band = determineBand(json.identifier),
            venueName = json.venue ?: "Unknown Venue",
            city = city,
            state = state,
            country = "USA",
            locationRaw = json.venue,
            averageRating = json.avgRating,
            totalReviews = json.reviews ?: 0,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
    }

    // Universal business logic helpers
    private fun extractYear(date: String): Int =
        date.split("-")[0].toIntOrNull() ?: 1970

    private fun determineBand(identifier: String): String = when {
        identifier.startsWith("gd") -> "Grateful Dead"
        identifier.startsWith("jgb") -> "Jerry Garcia Band"
        else -> "Grateful Dead"
    }

    private fun parseLocation(venue: String): Pair<String?, String?> {
        val parts = venue.split(",").map { it.trim() }
        return when {
            parts.size >= 3 -> Pair(parts[parts.size - 2], parts[parts.size - 1])
            parts.size == 2 -> Pair(parts[0], parts[1])
            else -> Pair(null, null)
        }
    }
}
```

**Platform Tool (expect/actual):**
```kotlin
// File: commonMain/.../data/ShowRepository.kt
expect class ShowRepository {
    suspend fun insertShow(show: ShowEntity)
    suspend fun getShowById(showId: String): ShowEntity?
    suspend fun getShowCount(): Long
    suspend fun deleteAllShows()
}

// File: androidMain/.../data/ShowRepository.kt
actual class ShowRepository(private val database: Database) {
    actual suspend fun insertShow(show: ShowEntity) {
        database.showQueries.insertShow(
            showId = show.showId,
            date = show.date,
            year = show.year.toLong(),
            month = show.month.toLong(),
            yearMonth = show.yearMonth,
            band = show.band,
            url = show.url,
            venueName = show.venueName,
            city = show.city,
            state = show.state,
            country = show.country,
            locationRaw = show.locationRaw,
            setlistStatus = show.setlistStatus,
            setlistRaw = show.setlistRaw,
            songList = show.songList,
            lineupStatus = show.lineupStatus,
            lineupRaw = show.lineupRaw,
            memberList = show.memberList,
            recordingCount = show.recordingCount.toLong(),
            bestRecordingId = show.bestRecordingId,
            averageRating = show.averageRating,
            totalReviews = show.totalReviews.toLong(),
            isInLibrary = if (show.isInLibrary) 1L else 0L,
            libraryAddedAt = show.libraryAddedAt,
            createdAt = show.createdAt,
            updatedAt = show.updatedAt
        )
    }

    actual suspend fun getShowCount(): Long =
        database.showQueries.getShowCount().executeAsOne()

    // ... other platform-specific database operations
}

// File: iosMain/.../data/ShowRepository.kt
actual class ShowRepository(private val database: Database) {
    // Same interface, same SQLDelight database operations
    // Platform differences handled by SQLDelight drivers
}
```

## Key Benefits

### 1. Single Source of Truth
When you change how `json.venue` maps to `showEntity.venueName`, you change it in **one place** and it automatically works on both platforms.

### 2. Testable Business Logic
All your business logic lives in `commonMain` and can be unit tested once. Field mapping, validation, error handling - all testable without platform concerns.

### 3. Clean Separation of Concerns
- Universal services handle workflow and business logic
- Platform tools handle only the low-level operations they must
- Clear dependency injection boundaries

### 4. Consistent Behavior
Progress tracking, error handling, and result formatting work identically across platforms because they're implemented once.

## Implementation Guidelines

### 1. Start with the Universal Service
Define what you want to accomplish at a high level:
- Extract files and return a list of extracted files
- Import JSON data and return success/error
- Download a file and return the local path

### 2. Identify Platform-Specific Operations
What operations can only be done with platform-specific APIs?
- ZIP file extraction mechanisms
- Database driver specifics
- File system access patterns

### 3. Design the Platform Tool Interface
Create an `expect` interface that captures only the platform-specific operations:
```kotlin
expect class PlatformTool {
    suspend fun doSpecificOperation(input: UniversalInput): UniversalOutput
}
```

### 4. Implement Universal Logic First
Write all the business logic, workflow coordination, and data transformation in the universal service.

### 5. Implement Platform Tools
Create `actual` implementations that handle only the low-level operations and return the expected universal data structures.

## Common Mistakes to Avoid

### ❌ Don't Duplicate Business Logic
```kotlin
// BAD - duplicated mapping logic
// androidMain
actual fun importShow(json: String) {
    val show = Json.decodeFromString<ShowJsonSchema>(json)
    val entity = ShowEntity(show.identifier, show.date, /* mapping */)
    database.insert(entity)
}

// iosMain
actual fun importShow(json: String) {
    val show = Json.decodeFromString<ShowJsonSchema>(json)
    val entity = ShowEntity(show.identifier, show.date, /* same mapping */)
    database.insert(entity)
}
```

### ✅ Keep Business Logic Universal
```kotlin
// GOOD - mapping logic in commonMain
class ShowImportService(private val repository: ShowRepository) {
    suspend fun importShow(json: String): ImportResult {
        val show = Json.decodeFromString<ShowJsonSchema>(json)
        val entity = mapToEntity(show) // Universal mapping
        repository.insertShow(entity) // Platform-specific operation
        return ImportResult.Success
    }
}
```

### ❌ Don't Make Platform Tools Too Complex
```kotlin
// BAD - platform tool doing too much
expect class ShowRepository {
    suspend fun importShowFromJson(jsonContent: String): ImportResult
    suspend fun validateAndInsertShow(show: ShowEntity): ValidationResult
    suspend fun importShowsWithProgressTracking(
        jsonFiles: List<String>,
        callback: (Int, Int) -> Unit
    ): BatchImportResult
}
```

### ✅ Keep Platform Tools Simple and Focused
```kotlin
// GOOD - platform tool does only platform-specific operations
expect class ShowRepository {
    suspend fun insertShow(show: ShowEntity)
    suspend fun getShowById(showId: String): ShowEntity?
    suspend fun getShowCount(): Long
}
```

## Testing Strategy

### Universal Services (commonMain)
Test all business logic, field mapping, error handling, and workflow coordination:
```kotlin
class ShowImportServiceTest {
    @Test
    fun `should map venue correctly`() {
        val service = ShowImportService(MockShowRepository())
        val json = """{"identifier": "gd1977-05-08", "venue": "Barton Hall, Ithaca, NY"}"""

        val result = service.importShow(json)

        // Verify universal mapping logic
        verify(mockRepository).insertShow(
            argThat { show ->
                show.city == "Ithaca" && show.state == "NY"
            }
        )
    }
}
```

### Platform Tools (androidMain/iosMain)
Test only platform-specific integration:
```kotlin
class AndroidShowRepositoryTest {
    @Test
    fun `should insert show to SQLDelight database`() {
        val repository = ShowRepository(testDatabase)
        val show = ShowEntity(/* test data */)

        repository.insertShow(show)

        val retrieved = testDatabase.showQueries.selectShowById(show.showId).executeAsOne()
        assertEquals(show.showId, retrieved.showId)
    }
}
```

## Migration Strategy

When refactoring existing code to this pattern:

1. **Identify universal vs platform-specific code** in current implementation
2. **Extract business logic** into universal service
3. **Create platform tool interface** for remaining operations
4. **Implement platform tools** with minimal logic
5. **Update dependency injection** to wire universal service with platform tools
6. **Remove old mixed implementations**

This pattern scales to any operation where you want maximum code sharing with clean platform separation.

## Current Implementation Status

### ✅ Implemented
- **FileExtractionService + ZipExtractor**: Universal ZIP extraction with platform-specific implementations
- **DataImportService + ShowRepository**: Universal JSON parsing/mapping with platform-specific database operations
- **DataSyncOrchestrator**: Coordinates complete workflow (download → extract → import → cleanup)
- **File Caching**: `DataSyncOrchestrator` checks for cached `data.zip` file before downloading

### ⚠️ Cache Management UI Missing
The old architecture had cache management UI in settings (show cache info, delete cache) but this was removed during the refactor.

**Current Behavior:**
- **First run**: Downloads and caches `data.zip` file locally
- **Subsequent runs**: Uses cached file, skips download
- **Force refresh**: Uses `dataSyncOrchestrator.forceRefreshData()` to clear cache and re-download

**Missing UI Features:**
- Display cached file size and last modified date
- Manual "Delete Cache" button to force re-download
- Cache status indicator

**TODO**: Implement cache management UI using the new architecture pattern:
- Universal cache info service + platform-specific file metadata tool
- Add cache info display to settings screen
- Add manual cache deletion functionality