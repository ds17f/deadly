package com.grateful.deadly.services.data

import com.grateful.deadly.core.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * File discovery service for data.zip files.
 * Discovers local cached files and remote GitHub releases.
 */
class FileDiscoveryService(
    private val httpClient: HttpClient,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {

    companion object {
        private const val TAG = "FileDiscoveryService"
        private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/ds17f/dead-metadata/releases/latest"
        private const val DATA_ZIP_FILENAME = "data.zip"
    }

    data class LocalFile(
        val path: String,
        val sizeBytes: Long,
        val type: FileType
    )

    data class RemoteFile(
        val name: String,
        val downloadUrl: String,
        val sizeBytes: Long,
        val type: FileType
    )

    enum class FileType {
        DATA_ZIP
    }

    data class DiscoveryResult(
        val localFiles: List<LocalFile>,
        val remoteFiles: List<RemoteFile>
    )

    /**
     * Discover all available local and remote files
     */
    suspend fun discoverAvailableFiles(appFilesDir: String): DiscoveryResult {
        Logger.d(TAG, "Starting file discovery in directory: $appFilesDir")

        val localFiles = findLocalFiles(appFilesDir)
        val remoteFiles = findRemoteFiles()

        Logger.d(TAG, "Discovery complete: ${localFiles.size} local files, ${remoteFiles.size} remote files")

        return DiscoveryResult(localFiles, remoteFiles)
    }

    /**
     * Find local data.zip file in the app's files directory
     */
    private fun findLocalFiles(appFilesDir: String): List<LocalFile> {
        Logger.d(TAG, "Searching for local files in $appFilesDir")

        return try {
            val filesDir = appFilesDir.toPath()

            if (!fileSystem.exists(filesDir)) {
                Logger.d(TAG, "Files directory does not exist")
                return emptyList()
            }

            val dataZipPath = filesDir / DATA_ZIP_FILENAME

            if (fileSystem.exists(dataZipPath)) {
                val metadata = fileSystem.metadata(dataZipPath)
                val sizeBytes = metadata.size ?: 0L

                Logger.d(TAG, "Found local data file: $DATA_ZIP_FILENAME ($sizeBytes bytes)")

                listOf(LocalFile(
                    path = dataZipPath.toString(),
                    sizeBytes = sizeBytes,
                    type = FileType.DATA_ZIP
                ))
            } else {
                Logger.d(TAG, "No local data.zip file found")
                emptyList()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error searching for local files", e)
            emptyList()
        }
    }

    /**
     * Find remote data.zip file from GitHub releases
     */
    private suspend fun findRemoteFiles(): List<RemoteFile> {
        Logger.d(TAG, "Searching for remote files from GitHub releases")

        return try {
            val release = httpClient.get(GITHUB_RELEASES_URL).body<GitHubRelease>()

            Logger.d(TAG, "Found release: ${release.tagName} with ${release.assets.size} assets")

            val dataAssets = release.assets.filter { asset ->
                val fileName = asset.name.lowercase()
                fileName.startsWith("data") && fileName.endsWith(".zip")
            }

            dataAssets.map { asset ->
                Logger.d(TAG, "Found remote data file: ${asset.name} (${asset.size} bytes)")
                RemoteFile(
                    name = asset.name,
                    downloadUrl = asset.browserDownloadUrl,
                    sizeBytes = asset.size,
                    type = FileType.DATA_ZIP
                )
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error searching for remote files", e)
            emptyList()
        }
    }

    /**
     * Check if data.zip file exists locally
     */
    suspend fun hasLocalDataFile(appFilesDir: String): Boolean {
        val localFiles = findLocalFiles(appFilesDir)
        return localFiles.any { it.type == FileType.DATA_ZIP }
    }

    /**
     * Check if data.zip file is available remotely
     */
    suspend fun hasRemoteDataFile(): Boolean {
        val remoteFiles = findRemoteFiles()
        return remoteFiles.any { it.type == FileType.DATA_ZIP }
    }

    /**
     * Get the local data.zip file if it exists
     */
    suspend fun getLocalDataFile(appFilesDir: String): LocalFile? {
        val localFiles = findLocalFiles(appFilesDir)
        return localFiles.firstOrNull { it.type == FileType.DATA_ZIP }
    }

    /**
     * Get the remote data.zip file if available
     */
    suspend fun getRemoteDataFile(): RemoteFile? {
        val remoteFiles = findRemoteFiles()
        return remoteFiles.firstOrNull { it.type == FileType.DATA_ZIP }
    }
}