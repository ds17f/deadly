# Recently Played Shows Implementation Analysis

## Overview

This document analyzes the V2 recently played shows implementation and provides guidance for implementing this feature in the Kotlin Multiplatform Mobile (KMM) version of the Deadly app.

## V2 Implementation Analysis

### Architecture Overview

V2 uses a sophisticated 3-layer system for tracking recently played shows:

1. **MediaControllerRepository** → Observes Media3 playback events
2. **RecentShowsService** → Processes track plays into show-level events
3. **HomeService** → Consumes reactive StateFlow for UI

This architecture provides clean separation of concerns with the MediaController handling low-level playback observation, RecentShowsService applying business logic for meaningful play detection, and HomeService providing a unified interface for UI consumption.

### Database Design (Room/SQLite)

V2 uses an efficient UPSERT pattern with smart deduplication:

```kotlin
@Entity(tableName = "recent_shows")
data class RecentShowEntity(
    @PrimaryKey val showId: String,               // One record per show
    val lastPlayedTimestamp: Long,                // Updated each play
    val firstPlayedTimestamp: Long,               // Set once, for analytics
    val totalPlayCount: Int                       // Incremented each play
)
```

**Key Benefits:**
- ✅ **No GROUP BY queries** - UPSERT eliminates duplicates at write time
- ✅ **Efficient ordering** - Index on `lastPlayedTimestamp DESC`
- ✅ **Reactive updates** - `Flow<List<RecentShowEntity>>` for real-time UI
- ✅ **Analytics ready** - Tracks play counts and timestamps

### Smart Play Detection

V2 implements hybrid filtering to prevent rapid-skip spam while accurately detecting meaningful listens:

- ⏱️ **10-second rule** for tracks > 40 seconds
- 📊 **25% rule** for short tracks (≤40 seconds), capped at 10 seconds
- 🎯 **Debouncing** prevents rapid track changes from spamming database

**Logic Implementation:**
```kotlin
private fun shouldRecordPlay(playbackStatus: PlaybackStatus): Boolean {
    val position = playbackStatus.currentPosition
    val duration = playbackStatus.duration

    // Long tracks: simple 10 second rule
    if (duration > 40_000L) {
        return position >= 10_000L
    }

    // Short tracks: 25% rule, capped at 10 seconds
    val percentageThreshold = (duration * 0.25f).toLong()
    val actualThreshold = minOf(percentageThreshold, 10_000L)
    return position >= actualThreshold
}
```

### Reactive Data Flow

V2 provides real-time UI updates through StateFlow:

1. **MediaController** emits play events →
2. **RecentShowsService** filters meaningful plays →
3. **Database** UPSERT updates →
4. **Room Flow** detects changes →
5. **StateFlow** updates UI automatically

**HomeService Integration:**
```kotlin
override val homeContent: StateFlow<HomeContent> = combine(
    recentShowsService.recentShows,  // ← Real-time updates
    loadTodayInHistoryFlow(),
    collectionsService.featuredCollections
) { recentShows, todayInHistory, featuredCollections ->
    HomeContent(recentShows, todayInHistory, featuredCollections)
}
```

### RecentShowsService Interface

The V2 RecentShowsService provides a comprehensive API:

```kotlin
interface RecentShowsService {
    // Reactive StateFlow for UI
    val recentShows: StateFlow<List<Show>>

    // Manual recording (for explicit actions)
    suspend fun recordShowPlay(showId: String, playTimestamp: Long = System.currentTimeMillis())

    // Querying
    suspend fun getRecentShows(limit: Int = 8): List<Show>
    suspend fun isShowInRecent(showId: String): Boolean

    // Privacy controls
    suspend fun removeShow(showId: String)
    suspend fun clearRecentShows()

    // Analytics
    suspend fun getRecentShowsStats(): Map<String, Any>
}
```

### Database Operations

V2 uses efficient Room DAO operations:

```kotlin
@Dao
interface RecentShowDao {
    @Query("SELECT * FROM recent_shows ORDER BY lastPlayedTimestamp DESC LIMIT :limit")
    suspend fun getRecentShows(limit: Int = 8): List<RecentShowEntity>

    @Query("SELECT * FROM recent_shows ORDER BY lastPlayedTimestamp DESC LIMIT :limit")
    fun getRecentShowsFlow(limit: Int = 8): Flow<List<RecentShowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecentShowEntity)

    // UPSERT operations via custom update methods
    suspend fun updateShow(showId: String, timestamp: Long, playCount: Int)
}
```

## KMM Implementation Plan

### Required Components

#### 1. Database Layer (SQLDelight)

Create equivalent SQLDelight tables and queries:

```sql
CREATE TABLE recent_shows (
    showId TEXT PRIMARY KEY NOT NULL,
    lastPlayedTimestamp INTEGER NOT NULL,
    firstPlayedTimestamp INTEGER NOT NULL,
    totalPlayCount INTEGER NOT NULL
);

CREATE INDEX idx_recent_shows_timestamp ON recent_shows(lastPlayedTimestamp DESC);
```

#### 2. RecentShowsService (Common)

Implement as expect/actual pattern:
- **Common**: Interface and business logic
- **Platform**: Media session observation specifics

#### 3. MediaService Integration

Connect to existing KMM MediaService:
- Observe playback state changes
- Extract show IDs from current tracks
- Apply smart filtering logic

#### 4. Database Integration

Integrate with existing ShowRepository:
- Add recent shows operations
- Maintain domain model conversion
- Provide reactive flows

### Implementation Challenges

#### Platform Media APIs
- **Android**: Media3 session integration (similar to V2)
- **iOS**: AVAudioSession/MediaPlayer observation
- **Solution**: Platform-specific media observation with common business logic

#### Background Processing
- **Android**: Use existing service scope patterns
- **iOS**: Handle app lifecycle and background limitations
- **Solution**: Proper coroutine scope management with platform lifecycle awareness

#### Database Operations
- **UPSERT Pattern**: Ensure SQLDelight handles INSERT OR REPLACE correctly
- **Reactive Queries**: Implement Flow equivalent for real-time updates
- **Migration**: Plan for adding recent shows table to existing database

### Implementation Steps

1. **Phase 1: Database Foundation**
   - Add recent_shows table to SQLDelight schema
   - Implement basic DAO operations
   - Add to existing ShowRepository

2. **Phase 2: Service Layer**
   - Create RecentShowsService interface (common)
   - Implement platform-specific media observation
   - Add smart play detection logic

3. **Phase 3: Integration**
   - Connect to MediaService for playback events
   - Integrate with HomeService reactive patterns
   - Update UI components to display recent shows

4. **Phase 4: Features**
   - Add privacy controls (remove/clear)
   - Implement analytics and debugging
   - Add user preferences for tracking behavior

### Success Criteria

- ✅ Recently played shows appear in home screen within 10 seconds of meaningful play
- ✅ No duplicate shows in recent list (UPSERT working correctly)
- ✅ Survives app restarts and maintains play history
- ✅ Efficient performance with no UI lag during updates
- ✅ Privacy controls work as expected
- ✅ Cross-platform consistency between Android and iOS

## Conclusion

The V2 recently played implementation is production-ready and highly optimized. The key insights for KMM implementation are:

1. **UPSERT Pattern**: Essential for eliminating duplicate shows efficiently
2. **Smart Filtering**: Prevents spam while capturing meaningful listens
3. **Reactive Architecture**: StateFlow provides real-time UI updates
4. **Platform Abstraction**: Common business logic with platform-specific media observation

Following this architecture will ensure the KMM implementation maintains the same performance and user experience as V2 while working across both Android and iOS platforms.