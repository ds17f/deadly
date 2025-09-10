# KMM Migration Strategy: UI-First Development with Proven Libraries

This document is our **north star** for migrating features from the V2 Android app to Kotlin Multiplatform Mobile (KMM). We prioritize getting working UI quickly using proven libraries, then building real implementations incrementally.

---

## Core Philosophy: UI-First with Battle-Tested Libraries

### Our Migration Pattern
1. **Pick a feature** (e.g., Search)
2. **Analyze V2 service interfaces** and architecture
3. **Port to KMM using proven libraries** (Okio, SQLDelight, Ktor, etc.)
4. **Create stub implementations first** for immediate UI testing
5. **Validate UI works identically** on both platforms
6. **Replace stubs with real implementations** using shared business logic

**Key Insight**: Let established libraries handle platform differences. We focus on business logic in shared code.

---

## Technology Stack: Battle-Tested Libraries

### 📚 Core Libraries
- **File System**: Okio `3.9.0` - Multiplatform file operations
- **Database**: SQLDelight `2.0.2` - Type-safe SQL with shared queries  
- **Networking**: Ktor `2.3.6` - Multiplatform HTTP client
- **Preferences**: Multiplatform Settings `1.1.1` - Shared key-value storage
- **DI**: Koin `3.5.0` - Multiplatform dependency injection
- **Concurrency**: Kotlinx Coroutines `1.7.3` - Async programming

### Why These Libraries?
- **95% shared code** - only driver bindings are platform-specific
- **Production proven** - used by major apps
- **Active maintenance** - regular updates and bug fixes
- **Great documentation** - easy to learn and implement
- **Performance optimized** - no custom abstractions needed

---

## Target Architecture

### Clean KMM Structure
```
composeApp/src/
├── commonMain/kotlin/com/grateful/deadly/
│   ├── feature/search/                 # UI Layer
│   │   ├── SearchScreen.kt             # Shared Compose UI
│   │   └── SearchViewModel.kt          # Shared ViewModel
│   ├── domain/search/                  # Business Interfaces  
│   │   └── SearchService.kt            # Service contract
│   ├── services/search/                # Business Logic (95% shared)
│   │   ├── SearchServiceImpl.kt        # Real implementation 
│   │   └── SearchServiceStub.kt        # Mock implementation
│   ├── data/search/                    # Data Layer
│   │   ├── SearchRepository.kt         # Repository interface
│   │   ├── SearchRepositoryImpl.kt     # SQLDelight implementation
│   │   └── sqldelight/                 # SQL schemas & queries
│   └── di/
│       └── CommonModule.kt             # Shared DI bindings
├── androidMain/kotlin/                 # Platform Drivers (5%)
│   └── di/AndroidModule.kt             # Android-specific bindings
└── iosMain/kotlin/                     # Platform Drivers (5%)
    └── di/IOSModule.kt                 # iOS-specific bindings
```

### Key Architectural Principles
- **Shared business logic** - services contain zero platform-specific code
- **Library-driven platform abstraction** - let Okio/SQLDelight/Ktor handle differences
- **DI-based feature flags** - easy switching between stub/real implementations
- **SQLDelight as truth source** - shared schemas, generated type-safe queries
- **Repository pattern** - clean separation between services and data access

---

## V2 Architecture Analysis

### Current V2 Pattern (Reference)
```
v2/feature/search/          # UI Layer
├── screens/main/
│   ├── SearchScreen.kt     # Compose UI (800+ lines, rich debug tools)
│   └── models/
│       └── SearchViewModel.kt  # @HiltViewModel with reactive flows
└── navigation/

v2/core/api/search/         # Service Interface  
└── SearchService.kt        # Clean interface with Result types

v2/core/search/             # Service Implementation
├── SearchServiceStub.kt    # Rich mock data (470+ lines)
├── SearchServiceImpl.kt    # Real implementation (TBD)
└── di/SearchModule.kt      # Hilt DI binding
```

### V2 Design Principles to Preserve
- **Reactive flows** for all state management
- **Result-based error handling** with proper status types
- **Rich debug integration** built into every screen  
- **Stub-first development** for immediate UI work
- **Comprehensive test data** for realistic development

---

## Migration Implementation Guide

### Phase 1: Foundation Setup ✨ **START HERE**

#### Step 1.1: Add Libraries to gradle/libs.versions.toml
```toml
[versions]
okio = "3.9.0"
sqldelight = "2.0.2"
ktor = "2.3.6"
multiplatform-settings = "1.1.1"
koin = "4.1.0"

[libraries]
# File System
okio = { module = "com.squareup.okio:okio", version.ref = "okio" }

# Database  
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }

# Networking
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }

# Preferences
multiplatform-settings = { module = "com.russhwolf:multiplatform-settings", version.ref = "multiplatform-settings" }

# DI
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-androidx-compose = { module = "io.insert-koin:koin-androidx-compose", version.ref = "koin" }
```

#### Step 1.2: Update build.gradle.kts
```kotlin
commonMain {
    dependencies {
        implementation(libs.okio)
        implementation(libs.sqldelight.runtime)
        implementation(libs.sqldelight.coroutines)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.json)
        implementation(libs.multiplatform.settings)
        implementation(libs.koin.core)
    }
}

androidMain {
    dependencies {
        implementation(libs.koin.androidx.compose)
        implementation(libs.sqldelight.android.driver)
        implementation(libs.ktor.client.okhttp)
    }
}

iosMain {
    dependencies {
        implementation(libs.sqldelight.native.driver)
        implementation(libs.ktor.client.darwin)
    }
}
```

### Phase 2: Search Feature Migration

#### Step 2.1: Port V2 Models
```kotlin
// commonMain/domain/search/SearchModels.kt
data class SearchUiState(
    val searchQuery: String = "",
    val searchResults: List<SearchResultShow> = emptyList(),
    val searchStatus: SearchStatus = SearchStatus.IDLE,
    val recentSearches: List<RecentSearch> = emptyList(),
    val suggestedSearches: List<SuggestedSearch> = emptyList(),
    val searchStats: SearchStats = SearchStats(0, 0),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Port all related data classes from V2...
```

#### Step 2.2: Create Service Interface
```kotlin
// commonMain/domain/search/SearchService.kt
interface SearchService {
    val currentQuery: Flow<String>
    val searchResults: Flow<List<SearchResultShow>>
    val searchStatus: Flow<SearchStatus>
    val recentSearches: Flow<List<RecentSearch>>
    val suggestedSearches: Flow<List<SuggestedSearch>>
    val searchStats: Flow<SearchStats>
    
    suspend fun updateSearchQuery(query: String): Result<Unit>
    suspend fun clearSearch(): Result<Unit>
    suspend fun addRecentSearch(query: String): Result<Unit>
    suspend fun clearRecentSearches(): Result<Unit>
    suspend fun selectSuggestion(suggestion: SuggestedSearch): Result<Unit>
    suspend fun applyFilters(filters: List<SearchFilter>): Result<Unit>
    suspend fun clearFilters(): Result<Unit>
    suspend fun getSuggestions(partialQuery: String): Result<List<SuggestedSearch>>
    suspend fun populateTestData(): Result<Unit>
}
```

#### Step 2.3: Create Shared Stub Implementation
```kotlin
// commonMain/services/search/SearchServiceStub.kt
class SearchServiceStub(
    private val settings: Settings  // Multiplatform Settings for persistence
) : SearchService {
    
    private val _currentQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<SearchResultShow>>(emptyList())
    private val _recentSearches = MutableStateFlow<List<RecentSearch>>(emptyList())
    
    override val currentQuery: Flow<String> = _currentQuery.asStateFlow()
    override val searchResults: Flow<List<SearchResultShow>> = _searchResults.asStateFlow()
    override val recentSearches: Flow<List<RecentSearch>> = _recentSearches.asStateFlow()
    
    // Rich mock data from V2 (470+ lines of realistic test data)
    private val mockShows = listOf(
        // Cornell 5/8/77 - The legendary show
        Show(
            id = "gd1977-05-08",
            date = "1977-05-08", 
            venue = Venue("Barton Hall", "Ithaca", "NY", "USA"),
            averageRating = 4.8f,
            // ... complete show data
        ),
        // ... all other shows from V2 stub
    )
    
    override suspend fun addRecentSearch(query: String): Result<Unit> {
        // Persist using Multiplatform Settings (works on both platforms)
        val current = _recentSearches.value.toMutableList()
        current.removeAll { it.query == query }
        current.add(0, RecentSearch(query, System.currentTimeMillis()))
        
        if (current.size > 10) current.removeAt(10)
        
        val json = Json.encodeToString(current)
        settings.putString("recent_searches", json)
        
        _recentSearches.value = current
        return Result.success(Unit)
    }
    
    // ... implement all other methods from V2 stub
}
```

#### Step 2.4: Port ViewModel (Remove Hilt)
```kotlin
// commonMain/feature/search/SearchViewModel.kt
class SearchViewModel(
    private val searchService: SearchService  // Constructor injection via Koin
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    // Port complete logic from V2 SearchViewModel
    // Remove @HiltViewModel and @Inject annotations
    // Keep all the debounced search logic, state management, etc.
}
```

#### Step 2.5: Port Compose UI (Use KoinComponent Pattern)
```kotlin
// commonMain - Create DI helper for Koin 4.1.0
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object DIHelper : KoinComponent

// commonMain/feature/search/SearchScreen.kt
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,  // Accept as parameter instead of injecting
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (String) -> Unit,
    onNavigateToSearchResults: () -> Unit,
    initialEra: String? = null
) {
    // Port complete 800+ line UI from V2
    // Manual dependency injection instead of magic injection
}

// Usage in App.kt
@Composable
fun App() {
    val searchViewModel: SearchViewModel = DIHelper.get()
    SearchScreen(viewModel = searchViewModel, ...)
}
```

#### Step 2.6: Setup Koin DI
```kotlin
// commonMain/di/CommonModule.kt
val commonModule = module {
    
    // Multiplatform Settings (implementations provided by platform modules)
    // single<Settings> provided by platform modules
    
    // Services with feature flag switching
    single<SearchService> { 
        val useStubs = get<Settings>().getBoolean("use_stub_services", true)
        if (useStubs) {
            SearchServiceStub(get())
        } else {
            SearchServiceImpl(get(), get(), get())  // Real impl when ready
        }
    }
    
    // ViewModels
    factory { SearchViewModel(get()) }
}

// androidMain/di/AndroidModule.kt
val androidModule = module {
    single<Settings> {
        SharedPreferencesSettings(
            delegate = get<Context>().getSharedPreferences("deadly_settings", Context.MODE_PRIVATE)
        )
    }
}

// iosMain/di/IOSModule.kt  
val iosModule = module {
    single<Settings> {
        NSUserDefaultsSettings(delegate = NSUserDefaults.standardUserDefaults)
    }
}
```

**Validation Point**: Search UI works identically on Android and iOS with rich mock data and persistent recent searches.

### Phase 3: Data Layer with SQLDelight

#### Step 3.1: Create SQLDelight Schema
```sql
-- commonMain/sqldelight/com/grateful/deadly/database/Search.sq

CREATE TABLE RecentSearch (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    query TEXT NOT NULL UNIQUE,
    timestamp INTEGER NOT NULL,
    result_count INTEGER DEFAULT 0
);

CREATE INDEX idx_recent_search_timestamp ON RecentSearch(timestamp DESC);

-- Queries
selectAllRecentSearches:
SELECT * FROM RecentSearch ORDER BY timestamp DESC LIMIT 10;

insertRecentSearch:
INSERT OR REPLACE INTO RecentSearch(query, timestamp, result_count) 
VALUES (?, ?, ?);

deleteAllRecentSearches:
DELETE FROM RecentSearch;

-- Show search queries  
searchShows:
SELECT s.*, v.name as venue_name, v.city as venue_city, v.state as venue_state
FROM Show s 
JOIN Venue v ON s.venue_id = v.id
WHERE s.date LIKE '%' || ? || '%' 
   OR v.name LIKE '%' || ? || '%'
   OR s.location LIKE '%' || ? || '%'
ORDER BY s.date DESC;
```

#### Step 3.2: Setup SQLDelight Database
```kotlin
// commonMain/di/DatabaseModule.kt
val databaseModule = module {
    
    // SQLDelight Database (drivers provided by platform modules)
    single {
        Database(driver = get<SqlDriver>())
    }
    
    // Repository
    single<SearchRepository> { SearchRepositoryImpl(get()) }
}

// androidMain/di/AndroidModule.kt (add to existing module)
single<SqlDriver> {
    AndroidSqliteDriver(Database.Schema, get<Context>(), "deadly.db")
}

// iosMain/di/IOSModule.kt (add to existing module) 
single<SqlDriver> {
    NativeSqliteDriver(Database.Schema, "deadly.db")
}
```

#### Step 3.3: Real Service Implementation
```kotlin
// commonMain/services/search/SearchServiceImpl.kt
class SearchServiceImpl(
    private val database: Database,      // SQLDelight
    private val httpClient: HttpClient,  // Ktor (for future API integration)
    private val settings: Settings       // Multiplatform Settings
) : SearchService {
    
    private val _searchResults = MutableStateFlow<List<SearchResultShow>>(emptyList())
    override val searchResults: Flow<List<SearchResultShow>> = _searchResults.asStateFlow()
    
    override val recentSearches: Flow<List<RecentSearch>> = 
        database.searchQueries.selectAllRecentSearches()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> 
                rows.map { RecentSearch(it.query, it.timestamp) }
            }
    
    override suspend fun updateSearchQuery(query: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Use SQLDelight generated queries
            val results = database.searchQueries.searchShows(query, query, query).executeAsList()
            
            val searchResults = results.map { row ->
                SearchResultShow(
                    show = Show(
                        id = row.id,
                        date = row.date,
                        venue = Venue(row.venue_name, row.venue_city, row.venue_state, "USA"),
                        // ... map from SQLDelight result
                    ),
                    relevanceScore = calculateRelevance(row, query),
                    matchType = determineMatchType(row, query),
                    hasDownloads = row.recording_count > 0
                )
            }
            
            _searchResults.value = searchResults
            
            // Add to recent searches
            database.searchQueries.insertRecentSearch(
                query, 
                System.currentTimeMillis(),
                searchResults.size
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ... implement other methods using SQLDelight queries
}
```

### Phase 4: Data Import Service

#### Step 4.1: Complete Data Import with Libraries
```kotlin
// commonMain/services/database/DataImportService.kt
class DataImportService(
    private val database: Database,           // SQLDelight
    private val httpClient: HttpClient,       // Ktor
    private val fileSystem: FileSystem = FileSystem.SYSTEM,  // Okio
    private val settings: Settings            // Multiplatform Settings
) {
    
    suspend fun importFromDataZip(url: String = "https://archive.org/data/deadly-shows.zip"): ImportResult = 
        withContext(Dispatchers.IO) {
        
        try {
            // 1. Download using Ktor (shared across platforms)
            val response = httpClient.get(url)
            val zipBytes = response.body<ByteArray>()
            
            // 2. File operations using Okio (shared across platforms)
            val tempDir = fileSystem.temporaryDirectory
            val zipPath = tempDir / "deadly-import.zip"
            
            zipPath.writeBytes(zipBytes)
            
            // 3. Extract and parse (shared logic)
            val extractedFiles = extractZipContents(zipPath)
            val shows = parseShowsJson(extractedFiles["shows.json"]!!)
            val venues = parseVenuesJson(extractedFiles["venues.json"]!!)
            
            // 4. Database operations using SQLDelight (shared)
            database.transaction {
                // Clear existing data
                database.showQueries.deleteAllShows()
                database.venueQueries.deleteAllVenues()
                
                // Insert new data
                venues.forEach { venue ->
                    database.venueQueries.insertVenue(
                        name = venue.name,
                        city = venue.city,
                        state = venue.state,
                        country = venue.country
                    )
                }
                
                shows.forEach { show ->
                    database.showQueries.insertShow(
                        id = show.id,
                        date = show.date,
                        venue_id = show.venue.id,
                        location = show.location.displayText,
                        recording_count = show.recordingCount,
                        average_rating = show.averageRating?.toDouble()
                    )
                }
            }
            
            // 5. Save import status using Multiplatform Settings (shared)
            settings.putLong("last_import_timestamp", System.currentTimeMillis())
            settings.putInt("imported_shows_count", shows.size)
            settings.putInt("imported_venues_count", venues.size)
            
            // 6. Cleanup
            fileSystem.deleteRecursively(tempDir)
            
            ImportResult.Success(shows.size, venues.size)
            
        } catch (e: Exception) {
            ImportResult.Error("Import failed: ${e.message}")
        }
    }
    
    private fun parseShowsJson(json: String): List<Show> {
        // Shared JSON parsing using kotlinx.serialization
        return Json.decodeFromString<List<Show>>(json)
    }
    
    private fun extractZipContents(zipPath: Path): Map<String, String> {
        // Shared zip extraction using Okio
        val results = mutableMapOf<String, String>()
        
        zipPath.openZip().use { zipFileSystem ->
            zipFileSystem.list(Path("/")).forEach { path ->
                if (path.name.endsWith(".json")) {
                    val content = zipFileSystem.read(path) { readUtf8() }
                    results[path.name] = content
                }
            }
        }
        
        return results
    }
}
```

---

## Reusable Pattern Template

This exact pattern applies to **any V2 feature**:

### 1. Analysis Phase
- [ ] Identify V2 service interface in `v2/core/api/{feature}/`
- [ ] Examine ViewModel in `v2/feature/{feature}/screens/*/models/`
- [ ] Review UI components in `v2/feature/{feature}/screens/`
- [ ] Note existing stub implementation patterns
- [ ] Identify data models and flow types

### 2. KMM Port Phase  
- [ ] Port models to `commonMain/domain/{feature}/`
- [ ] Port service interface to `commonMain/domain/{feature}/`
- [ ] Port stub implementation to `commonMain/services/{feature}/`
- [ ] Create Koin module with feature flag switching
- [ ] Port ViewModel (remove Hilt, add Koin)
- [ ] Port Compose UI (replace hiltViewModel() with koinInject())

### 3. Validation Phase
- [ ] Build for Android - UI works with stubs
- [ ] Build for iOS - UI works with stubs
- [ ] Test all major user flows on both platforms  
- [ ] Validate debug tooling integration
- [ ] Confirm persistence works via Multiplatform Settings

### 4. Real Implementation Phase
- [ ] Create SQLDelight schemas in `commonMain/sqldelight/`
- [ ] Create repository interfaces and implementations
- [ ] Implement real service using SQLDelight + libraries
- [ ] Update DI to conditionally use real vs stub implementations
- [ ] Add data import/sync capabilities using Ktor + Okio

---

## Feature Priority Roadmap

### Tier 1: Foundation Features (Start Here)
1. **Search** ⭐ - Rich interface, excellent stub data, core to app
2. **Library** - CRUD operations, good for database patterns  
3. **Home** - Display logic, navigation hub, simpler scope

### Tier 2: Media Features
4. **Settings** - Platform-specific preferences using Multiplatform Settings
5. **Player** - Complex state management, media session integration
6. **Collections** - Data relationships, user-generated content

### Tier 3: Advanced Features  
7. **Recent** - Cross-feature integration, usage analytics
8. **Miniplayer** - UI coordination with Player service
9. **Playlists** - Complex data relationships, sync challenges

---

## Success Criteria

### Phase 1 Success (UI-First)
- [ ] Feature UI renders identically on Android/iOS
- [ ] All interactive elements work (taps, gestures, navigation)
- [ ] Debug tools integrated and functional
- [ ] Stub data provides realistic testing scenarios
- [ ] Feature flags enable easy stub/real switching
- [ ] Navigation flows work end-to-end

### Phase 2 Success (Real Implementation)
- [ ] Feature returns actual data from SQLDelight database  
- [ ] All business logic works correctly with real data
- [ ] Data persistence works across app sessions
- [ ] Error handling covers network/database failures
- [ ] Performance equals or exceeds V2 Android version

### Final Success (Production Ready)
- [ ] Complete feature parity with V2 Android version
- [ ] iOS-specific optimizations and polish applied
- [ ] Comprehensive test coverage (unit + integration)  
- [ ] Ready for other features to follow this exact pattern
- [ ] Documentation updated with any learnings/improvements

---

## Key Principles (Our North Star)

1. **UI-First Development**: Get visual feedback quickly, defer complex data work
2. **Library-Driven Architecture**: Use proven solutions, avoid custom platform code
3. **Shared Business Logic**: 95% of code lives in commonMain, platform modules only provide drivers
4. **Feature Flag Everything**: Easy switching between stub/real implementations  
5. **SQLDelight as Truth**: Shared schemas generate type-safe multiplatform queries
6. **Rich Debug Integration**: Every feature includes comprehensive debug tooling
7. **Validate Early**: Prove the pattern works before scaling to other features

**The Goal**: Establish a proven, repeatable pattern that any team member can follow to migrate V2 features to production-ready KMM implementations.

This document serves as our **north star** - refer back to it throughout implementation to stay aligned with our architectural decisions and proven approach.