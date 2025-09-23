package com.grateful.deadly.services.search

import com.grateful.deadly.domain.search.*
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.services.data.platform.ShowRepository
import com.grateful.deadly.services.search.platform.ShowSearchDao
import com.grateful.deadly.core.util.Logger
import com.russhwolf.settings.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers

/**
 * Real implementation of SearchService following V2's architecture pattern.
 *
 * Uses ShowSearchDao for FTS5 queries and ShowRepository for domain models,
 * exactly like V2's proven approach. Maintains reactive flows and error handling
 * while providing enhanced search capabilities with BM25 relevance ranking.
 */
class SearchServiceImpl(
    private val showRepository: ShowRepository,    // Domain models (like V2)
    private val showSearchDao: ShowSearchDao,      // FTS5 queries (like V2)
    private val settings: Settings
) : SearchService {

    companion object {
        private const val TAG = "SearchServiceImpl"
        private const val RECENT_SEARCHES_KEY = "recent_searches"
        private const val MAX_RECENT_SEARCHES = 10
    }

    // Reactive state management (same as SearchServiceStub)
    private val _currentQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<SearchResultShow>>(emptyList())
    private val _searchStatus = MutableStateFlow(SearchStatus.IDLE)
    private val _recentSearches = MutableStateFlow<List<RecentSearch>>(emptyList())
    private val _suggestedSearches = MutableStateFlow<List<SuggestedSearch>>(emptyList())
    private val _searchStats = MutableStateFlow(SearchStats(0, 0))
    private val _appliedFilters = MutableStateFlow<Set<SearchFilter>>(emptySet())

    // Public reactive flows (same as SearchServiceStub)
    override val currentQuery: Flow<String> = _currentQuery.asStateFlow()
    override val searchResults: Flow<List<SearchResultShow>> = _searchResults.asStateFlow()
    override val searchStatus: Flow<SearchStatus> = _searchStatus.asStateFlow()
    override val recentSearches: Flow<List<RecentSearch>> = _recentSearches.asStateFlow()
    override val suggestedSearches: Flow<List<SuggestedSearch>> = _suggestedSearches.asStateFlow()
    override val searchStats: Flow<SearchStats> = _searchStats.asStateFlow()

    init {
        loadRecentSearches()
        loadSuggestions()
    }

    override suspend fun updateSearchQuery(query: String): Result<Unit> {
        return try {
            Logger.d(TAG, "Updating search query: '$query'")
            _currentQuery.value = query

            if (query.isBlank()) {
                _searchResults.value = emptyList()
                _searchStatus.value = SearchStatus.IDLE
                _searchStats.value = SearchStats(0, 0)
                return Result.success(Unit)
            }

            _searchStatus.value = SearchStatus.SEARCHING

            // Simulate search delay for UI feedback
            delay(300)

            val startTime = Clock.System.now()

            // FTS4 search using ShowSearchDao (V2 pattern)
            val showIds = showSearchDao.searchShows(query)
            Logger.d(TAG, "FTS4 returned ${showIds.size} show IDs")

            // Get full Show domain models for matching IDs
            val shows = showRepository.getShowsByIds(showIds)
            Logger.d(TAG, "Retrieved ${shows.size} full shows")

            // Convert to SearchResultShow with FTS4 relevance scoring
            val results = shows.mapIndexed { index, show ->
                // Relevance score is preserved by FTS4 result ordering
                val relevanceScore = 1.0f - (index.toFloat() / shows.size.coerceAtLeast(1))
                val matchType = determineMatchType(show, query)

                SearchResultShow(
                    show = show,
                    relevanceScore = relevanceScore,
                    matchType = matchType,
                    hasDownloads = show.recordingCount > 0,
                    highlightedFields = getHighlightedFields(show, query)
                )
            }
            val searchDuration = (Clock.System.now() - startTime).inWholeMilliseconds

            _searchResults.value = results
            _searchStatus.value = if (results.isEmpty()) SearchStatus.NO_RESULTS else SearchStatus.SUCCESS
            _searchStats.value = SearchStats(
                totalResults = results.size,
                searchDuration = searchDuration,
                appliedFilters = _appliedFilters.value.map { it.displayName }
            )

            Logger.d(TAG, "Search completed: ${results.size} results in ${searchDuration}ms")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Search failed", e)
            _searchStatus.value = SearchStatus.ERROR
            Result.failure(e)
        }
    }

    override suspend fun clearSearch(): Result<Unit> {
        _currentQuery.value = ""
        _searchResults.value = emptyList()
        _searchStatus.value = SearchStatus.IDLE
        _searchStats.value = SearchStats(0, 0)
        return Result.success(Unit)
    }

    override suspend fun addRecentSearch(query: String): Result<Unit> {
        return try {
            val current = _recentSearches.value.toMutableList()

            // Remove existing occurrence of this query
            current.removeAll { it.query == query }

            // Add to the beginning
            current.add(0, RecentSearch(query, Clock.System.now().toEpochMilliseconds()))

            // Keep only the most recent entries
            if (current.size > MAX_RECENT_SEARCHES) {
                current.removeAt(current.size - 1)
            }

            _recentSearches.value = current
            saveRecentSearches(current)

            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to add recent search", e)
            Result.failure(e)
        }
    }

    override suspend fun clearRecentSearches(): Result<Unit> {
        _recentSearches.value = emptyList()
        settings.remove(RECENT_SEARCHES_KEY)
        return Result.success(Unit)
    }

    override suspend fun selectSuggestion(suggestion: SuggestedSearch): Result<Unit> {
        return updateSearchQuery(suggestion.query)
    }

    override suspend fun applyFilters(filters: List<SearchFilter>): Result<Unit> {
        _appliedFilters.value = filters.toSet()
        // Re-run current search with new filters
        if (_currentQuery.value.isNotBlank()) {
            return updateSearchQuery(_currentQuery.value)
        }
        return Result.success(Unit)
    }

    override suspend fun clearFilters(): Result<Unit> {
        _appliedFilters.value = emptySet()
        // Re-run current search without filters
        if (_currentQuery.value.isNotBlank()) {
            return updateSearchQuery(_currentQuery.value)
        }
        return Result.success(Unit)
    }

    override suspend fun getSuggestions(partialQuery: String): Result<List<SuggestedSearch>> {
        return try {
            // Generate suggestions using FTS4 search
            val suggestions = buildList<SuggestedSearch> {
                if (partialQuery.isNotBlank()) {
                    // Use FTS4 for suggestion generation
                    val showIds = showSearchDao.searchShows(partialQuery).take(10)
                    val shows = showRepository.getShowsByIds(showIds)

                    // Venue suggestions
                    val venueMatches = shows
                        .map { it.venue.name }
                        .distinct()
                        .take(3)

                    venueMatches.forEach { venue ->
                        add(SuggestedSearch(venue, resultCount = 1, type = SuggestionType.VENUE))
                    }

                    // Year suggestions
                    if (partialQuery.matches(Regex("\\d+"))) {
                        val yearMatches = shows
                            .filter { it.date.contains(partialQuery) }
                            .map { it.year.toString() }
                            .distinct()
                            .take(3)

                        yearMatches.forEach { year ->
                            add(SuggestedSearch(year, resultCount = 1, type = SuggestionType.YEAR))
                        }
                    }

                    // Location suggestions
                    val locationMatches = shows
                        .filter { show ->
                            show.location.city?.contains(partialQuery, ignoreCase = true) == true ||
                            show.location.state?.contains(partialQuery, ignoreCase = true) == true
                        }
                        .map { "${it.location.city}, ${it.location.state}" }
                        .distinct()
                        .take(2)

                    locationMatches.forEach { location ->
                        add(SuggestedSearch(location, resultCount = 1, type = SuggestionType.LOCATION))
                    }
                }
            }

            Result.success(suggestions)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get suggestions", e)
            Result.failure(e)
        }
    }


    private fun determineMatchType(show: Show, query: String): SearchMatchType {
        return when {
            show.venue.name.lowercase().contains(query) -> SearchMatchType.VENUE
            show.location.city?.lowercase()?.contains(query) == true -> SearchMatchType.LOCATION
            show.date.contains(query) || show.year.toString().contains(query) -> SearchMatchType.YEAR
            else -> SearchMatchType.GENERAL
        }
    }

    private fun getHighlightedFields(show: Show, query: String): List<String> {
        return buildList {
            if (show.venue.name.lowercase().contains(query)) add("venue")
            if (show.location.displayText.lowercase().contains(query)) add("location")
            if (show.date.contains(query) || show.year.toString().contains(query)) add("date")
        }
    }

    private fun loadRecentSearches() {
        try {
            val json = settings.getStringOrNull(RECENT_SEARCHES_KEY)
            if (json != null) {
                val searches = Json.decodeFromString<List<RecentSearch>>(json)
                _recentSearches.value = searches
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load recent searches", e)
        }
    }

    private fun saveRecentSearches(searches: List<RecentSearch>) {
        try {
            val json = Json.encodeToString(searches)
            settings.putString(RECENT_SEARCHES_KEY, json)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to save recent searches", e)
        }
    }

    private fun loadSuggestions() {
        // Popular venue suggestions (same as stub for now)
        // TODO: Generate from actual database data
        val popularSuggestions = listOf(
            SuggestedSearch("Cornell", resultCount = 1, type = SuggestionType.VENUE),
            SuggestedSearch("Fillmore East", resultCount = 5, type = SuggestionType.VENUE),
            SuggestedSearch("Winterland", resultCount = 8, type = SuggestionType.VENUE),
            SuggestedSearch("1977", resultCount = 15, type = SuggestionType.YEAR),
            SuggestedSearch("1972", resultCount = 12, type = SuggestionType.YEAR),
            SuggestedSearch("San Francisco", resultCount = 20, type = SuggestionType.LOCATION)
        )

        _suggestedSearches.value = popularSuggestions
    }
}