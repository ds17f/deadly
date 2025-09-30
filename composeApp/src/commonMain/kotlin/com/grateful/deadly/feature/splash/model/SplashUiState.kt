package com.grateful.deadly.feature.splash.model

/**
 * UI state for Splash screen following V2 pattern.
 *
 * Provides clear separation of different UI states:
 * - Initial loading
 * - Progress tracking with detailed phase information
 * - Error state with retry/skip options
 * - Ready state for navigation
 */
data class SplashUiState(
    val isReady: Boolean = false,
    val showProgress: Boolean = false,
    val showError: Boolean = false,
    val message: String = "Initializing...",
    val errorMessage: String? = null,
    val progress: SplashProgress = SplashProgress(
        phase = SyncPhase.IDLE
    )
)