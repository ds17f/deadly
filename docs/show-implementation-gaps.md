# Show Implementation Gaps Analysis

**Date**: 2025-01-20
**Status**: Current implementation has solid foundation but missing V2's proven domain layer

## Executive Summary

Our current Show implementation successfully imports and stores 2313+ Grateful Dead shows using the Universal Service + Platform Tool pattern. However, compared to V2's proven production architecture, we're missing the **domain layer** that provides rich business objects, query capabilities, and proper separation of concerns.

**Foundation**: ✅ Solid (database schema, import workflow, cross-platform setup)
**Domain Layer**: ❌ Missing (business objects, mappers, rich repository interface)
**Impact**: Can import data but limited functionality for features that consume show data

## Current Architecture vs V2 Comparison

### Database Schema: ✅ EXCELLENT MATCH

Our SQLDelight schema closely matches V2's proven Room schema:

| Component | V2 Room | Our SQLDelight | Status |
|-----------|---------|----------------|---------|
| Primary Key | `showId: String` | `showId TEXT PRIMARY KEY` | ✅ Perfect |
| Date Components | `date, year, month, yearMonth` | `date, year, month, yearMonth` | ✅ Perfect |
| Venue Data | `venueName, city, state, country, locationRaw` | `venueName, city, state, country, locationRaw` | ✅ Perfect |
| Setlist Storage | `setlistStatus, setlistRaw, songList` | `setlistStatus, setlistRaw, songList` | ✅ Perfect |
| Lineup Storage | `lineupStatus, lineupRaw, memberList` | `lineupStatus, lineupRaw, memberList` | ✅ Perfect |
| Recording Data | `recordingsRaw, recordingCount, bestRecordingId, averageRating` | `recordingCount, bestRecordingId, averageRating` | ⚠️ Missing `recordingsRaw` |
| Library Status | `isInLibrary, libraryAddedAt` | `isInLibrary, libraryAddedAt` | ✅ Perfect |
| Metadata | `createdAt, updatedAt` | `createdAt, updatedAt` | ✅ Perfect |
| **Missing Field** | `showSequence: Int` | N/A | ❌ Missing (for multiple shows same date) |

### Repository Layer: ❌ MAJOR GAP

**V2 ShowRepository (Domain Interface):**
```kotlin
interface ShowRepository {
    // Core queries
    suspend fun getShowById(showId: String): Show?
    suspend fun getAllShows(): List<Show>
    fun getAllShowsFlow(): Flow<List<Show>>
    suspend fun getShowCount(): Int

    // Date-based queries
    suspend fun getShowsByYear(year: Int): List<Show>
    suspend fun getShowsByYearMonth(yearMonth: String): List<Show>
    suspend fun getShowsInDateRange(startDate: String, endDate: String): List<Show>
    suspend fun getShowsForDate(month: Int, day: Int): List<Show>

    // Location queries
    suspend fun getShowsByVenue(venueName: String): List<Show>
    suspend fun getShowsByCity(city: String): List<Show>
    suspend fun getShowsByState(state: String): List<Show>

    // Content queries
    suspend fun getShowsBySong(songName: String): List<Show>

    // Popular/featured queries
    suspend fun getTopRatedShows(limit: Int = 20): List<Show>
    suspend fun getRecentShows(limit: Int = 20): List<Show>

    // Navigation queries
    suspend fun getNextShowByDate(currentDate: String): Show?
    suspend fun getPreviousShowByDate(currentDate: String): Show?

    // Recording queries (in same repository!)
    suspend fun getRecordingsForShow(showId: String): List<Recording>
    suspend fun getBestRecordingForShow(showId: String): Recording?
    suspend fun getRecordingById(identifier: String): Recording?
}
```

**Our Current ShowRepository (Platform Tool):**
```kotlin
expect class ShowRepository(database: Database) {
    // Only basic CRUD operations
    suspend fun insertShow(show: ShowEntity)
    suspend fun getShowById(showId: String): ShowEntity?
    suspend fun getShowCount(): Long
    suspend fun deleteAllShows()
    suspend fun insertShows(shows: List<ShowEntity>)
}
```

**Gap Analysis:**
- ❌ **Missing 15+ query methods** for date, location, content filtering
- ❌ **Returns raw entities** instead of domain models
- ❌ **No Flow support** for reactive UI updates
- ❌ **No navigation queries** for chronological browsing
- ❌ **No recording queries** (V2 puts these in ShowRepository)

### Domain Models: ❌ COMPLETELY MISSING

**V2 Rich Domain Models:**
```kotlin
// Primary domain model with computed properties
data class Show(
    val id: String,
    val date: String,
    val year: Int,
    val band: String,
    val venue: Venue,           // Structured value object
    val location: Location,     // Parsed location with city/state
    val setlist: Setlist?,      // Parsed setlist with songs/segues
    val lineup: Lineup?,        // Parsed lineup with instruments
    val recordingIds: List<String>,
    val bestRecordingId: String?,
    val recordingCount: Int,
    val averageRating: Float?,
    val totalReviews: Int,
    val isInLibrary: Boolean,
    val libraryAddedAt: Long?
) {
    // Computed business properties
    val displayTitle: String get() = "${venue.name} - $date"
    val hasRating: Boolean get() = averageRating != null && averageRating > 0f
    val displayRating: String get() = averageRating?.let { "%.1f★".format(it) } ?: "Not Rated"
    val hasMultipleRecordings: Boolean get() = recordingCount > 1
}

// Supporting value objects
data class Venue(
    val name: String,
    val city: String?,
    val state: String?,
    val country: String
) {
    val displayLocation: String get() = listOfNotNull(city, state, country.takeIf { it != "USA" }).joinToString(", ")
}

data class Location(
    val displayText: String,
    val city: String?,
    val state: String?
) {
    companion object {
        fun fromRaw(raw: String?, city: String?, state: String?): Location {
            val display = raw ?: listOfNotNull(city, state).joinToString(", ").ifEmpty { "Unknown Location" }
            return Location(display, city, state)
        }
    }
}

data class Setlist(
    val status: String,
    val sets: List<SetlistSet>,
    val raw: String?
) {
    companion object {
        fun parse(json: String?, status: String?): Setlist? {
            // Complex JSON parsing with error handling
            // Returns null on parse errors
        }
    }
}

data class SetlistSet(
    val name: String,           // "Set 1", "Set 2", "Encore"
    val songs: List<SetlistSong>
)

data class SetlistSong(
    val name: String,
    val position: Int,
    val hasSegue: Boolean = false,
    val segueSymbol: String? = null
) {
    val displayName: String get() = if (hasSegue && segueSymbol != null) "$name $segueSymbol" else name
}

data class Lineup(
    val status: String,
    val members: List<LineupMember>,
    val raw: String?
) {
    companion object {
        fun parse(json: String?, status: String?): Lineup? {
            // JSON parsing for lineup data
        }
    }
}

data class LineupMember(
    val name: String,
    val instruments: String
)
```

**Our Current Implementation:**
```kotlin
// Only raw data entity - NO domain models!
data class ShowEntity(
    val showId: String,
    val venueName: String,      // Raw string, no Venue object
    val setlistRaw: String?,    // Unparsed JSON, no Setlist object
    val lineupRaw: String?,     // Unparsed JSON, no Lineup object
    // No computed properties
    // No business logic
    // No value objects
)
```

### ShowMappers: ❌ COMPLETELY MISSING

**V2 ShowMappers (Entity↔Domain Conversion):**
```kotlin
@Singleton
class ShowMappers @Inject constructor(private val json: Json) {

    fun entityToDomain(entity: ShowEntity): Show {
        return Show(
            id = entity.showId,
            venue = Venue(
                name = entity.venueName,
                city = entity.city,
                state = entity.state,
                country = entity.country
            ),
            location = Location.fromRaw(entity.locationRaw, entity.city, entity.state),
            setlist = parseSetlist(entity.setlistRaw, entity.setlistStatus),
            lineup = parseLineup(entity.lineupRaw, entity.lineupStatus),
            recordingIds = parseRecordingIds(entity.recordingsRaw),
            // ... full domain model construction
        )
    }

    fun entitiesToDomain(entities: List<ShowEntity>): List<Show> = entities.map(::entityToDomain)

    private fun parseSetlist(json: String?, status: String?): Setlist? {
        return Setlist.parse(json, status)
    }

    private fun parseLineup(json: String?, status: String?): Lineup? {
        return Lineup.parse(json, status)
    }

    private fun parseRecordingIds(jsonString: String?): List<String> {
        // Safe JSON parsing with empty list fallback
    }
}
```

**Our Current Implementation:**
- ❌ No ShowMappers class exists
- ❌ No entity→domain conversion
- ❌ Services work directly with raw ShowEntity
- ❌ No business logic abstraction layer

### Query Capabilities: ❌ SEVERELY LIMITED

**V2 SQLite/Room Queries (20+ specialized methods):**
```sql
-- Date queries
SELECT * FROM shows WHERE year = :year ORDER BY date
SELECT * FROM shows WHERE yearMonth = :yearMonth ORDER BY date
SELECT * FROM shows WHERE date >= :startDate AND date <= :endDate ORDER BY date

-- Location queries
SELECT * FROM shows WHERE venueName LIKE '%' || :venueName || '%' ORDER BY date
SELECT * FROM shows WHERE city = :city ORDER BY date DESC
SELECT * FROM shows WHERE state = :state ORDER BY date DESC

-- Content queries
SELECT * FROM shows WHERE songList LIKE '%' || :songName || '%' ORDER BY date DESC

-- Navigation queries
SELECT * FROM shows WHERE date > :currentDate ORDER BY date ASC LIMIT 1
SELECT * FROM shows WHERE date < :currentDate ORDER BY date DESC LIMIT 1

-- Popular queries
SELECT * FROM shows WHERE averageRating IS NOT NULL ORDER BY averageRating DESC LIMIT :limit
SELECT * FROM shows ORDER BY date DESC LIMIT :limit

-- Anniversary queries
SELECT * FROM shows WHERE month = :month AND SUBSTR(date, 9, 2) = PRINTF('%02d', :day) ORDER BY year
```

**Our Current SQLDelight Queries (6 basic operations):**
```sql
-- Only basic CRUD
selectAllShows: SELECT * FROM Show ORDER BY date DESC;
selectShowById: SELECT * FROM Show WHERE showId = ?;
getShowCount: SELECT COUNT(*) FROM Show;
searchShows: SELECT * FROM Show WHERE date LIKE ? OR venueName LIKE ?; -- Very basic search
insertShow: INSERT OR REPLACE INTO Show (...);
deleteAllShows: DELETE FROM Show;
```

## Implementation Gap Summary

### ❌ Critical Missing Components:

1. **Show Domain Model** - Rich business object with computed properties
2. **Value Objects** - Venue, Location, Setlist, Lineup structured data
3. **ShowMappers** - Entity↔Domain conversion with JSON parsing
4. **Rich Repository Interface** - 15+ query methods for filtering/search
5. **Business Logic Layer** - Computed properties, formatting, validation

### ⚠️ Architectural Mismatches:

1. **Services use raw entities** instead of domain models
2. **No separation** between data layer (entities) and domain layer (models)
3. **Limited query interface** compared to V2's comprehensive search
4. **Missing reactive UI support** (no Flow-based queries)
5. **No navigation support** for chronological browsing

### ✅ Strong Foundation:

1. **Database Schema** - 95% match with V2's proven structure
2. **Import System** - Universal Service + Platform Tool pattern working
3. **Cross-platform Setup** - SQLDelight functioning correctly
4. **Data Quality** - 2313+ shows successfully imported

## Recommended Implementation Priority

**Phase 1: Core Domain Layer (High Priority)**
1. Create Show domain model with computed properties
2. Create value objects (Venue, Location, Setlist, Lineup)
3. Implement ShowMappers with JSON parsing
4. Update ShowRepository interface to return domain models

**Phase 2: Enhanced Queries (Medium Priority)**
1. Add date-based queries (by year, year-month, date range)
2. Add location queries (by venue, city, state)
3. Add content queries (by song name)
4. Add navigation queries (next/previous show)

**Phase 3: Advanced Features (Lower Priority)**
1. Add reactive Flow support for UI updates
2. Add full-text search capabilities
3. Add popular/featured show queries
4. Add anniversary/special date queries

This gap analysis provides the roadmap for bringing our Show implementation up to V2's proven production standards while maintaining our clean Universal Service + Platform Tool architecture.