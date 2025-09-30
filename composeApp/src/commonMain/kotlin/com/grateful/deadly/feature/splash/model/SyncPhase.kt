package com.grateful.deadly.feature.splash.model

/**
 * Phases of data synchronization for splash screen progress tracking.
 *
 * Maps to DataSyncOrchestrator workflow phases following V2 pattern.
 */
enum class SyncPhase {
    IDLE,
    DOWNLOADING,
    EXTRACTING,
    IMPORTING_SHOWS,
    IMPORTING_RECORDINGS,
    COMPLETED,
    ERROR
}