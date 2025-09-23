package com.grateful.deadly.services.search.platform

import com.grateful.deadly.core.util.Logger
import com.grateful.deadly.database.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// iOS: Use Dispatchers.Default instead of Dispatchers.IO for historical compatibility.
// While Dispatchers.IO is available on Kotlin/Native since kotlinx-coroutines 1.7.0,
// we maintain Dispatchers.Default to avoid potential compatibility issues.
// Current version: 1.8.1, Latest: 1.10.2

/**
 * iOS implementation of ShowSearchDao using SQLDelight FTS4 queries.
 *
 * Identical to Android implementation since SQLDelight provides cross-platform
 * database access. FTS4 is supported natively in iOS SQLite.
 */
actual class ShowSearchDao actual constructor(private val database: Database) {

    companion object {
        private const val TAG = "ShowSearchDao"
    }

    actual suspend fun searchShows(query: String): List<String> = withContext(Dispatchers.Default) {
        try {
            Logger.d(TAG, "FTS4 search for query: '$query'")
            val results = database.showSearchQueries.searchShows(query).executeAsList()
            Logger.d(TAG, "FTS4 returned ${results.size} show IDs")
            results.mapNotNull { it.showId }
        } catch (e: Exception) {
            Logger.e(TAG, "FTS4 search failed for query: '$query'", e)
            emptyList()
        }
    }

    actual suspend fun insertOrUpdateShowSearch(showId: String, searchText: String) = withContext(Dispatchers.Default) {
        try {
            database.showSearchQueries.insertOrUpdateShowSearch(showId, searchText)
            Logger.d(TAG, "Indexed show: $showId")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to index show: $showId", e)
        }
    }

    actual suspend fun insertShowSearchBatch(searchRecords: List<Pair<String, String>>) = withContext(Dispatchers.Default) {
        try {
            database.transaction {
                searchRecords.forEach { (showId, searchText) ->
                    database.showSearchQueries.insertOrUpdateShowSearch(showId, searchText)
                }
            }
            Logger.d(TAG, "Batch indexed ${searchRecords.size} shows")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to batch index ${searchRecords.size} shows", e)
        }
    }

    actual suspend fun getIndexedShowCount(): Int = withContext(Dispatchers.Default) {
        try {
            database.showSearchQueries.getIndexedShowCount().executeAsOne().toInt()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get indexed show count", e)
            0
        }
    }

    actual suspend fun isShowIndexed(showId: String): Boolean = withContext(Dispatchers.Default) {
        try {
            database.showSearchQueries.isShowIndexed(showId).executeAsOne() > 0
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to check if show is indexed: $showId", e)
            false
        }
    }

    actual suspend fun removeShowFromIndex(showId: String) = withContext(Dispatchers.Default) {
        try {
            database.showSearchQueries.removeShowFromIndex(showId)
            Logger.d(TAG, "Removed show from index: $showId")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to remove show from index: $showId", e)
        }
    }

    actual suspend fun clearAllSearchData() = withContext(Dispatchers.Default) {
        try {
            database.showSearchQueries.clearAllSearchData()
            Logger.d(TAG, "Cleared all search data")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to clear search data", e)
        }
    }
}