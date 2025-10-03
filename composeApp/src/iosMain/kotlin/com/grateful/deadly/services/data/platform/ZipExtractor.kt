package com.grateful.deadly.services.data.platform

import com.grateful.deadly.services.data.models.ExtractedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.cinterop.*
import platform.Foundation.*

/**
 * iOS implementation of ZIP extraction using callback bridge to host app.
 *
 * This is the platform tool that handles only iOS-specific ZIP operations.
 * All universal logic (progress, error handling, workflow) is in FileExtractionService.
 *
 * Uses PlatformUnzipBridge to request extraction from iOS app (which uses ZIPFoundation),
 * then enumerates extracted files using NSFileManager - same pattern as Android using java.util.zip.
 */
actual class ZipExtractor actual constructor() {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun extractAll(
        zipPath: String,
        outputDir: String,
        progressCallback: ((current: Int, total: Int) -> Unit)?
    ): List<ExtractedFile> = withContext(Dispatchers.Default) {
        val extractedFiles = mutableListOf<ExtractedFile>()
        val fileManager = NSFileManager.defaultManager

        try {
            progressCallback?.invoke(0, 1)

            // Request unzip via callback bridge to iOS app
            val result = PlatformUnzipBridge.requestUnzip(
                sourcePath = zipPath,
                destinationPath = outputDir,
                overwrite = true
            )

            // Check if unzip succeeded
            result.getOrThrow()

            // Enumerate all extracted files using NSFileManager
            extractedFiles.addAll(enumerateExtractedFiles(outputDir, outputDir, fileManager))

            progressCallback?.invoke(1, 1)

        } catch (e: Exception) {
            throw Exception("iOS ZIP extraction failed: ${e.message}")
        }

        extractedFiles
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun enumerateExtractedFiles(
        rootDir: String,
        currentDir: String,
        fileManager: NSFileManager
    ): List<ExtractedFile> {
        val files = mutableListOf<ExtractedFile>()

        val contents = fileManager.contentsOfDirectoryAtPath(currentDir, error = null) as? List<String>
        contents?.forEach { fileName ->
            val filePath = "$currentDir/$fileName"

            val isDirectory = memScoped {
                val isDir = alloc<BooleanVar>()
                fileManager.fileExistsAtPath(filePath, isDirectory = isDir.ptr)
                isDir.value
            }

            val fileSize = if (!isDirectory) {
                val attributes = fileManager.attributesOfItemAtPath(filePath, error = null) as? Map<String, Any>
                (attributes?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
            } else {
                0L
            }

            // Calculate relative path from root extraction directory
            val relativePath = if (currentDir == rootDir) {
                fileName
            } else {
                val relativeDir = currentDir.removePrefix("$rootDir/")
                "$relativeDir/$fileName"
            }

            files.add(
                ExtractedFile(
                    path = filePath,
                    relativePath = relativePath,
                    isDirectory = isDirectory,
                    sizeBytes = fileSize
                )
            )

            // Recursively process subdirectories
            if (isDirectory) {
                files.addAll(enumerateExtractedFiles(rootDir, filePath, fileManager))
            }
        }

        return files
    }

}