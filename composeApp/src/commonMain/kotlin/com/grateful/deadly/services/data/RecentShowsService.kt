package com.grateful.deadly.services.data

import com.grateful.deadly.domain.models.Show
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock

interface RecentShowsService {
    // Reactive StateFlow for UI (matches V2 pattern exactly)
    val recentShows: StateFlow<List<Show>>

    // Manual recording (for explicit actions)
    suspend fun recordShowPlay(showId: String, playTimestamp: Long = Clock.System.now().toEpochMilliseconds())

    // Querying
    suspend fun getRecentShows(limit: Int = 8): List<Show>
    suspend fun isShowInRecent(showId: String): Boolean

    // Privacy controls
    suspend fun removeShow(showId: String)
    suspend fun clearRecentShows()

    // Analytics (V2 returns Map<String, Any>)
    suspend fun getRecentShowsStats(): Map<String, Any>

    // Lifecycle management
    fun startTracking()
    fun stopTracking()
}