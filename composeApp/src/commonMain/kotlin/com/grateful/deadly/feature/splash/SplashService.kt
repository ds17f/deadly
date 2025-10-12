package com.grateful.deadly.feature.splash

import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.feature.splash.model.SplashProgress
import com.grateful.deadly.feature.splash.model.SplashUiState
import com.grateful.deadly.feature.splash.model.SyncPhase
import com.grateful.deadly.services.data.DataSyncOrchestrator
import com.grateful.deadly.services.data.SyncProgress as OrchestratorProgress
import com.grateful.deadly.services.data.SyncResult
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

/**
 * Universal service for splash screen coordination following V2 pattern.
 *
 * Transforms DataSyncOrchestrator progress into splash-specific UI state
 * with phase-appropriate messages and progress tracking.
 *
 * Follows Universal Service + Platform Tool pattern:
 * - All business logic universal (commonMain)
 * - Delegates platform-specific operations to DataSyncOrchestrator
 * - Returns standardized UI state for cross-platform Compose UI
 */
class SplashService(
    private val dataSyncOrchestrator: DataSyncOrchestrator
) {
    companion object {
        private const val TAG = "SplashService"
    }

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private var initStartTimeMs: Long = 0L

    /**
     * Initialize data synchronization with progress tracking.
     * Returns true if initialization started, false if data already exists.
     */
    suspend fun initializeData(): Boolean {
        return try {
            Logger.i(TAG, "🔍 Starting data synchronization")
            initStartTimeMs = Clock.System.now().toEpochMilliseconds()

            updateUiState(
                showProgress = true,
                message = "Initializing..."
            )

            // Collect progress updates from orchestrator
            coroutineScope {
                launch {
                    dataSyncOrchestrator.progress.collect { orchestratorProgress ->
                        transformProgress(orchestratorProgress)
                    }
                }

                // Start synchronization
                val result = dataSyncOrchestrator.syncData()

                when (result) {
                    is SyncResult.Success -> {
                        Logger.d(TAG, "Data synchronization completed: ${result.showCount} shows, ${result.recordingCount} recordings")
                        updateUiState(
                            isReady = true,
                            showProgress = false,
                            message = "Ready! Loaded ${result.showCount} shows"
                        )
                        true
                    }
                    is SyncResult.AlreadyExists -> {
                        Logger.d(TAG, "Data already exists: ${result.showCount} shows, ${result.recordingCount} recordings")
                        updateUiState(
                            isReady = true,
                            showProgress = false,
                            message = "Ready! ${result.showCount} shows loaded"
                        )
                        false
                    }
                    is SyncResult.Error -> {
                        Logger.e(TAG, "Data synchronization failed: ${result.message}")
                        updateUiState(
                            showError = true,
                            showProgress = false,
                            message = "Initialization failed",
                            errorMessage = result.message
                        )
                        false
                    }
                    is SyncResult.Cleared -> {
                        Logger.d(TAG, "Data cleared")
                        updateUiState(
                            isReady = true,
                            showProgress = false,
                            message = "Data cleared"
                        )
                        false
                    }
                }
            }

        } catch (e: Exception) {
            Logger.e(TAG, "Data synchronization exception", e)
            updateUiState(
                showError = true,
                showProgress = false,
                message = "Initialization failed",
                errorMessage = e.message
            )
            false
        }
    }

    /**
     * Transform orchestrator progress into splash-specific progress with phase messages.
     */
    private fun transformProgress(orchestratorProgress: OrchestratorProgress) {
        val (phase, message, currentItems, totalItems, currentItemName) = when (orchestratorProgress) {
            is OrchestratorProgress.Idle -> {
                Tuple5(SyncPhase.IDLE, "Preparing...", 0, 0, "")
            }
            is OrchestratorProgress.Downloading -> {
                Tuple5(SyncPhase.DOWNLOADING, "Downloading data files...", 0, 0, "")
            }
            is OrchestratorProgress.Extracting -> {
                Tuple5(SyncPhase.EXTRACTING, "Extracting data files...", 0, 0, "")
            }
            is OrchestratorProgress.ImportingShows -> {
                val current = orchestratorProgress.current
                val total = orchestratorProgress.total
                Tuple5(
                    SyncPhase.IMPORTING_SHOWS,
                    "Importing shows ($current / $total)",
                    current,
                    total,
                    ""
                )
            }
            is OrchestratorProgress.ImportingRecordings -> {
                val current = orchestratorProgress.current
                val total = orchestratorProgress.total
                Tuple5(
                    SyncPhase.IMPORTING_RECORDINGS,
                    "Importing recordings ($current / $total)",
                    current,
                    total,
                    ""
                )
            }
            is OrchestratorProgress.Clearing -> {
                Tuple5(SyncPhase.IDLE, "Clearing data...", 0, 0, "")
            }
        }

        updateUiState(
            showProgress = phase != SyncPhase.IDLE,
            message = message,
            progress = SplashProgress(
                phase = phase,
                currentItems = currentItems,
                totalItems = totalItems,
                currentItemName = currentItemName,
                startTimeMs = initStartTimeMs
            )
        )
    }

    /**
     * Retry initialization after error.
     */
    suspend fun retry(): Boolean {
        Logger.d(TAG, "Retrying initialization")
        updateUiState(
            showError = false,
            showProgress = true,
            message = "Retrying...",
            errorMessage = null
        )
        return initializeData()
    }

    /**
     * Skip initialization and proceed to app.
     */
    fun skip() {
        Logger.d(TAG, "Skipping initialization")
        updateUiState(
            isReady = true,
            showError = false,
            showProgress = false,
            message = "Skipped initialization"
        )
    }

    /**
     * Update UI state with optional parameters.
     */
    private fun updateUiState(
        isReady: Boolean = _uiState.value.isReady,
        showProgress: Boolean = _uiState.value.showProgress,
        showError: Boolean = _uiState.value.showError,
        message: String = _uiState.value.message,
        errorMessage: String? = _uiState.value.errorMessage,
        progress: SplashProgress = _uiState.value.progress
    ) {
        _uiState.value = SplashUiState(
            isReady = isReady,
            showProgress = showProgress,
            showError = showError,
            message = message,
            errorMessage = errorMessage,
            progress = progress
        )
    }
}

/**
 * Helper class for multi-value returns.
 */
private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)