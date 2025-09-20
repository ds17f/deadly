package com.grateful.deadly.services.data

import okio.FileSystem
import okio.Path

/**
 * Cross-platform ZIP extraction interface using expect/actual pattern.
 */
expect class ZipExtractor(fileSystem: FileSystem) {
    /**
     * Extract JSON files from a ZIP file to the specified directory.
     * Returns list of extracted file paths.
     */
    suspend fun extractJsonFiles(
        zipPath: Path,
        extractionDir: Path,
        progressCallback: ((current: Int, total: Int, currentFile: String) -> Unit)? = null
    ): List<String>
}