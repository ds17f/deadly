package com.grateful.deadly.services.data.platform

import com.grateful.deadly.database.Database
import com.grateful.deadly.services.data.models.ShowEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS implementation of ShowRepository using SQLDelight database.
 *
 * This implementation is identical to Android because SQLDelight handles
 * platform differences at the driver level. This demonstrates how the
 * Universal Service + Platform Tool pattern can result in identical
 * implementations when the underlying abstraction is cross-platform.
 */
actual class ShowRepository actual constructor(
    private val database: Database
) {

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

    actual suspend fun getShowById(showId: String): ShowEntity? = withContext(Dispatchers.Default) {
        val show = database.showQueries.selectShowById(showId).executeAsOneOrNull()
        show?.let {
            ShowEntity(
                showId = it.showId,
                date = it.date,
                year = it.year.toInt(),
                month = it.month.toInt(),
                yearMonth = it.yearMonth,
                band = it.band,
                url = it.url,
                venueName = it.venueName,
                city = it.city,
                state = it.state,
                country = it.country,
                locationRaw = it.locationRaw,
                setlistStatus = it.setlistStatus,
                setlistRaw = it.setlistRaw,
                songList = it.songList,
                lineupStatus = it.lineupStatus,
                lineupRaw = it.lineupRaw,
                memberList = it.memberList,
                recordingCount = it.recordingCount.toInt(),
                bestRecordingId = it.bestRecordingId,
                averageRating = it.averageRating,
                totalReviews = it.totalReviews.toInt(),
                isInLibrary = it.isInLibrary == 1L,
                libraryAddedAt = it.libraryAddedAt,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
    }

    actual suspend fun getShowCount(): Long = withContext(Dispatchers.Default) {
        database.showQueries.getShowCount().executeAsOne()
    }

    actual suspend fun deleteAllShows() = withContext(Dispatchers.Default) {
        database.showQueries.deleteAllShows()
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
}