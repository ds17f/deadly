# ShowDetail Screen Implementation Plan (Final)

## Overview
Implement a comprehensive ShowDetail screen (formerly "playlist" screen in V2) following V2's proven architecture for network calls, domain model caching, and dual route navigation. This will strictly follow the Universal Service + Platform Tool pattern with maximum simplicity: platform tools only handle generic operations (HTTP, file I/O, media playback) while ALL business logic remains universal.

## Architecture Analysis

### V2 Pattern Study
**Core Components**:
- **PlaylistScreen.kt** - Main screen with LazyColumn, floating back button, progressive loading
- **PlaylistViewModel** - Reactive state management with multiple UI state flows
- **PlaylistService** - Business logic interface with media controller integration
- **ArchiveService** - Network layer with domain model caching
- **11 Dedicated Components** - Header, AlbumArt, ShowInfo, Rating, ActionRow, TrackList, etc.

**Navigation Pattern**:
- Dual routes: `playlist/{showId}` and `playlist/{showId}/{recordingId}`
- Service method: `loadShow(showId: String?, recordingId: String?)`
- Auto-selection of best recording when recordingId is null

**Network & Caching Architecture**:
- **Retrofit** (Android HTTP library) - Interface with annotations for Archive.org API
- **Domain Model Caching** - Cache converted models, not raw JSON
- **Filename Pattern** - `{recordingId}.{type}.json` (metadata, tracks, reviews)
- **Cache-First Strategy** - Check cache → network → cache result
- **Background Prefetching** - Adjacent show prefetching with coroutine management
- **Smart Format Selection** - Priority-based format selection (VBR MP3 > MP3 > Ogg Vorbis)

### Revised Architecture: Universal Service + Minimal Platform Tools

**Universal Services** (commonMain - ALL business logic):
- **ShowDetailService** - Show loading, navigation, state management, prefetching
- **ArchiveService** - Cache-first logic, URL building, domain conversion, smart format selection
- **MediaPlayerRepository** - Playback coordination, queue management, reactive state flows

**Minimal Platform Tools** (expect/actual - generic operations only):
- **NetworkClient** - Generic HTTP: `getJson(url): Result<String>`
- **CacheManager** - Generic file I/O: `get(key, type)`, `put(key, type, data)`
- **PlatformMediaPlayer** - Generic media: `loadAndPlay(url)`, `pause()`, `resume()`

## Implementation Strategy

### Phase 1: Foundation & Navigation
**Navigation Integration**:
```kotlin
// Update AppScreen sealed interface
sealed interface AppScreen {
    data class ShowDetail(val showId: String, val recordingId: String? = null) : AppScreen
}

// Route handling in DeadlyNavHost
fun AppScreen.route(): String = when (this) {
    is AppScreen.ShowDetail -> if (recordingId != null) {
        "showdetail/$showId/$recordingId"
    } else {
        "showdetail/$showId"
    }
}

// Screen composable
@Composable
fun ShowDetailScreen(
    showId: String?,
    recordingId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    viewModel: ShowDetailViewModel
) {
    LaunchedEffect(showId, recordingId) {
        viewModel.loadShow(showId, recordingId)
    }
    // LazyColumn layout matching V2...
}
```

### Phase 2: Minimal Platform Tools
**Platform Tool 1: NetworkClient (Generic HTTP)**
```kotlin
// commonMain - Generic interface
expect class NetworkClient {
    suspend fun getJson(url: String): Result<String>
}

// androidMain - OkHttp implementation
actual class NetworkClient(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    actual suspend fun getJson(url: String): Result<String> {
        return try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                Result.success(response.body!!.string())
            } else {
                Result.failure(Exception("HTTP error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// iosMain - Ktor implementation
actual class NetworkClient(
    private val httpClient: HttpClient = HttpClient()
) {
    actual suspend fun getJson(url: String): Result<String> {
        return try {
            val response: String = httpClient.get(url).bodyAsText()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Platform Tool 2: CacheManager (Generic File I/O)**
```kotlin
// commonMain - Generic interface
enum class CacheType { METADATA, TRACKS, REVIEWS }

expect class CacheManager {
    suspend fun get(key: String, type: CacheType): String?
    suspend fun put(key: String, type: CacheType, data: String)
    suspend fun isExpired(key: String, type: CacheType): Boolean
    suspend fun clear(key: String?, type: CacheType?)
}

// androidMain - Android filesystem
actual class CacheManager(
    private val context: Context
) {
    private val cacheDir: File
        get() = File(context.cacheDir, "archive").apply { mkdirs() }

    actual suspend fun get(key: String, type: CacheType): String? = withContext(Dispatchers.IO) {
        val file = File(cacheDir, "$key.${type.name.lowercase()}.json")
        if (file.exists() && !isExpired(key, type)) {
            file.readText()
        } else null
    }

    actual suspend fun put(key: String, type: CacheType, data: String) = withContext(Dispatchers.IO) {
        val file = File(cacheDir, "$key.${type.name.lowercase()}.json")
        file.writeText(data)
    }

    actual suspend fun isExpired(key: String, type: CacheType): Boolean {
        val file = File(cacheDir, "$key.${type.name.lowercase()}.json")
        if (!file.exists()) return true
        val ageHours = (System.currentTimeMillis() - file.lastModified()) / (1000 * 60 * 60)
        return ageHours > 168 // 1 week expiry
    }

    actual suspend fun clear(key: String?, type: CacheType?) = withContext(Dispatchers.IO) {
        if (key != null && type != null) {
            File(cacheDir, "$key.${type.name.lowercase()}.json").delete()
        } else {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }
}

// iosMain - iOS NSFileManager implementation
actual class CacheManager {
    private val cacheDir: String
        get() = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            .first() as String + "/archive"

    actual suspend fun get(key: String, type: CacheType): String? {
        // iOS NSFileManager implementation
        val filePath = "$cacheDir/$key.${type.name.lowercase()}.json"
        return if (NSFileManager.defaultManager.fileExistsAtPath(filePath) && !isExpired(key, type)) {
            NSString.stringWithContentsOfFile(filePath, NSUTF8StringEncoding, null)
        } else null
    }

    // Similar implementations for put, isExpired, clear...
}
```

**Platform Tool 3: PlatformMediaPlayer (Generic Media)**
```kotlin
// commonMain - Generic interface
data class PlatformPlaybackState(
    val isPlaying: Boolean,
    val currentPositionMs: Long,
    val durationMs: Long,
    val isLoading: Boolean,
    val error: String?
)

expect class PlatformMediaPlayer {
    suspend fun loadAndPlay(url: String): Result<Unit>
    suspend fun pause(): Result<Unit>
    suspend fun resume(): Result<Unit>
    suspend fun seekTo(positionMs: Long): Result<Unit>
    val playbackState: Flow<PlatformPlaybackState>
}

// androidMain - ExoPlayer implementation
actual class PlatformMediaPlayer(
    private val context: Context
) {
    private val exoPlayer = ExoPlayer.Builder(context).build()
    private val _playbackState = MutableStateFlow(PlatformPlaybackState(false, 0, 0, false, null))

    actual val playbackState: Flow<PlatformPlaybackState> = _playbackState.asStateFlow()

    actual suspend fun loadAndPlay(url: String): Result<Unit> {
        return try {
            _playbackState.value = _playbackState.value.copy(isLoading = true)
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            Result.success(Unit)
        } catch (e: Exception) {
            _playbackState.value = _playbackState.value.copy(isLoading = false, error = e.message)
            Result.failure(e)
        }
    }

    // pause, resume, seekTo implementations...
}

// iosMain - AVPlayer implementation
actual class PlatformMediaPlayer {
    private val avPlayer = AVPlayer()
    // Similar AVPlayer implementation...
}
```

### Phase 3: Universal Services
**Universal ArchiveService (ALL business logic)**
```kotlin
// commonMain - ALL Archive.org knowledge and logic here
class ArchiveServiceImpl(
    private val networkClient: NetworkClient,
    private val cacheManager: CacheManager
) : ArchiveService {

    companion object {
        private const val TAG = "ArchiveService"
        private const val ARCHIVE_BASE_URL = "https://archive.org/metadata"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Universal cache-first logic with Archive.org URL building
    override suspend fun getRecordingTracks(recordingId: String): Result<List<Track>> {
        return try {
            Log.d(TAG, "getRecordingTracks($recordingId)")

            // 1. Check cache first (universal cache logic)
            val cached = cacheManager.get(recordingId, CacheType.TRACKS)
            if (cached != null) {
                Log.d(TAG, "Cache hit for tracks: $recordingId")
                val tracks = json.decodeFromString<List<Track>>(cached)
                return Result.success(tracks)
            }

            // 2. Cache miss - build Archive.org URL (universal business logic)
            Log.d(TAG, "Cache miss for tracks: $recordingId, fetching from API")
            val url = "$ARCHIVE_BASE_URL/$recordingId"

            // 3. Fetch JSON (generic platform tool)
            val jsonResult = networkClient.getJson(url)

            if (jsonResult.isSuccess) {
                val rawJson = jsonResult.getOrNull()!!

                // 4. Parse Archive.org response (universal business logic)
                val apiResponse = json.decodeFromString<ArchiveMetadataResponse>(rawJson)
                val tracks = archiveMapper.mapToTracks(apiResponse) // Universal domain conversion

                // 5. Cache the converted domain models (universal cache logic)
                val serializedTracks = json.encodeToString(tracks)
                cacheManager.put(recordingId, CacheType.TRACKS, serializedTracks)

                Log.d(TAG, "Cached ${tracks.size} tracks for: $recordingId")
                return Result.success(tracks)
            } else {
                Log.w(TAG, "Network error for tracks: $recordingId - ${jsonResult.exceptionOrNull()}")
                return Result.failure(jsonResult.exceptionOrNull() ?: Exception("Unknown network error"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting tracks for $recordingId", e)
            return Result.failure(e)
        }
    }

    override suspend fun getRecordingMetadata(recordingId: String): Result<RecordingMetadata> {
        // Same pattern: cache-first, URL building, domain conversion, cache storage
        return try {
            val cached = cacheManager.get(recordingId, CacheType.METADATA)
            if (cached != null) {
                return Result.success(json.decodeFromString<RecordingMetadata>(cached))
            }

            val url = "$ARCHIVE_BASE_URL/$recordingId"
            val jsonResult = networkClient.getJson(url)

            if (jsonResult.isSuccess) {
                val rawJson = jsonResult.getOrNull()!!
                val apiResponse = json.decodeFromString<ArchiveMetadataResponse>(rawJson)
                val metadata = archiveMapper.mapToRecordingMetadata(apiResponse)

                cacheManager.put(recordingId, CacheType.METADATA, json.encodeToString(metadata))
                return Result.success(metadata)
            } else {
                return Result.failure(jsonResult.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecordingReviews(recordingId: String): Result<List<Review>> {
        // Same pattern for reviews...
    }

    // Universal cache management
    override suspend fun clearCache(recordingId: String): Result<Unit> {
        return try {
            cacheManager.clear(recordingId, CacheType.TRACKS)
            cacheManager.clear(recordingId, CacheType.METADATA)
            cacheManager.clear(recordingId, CacheType.REVIEWS)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAllCache(): Result<Unit> {
        return try {
            cacheManager.clear(null, null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Universal ShowDetailService (ALL show business logic)**
```kotlin
// commonMain - ALL show management logic here
class ShowDetailServiceImpl(
    private val showRepository: ShowRepository,
    private val archiveService: ArchiveService,
    private val mediaPlayerRepository: MediaPlayerRepository
) : ShowDetailService {

    companion object {
        private const val TAG = "ShowDetailService"
        private const val DEFAULT_SHOW_ID = "1977-05-08" // Cornell '77 fallback

        // Universal format priority (V2 pattern)
        val DEFAULT_FORMAT_PRIORITY = listOf(
            "VBR MP3",      // Best balance for streaming
            "MP3",          // Universal fallback
            "Ogg Vorbis"    // Good quality, efficient
        )
    }

    // Universal state management
    private var currentShow: Show? = null
    private var currentRecordingId: String? = null
    private var currentSelectedFormat: String? = null

    // V2's dual-layer caching (universal performance optimization)
    private val trackCache = ConcurrentHashMap<String, List<Track>>()
    private val prefetchJobs = ConcurrentHashMap<String, Job>()

    // Universal show loading (V2 pattern)
    override suspend fun loadShow(showId: String?, recordingId: String?) {
        Log.d(TAG, "Loading show: $showId, recordingId: $recordingId")

        currentShow = if (showId != null) {
            showRepository.getShowById(showId)
        } else {
            showRepository.getShowById(DEFAULT_SHOW_ID)
        }

        if (currentShow != null) {
            Log.d(TAG, "Loaded show: ${currentShow!!.displayTitle}")

            // Use provided recordingId (from navigation) or fall back to best recording (universal logic)
            currentRecordingId = recordingId ?: currentShow!!.bestRecordingId
            Log.d(TAG, "Using recording: $currentRecordingId")
        } else {
            Log.w(TAG, "Failed to load show with ID: $showId")
        }
    }

    // Universal track loading with cache optimization (V2 pattern)
    override suspend fun getTrackList(): List<TrackInfo> {
        Log.d(TAG, "getTrackList() - Cache-first implementation with prefetch")

        val recordingId = currentRecordingId ?: return emptyList()

        // 1. Check memory cache first (universal session performance)
        trackCache[recordingId]?.let { cachedTracks ->
            Log.d(TAG, "Memory cache HIT for recording: $recordingId (${cachedTracks.size} tracks)")

            // Universal smart format selection
            val selectedFormat = selectBestAvailableFormat(cachedTracks)
            if (selectedFormat == null) {
                Log.w(TAG, "No compatible format found in cached tracks")
                return emptyList()
            }

            currentSelectedFormat = selectedFormat
            val filteredTracks = filterTracksToFormat(cachedTracks, selectedFormat)

            // Universal background prefetch
            startAdjacentPrefetch()
            return convertTracksToTrackInfo(filteredTracks)
        }

        // 2. Memory cache miss - get from ArchiveService (universal service, handles filesystem cache + network)
        Log.d(TAG, "Memory cache MISS - Loading from ArchiveService")
        val result = archiveService.getRecordingTracks(recordingId)

        if (result.isSuccess) {
            val tracks = result.getOrNull() ?: emptyList()
            Log.d(TAG, "Got ${tracks.size} tracks from ArchiveService")

            // Store in memory cache (universal performance optimization)
            trackCache[recordingId] = tracks

            // Universal smart format selection
            val selectedFormat = selectBestAvailableFormat(tracks)
            if (selectedFormat == null) {
                Log.w(TAG, "No compatible format found in loaded tracks")
                return emptyList()
            }

            currentSelectedFormat = selectedFormat
            val filteredTracks = filterTracksToFormat(tracks, selectedFormat)

            // Universal background prefetch
            startAdjacentPrefetch()

            return convertTracksToTrackInfo(filteredTracks)
        } else {
            Log.e(TAG, "Error loading tracks: ${result.exceptionOrNull()}")
            return emptyList()
        }
    }

    // Universal smart format selection (V2 pattern)
    private fun selectBestAvailableFormat(tracks: List<Track>): String? {
        Log.d(TAG, "Selecting best format from ${tracks.size} tracks")
        Log.d(TAG, "Available formats: ${tracks.map { it.format }.distinct()}")

        for (preferredFormat in DEFAULT_FORMAT_PRIORITY) {
            val tracksInFormat = tracks.filter {
                it.format.equals(preferredFormat, ignoreCase = true)
            }
            if (tracksInFormat.isNotEmpty()) {
                Log.d(TAG, "Selected format '$preferredFormat' (${tracksInFormat.size} tracks)")
                return preferredFormat
            }
        }

        Log.w(TAG, "No tracks found in any preferred format")
        return null
    }

    // Universal background prefetching (V2 pattern)
    private fun startAdjacentPrefetch() {
        currentShow?.let { current ->
            coroutineScope.launch {
                Log.d(TAG, "Starting adjacent prefetch for current show: ${current.displayTitle}")

                // Prefetch next 2 shows (universal database + service logic)
                var currentNextDate = current.date
                repeat(2) { index ->
                    val nextShow = showRepository.getNextShowByDate(currentNextDate)
                    if (nextShow != null) {
                        currentNextDate = nextShow.date
                        val recordingId = nextShow.bestRecordingId
                        if (recordingId != null && !trackCache.containsKey(recordingId)) {
                            startPrefetchInternal(nextShow, recordingId, "next+${index + 1}")
                        }
                    }
                }

                // Prefetch previous 2 shows (universal database + service logic)
                var currentPrevDate = current.date
                repeat(2) { index ->
                    val previousShow = showRepository.getPreviousShowByDate(currentPrevDate)
                    if (previousShow != null) {
                        currentPrevDate = previousShow.date
                        val recordingId = previousShow.bestRecordingId
                        if (recordingId != null && !trackCache.containsKey(recordingId)) {
                            startPrefetchInternal(previousShow, recordingId, "previous+${index + 1}")
                        }
                    }
                }
            }
        }
    }

    // All other universal business logic methods (navigation, recording selection, etc.)...
}
```

### Phase 4: Component Architecture
**UI Components (Universal - commonMain)**:
All UI components remain in commonMain with no platform-specific code:

1. **ShowDetailHeader.kt** - Floating back button overlay
2. **ShowDetailAlbumArt.kt** - Fixed album artwork section
3. **ShowDetailInfo.kt** - Show metadata with prev/next navigation
4. **ShowDetailRating.kt** - Interactive rating display
5. **ShowDetailActions.kt** - Play, library, download, menu buttons
6. **ShowDetailTrackList.kt** - Track listing with progressive loading
7. **Modal Sheets** - Menu, reviews, setlist, recording selection

### Phase 5: ViewModel & State Management
**ShowDetailViewModel (Universal - commonMain)**:
```kotlin
// commonMain - ALL state management logic here
class ShowDetailViewModel(
    private val showDetailService: ShowDetailService,
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val libraryService: LibraryService
) : ViewModel() {

    private val _baseUiState = MutableStateFlow(ShowDetailUiState())

    // Universal reactive state combining (V2 pattern)
    val uiState: StateFlow<ShowDetailUiState> = combine(
        _baseUiState,
        showDetailService.currentShow,
        showDetailService.trackList,
        mediaPlayerRepository.playbackState,
        libraryService.isInLibrary(showId)
    ) { baseState, show, tracks, playback, isInLibrary ->
        // Universal state combining logic
        ShowDetailUiState(
            isLoading = baseState.isLoading,
            isTrackListLoading = baseState.isTrackListLoading,
            error = baseState.error,
            showData = show?.copy(isInLibrary = isInLibrary),
            tracks = tracks.map { updateWithPlaybackState(it, playback) },
            isPlaying = playback.isPlaying,
            isCurrentShowAndRecording = isCurrentContext(show, playback),
            showReviewSheet = baseState.showReviewSheet,
            showMenuSheet = baseState.showMenuSheet,
            showSetlistSheet = baseState.showSetlistSheet
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShowDetailUiState())

    // Universal user interaction handling
    fun loadShow(showId: String?, recordingId: String?) {
        viewModelScope.launch {
            try {
                _baseUiState.value = _baseUiState.value.copy(isLoading = true, error = null)

                // Load show data immediately (database)
                showDetailService.loadShow(showId, recordingId)
                val showData = showDetailService.getCurrentShowInfo()
                _baseUiState.value = _baseUiState.value.copy(
                    isLoading = false,
                    showData = showData
                )

                // Load tracks asynchronously (cache + network)
                loadTrackListAsync()

            } catch (e: Exception) {
                _baseUiState.value = _baseUiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun loadTrackListAsync() {
        _baseUiState.value = _baseUiState.value.copy(isTrackListLoading = true)
        viewModelScope.launch {
            try {
                val tracks = showDetailService.getTrackList()
                _baseUiState.value = _baseUiState.value.copy(
                    isTrackListLoading = false,
                    tracks = tracks
                )
            } catch (e: Exception) {
                _baseUiState.value = _baseUiState.value.copy(
                    isTrackListLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun playTrack(trackInfo: TrackInfo) {
        viewModelScope.launch {
            val showData = uiState.value.showData
            val selectedFormat = showDetailService.getCurrentSelectedFormat()

            if (showData != null && selectedFormat != null) {
                val showContext = ShowContext(
                    showId = showData.showId,
                    showDate = showData.displayDate,
                    venue = showData.venue
                )

                mediaPlayerRepository.playTrack(
                    trackIndex = trackInfo.number - 1,
                    recordingId = showData.currentRecordingId ?: "",
                    format = selectedFormat,
                    showContext = showContext
                )
            }
        }
    }

    fun selectRecording(recordingId: String) {
        viewModelScope.launch {
            showDetailService.selectRecording(recordingId)
            loadTrackListAsync()
        }
    }

    fun refreshCurrentShow() {
        viewModelScope.launch {
            val recordingId = uiState.value.showData?.currentRecordingId
            if (recordingId != null) {
                archiveService.clearCache(recordingId)
                loadTrackListAsync()
            }
        }
    }
}
```

### Phase 6: Cache Controls & Integration
**Cache Management UI (Universal)**:
```kotlin
// Pull-to-refresh (universal UI)
PullToRefreshBox(
    isRefreshing = uiState.isRefreshing,
    onRefresh = { viewModel.refreshCurrentShow() }
) {
    LazyColumn {
        // ShowDetail content...
    }
}

// Settings cache controls (universal)
fun clearAllCache() {
    viewModelScope.launch {
        val result = archiveService.clearAllCache()
        if (result.isSuccess) {
            // Show success message
        } else {
            // Show error message
        }
    }
}
```

## File Structure (Final)

```
composeApp/src/commonMain/kotlin/com/grateful/deadly/
├── navigation/
│   └── AppScreen.kt                          # ShowDetail(showId, recordingId?)
├── feature/showdetail/
│   ├── ShowDetailScreen.kt                   # Universal UI
│   ├── ShowDetailViewModel.kt                # Universal state management
│   └── components/                           # All universal UI components
│       ├── ShowDetailHeader.kt
│       ├── ShowDetailAlbumArt.kt
│       ├── ShowDetailInfo.kt
│       ├── ShowDetailRating.kt
│       ├── ShowDetailActions.kt
│       ├── ShowDetailTrackList.kt
│       └── sheets/
│           ├── ShowDetailMenuSheet.kt
│           ├── ShowDetailReviewSheet.kt
│           └── ShowDetailSetlistSheet.kt
├── services/showdetail/
│   ├── ShowDetailService.kt                  # Universal interface
│   └── ShowDetailServiceImpl.kt              # Universal implementation
├── services/archive/
│   ├── ArchiveService.kt                     # Universal interface
│   ├── ArchiveServiceImpl.kt                 # Universal implementation (ALL Archive.org logic)
│   └── platform/
│       ├── NetworkClient.kt                  # expect class - generic HTTP
│       └── CacheManager.kt                   # expect class - generic file I/O
├── services/media/
│   ├── MediaPlayerRepository.kt              # Universal interface
│   ├── MediaPlayerRepositoryImpl.kt          # Universal implementation (ALL playback logic)
│   └── platform/
│       └── PlatformMediaPlayer.kt            # expect class - generic media playback
└── domain/models/
    ├── ShowDetail.kt                         # Universal UI models
    └── Archive.kt                            # Universal domain models

composeApp/src/androidMain/kotlin/com/grateful/deadly/services/
├── archive/platform/
│   ├── NetworkClient.kt                      # OkHttp implementation
│   └── CacheManager.kt                       # Android File implementation
└── media/platform/
    └── PlatformMediaPlayer.kt                # ExoPlayer implementation

composeApp/src/iosMain/kotlin/com/grateful/deadly/services/
├── archive/platform/
│   ├── NetworkClient.kt                      # Ktor implementation
│   └── CacheManager.kt                       # iOS NSFileManager implementation
└── media/platform/
    └── PlatformMediaPlayer.kt                # AVPlayer implementation
```

## Key Architectural Benefits

### ✅ Maximum Business Logic Centralization
- **Archive.org URLs** - Built in universal ArchiveServiceImpl
- **Cache-first logic** - Single implementation in universal ArchiveServiceImpl
- **Smart format selection** - Universal logic in ShowDetailServiceImpl
- **Background prefetching** - Universal coroutine management
- **Playback coordination** - Universal state management in MediaPlayerRepositoryImpl
- **Domain model conversion** - Universal mapping logic
- **Error handling** - Universal retry and fallback strategies

### ✅ Minimal Platform Tools
- **NetworkClient** - Generic `getJson(url)` operation (no Archive.org knowledge)
- **CacheManager** - Generic file I/O operations (no business logic)
- **PlatformMediaPlayer** - Generic media operations (no Archive.org knowledge)

### ✅ Maximum Testability
- **Universal services** - Test all business logic in commonTest
- **Mock platform tools** - Simple interfaces to mock (`getJson`, `get/put`, `loadAndPlay`)
- **No platform dependencies** - Business logic tests run without Android/iOS

## Success Criteria

### Architecture Compliance
- [ ] Universal services contain ALL business logic (Archive.org URLs, caching strategies, domain conversion)
- [ ] Platform tools are minimal, generic interfaces (HTTP, file I/O, media playback)
- [ ] No Archive.org knowledge in platform tools
- [ ] No business logic in platform tools
- [ ] Universal services fully testable in commonTest

### V2 Functional Parity
- [ ] Domain model caching with `{recordingId}.{type}.json` filenames
- [ ] Cache-first strategy with long expiry (1 week)
- [ ] Smart format selection with priority fallback (VBR MP3 > MP3 > Ogg Vorbis)
- [ ] Background prefetching for adjacent shows (next 2, previous 2)
- [ ] Dual route navigation: `showdetail/{showId}` and `showdetail/{showId}/{recordingId}`
- [ ] Progressive loading (show immediate, tracks with spinner)
- [ ] All V2 UI components and modal interactions

### Cross-Platform Consistency
- [ ] Identical business logic behavior on Android and iOS
- [ ] Platform-optimized HTTP libraries (OkHttp vs Ktor)
- [ ] Platform-optimized media players (ExoPlayer vs AVPlayer)
- [ ] Consistent caching and format selection across platforms

### Cache Management
- [ ] Pull-to-refresh on ShowDetail screen (clears cache for current recording)
- [ ] Settings → Clear All Cache button (clears all cached domain models)
- [ ] Manual cache controls via triple-dot menu
- [ ] Long cache expiry (1 week) with manual refresh options

## Timeline

- **Phase 1**: Navigation foundation with dual route setup
- **Phase 2**: Minimal platform tools (NetworkClient.getJson, CacheManager, PlatformMediaPlayer)
- **Phase 3**: Universal ArchiveService with cache-first logic and Archive.org URL building
- **Phase 4**: Universal ShowDetailService with V2 patterns, prefetching, and smart format selection
- **Phase 5**: UI components and universal ShowDetailViewModel with reactive state management
- **Phase 6**: Universal MediaPlayerRepository with playback coordination and Archive.org URL building
- **Phase 7**: Cache controls (pull-to-refresh, settings), integration testing, and polish

This architecture achieves maximum simplicity for platform tools while keeping ALL Archive.org knowledge, business logic, caching strategies, and URL building centralized in universal services.