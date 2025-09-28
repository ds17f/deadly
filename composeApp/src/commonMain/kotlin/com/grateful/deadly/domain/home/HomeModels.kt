package com.grateful.deadly.domain.home

import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.domain.models.Collection
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock

/**
 * Comprehensive home content data model
 *
 * Contains all data needed for home screen UI components following V2 patterns
 */
@Serializable
data class HomeContent(
    val recentShows: List<Show>,
    val todayInHistory: List<Show>,
    val featuredCollections: List<Collection>,
    val lastRefresh: Long
) {
    companion object {
        fun initial() = HomeContent(
            recentShows = emptyList(),
            todayInHistory = emptyList(),
            featuredCollections = emptyList(),
            lastRefresh = 0L
        )
    }

    /**
     * Whether there is any content to display
     */
    val hasContent: Boolean
        get() = recentShows.isNotEmpty() || todayInHistory.isNotEmpty() || featuredCollections.isNotEmpty()

    /**
     * Whether the content is fresh (last refreshed in the last hour)
     */
    val isFresh: Boolean
        get() = (Clock.System.now().toEpochMilliseconds() - lastRefresh) < 3600000 // 1 hour
}