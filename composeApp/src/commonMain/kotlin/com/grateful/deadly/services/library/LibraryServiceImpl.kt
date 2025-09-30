package com.grateful.deadly.services.library

import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.domain.models.LibraryShow
import com.grateful.deadly.domain.models.LibraryStats
import com.grateful.deadly.services.library.platform.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock

/**
 * Universal LibraryService implementation (V2 pattern)
 *
 * Provides business logic and reactive StateFlows for library operations.
 * All logic is in commonMain - delegates to platform LibraryRepository.
 * Uses Result-based error handling and comprehensive logging.
 */
class LibraryServiceImpl(
    private val libraryRepository: LibraryRepository,
    private val coroutineScope: CoroutineScope
) : LibraryService {

    companion object {
        private const val TAG = "LibraryService"
        private const val STATEFLOW_TIMEOUT = 5000L // 5 seconds
    }

    // === Reactive StateFlows (V2 pattern) ===

    /**
     * Real reactive StateFlow backed by database (V2 pattern)
     * Automatically emits updates when library changes
     */
    private val _currentShows: StateFlow<List<LibraryShow>> = libraryRepository
        .getLibraryShowsFlow()
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(STATEFLOW_TIMEOUT),
            initialValue = emptyList()
        )

    /**
     * Real reactive StateFlow for library statistics (V2 pattern)
     */
    private val _libraryStats: StateFlow<LibraryStats> = libraryRepository
        .getLibraryStatsFlow()
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(STATEFLOW_TIMEOUT),
            initialValue = LibraryStats(totalShows = 0, totalPinned = 0)
        )

    // === Public Interface ===

    override fun getCurrentShows(): StateFlow<List<LibraryShow>> {
        Logger.d(TAG, "getCurrentShows() - returning StateFlow with ${_currentShows.value.size} shows")
        return _currentShows
    }

    override fun getLibraryStats(): StateFlow<LibraryStats> {
        Logger.d(TAG, "getLibraryStats() - returning StateFlow with stats: ${_libraryStats.value}")
        return _libraryStats
    }

    override suspend fun addToLibrary(showId: String): Result<Unit> {
        Logger.d(TAG, "addToLibrary() - Adding show '$showId' to library")
        val timestamp = Clock.System.now().toEpochMilliseconds()

        return libraryRepository.addShowToLibrary(showId, timestamp).also { result ->
            if (result.isSuccess) {
                Logger.i(TAG, "addToLibrary() - Successfully added show '$showId' to library")
            } else {
                Logger.e(TAG, "addToLibrary() - Failed to add show '$showId' to library: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    override suspend fun removeFromLibrary(showId: String): Result<Unit> {
        Logger.d(TAG, "removeFromLibrary() - Removing show '$showId' from library")

        return libraryRepository.removeShowFromLibrary(showId).also { result ->
            if (result.isSuccess) {
                Logger.i(TAG, "removeFromLibrary() - Successfully removed show '$showId' from library")
            } else {
                Logger.e(TAG, "removeFromLibrary() - Failed to remove show '$showId' from library: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    override suspend fun clearLibrary(): Result<Unit> {
        Logger.d(TAG, "clearLibrary() - Clearing entire library")

        return libraryRepository.clearLibrary().also { result ->
            if (result.isSuccess) {
                Logger.i(TAG, "clearLibrary() - Successfully cleared entire library")
            } else {
                Logger.e(TAG, "clearLibrary() - Failed to clear library: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    override fun isShowInLibrary(showId: String): StateFlow<Boolean> {
        Logger.d(TAG, "isShowInLibrary() - Creating reactive StateFlow for show '$showId'")

        return libraryRepository.isShowInLibraryFlow(showId)
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(STATEFLOW_TIMEOUT),
                initialValue = false
            )
    }

    override suspend fun pinShow(showId: String): Result<Unit> {
        Logger.d(TAG, "pinShow() - Pinning show '$showId'")

        return libraryRepository.updatePinStatus(showId, isPinned = true).also { result ->
            if (result.isSuccess) {
                Logger.i(TAG, "pinShow() - Successfully pinned show '$showId'")
            } else {
                Logger.e(TAG, "pinShow() - Failed to pin show '$showId': ${result.exceptionOrNull()?.message}")
            }
        }
    }

    override suspend fun unpinShow(showId: String): Result<Unit> {
        Logger.d(TAG, "unpinShow() - Unpinning show '$showId'")

        return libraryRepository.updatePinStatus(showId, isPinned = false).also { result ->
            if (result.isSuccess) {
                Logger.i(TAG, "unpinShow() - Successfully unpinned show '$showId'")
            } else {
                Logger.e(TAG, "unpinShow() - Failed to unpin show '$showId': ${result.exceptionOrNull()?.message}")
            }
        }
    }

    override fun isShowPinned(showId: String): StateFlow<Boolean> {
        Logger.d(TAG, "isShowPinned() - Creating reactive StateFlow for show '$showId'")

        return libraryRepository.isShowPinnedFlow(showId)
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(STATEFLOW_TIMEOUT),
                initialValue = false
            )
    }

    override suspend fun updateLibraryNotes(showId: String, notes: String?): Result<Unit> {
        Logger.d(TAG, "updateLibraryNotes() - Updating notes for show '$showId'")

        return libraryRepository.updateLibraryNotes(showId, notes).also { result ->
            if (result.isSuccess) {
                Logger.i(TAG, "updateLibraryNotes() - Successfully updated notes for show '$showId'")
            } else {
                Logger.e(TAG, "updateLibraryNotes() - Failed to update notes for show '$showId': ${result.exceptionOrNull()?.message}")
            }
        }
    }

    override suspend fun shareShow(showId: String): Result<Unit> {
        Logger.d(TAG, "shareShow() - Sharing show '$showId' (placeholder implementation)")

        // TODO: Implement platform-specific sharing
        // This would delegate to a platform-specific sharing service
        // For now, return success as placeholder
        Logger.i(TAG, "shareShow() - Share show '$showId' (not yet implemented)")
        return Result.success(Unit)
    }

    override suspend fun unpinAllShows(): Result<Unit> {
        Logger.d(TAG, "unpinAllShows() - Unpinning all shows in library")

        return libraryRepository.unpinAllShows().also { result ->
            if (result.isSuccess) {
                Logger.i(TAG, "unpinAllShows() - Successfully unpinned all shows")
            } else {
                Logger.e(TAG, "unpinAllShows() - Failed to unpin all shows: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}