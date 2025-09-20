package com.grateful.deadly.services.data.models

/**
 * Represents a file extracted from a ZIP archive.
 */
data class ExtractedFile(
    val path: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long
)

/**
 * Progress tracking for file extraction operations.
 */
sealed class ExtractionProgress {
    object Started : ExtractionProgress()
    data class Progress(val current: Int, val total: Int) : ExtractionProgress() {
        val percent: Float get() = if (total > 0) current.toFloat() / total else 0f
    }
    object Completed : ExtractionProgress()
}

/**
 * Result of a file extraction operation.
 */
sealed class ExtractionResult {
    data class Success(val extractedFiles: List<ExtractedFile>) : ExtractionResult()
    data class Error(val message: String) : ExtractionResult()
}