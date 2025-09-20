package com.grateful.deadly.services.data.platform

import com.grateful.deadly.services.data.models.ExtractedFile

/**
 * Platform-specific ZIP extraction tool.
 *
 * This is the platform tool in the Universal Service + Platform Tool pattern.
 * It handles only the low-level, platform-specific ZIP extraction operations.
 * The universal FileExtractionService handles workflow, progress, and error handling.
 */
expect class ZipExtractor() {
    /**
     * Extract all files from a ZIP archive to the specified output directory.
     * Preserves the original directory structure from the ZIP file.
     *
     * @param zipPath Absolute path to the ZIP file
     * @param outputDir Absolute path to the extraction directory
     * @param progressCallback Optional callback for progress updates (current, total)
     * @return List of successfully extracted files
     */
    suspend fun extractAll(
        zipPath: String,
        outputDir: String,
        progressCallback: ((current: Int, total: Int) -> Unit)? = null
    ): List<ExtractedFile>
}