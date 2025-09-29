package com.grateful.deadly.services.home

import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.domain.home.HomeService
import com.grateful.deadly.domain.home.HomeContent
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.domain.models.Collection
import com.grateful.deadly.domain.models.Venue
import com.grateful.deadly.domain.models.Location
import com.grateful.deadly.services.data.platform.ShowRepository
import com.grateful.deadly.services.media.MediaService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * HomeService implementation following the Universal Service + Platform Tool pattern.
 *
 * Features:
 * - Recent shows from user activity (mock data for now, will integrate with MediaService later)
 * - Today in History from database queries with date matching
 * - Featured collections from curated content (mock data for now)
 *
 * Uses reactive StateFlow for real-time UI updates following SearchService patterns.
 */
class HomeServiceImpl(
    private val showRepository: ShowRepository,
    private val mediaService: MediaService
) : HomeService {

    companion object {
        private const val TAG = "HomeServiceImpl"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Reactive home content state
    private val _homeContent = MutableStateFlow(HomeContent.initial())
    override val homeContent: StateFlow<HomeContent> = _homeContent.asStateFlow()

    init {
        Logger.d(TAG, "HomeServiceImpl initialized")
        // Load initial content
        loadInitialContent()
    }

    /**
     * Load initial home content
     */
    private fun loadInitialContent() {
        serviceScope.launch {
            try {
                val content = HomeContent(
                    recentShows = generateMockRecentShows(),
                    todayInHistory = getTodayInHistoryShows(),
                    featuredCollections = generateMockCollections(),
                    lastRefresh = Clock.System.now().toEpochMilliseconds()
                )
                _homeContent.value = content
                Logger.d(TAG, "Loaded initial content: ${content.recentShows.size} recent, ${content.todayInHistory.size} history, ${content.featuredCollections.size} collections")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to load initial content", e)
            }
        }
    }

    override suspend fun refreshAll(): Result<Unit> {
        Logger.d(TAG, "refreshAll() called")

        return try {
            // TODO: Integrate with real data sources:
            // - Recent shows from MediaService play history
            // - Featured collections from database or API

            val content = HomeContent(
                recentShows = generateMockRecentShows(),
                todayInHistory = getTodayInHistoryShows(),
                featuredCollections = generateMockCollections(),
                lastRefresh = Clock.System.now().toEpochMilliseconds()
            )
            _homeContent.value = content
            Logger.d(TAG, "Refreshed content: ${content.recentShows.size} recent, ${content.todayInHistory.size} history, ${content.featuredCollections.size} collections")

            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to refresh", e)
            Result.failure(e)
        }
    }

    /**
     * Generate mock recent shows (placeholder)
     * TODO: Replace with MediaService integration for actual play history
     */
    private fun generateMockRecentShows(): List<Show> {
        return listOf(
            Show(
                id = "gd1977-05-08-recent",
                date = "1977-05-08",
                year = 1977,
                band = "Grateful Dead",
                venue = Venue("Barton Hall, Cornell University", "Ithaca", "NY", "USA"),
                location = Location("Ithaca, NY", "Ithaca", "NY"),
                setlist = null,
                lineup = null,
                recordingIds = listOf("gd1977-05-08.sbd.hicks.4982.sbeok.shnf"),
                bestRecordingId = "gd1977-05-08.sbd.hicks.4982.sbeok.shnf",
                recordingCount = 1,
                averageRating = 4.8f,
                totalReviews = 245,
                isInLibrary = true,
                libraryAddedAt = Clock.System.now().toEpochMilliseconds() - 86400000 // 1 day ago
            ),
            Show(
                id = "gd1978-05-08-recent",
                date = "1978-05-08",
                year = 1978,
                band = "Grateful Dead",
                venue = Venue("Horton Field House", "Normal", "IL", "USA"),
                location = Location("Normal, IL", "Normal", "IL"),
                setlist = null,
                lineup = null,
                recordingIds = listOf("gd78-05-08.sbd.unknown.12345.shnf"),
                bestRecordingId = "gd78-05-08.sbd.unknown.12345.shnf",
                recordingCount = 1,
                averageRating = 4.2f,
                totalReviews = 123,
                isInLibrary = false,
                libraryAddedAt = null
            )
        )
    }

    /**
     * Get "Today in History" shows from database
     * Query for shows that happened on today's date in previous years
     */
    private suspend fun getTodayInHistoryShows(): List<Show> {
        return try {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            Logger.d(TAG, "🗓️ Getting TIGDH shows for ${today.monthNumber}/${today.dayOfMonth}")

            val todayInHistoryShows = showRepository.getShowsForDate(today.monthNumber, today.dayOfMonth)

            Logger.d(TAG, "🗓️ Found ${todayInHistoryShows.size} shows for today in history (${today.monthNumber}/${today.dayOfMonth})")

            if (todayInHistoryShows.isNotEmpty()) {
                Logger.d(TAG, "🗓️ Sample TIGDH shows:")
                todayInHistoryShows.take(3).forEach { show ->
                    Logger.d(TAG, "  - ID: ${show.id} | ${show.date}: ${show.venue.name}, ${show.location.city}")
                }
            } else {
                Logger.w(TAG, "🗓️ No TIGDH shows found - checking if any shows exist in database...")
                val totalShows = showRepository.getShowCount()
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