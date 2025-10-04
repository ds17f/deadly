# Navigation System Implementation Guide (COMPLETED)

**Status**: ✅ **IMPLEMENTED** - This document describes the completed navigation architecture.

The navigation system has been successfully implemented in the KMM project using **expect/actual pattern** with `DeadlyNavHost` abstraction.

## ✅ Completed Implementation

The following features have been successfully implemented:
- **Cross-platform Navigation**: `DeadlyNavHost` expect/actual pattern abstracts platform differences
- **AppScaffold**: Unified layout manager with TopBar/BottomBar coordination
- **5-Tab Bottom Navigation**: Home, Search, Library, Collections, Settings (all working)
- **SearchScreen → SearchResultsScreen**: Complete navigation flow with working back navigation
- **Feature-colocated Configuration**: Each feature defines its own bar configuration
- **Shared ViewModel Navigation**: SearchViewModel works across multiple screens
- **V2 Design Patterns**: Pin indicators, LibraryV2-style cards, integrated search headers

## Current Architecture Overview

Implemented navigation system includes:
- **expect/actual Navigation**: `DeadlyNavHost` abstracts platform differences between Android NavHost and iOS NavigationStack
- **AppScaffold**: Unified layout manager with TopBar/BottomBar coordination
- **5-Tab Bottom Navigation**: Home, Search, Library, Collections, Settings (with working navigation)
- **Feature-colocated Configuration**: Each screen defines its bar configuration following V2 patterns
- **Cross-platform Icon System**: Material Symbols (Android) + SF Symbols (iOS)
- **Shared ViewModel Pattern**: SearchViewModel handles navigation across SearchScreen and SearchResultsScreen

## ✅ Implemented Navigation Files

The following navigation components have been implemented:

### Core Navigation Components:
- `composeApp/src/commonMain/kotlin/com/grateful/deadly/navigation/` - Navigation models and configuration
- `composeApp/src/commonMain/kotlin/com/grateful/deadly/core/design/scaffold/AppScaffold.kt` - Layout manager
- `composeApp/src/commonMain/kotlin/com/grateful/deadly/core/design/component/topbar/TopBar.kt` - TopBar component
- `composeApp/src/commonMain/kotlin/com/grateful/deadly/core/design/component/bottombar/BottomNavigationBar.kt` - Bottom navigation
- `composeApp/src/commonMain/kotlin/com/grateful/deadly/core/navigation/DeadlyNavHost.kt` - Cross-platform navigation abstraction

### Platform Implementations:
- `composeApp/src/androidMain/kotlin/com/grateful/deadly/core/navigation/DeadlyNavHost.android.kt` - Android NavHost integration
- `composeApp/src/iosMain/kotlin/com/grateful/deadly/core/navigation/DeadlyNavHost.ios.kt` - iOS NavigationStack integration

## ✅ Implementation Status - All Checkpoints Completed

All navigation checkpoints have been successfully implemented and tested on both Android and iOS.

### ✅ Checkpoint 1: Navigation Foundation (COMPLETED)
**Status**: ✅ **IMPLEMENTED**

**Completed Features**:
- ✅ Navigation models implemented in `commonMain/kotlin/com/grateful/deadly/navigation/`
- ✅ Cross-platform navigation working on both Android and iOS
- ✅ Type-safe route handling implemented

**Implementation**:
1. **Create AppScreen sealed interface (`commonMain/navigation/AppScreen.kt`)**:
   ```kotlin
   sealed interface AppScreen {
       object Home : AppScreen
       object Search : AppScreen
       data class ShowDetail(val id: String) : AppScreen
       object Library : AppScreen
       object Collections : AppScreen
       object Settings : AppScreen
       data class SearchResults(val query: String = "") : AppScreen
   }
   ```

2. **Add RouteSpec interface for type-safe routing (`commonMain/navigation/RouteSpec.kt`)**:
   ```kotlin
   interface RouteSpec<T : AppScreen> {
       val pattern: String
       fun toRoute(screen: T): String
       fun fromRoute(route: String): T?
   }
   
   // Example implementation for ShowDetail
   object ShowDetailRouteSpec : RouteSpec<AppScreen.ShowDetail> {
       override val pattern = "show/{id}"
       
       override fun toRoute(screen: AppScreen.ShowDetail): String {
           return "show/${screen.id}"
       }
       
       override fun fromRoute(route: String): AppScreen.ShowDetail? {
           return if (route.startsWith("show/")) {
               val id = route.removePrefix("show/")
               if (id.isNotBlank()) AppScreen.ShowDetail(id) else null
           } else null
       }
   }
   ```

3. **Add route conversion extensions that use RouteSpec (`commonMain/navigation/AppScreenExtensions.kt`)**:
   ```kotlin
   // Add RouteSpec for SearchResults using path params (better Navigation Compose support)
   object SearchResultsRouteSpec : RouteSpec<AppScreen.SearchResults> {
       override val pattern = "search-results/{query?}"
       
       override fun toRoute(screen: AppScreen.SearchResults): String {
           return if (screen.query.isNotEmpty()) "search-results/${screen.query}" else "search-results/"
       }
       
       override fun fromRoute(route: String): AppScreen.SearchResults? {
           return when {
               route == "search-results/" -> AppScreen.SearchResults("")
               route.startsWith("search-results/") -> {
                   val query = route.removePrefix("search-results/")
                   AppScreen.SearchResults(query)
               }
               else -> null
           }
       }
   }
   
   // Extensions that delegate to RouteSpec for type safety
   fun AppScreen.route(): String = when (this) {
       AppScreen.Home -> "home"
       AppScreen.Search -> "search"
       AppScreen.Library -> "library"
       AppScreen.Collections -> "collections"
       AppScreen.Settings -> "settings"
       is AppScreen.SearchResults -> SearchResultsRouteSpec.toRoute(this)
       is AppScreen.ShowDetail -> ShowDetailRouteSpec.toRoute(this)
   }
   
   fun String.toAppScreen(): AppScreen? = when {
       this == "home" -> AppScreen.Home
       this == "search" -> AppScreen.Search
       this == "library" -> AppScreen.Library
       this == "collections" -> AppScreen.Collections
       this == "settings" -> AppScreen.Settings
       this.startsWith("search-results") -> SearchResultsRouteSpec.fromRoute(this)
       this.startsWith("show/") -> ShowDetailRouteSpec.fromRoute(this)
       else -> null
   }
   ```

4. **Test**: Verify sealed class compiles and route conversion works correctly

> **📝 Note on Code Duplication**: You may notice that `AppScreenExtensions.kt` maps routes while `DeadlyNavGraphBuilder` also handles routing. This duplication is temporary - as the navigation system matures, `DeadlyNavGraphBuilder` will automatically bind `AppScreen` objects to their Composable destinations, eliminating the need for manual route string mapping. The `RouteSpec` pattern provides the foundation for this future automation.

---

### ✅ Checkpoint 2: ViewModels Emit Navigation Events (COMPLETED)
**Status**: ✅ **IMPLEMENTED**

**Completed Features**:
- ✅ SearchViewModel handles navigation across multiple screens
- ✅ Navigation callbacks work without platform-specific references
- ✅ Search → SearchResults navigation flow implemented and working

**Implementation**:
1. **Create navigation event system (`commonMain/navigation/NavigationEvent.kt`)**:
   ```kotlin
   data class NavigationEvent(
       val screen: AppScreen,
       val clearBackStack: Boolean = false
   )
   ```

2. **Update SearchViewModel to emit navigation events**:
   ```kotlin
   class SearchViewModel {
       private val _navigation = MutableSharedFlow<NavigationEvent>()
       val navigation: SharedFlow<NavigationEvent> = _navigation

       suspend fun onSearchResultSelected(showId: String) {
           _navigation.emit(NavigationEvent(AppScreen.ShowDetail(showId)))
       }

       suspend fun onSearchQuerySubmitted(query: String) {
           _navigation.emit(NavigationEvent(AppScreen.SearchResults(query)))
       }
       
       suspend fun onNavigateToPlayer(recordingId: String) {
           Logger.i("SearchViewModel", "Navigate to player: $recordingId")
           _navigation.emit(NavigationEvent(AppScreen.Player))
       }
   }
   ```

3. **Add navigation event collection pattern**:
   ```kotlin
   // Example of how navigation events are consumed
   @Composable
   fun SomeScreen(viewModel: SearchViewModel) {
       val navController = rememberNavController()
       
       LaunchedEffect(Unit) {
           viewModel.navigation.collect { event ->
               if (event.clearBackStack) {
                   navController.navigate(event.screen.route()) {
                       popUpTo(0) { inclusive = true }
                   }
               } else {
                   navController.navigate(event.screen.route())
               }
           }
       }
       
       // Screen content...
   }
   ```

4. **Test**: Verify ViewModels can emit navigation events without crashes

---

### ✅ Checkpoint 3: Cross-Platform NavHost Integration (COMPLETED)
**Status**: ✅ **IMPLEMENTED**

**Completed Features**:
- ✅ DeadlyNavHost expect/actual abstraction implemented
- ✅ Android NavHost integration working
- ✅ iOS NavigationStack integration working
- ✅ Navigation works correctly on both platforms

**Implementation**:

> **Note**: This checkpoint focuses on Android implementation using `NavHost`. On iOS, this will be implemented later using SwiftUI's `NavigationStack` with the same `AppScreen` sealed class, ensuring consistent navigation behavior across platforms.

1. **Create DeadlyNavHost abstraction (`androidMain/navigation/DeadlyNavHost.android.kt`)**:
   ```kotlin
   // Cross-platform navigation graph builder
   actual class DeadlyNavGraphBuilder {
       private val routes = mutableMapOf<AppScreen, @Composable () -> Unit>()
       private val parameterizedRoutes = mutableMapOf<String, @Composable (Map<String, String>) -> Unit>()
       
       actual fun composable(
           screen: AppScreen,
           content: @Composable () -> Unit
       ) {
           routes[screen] = content
       }
       
       actual fun composable(
           route: String,
           content: @Composable (Map<String, String>) -> Unit
       ) {
           parameterizedRoutes[route] = content
       }
       
       fun build(): NavGraphBuilder.() -> Unit = {
           // Build standard routes
           routes.forEach { (screen, content) ->
               composable(screen.route()) { content() }
           }
           
           // Build parameterized routes
           parameterizedRoutes.forEach { (route, content) ->
               composable(route) { backStackEntry ->
                   val args = backStackEntry.arguments?.let { bundle ->
                       bundle.keySet().associateWith { key -> bundle.getString(key) ?: "" }
                   } ?: emptyMap()
                   content(args)
               }
           }
       }
   }

   @Composable
   actual fun DeadlyNavHost(
       navigationController: NavigationController,
       startDestination: AppScreen,
       modifier: Modifier,
       content: DeadlyNavGraphBuilder.() -> Unit
   ) {
       val navController = navigationController.navController
       val builder = DeadlyNavGraphBuilder()
       builder.content()
       
       NavHost(
           navController = navController,
           startDestination = startDestination.route(),
           modifier = modifier,
           builder = builder.build()
       )
   }
   ```

2. **Update App.kt to use DeadlyNavHost abstraction**:
   ```kotlin
   @Composable
   fun App() {
       Logger.i("App", "🎵 Deadly app UI starting")
       
       MaterialTheme {
           val navigationController = rememberNavigationController()
           val searchViewModel: SearchViewModel = DIHelper.get()
           
           // Collect navigation events from ViewModels
           LaunchedEffect(Unit) {
               searchViewModel.navigation.collect { event ->
                   navigationController.navigate(event.screen)
               }
           }
           
           // Use DeadlyNavHost abstraction (works on Android now, iOS later)
           DeadlyNavHost(
               navigationController = navigationController,
               startDestination = AppScreen.Search
           ) {
               composable(AppScreen.Home) {
                   Text("Home Screen")
               }
               
               composable(AppScreen.Search) {
                   SearchScreen(
                       viewModel = searchViewModel,
                       // ViewModel handles navigation via Flow events
                       onNavigateToPlayer = { recordingId -> 
                           searchViewModel.onNavigateToPlayer(recordingId)
                       },
                       onNavigateToShow = { showId ->
                           searchViewModel.onSearchResultSelected(showId)
                       },
                       onNavigateToSearchResults = {
                           searchViewModel.onSearchQuerySubmitted("")
                       }
                   )
               }
               
               composable(AppScreen.SearchResults()) {
                   Text("Search Results Screen")
               }
               
               composable(AppScreen.Library) {
                   Text("Library Screen")
               }
               
               composable(AppScreen.Collections) {
                   Text("Collections Screen")
               }
               
               composable(AppScreen.Settings) {
                   Text("Settings Screen")
               }
               
               composable("show/{id}") { args ->
                   val id = args["id"] ?: ""
                   Text("Show Detail Screen: $id")
               }
           }
       }
   }
   ```

2. **Test**: Verify search screen appears via NavHost and navigation works

---

### ✅ Checkpoint 4: AppScaffold with TopBar + MiniPlayer Slot (COMPLETED)
**Status**: ✅ **IMPLEMENTED**

**Completed Features**:
- ✅ AppScaffold implemented with TopBar and BottomBar coordination
- ✅ Feature-colocated bar configuration implemented
- ✅ TopBar shows appropriate titles and back buttons
- ✅ Content properly padded below TopBar
- ✅ Foundation ready for future MiniPlayer integration

**Implementation**:
1. **Create TopBar modes (`commonMain/design/topbar/TopBarMode.kt`)**:
   ```kotlin
   sealed class TopBarMode {
       object SOLID : TopBarMode()      // Content padded below status bar
       object IMMERSIVE : TopBarMode()  // Content behind status bar with scrim
   }
   ```

2. **Create BarConfiguration system with defaults (`commonMain/navigation/BarConfiguration.kt`)**:
   ```kotlin
   data class TopBarConfig(
       val visible: Boolean = true,
       val title: String = "",
       val mode: TopBarMode = TopBarMode.SOLID,
       val showBackButton: Boolean = false
   )
   
   data class BottomBarConfig(
       val visible: Boolean = true
   )
   
   data class BarConfiguration(
       val topBar: TopBarConfig? = null,
       val bottomBar: BottomBarConfig = BottomBarConfig()
   )
   
   object NavigationBarConfig {
       private val defaults = BarConfiguration(
           topBar = TopBarConfig(title = "", visible = false),
           bottomBar = BottomBarConfig(visible = true)
       )

       fun getBarConfig(screen: AppScreen): BarConfiguration = when (screen) {
           AppScreen.Home -> defaults.copy(topBar = TopBarConfig(title = "Home"))
           AppScreen.Search -> defaults.copy(topBar = TopBarConfig(title = "Search"))
           is AppScreen.SearchResults -> defaults.copy(topBar = TopBarConfig(title = "Search Results", showBackButton = true))
           AppScreen.Library -> defaults.copy(topBar = TopBarConfig(title = "Library"))
           AppScreen.Collections -> defaults.copy(topBar = TopBarConfig(title = "Collections"))
           AppScreen.Settings -> defaults.copy(topBar = TopBarConfig(title = "Settings"))
           is AppScreen.ShowDetail -> defaults.copy(topBar = TopBarConfig(title = "Show Detail", showBackButton = true))
       }
   }
   ```

3. **Create AppScaffold with MiniPlayer slot (`commonMain/design/scaffold/AppScaffold.kt`)**:
   ```kotlin
   @Composable
   fun AppScaffold(
       topBarConfig: TopBarConfig?,
       bottomBarConfig: BottomBarConfig,
       currentScreen: AppScreen,
       onNavigateBack: () -> Unit,
       onNavigateToTab: (AppScreen) -> Unit = {},
       miniPlayerContent: @Composable (() -> Unit)? = null,
       content: @Composable (PaddingValues) -> Unit
   ) {
       Scaffold(
           topBar = {
               topBarConfig?.let {
                   TopBar(it, onNavigateBack)
               }
           },
           bottomBar = {
               if (bottomBarConfig.visible) {
                   // Spotify-style layering: MiniPlayer above BottomNav
                   Column {
                       miniPlayerContent?.invoke()
                       BottomNavigationBar(
                           currentScreen = currentScreen,
                           onNavigateToTab = onNavigateToTab
                       )
                   }
               }
           }
       ) { padding ->
           content(padding)
       }
   }
   ```

4. **Test**: Search screen should now have a "Search" top bar and MiniPlayer slot ready

---

### ✅ Checkpoint 5: Bottom Navigation Tabs (COMPLETED)
**Status**: ✅ **IMPLEMENTED**

**Completed Features**:
- ✅ 5-tab bottom navigation implemented: Home, Search, Library, Collections, Settings
- ✅ Tab navigation working between all screens
- ✅ Search tab shows complete SearchScreen
- ✅ Current tab highlighting working correctly
- ✅ Cross-platform AppIcon system integrated (Material Symbols + SF Symbols)

**Implementation**:
1. **Create BottomNavTab (`commonMain/navigation/BottomNavTab.kt`)**:
   ```kotlin
   data class BottomNavTab(
       val screen: AppScreen,
       val label: String,
       val icon: AppIcon
   )

   val bottomNavTabs = listOf(
       BottomNavTab(AppScreen.Home, "Home", AppIcon.Home),
       BottomNavTab(AppScreen.Search, "Search", AppIcon.Search),
       BottomNavTab(AppScreen.Library, "Library", AppIcon.Library),
       BottomNavTab(AppScreen.Collections, "Collections", AppIcon.Collections),
       BottomNavTab(AppScreen.Settings, "Settings", AppIcon.Settings)
   )
   ```

2. **Update AppScaffold to support bottom navigation**:
   Add bottomBar parameter and BottomNavigationBar component
   
   **⚠️ MiniPlayer Space Reservation**: When implementing BottomBar, reserve space above it using `Box`/`Spacer` for future MiniPlayer layering (Spotify-style)

3. **Update App.kt with centralized navigation using DeadlyNavHost**:
   ```kotlin
   @Composable
   fun App() {
       MaterialTheme {
           val navigationController = rememberNavigationController()
           val currentScreen = navigationController.currentScreen ?: AppScreen.Home
           val barConfig = NavigationBarConfig.getBarConfig(currentScreen)
           val searchViewModel: SearchViewModel = DIHelper.get()
           
           // Collect navigation events from ViewModels
           LaunchedEffect(Unit) {
               searchViewModel.navigation.collect { event ->
                   navigationController.navigate(event.screen)
               }
           }
           
           AppScaffold(
               topBarConfig = barConfig.topBar,
               bottomBarConfig = barConfig.bottomBar,
               currentScreen = currentScreen,
               onNavigateToTab = { screen -> 
                   navigationController.navigate(screen)
               },
               onNavigateBack = { navigationController.navigateUp() }
           ) { paddingValues ->
               // Use DeadlyNavHost abstraction consistently
               DeadlyNavHost(
                   navigationController = navigationController,
                   startDestination = AppScreen.Home,
                   modifier = Modifier.padding(paddingValues)
               ) {
                   // Simplified composable definitions - no repeated AppScaffold
                   composable(AppScreen.Home) {
                       Text("Home Screen")
                   }
                   composable(AppScreen.Search) {
                       SearchScreen(viewModel = searchViewModel, ...)
                   }
                   // ... other routes use AppScreen directly
               }
           }
       }
   }
   ```

4. **Test**: Verify bottom navigation works and all screens are reachable

---

### ✅ Checkpoint 6: SearchResultsScreen Navigation (COMPLETED)
**Status**: ✅ **IMPLEMENTED**

**Completed Features**:
- ✅ SearchResultsScreen implemented with full V2 patterns
- ✅ Search box navigation working (SearchScreen → SearchResultsScreen)
- ✅ Back navigation working correctly
- ✅ Bottom navigation maintained across screens
- ✅ Pin indicators and LibraryV2-style cards implemented
- ✅ Integrated TopBar with proper search headers

**Implementation**:
1. **Create SearchResultsScreen (`commonMain/feature/search/SearchResultsScreen.kt`)**:
   ```kotlin
   @Composable
   fun SearchResultsScreen(
       searchQuery: String = "",
       modifier: Modifier = Modifier
   ) {
       Column(
           modifier = modifier.fillMaxSize().padding(16.dp),
           verticalArrangement = Arrangement.spacedBy(16.dp)
       ) {
           Text(
               text = "Search Results",
               style = MaterialTheme.typography.headlineMedium
           )
           
           if (searchQuery.isNotEmpty()) {
               Text("Searching for: $searchQuery")
           }
           
           Text("Results will appear here...")
       }
   }
   ```

2. **Update AppScreen to support query parameters**:
   ```kotlin
   sealed interface AppScreen {
       // ... existing screens
       data class SearchResults(val query: String = "") : AppScreen
   }
   
   // Update route extensions
   fun AppScreen.route(): String = when (this) {
       // ... existing routes
       is AppScreen.SearchResults -> if (query.isNotEmpty()) "search-results?query=$query" else "search-results"
   }
   ```

3. **Add search-results route to NavHost**:
   ```kotlin
   composable(
       "search-results?query={query}",
       arguments = listOf(navArgument("query") { defaultValue = "" })
   ) { backStackEntry ->
       val query = backStackEntry.arguments?.getString("query") ?: ""
       SearchResultsScreen(searchQuery = query)
   }
   ```

4. **Update SearchScreen navigation callbacks**:
   ```kotlin
   onNavigateToSearchResults = {
       val screen = AppScreen.SearchResults(query = "test")
       navController.navigate(screen.route())
   }
   ```

5. **Test**: Search box should navigate to results screen with back navigation working

---

## ✅ Dependencies Implemented

**Completed Cross-Platform Navigation Strategy**:
`DeadlyNavHost` abstraction successfully implemented wrapping platform-specific navigation:
- **Android**: `androidx.navigation.compose` for NavHost ✅ Working
- **iOS**: SwiftUI NavigationStack integration ✅ Working

**Implemented Dependencies:**
```toml
[versions]
navigation-compose = "2.8.5"

[libraries]
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation-compose" }
```

## ✅ Testing Status

All navigation features have been tested and verified working:

1. **Android Test**: ✅ **PASSED**
   - Visual appearance matches V2 patterns
   - Navigation flows work correctly
   - Logs verified with `make logs-android-search`

2. **iOS Test**: ✅ **PASSED**
   - Visual appearance matches native expectations
   - Navigation flows work correctly
   - Logs verified with `make logs-ios-search`

3. **Integration Test**: ✅ **PASSED**
   - Navigation through all screens working
   - Back navigation working correctly
   - Bottom navigation tab switching working
   - No crashes or errors detected
   - Cross-platform builds with `make build-all` working
   - Cross-platform testing with `make run-all` working

## ✅ Achieved Benefits of Navigation Implementation

✅ **Cross-platform abstraction** - DeadlyNavHost works on Android and iOS
✅ **Feature-colocated configuration** - Each screen defines its bar setup
✅ **Shared ViewModel navigation** - SearchViewModel works across screens
✅ **V2 design patterns** - Pin indicators, cards, integrated headers
✅ **Working navigation flows** - Search → Results → Back navigation
✅ **5-tab bottom navigation** - All tabs working with proper highlighting
✅ **Cross-platform icon system** - Material Symbols + SF Symbols unified API

## 🚀 Available for Future Enhancement

- **MiniPlayer Integration**: Foundation ready for Spotify-style layering above bottom navigation
- **Navigation Animations**: Smooth transitions between screens (framework supports this)
- **Deep Linking**: Foundation ready for URL-based navigation integration
- **Advanced TopBar**: IMMERSIVE mode support for edge-to-edge content
- **State Persistence**: Add rememberSaveable to persist current tab across config changes

**Note**: iOS NavigationStack integration and theme integration have been completed.

## ✅ Success Metrics - All Achieved

- ✅ **5-tab bottom navigation working on both platforms**
- ✅ **Search → Search Results → Back navigation flow working**
- ✅ **TopBar shows appropriate titles and back buttons**
- ✅ **Clean, maintainable architecture using expect/actual pattern**
- ✅ **Foundation ready for additional screens and features**
- ✅ **Cross-platform navigation working throughout the app**
- ✅ **Feature-colocated bar configuration implemented**
- ✅ **All screens have proper bar configuration**
- ✅ **Foundation ready for MiniPlayer integration**
- ✅ **DeadlyNavHost abstraction working on both Android and iOS**

---

# 📐 Appendix: Navigation Architecture Overview

This diagram shows how the navigation pieces fit together in the KMM project.

```
 ┌─────────────────────────┐
 │       AppScreen         │
 │ (sealed interface)      │
 │  • Home                 │
 │  • Search               │
 │  • ShowDetail(id)       │
 │  • Library              │
 │  • Collections          │
 │  • Settings             │
 │  • SearchResults(query) │
 └───────────┬─────────────┘
             │ implements
             ▼
 ┌─────────────────────────┐
 │       RouteSpec         │
 │ (encode/decode routes)  │
 │  • pattern: String      │
 │  • toRoute(AppScreen)   │
 │  • fromBackStack(...)   │
 └───────────┬─────────────┘
             │ used by
             ▼
 ┌─────────────────────────┐
 │   AppScreenExtensions   │
 │ (helpers, delegates to  │
 │  RouteSpec to avoid     │
 │  string duplication)    │
 └───────────┬─────────────┘
             │
             ▼
 ┌─────────────────────────┐
 │    NavigationEvent      │
 │ (sealed class, emitted  │
 │  from ViewModels)       │
 │  • NavigateTo(screen)   │
 │  • Back                 │
 └───────────┬─────────────┘
             │ flows into
             ▼
 ┌─────────────────────────┐
 │     DeadlyNavHost       │
 │ (expect/actual wrapper  │
 │  around navigation impl)│
 │  • Android → NavHost    │
 │  • iOS → NavigationStack│
 └───────────┬─────────────┘
             │ rendered inside
             ▼
 ┌─────────────────────────┐
 │      AppScaffold        │
 │ (layout shell w/        │
 │  TopBar, BottomNav,     │
 │  MiniPlayer, content)   │
 └─────────────────────────┘
```

---

### 🔑 Key Flow

1. **AppScreen**
   Defines all possible destinations (sealed interface).

2. **RouteSpec**
   Encodes/decodes routes for each `AppScreen`.

3. **NavigationEvent**
   ViewModels emit these events → "navigate to screen X".

4. **DeadlyNavHost**
   Platform-specific navigation host.

   * On Android: delegates to `NavHost` (Navigation Compose).
   * On iOS: delegates to `NavigationStack` (SwiftUI).

5. **AppScaffold**
   Wraps the whole UI in a consistent frame with `TopBar`, `BottomNav`, and optional `MiniPlayer`.

---

This appendix gives a **bird's-eye map** so devs can see how the checkpoints connect.