package com.grateful.deadly.services.data.platform

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.grateful.deadly.database.Database
import com.grateful.deadly.domain.mappers.ShowMappers
import com.grateful.deadly.domain.models.Recording
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.domain.models.ShowFilters
import com.grateful.deadly.domain.models.ShowOrderBy
import com.grateful.deadly.services.data.models.RecordingEntity
import com.grateful.deadly.services.data.models.ShowEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Android implementation of ShowRepository using SQLDelight database.
 *
 * This platform tool handles database operations and entity↔domain conversion.
 * Uses one flexible SQLDelight query with type-safe filters.
 */
actual class ShowRepository actual constructor(
    private val database: Database
) : KoinComponent {
    private val showMappers = ShowMappers()

    // === Core Operations ===

    actual suspend fun insertShow(show: ShowEntity) = withContext(Dispatchers.IO) {
        database.showQueries.insertShow(
            showId = show.showId,
            date = show.date,
            year = show.year.toLong(),
            month = show.month.toLong(),
            yearMonth = show.yearMonth,
            band = show.band,
            url = show.url,
            venueName = show.venueName,
            city = show.city,
            state = show.state,
            country = show.country,
            locationRaw = show.locationRaw,
            setlistStatus = show.setlistStatus,
            setlistRaw = show.setlistRaw,
            songList = show.songList,
            lineupStatus = show.lineupStatus,
            lineupRaw = show.lineupRaw,
            memberList = show.memberList,
            showSequence = show.showSequence.toLong(),
            recordingsRaw = show.recordingsRaw,
            recordingCount = show.recordingCount.toLong(),
            bestRecordingId = show.bestRecordingId,
            averageRating = show.averageRating,
            totalReviews = show.totalReviews.toLong(),
            isInLibrary = if (show.isInLibrary) 1L else 0L,
            libraryAddedAt = show.libraryAddedAt,
            createdAt = show.createdAt,
            updatedAt = show.updatedAt
        )
    }

    actual suspend fun insertShows(shows: List<ShowEntity>) = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                shows.forEach { show ->
                    database.showQueries.insertShow(
                        showId = show.showId,
                        date = show.date,
                        year = show.year.toLong(),
                        month = show.month.toLong(),
                        yearMonth = show.yearMonth,
                        band = show.band,
                        url = show.url,
                        venueName = show.venueName,
                        city = show.city,
                        state = show.state,
                        country = show.country,
                        locationRaw = show.locationRaw,
                        setlistStatus = show.setlistStatus,
                        setlistRaw = show.setlistRaw,
                        songList = show.songList,
                        lineupStatus = show.lineupStatus,
                        lineupRaw = show.lineupRaw,
                        memberList = show.memberList,
                        showSequence = show.showSequence.toLong(),
                        recordingsRaw = show.recordingsRaw,
                        recordingCount = show.recordingCount.toLong(),
                        bestRecordingId = show.bestRecordingId,
                        averageRating = show.averageRating,
                        totalReviews = show.totalReviews.toLong(),
                        isInLibrary = if (show.isInLibrary) 1L else 0L,
                        libraryAddedAt = show.libraryAddedAt,
                        createdAt = show.createdAt,
                        updatedAt = show.updatedAt
                    )
                }
            }
        } catch (e: Exception) {
            throw Exception("Database schema mismatch or insert failed. Try clearing the database first. Error: ${e.message}", e)
        }
    }

    actual suspend fun getShowById(showId: String): Show? = withContext(Dispatchers.IO) {
        val showRow = database.showQueries.selectShowById(showId).executeAsOneOrNull()
        showRow?.let { row ->
            val entity = mapRowToEntity(row)
            showMappers.entityToDomain(entity)
        }
    }

    actual suspend fun getShowsByIds(showIds: List<String>): List<Show> = withContext(Dispatchers.IO) {
        if (showIds.isEmpty()) return@withContext emptyList()

        // Preserve the order of showIds for FTS4 relevance ranking
        val shows = showIds.mapNotNull { showId ->
            getShowById(showId)
        }
        shows
    }

    /**
     * Main query method using flexible filters.
     * All other query methods delegate to this one.
     */
    actual suspend fun getShows(filters: ShowFilters): List<Show> = withContext(Dispatchers.IO) {
        val showRows = database.showQueries.getShows(
            year = filters.year?.toLong(),
            startYear = filters.startYear?.toLong(),
            endYear = filters.endYear?.toLong(),
            yearMonth = filters.yearMonth,
            startDate = filters.startDate,
            endDate = filters.endDate,
            venueName = filters.venueName,
            city = filters.city,
            state = filters.state,
            hasSetlist = filters.hasSetlist?.let { if (it) 1L else 0L },
            songName = filters.songName,
            hasLineup = filters.hasLineup?.let { if (it) 1L else 0L },
            memberName = filters.memberName,
            hasRecordings = filters.hasRecordings?.let { if (it) 1L else 0L },
            minRating = filters.minRating,
            minReviews = filters.minReviews?.toLong(),
            isInLibrary = filters.isInLibrary?.let { if (it) 1L else 0L },
            orderBy = filters.orderBy.value,
            limit = filters.limit?.toLong()
        ).executeAsList()

        val entities = showRows.map { row -> mapRowToEntity(row) }
        showMappers.entitiesToDomain(entities)
    }

    actual suspend fun getShowCount(): Long = withContext(Dispatchers.IO) {
        database.showQueries.getShowCount().executeAsOne()
    }

    actual suspend fun deleteAllShows() = withContext(Dispatchers.IO) {
        database.showQueries.deleteAllShows()
    }

    actual suspend fun deleteDatabaseFile(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Use Context to get the proper database path
            val context = get<Context>()
            val dbFile = context.getDatabasePath("deadly.db")

            // Delete the database file - Android will recreate it on next access
            val deleted = if (dbFile.exists()) {
                dbFile.delete()
            } else {
                true // Already doesn't exist
            }

            deleted
        } catch (e: Exception) {
            // Fallback: just clear the table data
            try {
                database.showQueries.deleteAllShows()
                true
            } catch (fallbackException: Exception) {
                false
            }
        }
    }

    actual suspend fun updateShowLibraryStatus(showId: String, isInLibrary: Boolean, addedAt: Long?) = withContext(Dispatchers.IO) {
        database.showQueries.updateShowLibraryStatus(
            isInLibrary = if (isInLibrary) 1L else 0L,
            libraryAddedAt = addedAt,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            showId = showId
        )
    }

    // === Convenience Methods (wrappers around getShows) ===

    actual suspend fun getAllShows(): List<Show> = getShows(ShowFilters.all())

    actual suspend fun getShowsByYear(year: Int): List<Show> = getShows(ShowFilters.forYear(year))

    actual suspend fun getShowsByYearRange(startYear: Int, endYear: Int): List<Show> =
        getShows(ShowFilters.forYearRange(startYear, endYear))

    actual suspend fun getShowsByYearMonth(yearMonth: String): List<Show> =
        getShows(ShowFilters.forYearMonth(yearMonth))

    actual suspend fun getShowsInDateRange(startDate: String, endDate: String): List<Show> =
        getShows(ShowFilters.forDateRange(startDate, endDate))

    actual suspend fun getShowsForDate(month: Int, day: Int): List<Show> = withContext(Dispatchers.IO) {
        val showRows = database.showQueries.getShowsForDate(month.toLong(), day.toLong()).executeAsList()
        val entities = showRows.map { row -> mapRowToEntity(row) }
        showMappers.entitiesToDomain(entities)
    }

    actual suspend fun getShowsByVenue(venueName: String): List<Show> =
        getShows(ShowFilters.forVenue(venueName))

    actual suspend fun getShowsByCity(city: String): List<Show> =
        getShows(ShowFilters.forCity(city))

    actual suspend fun getShowsByState(state: String): List<Show> =
        getShows(ShowFilters.forState(state))

    actual suspend fun getShowsWithSetlists(): List<Show> =
        getShows(ShowFilters.withSetlists())

    actual suspend fun getShowsBySong(songName: String): List<Show> =
        getShows(ShowFilters.withSong(songName))

    actual suspend fun getShowsWithLineups(): List<Show> =
        getShows(ShowFilters.withLineups())

    actual suspend fun getShowsByMember(memberName: String): List<Show> =
        getShows(ShowFilters.withMember(memberName))

    actual suspend fun getShowsWithRecordings(): List<Show> =
        getShows(ShowFilters.withRecordings())

    actual suspend fun getTopRatedShows(minReviews: Int, limit: Int): List<Show> =
        getShows(ShowFilters.topRated(minReviews, limit))

    actual suspend fun getLibraryShows(): List<Show> =
        getShows(ShowFilters.inLibrary())

    actual suspend fun searchShows(query: String): List<Show> =
        getShows(ShowFilters(venueName = query, city = query, songName = query, memberName = query))

    // === Statistics ===

    actual suspend fun getShowCountByYear(): Map<Int, Int> = withContext(Dispatchers.IO) {
        val results = database.showQueries.getShowCountByYear().executeAsList()
        results.associate { it.year.toInt() to it.count.toInt() }
    }

    actual suspend fun getShowCountByVenue(): Map<String, Int> = withContext(Dispatchers.IO) {
        val results = database.showQueries.getShowCountByVenue().executeAsList()
        results.associate { it.venueName to it.count.toInt() }
    }

    actual suspend fun getAverageRatingStats(): RatingStats = withContext(Dispatchers.IO) {
        val result = database.showQueries.getAverageRatingStats().executeAsOne()
        RatingStats(
            totalShows = result.totalShows.toInt(),
            ratedShows = result.ratedShows.toInt(),
            averageRating = result.avgRating,
            minRating = result.minRating,
            maxRating = result.maxRating
        )
    }

    // === Helper Functions ===

    /**
     * Convert SQLDelight row to ShowEntity.
     * Handles platform-specific type conversions (Long ↔ Int, Boolean mapping).
     */
    private fun mapRowToEntity(row: com.grateful.deadly.database.Show): ShowEntity {
        return ShowEntity(
            showId = row.showId,
            date = row.date,
            year = row.year.toInt(),
            month = row.month.toInt(),
            yearMonth = row.yearMonth,
            band = row.band,
            url = row.url,
            venueName = row.venueName,
            city = row.city,
            state = row.state,
            country = row.country,
            locationRaw = row.locationRaw,
            setlistStatus = row.setlistStatus,
            setlistRaw = row.setlistRaw,
            songList = row.songList,
            lineupStatus = row.lineupStatus,
            lineupRaw = row.lineupRaw,
            memberList = row.memberList,
            showSequence = row.showSequence.toInt(),
            recordingsRaw = row.recordingsRaw,
            recordingCount = row.recordingCount.toInt(),
            bestRecordingId = row.bestRecordingId,
            averageRating = row.averageRating,
            totalReviews = row.totalReviews.toInt(),
            isInLibrary = row.isInLibrary == 1L,
            libraryAddedAt = row.libraryAddedAt,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt
        )
    }

    // === Recording Operations (V2 Single-Repository Pattern) ===

    actual suspend fun insertRecording(recording: RecordingEntity) = withContext(Dispatchers.IO) {
        database.recordingQueries.insertRecording(
            identifier = recording.identifier,
            showId = recording.showId,
            sourceType = recording.sourceType,
            taper = recording.taper,
            source = recording.source,
            lineage = recording.lineage,
            sourceTypeString = recording.sourceTypeString,
            rating = recording.rating,
            rawRating = recording.rawRating,
            reviewCount = recording.reviewCount.toLong(),
            confidence = recording.confidence,
            highRatings = recording.highRatings.toLong(),
            lowRatings = recording.lowRatings.toLong(),
            collectionTimestamp = recording.collectionTimestamp
        )
    }

    actual suspend fun insertRecordings(recordings: List<RecordingEntity>) = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                recordings.forEach { recording ->
                    database.recordingQueries.insertRecording(
                        identifier = recording.identifier,
                        showId = recording.showId,
                        sourceType = recording.sourceType,
                        taper = recording.taper,
                        source = recording.source,
                        lineage = recording.lineage,
                        sourceTypeString = recording.sourceTypeString,
                        rating = recording.rating,
                        rawRating = recording.rawRating,
                        reviewCount = recording.reviewCount.toLong(),
                        confidence = recording.confidence,
                        highRatings = recording.highRatings.toLong(),
                        lowRatings = recording.lowRatings.toLong(),
                        collectionTimestamp = recording.collectionTimestamp
                    )
                }
            }
        } catch (e: Exception) {
            throw Exception("Failed to insert recordings: ${e.message}", e)
        }
    }

    actual suspend fun getRecordingById(identifier: String): Recording? = withContext(Dispatchers.IO) {
        val recordingRow = database.recordingQueries.selectRecordingById(identifier).executeAsOneOrNull()
        recordingRow?.let { row ->
            val entity = mapRecordingRowToEntity(row)
            showMappers.recordingEntityToDomain(entity)
        }
    }

    actual suspend fun getRecordingsForShow(showId: String): List<Recording> = withContext(Dispatchers.IO) {
        val recordingRows = database.recordingQueries.selectRecordingsForShow(showId).executeAsList()
        val entities = recordingRows.map { row -> mapRecordingRowToEntity(row) }
        showMappers.recordingEntitiesToDomain(entities)
    }

    actual suspend fun getBestRecordingForShow(showId: String): Recording? = withContext(Dispatchers.IO) {
        val recordingRow = database.recordingQueries.selectBestRecordingForShow(showId).executeAsOneOrNull()
        recordingRow?.let { row ->
            val entity = mapRecordingRowToEntity(row)
            showMappers.recordingEntityToDomain(entity)
        }
    }

    actual suspend fun getRecordingsBySourceType(sourceType: String): List<Recording> = withContext(Dispatchers.IO) {
        val recordingRows = database.recordingQueries.selectRecordingsBySourceType(sourceType).executeAsList()
        val entities = recordingRows.map { row -> mapRecordingRowToEntity(row) }
        showMappers.recordingEntitiesToDomain(entities)
    }

    actual suspend fun getTopRatedRecordings(minRating: Double, minReviews: Int, limit: Int): List<Recording> = withContext(Dispatchers.IO) {
        val recordingRows = database.recordingQueries.selectTopRatedRecordings(minRating, minReviews.toLong(), limit.toLong()).executeAsList()
        val entities = recordingRows.map { row -> mapRecordingRowToEntity(row) }
        showMappers.recordingEntitiesToDomain(entities)
    }

    actual suspend fun getRecordingCount(): Long = withContext(Dispatchers.IO) {
        database.recordingQueries.getRecordingCount().executeAsOne()
    }

    actual suspend fun getRecordingCountForShow(showId: String): Long = withContext(Dispatchers.IO) {
        database.recordingQueries.getRecordingCountForShow(showId).executeAsOne()
    }

    actual suspend fun deleteAllRecordings() = withContext(Dispatchers.IO) {
        database.recordingQueries.deleteAllRecordings()
    }

    actual suspend fun deleteRecordingsForShow(showId: String) = withContext(Dispatchers.IO) {
        database.recordingQueries.deleteRecordingsForShow(showId)
    }

    /**
     * Convert SQLDelight recording row to RecordingEntity.
     * Handles platform-specific type conversions (Long ↔ Int).
     */
    private fun mapRecordingRowToEntity(row: com.grateful.deadly.database.Recording): RecordingEntity {
        return RecordingEntity(
            identifier = row.identifier,
            showId = row.showId,
            sourceType = row.sourceType,
            taper = row.taper,
            source = row.source,
            lineage = row.lineage,
            sourceTypeString = row.sourceTypeString,
            rating = row.rating,
            rawRating = row.rawRating,
            reviewCount = row.reviewCount.toInt(),
            confidence = row.confidence,
            highRatings = row.highRatings.toInt(),
            lowRatings = row.lowRatings.toInt(),
            collectionTimestamp = row.collectionTimestamp
        )
    }
}