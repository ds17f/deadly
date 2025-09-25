# ShowDetail Screen Implementation Plan - Corrected Architecture

## **Executive Summary**

This document provides a corrected implementation plan for the ShowDetail screen (formerly "Playlist" in V2) after analyzing the failed stashed attempt and properly understanding V2's proven architecture patterns. The plan leverages our existing strong foundations while correctly replicating V2's successful patterns using the Universal Service + Platform Tool architecture.

## **Problem Analysis - What Went Wrong in the Stash**

### **❌ Critical Architectural Violations**

1. **Navigation Confusion**: The stash attempted to support both `showDetail/{showId}` and `showDetail/{showId}/{recordingId}` routes but implemented them incorrectly, misunderstanding V2's actual navigation patterns.

2. **Mixed Responsibilities**: The `ShowDetailViewModel` was attempting to handle Archive.org API calls directly, violating the Universal Service pattern where business logic should be in services, not ViewModels.

3. **Domain Model Duplication**: Created new `ShowMetadata`, `ShowTrack`, `ShowReview` models instead of using our existing rich domain models (`Show`, `Recording`) that are already in the database with proper relationships.

4. **Stub Implementation**: Created empty stub methods in `ArchiveService` but didn't implement the actual Archive.org integration following V2's proven patterns.

5. **Wrong Service Architecture**: Put Archive.org business logic in the wrong places instead of following the Universal Service + Platform Tool pattern correctly.

### **✅ V2's Proven Architecture (What We Should Replicate)**

From comprehensive analysis of V2's implementation, the proven patterns are:

**Database-First Approach**:
- Show data comes from local database immediately (no loading spinners for navigation)
- Archive.org API enhances the show with tracks/metadata asynchronously
- Progressive loading: DB → UI → API → enhanced UI

**Smart Caching Strategy**:
- Domain models cached as JSON files with 24-hour expiry
- Cache-first: Check cache → network → cache result
- Background prefetching of adjacent shows

**Format Selection Logic**:
- Priority: VBR MP3 → MP3 → Ogg Vorbis (excludes FLAC for streaming)
- Explicit failure handling with available formats list
- Smart fallback system

**Reactive State Management**:
- StateFlow-based combining multiple data streams
- Multiple reactive streams combined into single UI state
- Clean separation: services provide data, ViewModels handle UI state

**Navigation Patterns**:
- `playlist/{showId}` - Show chooses best recording automatically
- `playlist/{showId}/{recordingId}` - Specific recording (from Player navigation)
- Service method: `loadShow(showId: String?, recordingId: String?)`

## **Current Codebase Strengths**

We already have excellent foundations that the stash ignored:

### **✅ Rich Domain Models**
- `Show`: Complete business object with computed properties, venue, location, setlist, lineup
- `Recording`: Source types, ratings, display methods
- `Venue`, `Location`: Rich geographical and venue data
- All properly serializable and with business logic built-in

### **✅ SQLDelight Database**
- Complete show and recording data with relationships
- Efficient queries for navigation (next/previous shows)
- User library state management
- Already populated with comprehensive data

### **✅ Universal Service + Platform Tool Pattern**
- Established pattern with NetworkClient, CacheManager, PlatformMediaPlayer
- DI system properly configured
- Service interfaces defined

### **✅ Cross-Platform Infrastructure**
- Navigation system working with expect/actual pattern
- Icon system supporting both platforms
- Build system and remote development workflow

## **Corrected Implementation Plan**

### **Phase 1: Fix Navigation & Core Service Architecture**

#### **Navigation Implementation**
- **Support Both Routes**:
  - `showDetail/{showId}` - Primary route for show discovery
  - `showDetail/{showId}/{recordingId}` - Specific recording selection
- **Route Parsing**: Update navigation to handle both parameter patterns correctly
- **Parameter Handling**: ShowDetailViewModel handles both showId-only and showId+recordingId cases

#### **ShowDetailService Creation**
```kotlin
// Universal service coordinating database + Archive.org
class ShowDetailService(
    private val showRepository: ShowRepository,
    private val recordingRepository: RecordingRepository,
    private val archiveService: ArchiveService
) {
    suspend fun loadShow(showId: String?, recordingId: String?): ShowDetailData
    suspend fun getShowTracks(recordingId: String): List<Track>
    suspend fun selectRecording(recordingId: String)
    suspend fun getAdjacentShows(currentShowId: String): AdjacentShows
    // ... other coordination methods
}
```

#### **Domain Integration Strategy**
- **Reuse Existing Models**: Enhance `Show`/`Recording` models with Archive.org data
- **Progressive Enhancement**: Database data immediate, API data additive
- **No Duplication**: Don't create new models when we have rich domain objects

### **Phase 2: Complete ArchiveService Implementation**

#### **Real Archive.org API Integration**
```kotlin
class ArchiveService(
    private val networkClient: NetworkClient,
    private val cacheManager: CacheManager
) {
    // Replace stubs with real implementations
    suspend fun getShowMetadata(recordingId: String): Result<ArchiveMetadata>
    suspend fun getShowTracks(recordingId: String): Result<List<Track>>
    suspend fun getShowReviews(recordingId: String): Result<List<Review>>
}
```

#### **Track Discovery & Format Selection**
- **Parse Files Array**: Extract audio files from Archive.org metadata response
- **Smart Filtering**: Remove short files, non-audio files, duplicates
- **Format Priority**: Implement V2's VBR MP3 → MP3 → Ogg Vorbis logic
- **URL Construction**: Build proper Archive.org download URLs

#### **V2-Compatible Caching**
- **Cache Strategy**: Domain models cached as JSON (not raw API responses)
- **Expiry Logic**: 24-hour cache expiry with manual refresh options
- **Cache Structure**: `{recordingId}.{type}.json` pattern matching V2
- **Background Updates**: Prefetch adjacent shows transparently

#### **Error Handling & Resilience**
- **Result Pattern**: Use Result<T> throughout with detailed error messages
- **Graceful Degradation**: Show works without Archive.org data
- **Retry Logic**: Smart retry for transient failures
- **Format Fallback**: Explicit failure with available formats

### **Phase 3: ShowDetailViewModel & Reactive State**

#### **Pure Reactive State Management**
```kotlin
class ShowDetailViewModel(
    private val showDetailService: ShowDetailService,
    private val mediaService: MediaService
) {
    // Combine multiple streams like V2
    val uiState: StateFlow<ShowDetailUiState> = combine(
        _baseUiState,
        showDetailService.currentShow,
        showDetailService.currentTracks,
        mediaService.playbackState,
        libraryService.isInLibrary(showId)
    ) { baseState, show, tracks, playback, isInLibrary ->
        // Complex state transformation
    }.stateIn(scope, strategy, initialValue)
}
```

#### **Progressive Loading States**
- **Immediate**: Show database data (no loading spinner for navigation)
- **Loading**: Archive.org tracks loading indicator
- **Loaded**: Complete show with tracks and playback integration
- **Error**: Graceful error states with retry options

#### **Media Player Integration**
- **Reactive Playback State**: Observe MediaService for current track highlighting
- **Track Selection**: Handle play/pause, track selection, seeking
- **Queue Management**: Coordinate with global playback queue
- **Format Handling**: Work with MediaService's format preferences

### **Phase 4: UI Implementation**

#### **ShowDetailScreen Layout**
```kotlin
@Composable
fun ShowDetailScreen(
    showId: String,
    recordingId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: ShowDetailViewModel
) {
    // LazyColumn with V2's progressive loading pattern
    // Floating back button overlay
    // Error handling with snackbars
}
```

#### **Component Architecture**
- **ShowDetailHeader**: Show info, venue, date, rating, play button
- **ShowDetailTrackList**: Track listing with playback state integration
- **ShowDetailPlayer**: Integrated mini-player when track is playing
- **ShowDetailActions**: Library, share, download, menu actions
- **Modal Sheets**: Reviews, setlist, recording selection

#### **Loading & Error States**
- **Progressive Loading**: Show info immediate, tracks with loading indicator
- **Shimmer Effects**: For track list while loading
- **Error Boundaries**: Handle API failures gracefully
- **Retry Mechanisms**: Pull-to-refresh, manual retry buttons
- **Empty States**: Proper messaging when no tracks available

### **Phase 5: Advanced Features**

#### **Recording Selection**
- **Multiple Recordings**: Show available recordings with metadata
- **Recording Comparison**: Source type, rating, taper info
- **Smart Defaults**: Auto-select best recording based on V2 algorithm
- **User Preferences**: Remember user's recording choices per show

#### **Background Prefetching**
- **Adjacent Shows**: Prefetch next/previous shows transparently
- **Smart Caching**: Promote prefetch to foreground when user navigates
- **Memory Management**: Proper cleanup of prefetch operations
- **Network Awareness**: Respect user's data preferences

#### **Cache Management**
- **Settings Integration**: Cache size, clear cache options
- **Manual Refresh**: Pull-to-refresh clears current show cache
- **Storage Management**: Monitor cache size, implement LRU eviction
- **Cache Status**: Show cache hit/miss status for debugging

## **File Structure**

```
composeApp/src/commonMain/kotlin/com/grateful/deadly/
├── feature/showdetail/
│   ├── ShowDetailScreen.kt                   # Main UI screen
│   ├── ShowDetailViewModel.kt                # Reactive state management
│   ├── ShowDetailService.kt                  # Service interface
│   ├── ShowDetailServiceImpl.kt              # Service implementation
│   └── components/                           # UI components
│       ├── ShowDetailHeader.kt
│       ├── ShowDetailTrackList.kt
│       ├── ShowDetailPlayer.kt
│       ├── ShowDetailActions.kt
│       └── sheets/
│           ├── ReviewSheet.kt
│           ├── RecordingSelectionSheet.kt
│           └── SetlistSheet.kt
├── services/archive/
│   ├── ArchiveService.kt                     # Enhanced with real implementation
│   ├── models/
│   │   ├── ArchiveMetadata.kt               # Archive.org API models
│   │   ├── ArchiveTrack.kt
│   │   └── ArchiveReview.kt
│   └── platform/                             # Existing platform tools
│       ├── NetworkClient.kt
│       └── CacheManager.kt
└── domain/models/                            # Existing models to enhance
    ├── Show.kt                              # Add Archive.org integration
    ├── Recording.kt                         # Add track list support
    └── Track.kt                             # New model for Archive.org tracks
```

## **Success Criteria**

### **✅ Architecture Compliance**
- [ ] Universal services contain ALL business logic (Archive.org integration, caching)
- [ ] Platform tools remain minimal and generic (HTTP, file I/O, media)
- [ ] ViewModels only handle UI state, no business logic
- [ ] Proper separation of concerns throughout

### **✅ V2 Functional Parity**
- [ ] Database-first loading (immediate show data)
- [ ] Progressive enhancement with Archive.org tracks
- [ ] Smart format selection with V2's priority logic
- [ ] Cache-first strategy with 24-hour expiry
- [ ] Dual route navigation support
- [ ] Background prefetching of adjacent shows

### **✅ Domain Integration**
- [ ] Use existing `Show`/`Recording` models (no duplication)
- [ ] Enhance domain models with Archive.org data
- [ ] Maintain database relationships and computed properties
- [ ] Preserve existing business logic in domain models

### **✅ Cross-Platform Consistency**
- [ ] Identical behavior on Android and iOS
- [ ] Platform-optimized HTTP/caching implementations
- [ ] Consistent error handling and user experience
- [ ] Proper navigation integration

## **Key Architectural Principles**

### **✅ Database-First**
Always show local database data immediately for fast navigation, then enhance asynchronously with Archive.org data.

### **✅ Progressive Enhancement**
Database → UI → Archive.org API → Enhanced UI. Never block UI on API calls.

### **✅ Universal Service Pattern**
All Archive.org knowledge, caching strategies, and business logic in universal services. Platform tools only handle generic operations.

### **✅ Domain Model Reuse**
Enhance existing rich domain models instead of creating duplicates. Maintain the investment in our current data architecture.

### **✅ V2 Proven Patterns**
Replicate V2's successful caching, format selection, state management, and navigation patterns rather than reinventing them.

## **Timeline & Phases**

- **Phase 1** (Foundation): Navigation fix + ShowDetailService creation
- **Phase 2** (Core API): Complete ArchiveService implementation
- **Phase 3** (State Management): ViewModel + reactive state
- **Phase 4** (UI Implementation): Screen + components
- **Phase 5** (Polish): Advanced features + optimization

This plan leverages our excellent existing foundations while correctly implementing V2's proven architecture patterns using the Universal Service + Platform Tool approach.