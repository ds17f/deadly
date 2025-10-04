package com.grateful.deadly.services.home

import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.domain.home.HomeService
import com.grateful.deadly.domain.home.HomeContent
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.domain.models.Collection
import com.grateful.deadly.domain.models.Venue
import com.grateful.deadly.domain.models.Location
import com.grateful.deadly.services.show.ShowService
import com.grateful.deadly.services.data.RecentShowsService
import com.grateful.deadly.services.media.MediaService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * HomeService implementation following the Universal Service + Platform Tool pattern.
 *
 * Features:
 * - Recent shows from RecentShowsService (reactive StateFlow)
 * - Today in History from database queries with date matching
 * - Featured collections from curated content (mock data for now)
 *
 * Uses reactive StateFlow for real-time UI updates following V2 patterns.
 */
class HomeServiceImpl(
    private val showService: ShowService,
    private val recentShowsService: RecentShowsService,
    private val mediaService: MediaService
) : HomeService {

    companion object {
        private const val TAG = "HomeServiceImpl"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Reactive home content state - V2 pattern using combine() for real-time updates
    override val homeContent: StateFlow<HomeContent> = combine(
        recentShowsService.recentShows,
        getTodayInHistoryFlow(),
        getFeaturedCollectionsFlow()
    ) { recentShows, todayInHistory, featuredCollections ->
        Logger.d(TAG, "🏠 HomeContent updated: ${recentShows.size} recent, ${todayInHistory.size} history, ${featuredCollections.size} collections")
        HomeContent(
            recentShows = recentShows,
            todayInHistory = todayInHistory,
            featuredCollections = featuredCollections,
            lastRefresh = Clock.System.now().toEpochMilliseconds()
        )
    }.stateIn(
        scope = serviceScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeContent.initial()
    )

    init {
        Logger.d(TAG, "HomeServiceImpl initialized with reactive StateFlow architecture")
    }

    /**
     * Create a flow for "Today in History" shows (reactive pattern)
     */
    private fun getTodayInHistoryFlow(): StateFlow<List<Show>> {
        val flow = MutableStateFlow<List<Show>>(emptyList())

        serviceScope.launch {
            try {
                val shows = getTodayInHistoryShows()
                flow.value = shows
            } catch (e: Exception) {
                Logger.e(TAG, "🗓️ Failed to load TIGDH shows", e)
                flow.value = emptyList()
            }
        }

        return flow.asStateFlow()
    }

    /**
     * Create a flow for featured collections (reactive pattern)
     */
    private fun getFeaturedCollectionsFlow(): StateFlow<List<Collection>> {
        val flow = MutableStateFlow<List<Collection>>(emptyList())

        serviceScope.launch {
            try {
                val collections = generateMockCollections()
                flow.value = collections
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to load featured collections", e)
                flow.value = emptyList()
            }
        }

        return flow.asStateFlow()
    }

    override suspend fun refreshAll(): Result<Unit> {
        Logger.d(TAG, "refreshAll() called")

        return try {
            // With reactive architecture, the StateFlow will automatically update
            // when underlying data sources change. No manual refresh needed.
            // The combine operator will re-emit when any source flow emits.
            Logger.d(TAG, "Using reactive architecture - no manual refresh needed")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to refresh", e)
            Result.failure(e)
        }
    }


    /**
     * Get "Today in History" shows from database
     * Query for shows that happened on today's date in previous years
     */
    private suspend fun getTodayInHistoryShows(): List<Show> {
        return try {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            Logger.d(TAG, "🗓️ Getting TIGDH shows for ${today.monthNumber}/${today.dayOfMonth}")

            val todayInHistoryShows = showService.getShowsForDate(today.monthNumber, today.dayOfMonth)

            Logger.d(TAG, "🗓️ Found ${todayInHistoryShows.size} shows for today in history (${today.monthNumber}/${today.dayOfMonth})")

            if (todayInHistoryShows.isNotEmpty()) {
                Logger.d(TAG, "🗓️ Sample TIGDH shows:")
                todayInHistoryShows.take(3).forEach { show ->
                    Logger.d(TAG, "  - ID: ${show.id} | ${show.date}: ${show.venue.name}, ${show.location.city}")
                }
            } else {
                Logger.w(TAG, "🗓️ No TIGDH shows found - checking if any shows exist in database...")
                val totalShows = showService.getShowCount()
                Logger.d(TAG, "🗓️ Total shows in database: $totalShows")
            }

            todayInHistoryShows
        } catch (e: Exception) {
            Logger.e(TAG, "🗓️ Failed to get today in history shows", e)
            emptyList()
        }
    }

    /**
     * Generate mock featured collections (placeholder)
     * TODO: Replace with actual collections from database or API
     */
    private fun generateMockCollections(): List<Collection> {
        return listOf(
            Collection(
                id = "europe-72",
                name = "Europe '72",
                description = "The legendary European tour that changed everything",
                showIds = listOf("gd1972-05-03", "gd1972-05-04", "gd1972-05-07"),
                showCount = 22,
                coverImageUrl = null,
                isFeatured = true,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                updatedAt = Clock.System.now().toEpochMilliseconds()
            ),
            Collection(
                id = "cornell-77",
                name = "Cornell '77",
                description = "May 8, 1977 - Barton Hall, Cornell University",
                showIds = listOf("gd1977-05-08"),
                showCount = 1,
                coverImageUrl = null,
                isFeatured = true,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                updatedAt = Clock.System.now().toEpochMilliseconds()
            ),
            Collection(
                id = "best-of-77",
                name = "Best of 1977",
                description = "The cream of the crop from the legendary year",
                showIds = listOf("gd1977-05-08", "gd1977-05-09", "gd1977-05-22"),
                showCount = 15,
                coverImageUrl = null,
                isFeatured = true,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        )
    }
}