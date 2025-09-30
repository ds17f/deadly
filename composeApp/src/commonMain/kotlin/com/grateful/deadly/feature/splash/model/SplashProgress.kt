package com.grateful.deadly.feature.splash.model

import kotlinx.datetime.Clock

/**
 * Progress tracking for splash screen data synchronization.
 *
 * Follows V2 pattern with phase-specific progress tracking,
 * elapsed time display, and current item information.
 */
data class SplashProgress(
    val phase: SyncPhase,
    val currentItems: Int = 0,
    val totalItems: Int = 0,
    val currentItemName: String = "",
    val startTimeMs: Long = 0L,
    val error: String? = null
) {
    /**
     * Progress percentage for current phase (0-100).
     */
    val progressPercentage: Float
        get() = if (totalItems > 0) {
            (currentItems.toFloat() / totalItems) * 100f
        } else {
            0f
        }

    /**
     * Formatted elapsed time string (MM:SS).
     */
    fun getElapsedTimeString(currentTimeMs: Long = Clock.System.now().toEpochMilliseconds()): String {
        if (startTimeMs == 0L) return "00:00"

        val elapsedMs = currentTimeMs - startTimeMs
        val totalSeconds = elapsedMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }

    /**
     * Whether this phase should show progress indicators.
     */
    val isInProgress: Boolean
        get() = phase in listOf(
            SyncPhase.DOWNLOADING,
            SyncPhase.EXTRACTING,
            SyncPhase.IMPORTING_SHOWS,
            SyncPhase.IMPORTING_RECORDINGS
        )
}