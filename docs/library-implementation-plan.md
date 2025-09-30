# Library System Implementation Plan

## Overview
The library system allows users to save shows, organize them, pin favorites, and manage downloads. It's a complex reactive system where toggling library status in one place (like ShowDetail) automatically updates all UIs observing that show.

**Critical:** This plan ports V2's proven, working architecture to Deadly's KMM patterns. We are NOT getting creative or reductive - we're taking a working system and adapting it to our Universal Service + Platform Tool pattern.

## Implementation Progress

### ✅ Completed
- **Phase 1: Database Schema** - LibraryShow table created in SQLDelight with V2's exact structure
- **Phase 2: Domain Models** - LibraryModels.kt with LibraryShow, LibraryStats, enums, verified against V2

### 🚧 In Progress
- **Phase 3: Platform LibraryRepository** - Next up

### ⏳ Pending
- Phases 4-11 (Service, ViewModel, UI, Integration, Navigation, DI, Logging)

## V2 Architecture (Reference Implementation)

### Database Structure
V2 uses a **hybrid approach** for optimal performance:

1. **Show table has denormalized library columns** for fast queries without JOINs:
   - `isInLibrary: Boolean` - Fast filtering in queries
   - `libraryAddedAt: Long?` - Fast sorting by date added

2. **Separate `library_shows` table** for rich metadata:
```kotlin
@Entity(
    tableName = "library_shows",
    foreignKeys = [
        ForeignKey(
            entity = ShowEntity::class,
            parentColumns = ["showId"],
            childColumns = ["showId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["showId"], unique = true),
        Index(value = ["addedToLibraryAt"]),
        Index(value = ["isPinned"])
    ]
)
data class LibraryShowEntity(
    @PrimaryKey val showId: String,
    val addedToLibraryAt: Long,
    val isPinned: Boolean = false,
    val libraryNotes: String? = null,
    val customRating: Float? = null,
    val lastAccessedAt: Long? = null,
    val tags: String? = null
)
```

**Why both?**
- Show table columns enable fast `WHERE isInLibrary = 1` queries without JOIN
- Separate table stores rich metadata (pin, notes, custom rating) without bloating Show table
- This is V2's proven pattern - we copy it exactly

### V2 Data Flow
1. **LibraryDao** - Room database operations with Flow queries
2. **LibraryRepository** - Combines LibraryDao + ShowRepository to create rich `LibraryShow` domain models
3. **LibraryService** - Provides reactive StateFlows and Result-based operations
4. **LibraryViewModel** - Transforms service StateFlows into UI state with sorting/filtering
5. **LibraryScreen** - UI with hierarchical filtering, sorting, list/grid views

### V2 Reactive Pattern
```
User action (ShowDetail)
  → LibraryService.addToLibrary(showId)
  → LibraryRepository.addShowToLibrary(showId)
  → LibraryDao.addToLibrary(entity)
  → Room emits Flow update
  → LibraryRepository transforms to LibraryShow
  → LibraryService emits StateFlow update
  → ALL observing UIs update automatically (Home, Search, Library, ShowDetail)
```

## Current Deadly Architecture Gap

**Current State:** Deadly's Show table has `isInLibrary` and `libraryAddedAt` columns. ✅ This is correct per V2 pattern.

**Missing:** Separate `library_shows` table for rich metadata (pin status, notes, custom rating, tags).

**Solution:** Add the `library_shows` table to complete V2's hybrid architecture.

## Implementation Phases

### Phase 1: Database Schema (SQLDelight)
**Goal:** Create library_shows table matching V2's structure exactly

**File to create:**
`composeApp/src/commonMain/sqldelight/com/grateful/deadly/database/LibraryShow.sq`

```sql
-- Library shows table (V2 pattern - separate from Show table)
CREATE TABLE LibraryShow (
    showId TEXT PRIMARY KEY NOT NULL,
    addedToLibraryAt INTEGER NOT NULL,
    isPinned INTEGER NOT NULL DEFAULT 0,
    libraryNotes TEXT,
    customRating REAL,
    lastAccessedAt INTEGER,
    tags TEXT,

    FOREIGN KEY(showId) REFERENCES Show(showId) ON DELETE CASCADE
);

-- Indexes for performance (V2 pattern)
CREATE INDEX idx_library_shows_addedAt ON LibraryShow(addedToLibraryAt);
CREATE INDEX idx_library_shows_isPinned ON LibraryShow(isPinned);

-- Core CRUD operations
addToLibrary:
INSERT OR REPLACE INTO LibraryShow (showId, addedToLibraryAt, isPinned)
VALUES (?, ?, 0);

removeFromLibrary:
DELETE FROM LibraryShow WHERE showId = ?;

updatePinStatus:
UPDATE LibraryShow SET isPinned = ? WHERE showId = ?;

updateLibraryNotes:
UPDATE LibraryShow SET libraryNotes = ? WHERE showId = ?;

-- Reactive queries (return Flow)
getAllLibraryShowsFlow:
SELECT * FROM LibraryShow ORDER BY isPinned DESC, addedToLibraryAt DESC;

getPinnedLibraryShowsFlow:
SELECT * FROM LibraryShow WHERE isPinned = 1 ORDER BY addedToLibraryAt DESC;

getLibraryShowByIdFlow:
SELECT * FROM LibraryShow WHERE showId = ?;

-- Status checks (reactive)
isShowInLibraryFlow:
SELECT EXISTS(SELECT 1 FROM LibraryShow WHERE showId = ?) AS result;

isShowPinnedFlow:
SELECT EXISTS(SELECT 1 FROM LibraryShow WHERE showId = ? AND isPinned = 1) AS result;

-- Statistics (reactive)
getLibraryShowCountFlow:
SELECT COUNT(*) FROM LibraryShow;

getPinnedShowCountFlow:
SELECT COUNT(*) FROM LibraryShow WHERE isPinned = 1;

-- Bulk operations
clearLibrary:
DELETE FROM LibraryShow;

unpinAllShows:
UPDATE LibraryShow SET isPinned = 0;
```

**Migration needed:** Remove `isInLibrary` and `libraryAddedAt` from Show.sq (they're now in separate table)

**Testing:**
- Build project, verify SQLDelight generates Kotlin code
- Verify Database class includes LibraryShowQueries
- Test on both Android and iOS

---

### Phase 2: Domain Models
**Goal:** Create library-specific domain models matching V2 exactly

**File to create:**
`composeApp/src/commonMain/kotlin/com/grateful/deadly/domain/models/LibraryModels.kt`

```kotlin
package com.grateful.deadly.domain.models

import kotlinx.serialization.Serializable

/**
 * LibraryShow - Domain model combining Show with library metadata (V2 pattern)
 */
@Serializable
data class LibraryShow(
    val show: Show,
    val addedToLibraryAt: Long,
    val isPinned: Boolean = false,
    val libraryNotes: String? = null,
    val customRating: Float? = null,
    val lastAccessedAt: Long? = null,
    val tags: List<String>? = null,
    val downloadStatus: LibraryDownloadStatus = LibraryDownloadStatus.NOT_DOWNLOADED
) {
    // Convenience accessors
    val showId: String get() = show.id
    val date: String get() = show.date
    val venue: String get() = show.venue.name
    val location: String get() = show.location.displayText
    val displayTitle: String get() = show.displayTitle
    val averageRating: Float? get() = show.averageRating
    val totalReviews: Int get() = show.totalReviews

    // Library-specific computed properties
    val isPinnedAndDownloaded: Boolean get() = isPinned && downloadStatus == LibraryDownloadStatus.COMPLETED
    val libraryAge: Long get() = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - addedToLibraryAt
    val isDownloaded: Boolean get() = downloadStatus == LibraryDownloadStatus.COMPLETED
    val isDownloading: Boolean get() = downloadStatus == LibraryDownloadStatus.DOWNLOADING

    val libraryStatusDescription: String get() = when {
        isPinned && isDownloaded -> "Pinned & Downloaded"
        isPinned -> "Pinned"
        isDownloaded -> "Downloaded"
        isDownloading -> "Downloading..."
        else -> "In Library"
    }
}

/**
 * Download status for library shows (V2 pattern)
 */
@Serializable
enum class LibraryDownloadStatus {
    NOT_DOWNLOADED,
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Library statistics (V2 pattern)
 */
@Serializable
data class LibraryStats(
    val totalShows: Int,
    val totalPinned: Int,
    val totalDownloaded: Int = 0,
    val totalStorageUsed: Long = 0L
)

/**
 * Sort options for library display (V2 pattern)
 */
enum class LibrarySortOption(val displayName: String) {
    DATE_OF_SHOW("Show Date"),
    DATE_ADDED("Date Added"),
    VENUE("Venue"),
    RATING("Rating")
}

/**
 * Sort directions (V2 pattern)
 */
enum class LibrarySortDirection(val displayName: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
}

/**
 * Display modes for library (V2 pattern)
 */
enum class LibraryDisplayMode {
    LIST,
    GRID
}

/**
 * UI state for Library screens (V2 pattern)
 */
data class LibraryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val shows: List<LibraryShow> = emptyList(),
    val stats: LibraryStats = LibraryStats(0, 0),
    val selectedSortOption: LibrarySortOption = LibrarySortOption.DATE_ADDED,
    val selectedSortDirection: LibrarySortDirection = LibrarySortDirection.DESCENDING,
    val displayMode: LibraryDisplayMode = LibraryDisplayMode.LIST
)
```

**Testing:** Build project, verify models compile on both platforms

---

### Phase 3: Platform LibraryRepository (expect/actual)
**Goal:** Platform-specific database operations following V2's repository pattern

**File to create:**
`composeApp/src/commonMain/kotlin/com/grateful/deadly/services/library/platform/LibraryRepository.kt` (expect)

```kotlin
package com.grateful.deadly.services.library.platform

import com.grateful.deadly.database.Database
import com.grateful.deadly.domain.models.LibraryShow
import com.grateful.deadly.domain.models.LibraryStats
import kotlinx.coroutines.flow.Flow

/**
 * Platform-specific library database operations (V2 pattern)
 *
 * This is the platform tool in Universal Service + Platform Tool pattern.
 * Handles low-level database operations and joins LibraryShow table with Show table.
 */
expect class LibraryRepository(database: Database) {

    /**
     * Get all library shows as reactive flow (V2 pattern: combines library metadata + show data)
     */
    fun getLibraryShowsFlow(): Flow<List<LibraryShow>>

    /**
     * Get library statistics as reactive flow (V2 pattern)
     */
    fun getLibraryStatsFlow(): Flow<LibraryStats>

    /**
     * Add show to library (V2 pattern)
     */
    suspend fun addShowToLibrary(showId: String, timestamp: Long): Result<Unit>

    /**
     * Remove show from library (V2 pattern)
     */
    suspend fun removeShowFromLibrary(showId: String): Result<Unit>

    /**
     * Check if show is in library (reactive) (V2 pattern)
     */
    fun isShowInLibraryFlow(showId: String): Flow<Boolean>

    /**
     * Pin/unpin show (V2 pattern)
     */
    suspend fun updatePinStatus(showId: String, isPinned: Boolean): Result<Unit>

    /**
     * Check if show is pinned (reactive) (V2 pattern)
     */
    fun isShowPinnedFlow(showId: String): Flow<Boolean>

    /**
     * Update library notes (V2 pattern)
     */
    suspend fun updateLibraryNotes(showId: String, notes: String?): Result<Unit>

    /**
     * Clear entire library (V2 pattern)
     */
    suspend fun clearLibrary(): Result<Unit>

    /**
     * Unpin all shows (V2 pattern)
     */
    suspend fun unpinAllShows(): Result<Unit>
}
```

**Implementation pattern (same for Android and iOS):**
```kotlin
// androidMain/iosMain implementation
actual class LibraryRepository actual constructor(private val database: Database) {

    actual fun getLibraryShowsFlow(): Flow<List<LibraryShow>> {
        return database.libraryShowQueries.getAllLibraryShowsFlow()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .combine(database.showQueries.selectAllShows().asFlow().mapToList(Dispatchers.IO)) { libraryEntities, allShows ->
                // Join library metadata with show data
                val showsMap = allShows.associateBy { it.showId }
                libraryEntities.mapNotNull { libraryEntity ->
                    showsMap[libraryEntity.showId]?.let { showEntity ->
                        LibraryShow(
                            show = showEntity.toDomain(),
                            addedToLibraryAt = libraryEntity.addedToLibraryAt,
                            isPinned = libraryEntity.isPinned == 1L,
                            libraryNotes = libraryEntity.libraryNotes,
                            customRating = libraryEntity.customRating?.toFloat(),
                            lastAccessedAt = libraryEntity.lastAccessedAt,
                            tags = libraryEntity.tags?.split(",")?.map { it.trim() },
                            downloadStatus = LibraryDownloadStatus.NOT_DOWNLOADED // TODO: Add download integration
                        )
                    }
                }
            }
    }

    actual suspend fun addShowToLibrary(showId: String, timestamp: Long): Result<Unit> {
        return try {
            database.libraryShowQueries.addToLibrary(showId, timestamp)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ... other methods follow same pattern
}
```

**Testing:**
- Integration tests on both platforms
- Verify flows emit updates when data changes
- Test join logic returns complete LibraryShow objects

---

### Phase 4: Universal LibraryService
**Goal:** Universal business logic orchestrating library operations (V2 pattern)

**File to create:**
`composeApp/src/commonMain/kotlin/com/grateful/deadly/services/library/LibraryService.kt`

```kotlin
package com.grateful.deadly.services.library

import com.grateful.deadly.domain.models.LibraryShow
import com.grateful.deadly.domain.models.LibraryStats
import kotlinx.coroutines.flow.StateFlow

/**
 * Library service interface (V2 pattern)
 *
 * Provides reactive StateFlow for UI state management and Result-based operations.
 * All business logic is universal - delegates to platform LibraryRepository.
 */
interface LibraryService {

    /**
     * Get current library shows as reactive state (V2 pattern)
     */
    fun getCurrentShows(): StateFlow<List<LibraryShow>>

    /**
     * Get current library statistics (V2 pattern)
     */
    fun getLibraryStats(): StateFlow<LibraryStats>

    /**
     * Add a show to the user's library (V2 pattern)
     */
    suspend fun addToLibrary(showId: String): Result<Unit>

    /**
     * Remove a show from the user's library (V2 pattern)
     */
    suspend fun removeFromLibrary(showId: String): Result<Unit>

    /**
     * Clear all shows from the user's library (V2 pattern)
     */
    suspend fun clearLibrary(): Result<Unit>

    /**
     * Check if a show is in the user's library (reactive) (V2 pattern)
     */
    fun isShowInLibrary(showId: String): StateFlow<Boolean>

    /**
     * Pin a show for priority display (V2 pattern)
     */
    suspend fun pinShow(showId: String): Result<Unit>

    /**
     * Unpin a previously pinned show (V2 pattern)
     */
    suspend fun unpinShow(showId: String): Result<Unit>

    /**
     * Check if a show is pinned (reactive) (V2 pattern)
     */
    fun isShowPinned(showId: String): StateFlow<Boolean>

    /**
     * Update library notes for a show (V2 pattern)
     */
    suspend fun updateLibraryNotes(showId: String, notes: String?): Result<Unit>

    /**
     * Share a show (V2 pattern - delegates to platform share APIs)
     */
    suspend fun shareShow(showId: String): Result<Unit>
}
```

**Implementation:**
`composeApp/src/commonMain/kotlin/com/grateful/deadly/services/library/LibraryServiceImpl.kt`

```kotlin
class LibraryServiceImpl(
    private val libraryRepository: LibraryRepository,
    private val showRepository: ShowRepository,
    private val coroutineScope: CoroutineScope
) : LibraryService {

    // Real reactive StateFlows backed by database (V2 pattern)
    private val _currentShows: StateFlow<List<LibraryShow>> = libraryRepository
        .getLibraryShowsFlow()
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    override fun getCurrentShows(): StateFlow<List<LibraryShow>> = _currentShows

    override suspend fun addToLibrary(showId: String): Result<Unit> {
        Logger.d(TAG, "Adding show '$showId' to library")
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return libraryRepository.addShowToLibrary(showId, timestamp)
    }

    override fun isShowInLibrary(showId: String): StateFlow<Boolean> {
        return libraryRepository.isShowInLibraryFlow(showId)
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )
    }

    // ... other methods delegate to repository
}
```

**Testing:**
- Unit tests for business logic
- Verify reactive flows work correctly
- Test error handling

---

### Phase 5: LibraryViewModel
**Goal:** Transform service StateFlows into UI state with sorting/filtering (V2 pattern)

**File to create:**
`composeApp/src/commonMain/kotlin/com/grateful/deadly/feature/library/LibraryViewModel.kt`

```kotlin
class LibraryViewModel(
    private val libraryService: LibraryService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadLibrary()
    }

    /**
     * Load library shows and create reactive UI state (V2 pattern)
     */
    private fun loadLibrary() {
        viewModelScope.launch {
            try {
                // Create reactive UI state from service flows (V2 pattern)
                combine(
                    libraryService.getCurrentShows(),
                    libraryService.getLibraryStats()
                ) { shows, stats ->
                    LibraryUiState(
                        isLoading = false,
                        error = null,
                        shows = shows,
                        stats = stats
                    )
                }.collect { newState ->
                    _uiState.value = newState
                    Logger.d(TAG, "UI state updated: ${newState.shows.size} shows")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error loading library", e)
                _uiState.value = LibraryUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load library"
                )
            }
        }
    }

    // User actions delegate to service (V2 pattern)
    fun addToLibrary(showId: String) {
        viewModelScope.launch {
            libraryService.addToLibrary(showId)
        }
    }

    fun removeFromLibrary(showId: String) {
        viewModelScope.launch {
            libraryService.removeFromLibrary(showId)
        }
    }

    fun pinShow(showId: String) {
        viewModelScope.launch {
            libraryService.pinShow(showId)
        }
    }

    fun unpinShow(showId: String) {
        viewModelScope.launch {
            libraryService.unpinShow(showId)
        }
    }

    // ... other actions
}
```

**Testing:**
- Unit tests for sorting/filtering
- Verify reactive updates work
- Test error handling and retry

---

### Phase 6: UI Components (Cross-Platform)
**Goal:** Create reusable library UI components matching V2

**Files to create:**

1. `composeApp/src/commonMain/kotlin/com/grateful/deadly/core/design/components/HierarchicalFilter.kt`
   - Port V2's HierarchicalFilter to Compose Multiplatform
   - FilterNode, FilterPath data classes
   - FilterTrees utility for decade/season trees
   - **Copy from V2, adapt Material 3 imports**

2. `composeApp/src/commonMain/kotlin/com/grateful/deadly/feature/library/components/LibraryShowItems.kt`
   - `LibraryShowListItem` (V2 pattern: 2-line layout with pin/download indicators)
   - `LibraryShowGridItem` (V2 pattern: compact grid layout)

3. `composeApp/src/commonMain/kotlin/com/grateful/deadly/feature/library/components/LibraryBottomSheets.kt`
   - `SortOptionsBottomSheet` (V2 pattern)
   - `ShowActionsBottomSheet` (V2 pattern: share/pin/download/remove)

**Testing:**
- Visual testing on both platforms
- Verify Material 3 components render correctly
- Test interactions (clicks, long-press)

---

### Phase 7: LibraryScreen
**Goal:** Main library screen with all features (V2 pattern)

**File to create:**
`composeApp/src/commonMain/kotlin/com/grateful/deadly/feature/library/LibraryScreen.kt`

```kotlin
@Composable
fun LibraryScreen(
    onNavigateToShow: (String) -> Unit = {},
    onNavigateToPlayer: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: LibraryViewModel = remember { DIHelper.get() }
) {
    val uiState by viewModel.uiState.collectAsState()

    // UI State (V2 pattern)
    var filterPath by remember { mutableStateOf(FilterPath()) }
    var sortBy by remember { mutableStateOf(LibrarySortOption.DATE_ADDED) }
    var sortDirection by remember { mutableStateOf(LibrarySortDirection.DESCENDING) }
    var displayMode by remember { mutableStateOf(LibraryDisplayMode.LIST) }
    var showSortBottomSheet by remember { mutableStateOf(false) }
    var selectedShowForActions by remember { mutableStateOf<LibraryShow?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Hierarchical Filters (V2 pattern)
        HierarchicalFilter(
            filterTree = FilterTrees.buildDeadToursTree(),
            selectedPath = filterPath,
            onSelectionChanged = { filterPath = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Sort Controls and Display Toggle (V2 pattern)
        SortAndDisplayControls(
            sortBy = sortBy,
            sortDirection = sortDirection,
            displayMode = displayMode,
            onSortSelectorClick = { showSortBottomSheet = true },
            onDisplayModeChanged = { displayMode = it }
        )

        // Main Content (V2 pattern)
        when {
            uiState.isLoading -> LoadingContent()
            uiState.error != null -> ErrorContent(uiState.error!!, viewModel::retry)
            uiState.shows.isEmpty() -> EmptyLibraryContent()
            else -> {
                // Apply filtering and sorting (V2 pattern: pin priority)
                val filteredAndSortedShows = applyFiltersAndSorting(
                    shows = uiState.shows,
                    filterPath = filterPath,
                    sortBy = sortBy,
                    sortDirection = sortDirection
                )

                LibraryContent(
                    shows = filteredAndSortedShows,
                    displayMode = displayMode,
                    onShowClick = onNavigateToShow,
                    onShowLongPress = { show -> selectedShowForActions = show }
                )
            }
        }
    }

    // Bottom Sheets (V2 pattern)
    if (showSortBottomSheet) {
        SortOptionsBottomSheet(
            currentSortOption = sortBy,
            currentSortDirection = sortDirection,
            onSortOptionSelected = { option, direction ->
                sortBy = option
                sortDirection = direction
                showSortBottomSheet = false
            },
            onDismiss = { showSortBottomSheet = false }
        )
    }

    selectedShowForActions?.let { show ->
        ShowActionsBottomSheet(
            show = show,
            onDismiss = { selectedShowForActions = null },
            onShare = { viewModel.shareShow(show.showId); selectedShowForActions = null },
            onRemoveFromLibrary = { viewModel.removeFromLibrary(show.showId); selectedShowForActions = null },
            onPin = { viewModel.pinShow(show.showId); selectedShowForActions = null },
            onUnpin = { viewModel.unpinShow(show.showId); selectedShowForActions = null }
        )
    }
}

/**
 * Apply filtering and sorting (V2 pattern: pin priority)
 */
private fun applyFiltersAndSorting(
    shows: List<LibraryShow>,
    filterPath: FilterPath,
    sortBy: LibrarySortOption,
    sortDirection: LibrarySortDirection
): List<LibraryShow> {
    // Filter first
    val filtered = if (filterPath.isEmpty) shows else applyHierarchicalFiltering(shows, filterPath)

    // Sort with pin priority (V2 pattern: pinned shows always first)
    return when (sortBy) {
        LibrarySortOption.DATE_OF_SHOW -> {
            if (sortDirection == LibrarySortDirection.ASCENDING) {
                filtered.sortedWith(compareBy<LibraryShow> { !it.isPinned }.thenBy { it.date })
            } else {
                filtered.sortedWith(compareBy<LibraryShow> { !it.isPinned }.thenByDescending { it.date })
            }
        }
        LibrarySortOption.DATE_ADDED -> {
            if (sortDirection == LibrarySortDirection.ASCENDING) {
                filtered.sortedWith(compareBy<LibraryShow> { !it.isPinned }.thenBy { it.addedToLibraryAt })
            } else {
                filtered.sortedWith(compareBy<LibraryShow> { !it.isPinned }.thenByDescending { it.addedToLibraryAt })
            }
        }
        // ... other sort options
    }
}
```

**Testing:**
- Full E2E testing on both platforms
- Verify all interactions work
- Test filtering, sorting, list/grid toggle
- Verify bottom sheets work

---

### Phase 8: Integration with Existing Screens
**Goal:** Connect library system to Home, Search, ShowDetail (V2 reactive pattern)

**Files to modify:**

1. **ShowDetailScreen.kt** - Integrate library actions
```kotlin
@Composable
fun ShowDetailScreen(...) {
    val libraryService: LibraryService = remember { DIHelper.get() }
    val isInLibrary by libraryService.isShowInLibrary(showId).collectAsState()

    ShowDetailActionRow(
        showData = showData,
        onLibraryAction = {
            coroutineScope.launch {
                if (isInLibrary) {
                    libraryService.removeFromLibrary(showId)
                } else {
                    libraryService.addToLibrary(showId)
                }
            }
        },
        // ... other actions
    )
}
```

2. **ShowDetailActionRow.kt** - Update library button to reflect real state
```kotlin
// Library button - shows different states based on reactive flow
IconButton(onClick = onLibraryAction) {
    val iconToUse = if (isInLibrary) {
        AppIcon.LibraryAddCheck
    } else {
        AppIcon.LibraryAdd
    }
    iconToUse.Render(
        size = 24.dp,
        tint = if (isInLibrary) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}
```

3. **HomeScreen.kt** - Add library indicators
```kotlin
// Add small library indicator icon to show cards if in library
val libraryService: LibraryService = remember { DIHelper.get() }
shows.forEach { show ->
    val isInLibrary by libraryService.isShowInLibrary(show.id).collectAsState()
    ShowCard(
        show = show,
        isInLibrary = isInLibrary, // Pass to card for display
        // ...
    )
}
```

**Testing:**
- Toggle library in ShowDetail → verify Home/Search update immediately
- Add to library from search → verify ShowDetail reflects change
- Remove from library → verify all UIs update
- Test reactive updates work on both platforms

---

### Phase 9: Navigation Integration
**Goal:** Add Library tab to bottom navigation

**Files to modify:**

1. **AppScreen.kt** - Add Library screen
```kotlin
sealed interface AppScreen {
    // ... existing screens
    object Library : AppScreen
}
```

2. **BarConfiguration.kt** - Add library bar config
```kotlin
AppScreen.Library -> BarConfiguration(
    topBar = TopBarConfig(
        visible = true,
        title = "Library",
        showBackButton = false
    ),
    bottomBar = BottomBarConfig(visible = true)
)
```

3. **App.kt** - Add library composable
```kotlin
composable(AppScreen.Library) {
    val libraryViewModel: LibraryViewModel = remember { DIHelper.get() }

    LibraryScreen(
        viewModel = libraryViewModel,
        onNavigateToShow = { showId ->
            navigationController.navigate(AppScreen.ShowDetail(showId))
        },
        onNavigateToPlayer = { recordingId ->
            // TODO: Player navigation
        }
    )
}
```

**Testing:**
- Navigate to Library tab
- Verify bottom navigation highlighting works
- Test navigation to ShowDetail from Library
- Test back navigation

---

### Phase 10: Koin DI Registration
**Goal:** Register all library components in dependency injection

**File to modify:**
`composeApp/src/commonMain/kotlin/com/grateful/deadly/di/CommonModule.kt`

```kotlin
val commonModule = module {
    // ... existing registrations

    // Library system (V2 pattern)
    single { LibraryRepository(database = get()) }
    single<LibraryService> {
        LibraryServiceImpl(
            libraryRepository = get(),
            showRepository = get(),
            coroutineScope = get()
        )
    }
    factory { LibraryViewModel(libraryService = get()) }
}
```

**Testing:**
- App starts correctly
- No circular dependencies
- All library screens work
- Verify DI on both platforms

---

### Phase 11: Logging and Observability
**Goal:** Add comprehensive logging for debugging

**File to modify:**
`scripts/readlogs` - Add library concept group

```bash
"library")
    echo "LibraryService|LibraryRepository|LibraryViewModel|LibraryScreen"
    ;;
```

**Add logging to:**
- LibraryService: All operations with showIds and results
- LibraryRepository: Database operations and flow emissions
- LibraryViewModel: State changes and user actions
- LibraryScreen: User interactions

**Testing:**
- Run `./scripts/readlogs -a library`
- Verify logs show up for all operations
- Test on both platforms

---

## Implementation Order

1. **Phase 1** - Database schema (foundation)
2. **Phase 2** - Domain models (needed by everything)
3. **Phase 3** - Platform repository (before service)
4. **Phase 4** - Universal service (depends on repository)
5. **Phase 10** - DI registration (needed for testing)
6. **Phase 5** - ViewModel (depends on service)
7. **Phase 6** - UI components (can develop in parallel)
8. **Phase 7** - LibraryScreen (depends on ViewModel + components)
9. **Phase 9** - Navigation integration
10. **Phase 8** - Integration with existing screens
11. **Phase 11** - Logging (final polish)

## Build and Test After Each Phase

```bash
# Build for both platforms
./gradlew build

# Run on Android
make run-android-emulator

# Run on iOS
make run-ios-simulator

# Check logs
./scripts/readlogs -a library
```

## Success Criteria

- [ ] Separate library_shows table matching V2 structure
- [ ] User can add/remove shows from library in ShowDetail
- [ ] Library screen shows all library shows with pin indicators
- [ ] Hierarchical filtering works (decade → season)
- [ ] Sorting works (date, venue, rating) with pin priority
- [ ] List/grid display toggle works
- [ ] Pin/unpin shows from library screen and ShowDetail
- [ ] Toggling library status in one place updates all UIs immediately (reactive)
- [ ] Bottom sheets work (sort, actions)
- [ ] Navigation works (Library tab, ShowDetail)
- [ ] Works on both Android and iOS
- [ ] No performance issues with 100+ library shows
- [ ] Comprehensive logging for debugging
- [ ] Follows Universal Service + Platform Tool pattern
- [ ] Matches V2's proven architecture exactly

## Key Architectural Principles

1. **Separate library_shows table** - NOT columns in Show table
2. **V2's proven patterns** - Copy working architecture, don't get creative
3. **Universal Service + Platform Tool** - All business logic in commonMain
4. **Reactive flows throughout** - StateFlow for UI, Flow for database
5. **Result-based error handling** - All operations return Result<T>
6. **Pin priority in sorting** - Pinned shows always first
7. **Foreign key CASCADE** - Deleting show removes library entry
8. **Indexes for performance** - addedAt, isPinned, showId

## Migration Notes

**No migration needed for Show table!**

V2's hybrid pattern uses BOTH:
- Show table columns (`isInLibrary`, `libraryAddedAt`) - ✅ Already in place
- Separate library_shows table (pin, notes, rating, tags) - ⚠️ Need to add

Current Deadly Show table structure already matches V2. We just need to add the library_shows table for rich metadata.