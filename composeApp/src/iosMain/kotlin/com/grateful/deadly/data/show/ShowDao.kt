package com.grateful.deadly.data.show

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.grateful.deadly.database.Database
import com.grateful.deadly.services.data.models.RecordingEntity
import com.grateful.deadly.services.data.models.ShowEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * iOS implementation of ShowDao (Platform Tool)
 *
 * Handles only raw database operations using Dispatchers.Default.
 * All business logic (entity-to-domain mapping, complex filtering) is in ShowService.
 */
actual class ShowDao actual constructor(
    private val database: Database
) {

    // === Raw Database Operations ===

    actual suspend fun insertShow(show: ShowEntity) = withContext(Dispatchers.Default) {
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

    actual suspend fun insertShows(shows: List<ShowEntity>) = withContext(Dispatchers.Default) {
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
    }

    actual suspend fun getShowEntityById(showId: String): com.grateful.deadly.database.Show? = withContext(Dispatchers.Default) {
        database.showQueries.selectShowById(showId).executeAsOneOrNull()
    }

    actual suspend fun getShowEntitiesByIds(showIds: List<String>): List<com.grateful.deadly.database.Show> = withContext(Dispatchers.Default) {
        if (showIds.isEmpty()) return@withContext emptyList()
        showIds.mapNotNull { showId ->
            database.showQueries.selectShowById(showId).executeAsOneOrNull()
        }
    }

    actual suspend fun getAllShowEntities(): List<com.grateful.deadly.database.Show> = withContext(Dispatchers.Default) {
        database.showQueries.selectAllShows().executeAsList()
    }

    actual suspend fun getShowCount(): Long = withContext(Dispatchers.Default) {
        database.showQueries.getShowCount().executeAsOne()
    }

    actual suspend fun deleteAllShows() = withContext(Dispatchers.Default) {
        database.showQueries.deleteAllShows()
    }

    actual suspend fun updateShowLibraryStatus(showId: String, isInLibrary: Boolean, addedAt: Long?) = withContext(Dispatchers.Default) {
        database.showQueries.updateShowLibraryStatus(
            isInLibrary = if (isInLibrary) 1L else 0L,
            libraryAddedAt = addedAt,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            showId = showId
        )
    }

    // === Raw Recording Database Operations ===

    actual suspend fun insertRecording(recording: RecordingEntity) = withContext(Dispatchers.Default) {
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

    actual suspend fun insertRecordings(recordings: List<RecordingEntity>) = withContext(Dispatchers.Default) {
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
    }

    actual suspend fun getRecordingEntityById(identifier: String): com.grateful.deadly.database.Recording? = withContext(Dispatchers.Default) {
        database.recordingQueries.selectRecordingById(identifier).executeAsOneOrNull()
    }

    actual suspend fun getRecordingEntitiesForShow(showId: String): List<com.grateful.deadly.database.Recording> = withContext(Dispatchers.Default) {
        database.recordingQueries.selectRecordingsForShow(showId).executeAsList()
    }

    actual suspend fun getRecordingCount(): Long = withContext(Dispatchers.Default) {
        database.recordingQueries.getRecordingCount().executeAsOne()
    }

    actual suspend fun deleteAllRecordings() = withContext(Dispatchers.Default) {
        database.recordingQueries.deleteAllRecordings()
    }

    actual suspend fun deleteRecordingsForShow(showId: String) = withContext(Dispatchers.Default) {
        database.recordingQueries.deleteRecordingsForShow(showId)
    }

    // === Raw Recent Shows Database Operations ===

    actual suspend fun recordShowPlay(showId: String, playTimestamp: Long, recordingId: String?) = withContext(Dispatchers.Default) {
        database.recentShowsQueries.insertOrUpdateRecentShow(
            showId, playTimestamp, showId, playTimestamp, showId, recordingId
        )
    }

    actual suspend fun getRecentShowEntities(limit: Int): List<com.grateful.deadly.database.Show> = withContext(Dispatchers.Default) {
        database.recentShowsQueries.getRecentShows(limit.toLong())
            .executeAsList()
            .mapNotNull { recentShow ->
                database.showQueries.selectShowById(recentShow.showId).executeAsOneOrNull()
            }
    }

    actual fun getRecentShowEntitiesFlow(limit: Int): Flow<List<com.grateful.deadly.database.Show>> {
        return database.recentShowsQueries.getRecentShows(limit.toLong())
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { recentShows ->
                recentShows.mapNotNull { recentShow ->
                    database.showQueries.selectShowById(recentShow.showId).executeAsOneOrNull()
                }
            }
    }

    actual suspend fun removeRecentShow(showId: String) = withContext(Dispatchers.Default) {
        database.recentShowsQueries.removeRecentShow(showId)
    }

    actual suspend fun clearAllRecentShows() = withContext(Dispatchers.Default) {
        database.recentShowsQueries.clearAllRecentShows()
    }

    actual suspend fun cleanupOldRecentShows(keepCount: Int) = withContext(Dispatchers.Default) {
        database.recentShowsQueries.cleanupOldRecentShows(keepCount.toLong())
    }

    // === Raw Query Operations (SQLDelight direct) ===

    actual suspend fun executeShowQuery(
        yearFilter: Pair<Int, Int>?,
        monthFilter: Int?,
        venueFilter: String?,
        cityFilter: String?,
        stateFilter: String?,
        hasSetlist: Boolean?,
        hasLineup: Boolean?,
        hasRecordings: Boolean?,
        isInLibrary: Boolean?,
        minRating: Double?,
        minReviews: Int?,
        songFilter: String?,
        memberFilter: String?,
        orderBy: String,
        ascending: Boolean,
        limit: Int?
    ): List<com.grateful.deadly.database.Show> = withContext(Dispatchers.Default) {
        // Use the flexible SQLDelight query
        database.showQueries.getShows(
            year = yearFilter?.first?.toLong(),
            startYear = yearFilter?.first?.toLong(),
            endYear = yearFilter?.second?.toLong(),
            yearMonth = null,
            startDate = null,
            endDate = null,
            venueName = venueFilter,
            city = cityFilter,
            state = stateFilter,
            hasSetlist = hasSetlist?.let { if (it) 1L else 0L },
            songName = songFilter,
            hasLineup = hasLineup?.let { if (it) 1L else 0L },
            memberName = memberFilter,
            hasRecordings = hasRecordings?.let { if (it) 1L else 0L },
            minRating = minRating,
            minReviews = minReviews?.toLong(),
            isInLibrary = isInLibrary?.let { if (it) 1L else 0L },
            orderBy = orderBy,
            limit = limit?.toLong()
        ).executeAsList()
    }

    actual suspend fun getShowCountWithFilters(
        yearFilter: Pair<Int, Int>?,
        monthFilter: Int?,
        venueFilter: String?,
        cityFilter: String?,
        stateFilter: String?,
        hasSetlist: Boolean?,
        hasLineup: Boolean?,
        hasRecordings: Boolean?,
        isInLibrary: Boolean?,
        minRating: Double?,
        minReviews: Int?
    ): Long = withContext(Dispatchers.Default) {
        // This would need a corresponding SQLDelight query for counting with filters
        // For now, return basic count
        database.showQueries.getShowCount().executeAsOne()
    }

    actual suspend fun getShowCountByYear(): List<Pair<Int, Int>> = withContext(Dispatchers.Default) {
        database.showQueries.getShowCountByYear()
            .executeAsList()
            .map { it.year.toInt() to it.count.toInt() }
    }

    actual suspend fun getShowCountByVenue(): List<Pair<String, Int>> = withContext(Dispatchers.Default) {
        database.showQueries.getShowCountByVenue()
            .executeAsList()
            .map { it.venueName to it.count.toInt() }
    }

    actual suspend fun getShowsForDate(month: Int, day: Int): List<com.grateful.deadly.database.Show> = withContext(Dispatchers.Default) {
        database.showQueries.getShowsForDate(month.toLong(), day.toLong()).executeAsList()
    }
}