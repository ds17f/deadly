package com.grateful.deadly.services.search

import com.grateful.deadly.domain.search.*
import com.russhwolf.settings.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

/**
 * Comprehensive stub implementation of SearchService with realistic Dead show data.
 * 
 * This stub provides:
 * - Rich mock data spanning decades of Grateful Dead concerts
 * - Realistic search filtering and relevance scoring
 * - Search suggestions based on popular venues, years, and songs
 * - Recent search history management using Multiplatform Settings
 * - Proper search status management with loading states
 * 
 * Enables immediate UI development with comprehensive test data while validating
 * the KMM architecture patterns and service integration.
 */
class SearchServiceStub(
    private val settings: Settings
) : SearchService {
    
    companion object {
        private const val RECENT_SEARCHES_KEY = "recent_searches"
        private const val MAX_RECENT_SEARCHES = 10
    }
    
    // Reactive state management
    private val _currentQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<SearchResultShow>>(emptyList())
    private val _searchStatus = MutableStateFlow(SearchStatus.IDLE)
    private val _recentSearches = MutableStateFlow<List<RecentSearch>>(emptyList())
    private val _suggestedSearches = MutableStateFlow<List<SuggestedSearch>>(emptyList())
    private val _searchStats = MutableStateFlow(SearchStats(0, 0))
    private val _appliedFilters = MutableStateFlow<Set<SearchFilter>>(emptySet())
    
    // Public reactive flows
    override val currentQuery: Flow<String> = _currentQuery.asStateFlow()
    override val searchResults: Flow<List<SearchResultShow>> = _searchResults.asStateFlow()
    override val searchStatus: Flow<SearchStatus> = _searchStatus.asStateFlow()
    override val recentSearches: Flow<List<RecentSearch>> = _recentSearches.asStateFlow()
    override val suggestedSearches: Flow<List<SuggestedSearch>> = _suggestedSearches.asStateFlow()
    override val searchStats: Flow<SearchStats> = _searchStats.asStateFlow()
    
    // Comprehensive mock show data spanning decades
    private val mockShows = listOf(
        // Cornell 5/8/77 - The legendary show
        Show(
            id = "gd1977-05-08",
            date = "1977-05-08",
            year = 1977,
            band = "Grateful Dead",
            venue = Venue("Barton Hall", "Ithaca", "NY", "USA"),
            location = Location.fromRaw("Ithaca, NY", "Ithaca", "NY"),
            setlist = null,
            lineup = null,
            recordingIds = listOf("gd1977-05-08.sbd.hicks.4982.sbeok.shnf"),
            bestRecordingId = "gd1977-05-08.sbd.hicks.4982.sbeok.shnf",
            recordingCount = 1,
            averageRating = 4.8f,
            totalReviews = 245,
            isInLibrary = false,
            libraryAddedAt = null
        ),
        
        // Europe '72 Classic
        Show(
            id = "gd1972-05-03",
            date = "1972-05-03",
            year = 1972,
            band = "Grateful Dead",
            venue = Venue("Olympia Theatre", "Paris", null, "France"),
            location = Location.fromRaw("Paris, France", "Paris", null),
            setlist = null,
            lineup = null,
            recordingIds = listOf("gd72-05-03.sbd.unknown.30057.sbeok.shnf"),
            bestRecordingId = "gd72-05-03.sbd.unknown.30057.sbeok.shnf",
            recordingCount = 1,
            averageRating = 4.6f,
            totalReviews = 189,
            isInLibrary = false,
            libraryAddedAt = null
        ),
        
        // Woodstock 1969
        Show(
            id = "gd1969-08-16",
            date = "1969-08-16",
            year = 1969,
            band = "Grateful Dead",
            venue = Venue("Woodstock Music & Art Fair", "Bethel", "NY", "USA"),
            location = Location.fromRaw("Bethel, NY", "Bethel", "NY"),
            setlist = null,
            lineup = null,
            recordingIds = listOf("gd69-08-16.aud.vernon.16793.sbeok.shnf"),
            bestRecordingId = "gd69-08-16.aud.vernon.16793.sbeok.shnf",
            recordingCount = 1,
            averageRating = 4.2f,
            totalReviews = 156,
            isInLibrary = false,
            libraryAddedAt = null
        ),
        
        // Dick's Picks era
        Show(
            id = "gd1973-06-10",
            date = "1973-06-10",
            year = 1973,
            band = "Grateful Dead",
            venue = Venue("RFK Stadium", "Washington", "DC", "USA"),
            location = Location.fromRaw("Washington, DC", "Washington", "DC"),
            setlist = null,
            lineup = null,
            recordingIds = listOf("dp12"),
            bestRecordingId = "dp12",
            recordingCount = 1,
            averageRating = 4.7f,
            totalReviews = 203,
            isInLibrary = false,
            libraryAddedAt = null
        ),
        
        // 1990s era
        Show(
            id = "gd1995-07-09",
            date = "1995-07-09",
            year = 1995,
            band = "Grateful Dead",
            venue = Venue("Soldier Field", "Chicago", "IL", "USA"),
            location = Location.fromRaw("Chicago, IL", "Chicago", "IL"),
            setlist = null,
            lineup = null,
            recordingIds = listOf("gd95-07-09.sbd.miller.97483.flac1644"),
            bestRecordingId = "gd95-07-09.sbd.miller.97483.flac1644",
            recordingCount = 1,
            averageRating = 4.1f,
            totalReviews = 298,
            isInLibrary = false,
            libraryAddedAt = null
        ),
        
        // Fillmore East classics
        Show(
            id = "gd1970-02-14",
            date = "1970-02-14",
            year = 1970,
            band = "Grateful Dead",
            venue = Venue("Fillmore East", "New York", "NY", "USA"),
            location = Location.fromRaw("New York, NY", "New York", "NY"),
            setlist = null,
            lineup = null,
            recordingIds = listOf("gd70-02-14.sbd.miller.97484.flac1644", "gd70-02-14.aud.vernon.16794.sbeok.shnf"),
            bestRecordingId = "gd70-02-14.sbd.miller.97484.flac1644",
            recordingCount = 2,
            averageRating = 4.5f,
            totalReviews = 167,
            isInLibrary = false,
            libraryAddedAt = null
        ),
        
        // Winterland classics
        Show(
            id = "gd1977-10-29",
            date = "1977-10-29",
            year = 1977,
            band = "Grateful Dead",
            venue = Venue("Winterland Arena", "San Francisco", "CA", "USA"),
            location = Location.fromRaw("San Francisco, CA", "San Francisco", "CA"),
            setlist = null,
            lineup = null,
            recordingIds = listOf("gd77-10-29.sbd.cotsman.6823.sbeok.shnf", "gd77-10-29.aud.hunter.6824.sbeok.shnf", "gd77-10-29.sbd.miller.6825.flac16"),
            bestRecordingId = "gd77-10-29.sbd.cotsman.6823.sbeok.shnf",
            recordingCount = 3,
            averageRating = 4.3f,
            totalReviews = 134,
            isInLibrary = false,
            libraryAddedAt = null
        ),
        
        // Capitol Theatre
        Show(
            id = "gd1971-11-07",
            date = "1971-11-07",
            year = 1971,
            band = "Grateful Dead",
            venue = Venue("Capitol Theatre", "Port Chester", "NY", "USA"),
            location = Location.fromRaw("Port Chester, NY", "Port Chester", "NY"),
            setlist = null,
            lineup = null,
            recordingIds = listOf("gd71-11-07.sbd.miller.97485.flac1644"),
            bestRecordingId = "gd71-11-07.sbd.miller.97485.flac1644",
            recordingCount = 1,
            averageRating = 4.4f,
            totalReviews = 98,
            isInLibrary = false,
            libraryAddedAt = null
        ),
        
        // 1980s revival
        Show(
            id = "gd1989-07-17",
            date = "1989-07-17",
            year = 1989,
            band = "Grateful Dead",
            venue = Venue("Alpine Valley Music Theatre", "East Troy", "WI", "USA"),
            location = Location.fromRaw("East Troy, WI", "East Troy", "WI"),
            setlist = null,
            lineup = null,
            recordingIds = listOf("gd89-07-17.sbd.miller.97486.flac1644", "gd89-07-17.aud.hunter.16795.sbeok.shnf"),
            bestRecordingId = "gd89-07-17.sbd.miller.97486.flac1644",
            recordingCount = 2,
            averageRating = 4.0f,
            totalReviews = 212,
            isInLibrary = false,
            libraryAddedAt = null
        ),
        
        // Final years
        Show(
            id = "gd1994-10-19",
            date = "1994-10-19",
            year = 1994,
            band = "Grateful Dead",
            venue = Venue("Madison Square Garden", "New York", "NY", "USA"),
            location = Location.fromRaw("New York, NY", "New York", "NY"),
            setlist = null,
            lineup = null,
            recordingIds = listOf("gd94-10-19.sbd.miller.97487.flac1644"),
            bestRecordingId = "gd94-10-19.sbd.miller.97487.flac1644",
            recordingCount = 1,
            averageRating = 3.9f,
            totalReviews = 156,
            isInLibrary = false,
            libraryAddedAt = null
        )
    )
    
    init {
        loadRecentSearches()
        loadSuggestions()
    }
    
    override suspend fun updateSearchQuery(query: String): Result<Unit> {
        return try {
            _currentQuery.value = query
            
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                _searchStatus.value = SearchStatus.IDLE
                _searchStats.value = SearchStats(0, 0)
                return Result.success(Unit)
            }
            
            _searchStatus.value = SearchStatus.SEARCHING
            
            // Simulate search delay
            delay(300)
            
            val startTime = Clock.System.now()
            val results = performSearch(query)
            val searchDuration = (Clock.System.now() - startTime).inWholeMilliseconds
            
            _searchResults.value = results
            _searchStatus.value = if (results.isEmpty()) SearchStatus.NO_RESULTS else SearchStatus.SUCCESS
            _searchStats.value = SearchStats(
                totalResults = results.size,
                searchDuration = searchDuration,
                appliedFilters = _appliedFilters.value.map { it.displayName }
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
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
        val suggestions = buildList {
            if (partialQuery.isNotBlank()) {
                // Venue suggestions
                val venueMatches = mockShows
                    .filter { it.venue.name.contains(partialQuery, ignoreCase = true) }
                    .map { it.venue.name }
                    .distinct()
                    .take(3)
                
                venueMatches.forEach { venue ->
                    add(SuggestedSearch(venue, resultCount = 1, type = SuggestionType.VENUE))
                }
                
                // Year suggestions
                if (partialQuery.matches(Regex("\\d+"))) {
                    val yearMatches = mockShows
                        .filter { it.date.contains(partialQuery) }
                        .map { it.year.toString() }
                        .distinct()
                        .take(3)
                    
                    yearMatches.forEach { year ->
                        add(SuggestedSearch(year, resultCount = 1, type = SuggestionType.YEAR))
                    }
                }
                
                // Location suggestions
                val locationMatches = mockShows
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
        
        return Result.success(suggestions)
    }
    
    override suspend fun populateTestData(): Result<Unit> {
        // Add some test recent searches
        val testRecentSearches = listOf(
            RecentSearch("Cornell", Clock.System.now().toEpochMilliseconds() - 86400000),
            RecentSearch("1977", Clock.System.now().toEpochMilliseconds() - 172800000),
            RecentSearch("Fillmore", Clock.System.now().toEpochMilliseconds() - 259200000)
        )
        
        _recentSearches.value = testRecentSearches
        saveRecentSearches(testRecentSearches)
        
        return Result.success(Unit)
    }
    
    private fun performSearch(query: String): List<SearchResultShow> {
        val queryLower = query.lowercase()
        val appliedFilters = _appliedFilters.value
        
        return mockShows
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
            // Ignore json parsing errors, start with empty list
        }
    }
    
    private fun saveRecentSearches(searches: List<RecentSearch>) {
        try {
            val json = Json.encodeToString(searches)
            settings.putString(RECENT_SEARCHES_KEY, json)
        } catch (e: Exception) {
            // Ignore serialization errors
        }
    }
    
    private fun loadSuggestions() {
        // Popular venue suggestions
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