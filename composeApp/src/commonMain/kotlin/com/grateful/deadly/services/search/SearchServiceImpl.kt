package com.grateful.deadly.services.search

import com.grateful.deadly.domain.search.*
import com.grateful.deadly.data.search.SearchRepository
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
 * Real implementation of SearchService using SQLDelight repository.
 *
 * Maintains the exact same API as SearchServiceStub but uses real database data
 * instead of mock data. All the reactive flows, error handling, and UI patterns
 * remain identical to ensure seamless switching between stub and real implementations.
 */
class SearchServiceImpl(
    private val repository: SearchRepository,
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

            // USE REPOSITORY INSTEAD OF MOCK DATA - This is the key change!
            val shows = repository.searchShows(query)
            Logger.d(TAG, "Repository returned ${shows.size} shows for query '$query'")

            val results = performSearch(shows, query)
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
            // For now, use the same suggestion logic as stub
            // TODO: Generate suggestions from actual database data
            val suggestions = buildList {
                if (partialQuery.isNotBlank()) {
                    // Get shows from repository for suggestions
                    val shows = repository.searchShows(partialQuery)

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
                        .filter {
                            it.location.city?.contains(partialQuery, ignoreCase = true) == true ||
                            it.location.state?.contains(partialQuery, ignoreCase = true) == true
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

    /**
     * Process search results from repository data.
     * Same logic as SearchServiceStub but operates on repository results.
     */
    private fun performSearch(shows: List<Show>, query: String): List<SearchResultShow> {
        val queryLower = query.lowercase()
        val appliedFilters = _appliedFilters.value

        return shows
            .filter { show ->
                val matchesQuery = show.venue.name.lowercase().contains(queryLower) ||
                        show.location.city?.lowercase()?.contains(queryLower) == true ||
                        show.location.state?.lowercase()?.contains(queryLower) == true ||
                        show.date.contains(queryLower) ||
                        show.year.toString().contains(queryLower) ||
                        show.id.lowercase().contains(queryLower)

                val matchesFilters = appliedFilters.isEmpty() || appliedFilters.any { filter ->
                    when (filter) {
                        SearchFilter.HAS_DOWNLOADS -> show.recordingCount > 0
                        SearchFilter.VENUE -> show.venue.name.lowercase().contains(queryLower)
                        SearchFilter.YEAR -> show.year.toString().contains(queryLower)
                        SearchFilter.LOCATION -> show.location.displayText.lowercase().contains(queryLower)
                        else -> true
                    }
                }

                matchesQuery && matchesFilters
            }
            .map { show ->
                SearchResultShow(
                    show = show,
                    relevanceScore = calculateRelevanceScore(show, queryLower),
                    matchType = determineMatchType(show, queryLower),
                    hasDownloads = show.recordingCount > 0,
                    highlightedFields = getHighlightedFields(show, queryLower)
                )
            }
            .sortedByDescending { it.relevanceScore }
    }

    private fun calculateRelevanceScore(show: Show, query: String): Float {
        var score = 0f

        // Exact matches get highest score
        if (show.venue.name.lowercase() == query) score += 10f
        if (show.location.city?.lowercase() == query) score += 8f
        if (show.date.contains(query)) score += 6f

        // Partial matches get medium score
        if (show.venue.name.lowercase().contains(query)) score += 5f
        if (show.location.displayText.lowercase().contains(query)) score += 3f

        // Boost by rating and recording count
        show.averageRating?.let { score += it }
        score += show.recordingCount * 0.5f

        return score
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