package com.grateful.deadly.services.data

import com.grateful.deadly.database.Database
import com.grateful.deadly.core.util.Logger
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import okio.FileSystem

/**
 * Data import service for real Grateful Dead show data.
 * Handles download → extract → parse → import flow with progress tracking.
 */
class DataImportService(
    private val database: Database,
    private val httpClient: HttpClient,
    private val settings: Settings,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val getAppFilesDir: () -> String, // Platform-specific files directory
    private val zipExtractionService: ZipExtractionService
) {

    // Initialize services
    private val fileDiscoveryService = FileDiscoveryService(httpClient, fileSystem)
    private val downloadService = DownloadService(httpClient, fileSystem)
    private val jsonImportService = JsonImportService(database, fileSystem)

    companion object {
        private const val TAG = "DataImportService"
        private const val DATA_IMPORTED_KEY = "has_imported_data"
        private const val IMPORT_TIMESTAMP_KEY = "import_timestamp"
    }

    private val _progress = MutableStateFlow<ImportProgress>(ImportProgress.Idle)
    val progress: Flow<ImportProgress> = _progress.asStateFlow()

    /**
     * Initialize data if needed - called at app startup
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
            downloadAndImportRealData()

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

            // Delete cached data.zip to force re-download
            deleteCachedDataFile()

            // Download fresh data
            downloadAndImportRealData()

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
     * Download and import real data from GitHub releases
     */
    private suspend fun downloadAndImportRealData(): ImportResult {
        return try {
            Logger.d(TAG, "Starting real data import process...")
            _progress.value = ImportProgress.Downloading

            val appFilesDir = getAppFilesDir()

            // 1. Check for local data.zip file first
            val localFile = fileDiscoveryService.getLocalDataFile(appFilesDir)
            val dataZipPath = if (localFile != null) {
                Logger.d(TAG, "Using cached data.zip file: ${localFile.sizeBytes} bytes")
                localFile.path
            } else {
                // 2. Download from GitHub releases
                Logger.d(TAG, "No local data.zip found, downloading from GitHub...")
                val remoteFile = fileDiscoveryService.getRemoteDataFile()
                    ?: return ImportResult.Error("No data.zip file found in GitHub releases")

                val downloadResult = downloadService.downloadFile(remoteFile, appFilesDir) { progress ->
                    _progress.value = ImportProgress.Downloading
                }

                when (downloadResult) {
                    is DownloadService.DownloadResult.Success -> downloadResult.localFilePath
                    is DownloadService.DownloadResult.Error -> {
                        return ImportResult.Error("Download failed: ${downloadResult.message}")
                    }
                }
            }

            // 3. Extract ZIP file
            Logger.d(TAG, "Extracting data.zip...")
            _progress.value = ImportProgress.Extracting

            val extractionResult = zipExtractionService.extractDataZip(dataZipPath, appFilesDir) { progress ->
                _progress.value = ImportProgress.Extracting
            }

            val extractedFiles = when (extractionResult) {
                is ZipExtractionService.ExtractionResult.Success -> extractionResult.extractedFiles
                is ZipExtractionService.ExtractionResult.Error -> {
                    return ImportResult.Error("Extraction failed: ${extractionResult.message}")
                }
            }

            // 4. Import JSON files to database
            Logger.d(TAG, "Importing ${extractedFiles.size} JSON files...")
            _progress.value = ImportProgress.Importing(0, extractedFiles.size)

            val importResult = jsonImportService.importJsonFiles(extractedFiles) { progress ->
                _progress.value = ImportProgress.Importing(progress.importedFiles, progress.totalFiles)
            }

            val importedCount = when (importResult) {
                is JsonImportService.ImportResult.Success -> importResult.importedCount
                is JsonImportService.ImportResult.Error -> {
                    return ImportResult.Error("Import failed: ${importResult.message}")
                }
            }

            // 5. Cleanup extraction directory
            if (extractionResult is ZipExtractionService.ExtractionResult.Success) {
                zipExtractionService.cleanupExtraction(extractionResult.extractionDirectory)
            }

            // 6. Mark as completed
            settings.putBoolean(DATA_IMPORTED_KEY, true)
            settings.putLong(IMPORT_TIMESTAMP_KEY, Clock.System.now().toEpochMilliseconds())

            _progress.value = ImportProgress.Idle

            Logger.d(TAG, "Real data import completed: $importedCount shows imported")
            ImportResult.Success(importedCount)

        } catch (e: Exception) {
            Logger.e(TAG, "Real data import failed", e)
            _progress.value = ImportProgress.Idle
            ImportResult.Error("Import failed: ${e.message}")
        }
    }

    /**
     * Delete cached data.zip file to force re-download
     */
    suspend fun deleteCachedDataFile(): Boolean {
        return try {
            val appFilesDir = getAppFilesDir()
            val localFile = fileDiscoveryService.getLocalDataFile(appFilesDir)

            if (localFile != null) {
                downloadService.deleteLocalFile(localFile.path)
            } else {
                Logger.d(TAG, "No cached data.zip file to delete")
                true
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to delete cached data.zip file", e)
            false
        }
    }

    /**
     * Get cached data file info for Settings UI
     */
    suspend fun getCachedDataFileInfo(): CachedFileInfo? {
        return try {
            val appFilesDir = getAppFilesDir()
            val localFile = fileDiscoveryService.getLocalDataFile(appFilesDir)

            localFile?.let {
                CachedFileInfo(
                    fileName = "data.zip",
                    sizeBytes = it.sizeBytes,
                    lastModified = settings.getLongOrNull(IMPORT_TIMESTAMP_KEY) ?: 0L
                )
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get cached data file info", e)
            null
        }
    }

    data class CachedFileInfo(
        val fileName: String,
        val sizeBytes: Long,
        val lastModified: Long
    )

}

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