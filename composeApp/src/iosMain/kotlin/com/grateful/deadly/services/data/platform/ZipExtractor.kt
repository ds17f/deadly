package com.grateful.deadly.services.data.platform

import com.grateful.deadly.services.data.models.ExtractedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.*

/**
 * iOS implementation of ZIP extraction using native system unzip command.
 *
 * This is the platform tool that handles only iOS-specific ZIP operations.
 * All universal logic (progress, error handling, workflow) is in FileExtractionService.
 *
 * Uses the system's built-in `/usr/bin/unzip` command, then enumerates extracted
 * files using NSFileManager - same pattern as Android using java.util.zip.
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
            // Create output directory
            val outputDirURL = NSURL.fileURLWithPath(outputDir)
            fileManager.createDirectoryAtURL(
                outputDirURL,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )

            progressCallback?.invoke(0, 1)

            // Use native system unzip command via POSIX popen - same approach as Android using java.util.zip
            val command = "cd \"$outputDir\" && /usr/bin/unzip -o -q \"$zipPath\" 2>&1"
            val result = executeUnzipCommand(command)

            if (result.exitCode != 0) {
                throw Exception("Native unzip command failed with status ${result.exitCode}: ${result.output}")
            }

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

    /**
     * Execute unzip command using POSIX popen() for Kotlin Native iOS.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun executeUnzipCommand(command: String): CommandResult {
        val fp = popen(command, "r") ?: throw Exception("Failed to execute command: $command")

        val output = buildString {
            val buffer = ByteArray(4096)
            while (true) {
                val input = fgets(buffer.refTo(0), buffer.size, fp) ?: break
                append(input.toKString())
            }
        }

        val exitCode = pclose(fp)
        return CommandResult(exitCode, output.trim())
    }

    private data class CommandResult(
        val exitCode: Int,
        val output: String
    )
}