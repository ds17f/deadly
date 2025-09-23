package com.grateful.deadly.services.search.platform

import com.grateful.deadly.core.util.Logger
import com.grateful.deadly.database.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of ShowSearchDao using SQLDelight FTS4 queries.
 *
 * Uses direct SQLDelight access to FTS4 virtual table for maximum performance.
 * All operations are performed on IO dispatcher for database safety.
 */
actual class ShowSearchDao actual constructor(private val database: Database) {

    companion object {
        private const val TAG = "ShowSearchDao"
    }

    actual suspend fun searchShows(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            Logger.d(TAG, "FTS4 search for query: '$query'")

            // Check if FTS index has any data first
            val indexCount = database.showSearchQueries.getIndexedShowCount().executeAsOne()
            Logger.d(TAG, "FTS index contains $indexCount shows")

            if (indexCount == 0L) {
                Logger.w(TAG, "FTS index is empty - no shows have been indexed yet")
                return@withContext emptyList()
            }

            val results = database.showSearchQueries.searchShows(query).executeAsList()
            Logger.d(TAG, "FTS4 returned ${results.size} show IDs for query: '$query'")
            results.mapNotNull { it.showId }
        } catch (e: Exception) {
            Logger.e(TAG, "FTS4 search failed for query: '$query'", e)
            emptyList()
        }
    }

    actual suspend fun insertOrUpdateShowSearch(showId: String, searchText: String) = withContext(Dispatchers.IO) {
        try {
            database.showSearchQueries.insertOrUpdateShowSearch(showId, searchText)
            Logger.d(TAG, "Indexed show: $showId")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to index show: $showId", e)
        }
    }

    actual suspend fun insertShowSearchBatch(searchRecords: List<Pair<String, String>>) = withContext(Dispatchers.IO) {
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

    actual suspend fun getIndexedShowCount(): Int = withContext(Dispatchers.IO) {
        try {
            database.showSearchQueries.getIndexedShowCount().executeAsOne().toInt()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get indexed show count", e)
            0
        }
    }

    actual suspend fun isShowIndexed(showId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            database.showSearchQueries.isShowIndexed(showId).executeAsOne() > 0
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to check if show is indexed: $showId", e)
            false
        }
    }

    actual suspend fun removeShowFromIndex(showId: String) = withContext(Dispatchers.IO) {
        try {
            database.showSearchQueries.removeShowFromIndex(showId)
            Logger.d(TAG, "Removed show from index: $showId")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to remove show from index: $showId", e)
        }
    }

    actual suspend fun clearAllSearchData() = withContext(Dispatchers.IO) {
        try {
            database.showSearchQueries.clearAllSearchData()
            Logger.d(TAG, "Cleared all search data")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to clear search data", e)
        }
    }
}