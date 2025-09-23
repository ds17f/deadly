package com.grateful.deadly.data.search

import com.grateful.deadly.database.Database
import com.grateful.deadly.database.Show as ShowRow
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.domain.models.Venue
import com.grateful.deadly.domain.models.Location
import com.grateful.deadly.core.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList

/**
 * SQLDelight implementation of SearchRepository.
 * Maps between SQLDelight database rows and domain models.
 */
class SearchRepositoryImpl(
    private val database: Database
) : SearchRepository {

    companion object {
        private const val TAG = "SearchRepositoryImpl"
    }

    override suspend fun searchShows(query: String): List<Show> = withContext(Dispatchers.IO) {
        try {
            Logger.d(TAG, "Searching shows with query: '$query'")

            val results = database.showQueries.searchShows(
                query, query, query, query, query, query,  // 6 query parameters for LIKE clauses
                query, query, query  // 3 query parameters for CASE ordering
            ).executeAsList()

            Logger.d(TAG, "Found ${results.size} shows matching query")
            results.map { mapRowToShow(it) }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to search shows", e)
            emptyList()
        }
    }

    override suspend fun getShowById(id: String): Show? = withContext(Dispatchers.IO) {
        try {
            val result = database.showQueries.selectShowById(id).executeAsOneOrNull()
            result?.let { mapRowToShow(it) }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get show by id: $id", e)
            null
        }
    }

    override suspend fun getAllShows(): List<Show> = withContext(Dispatchers.IO) {
        try {
            val results = database.showQueries.selectAllShows().executeAsList()
            Logger.d(TAG, "Retrieved ${results.size} shows from database")
            results.map { mapRowToShow(it) }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get all shows", e)
            emptyList()
        }
    }

    override fun getAllShowsFlow(): Flow<List<Show>> {
        return database.showQueries.selectAllShows()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { mapRowToShow(it) } }
    }

    override suspend fun getShowCount(): Long = withContext(Dispatchers.IO) {
        try {
            database.showQueries.getShowCount().executeAsOne()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get show count", e)
            0L
        }
    }

    /**
     * Map SQLDelight row to domain Show model.
     * Follows the same structure as SearchServiceStub.mockShows for compatibility.
     */
    private fun mapRowToShow(row: ShowRow): Show {
        return Show(
            id = row.showId,
            date = row.date,
            year = row.year.toInt(),
            band = row.band,
            venue = Venue(
                name = row.venueName,
                city = row.city,
                state = row.state,
                country = row.country
            ),
            location = Location.fromRaw(
                raw = row.locationRaw ?: "${row.city ?: ""}, ${row.state ?: ""}".trim(',', ' '),
                city = row.city,
                state = row.state
            ),
            setlist = null, // TODO: Parse setlistRaw JSON when available
            lineup = null, // TODO: Parse lineupRaw JSON when available
            recordingIds = emptyList(), // TODO: Parse recording IDs when available
            bestRecordingId = row.bestRecordingId,
            recordingCount = row.recordingCount.toInt(),
            averageRating = row.averageRating?.toFloat(),
            totalReviews = row.totalReviews.toInt(),
            isInLibrary = row.isInLibrary == 1L,
            libraryAddedAt = row.libraryAddedAt
        )
    }
}