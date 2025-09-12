# Navigation System Implementation Guide

This document provides a step-by-step plan to implement the sophisticated navigation architecture from V2 Android app into our KMM project, using **sealed classes for shared, type-safe navigation**. This approach ensures Android and iOS share a **single source of truth for routes**.

## Overview

We're porting V2's navigation system which includes:
- **AppScaffold**: Unified layout manager with TopBar/BottomBar/MiniPlayer coordination
- **5-Tab Bottom Navigation**: Home, Search, Library, Collections, Settings
- **Route-based UI Configuration**: Each screen defines its bar visibility
- **Spotify-style Layering**: MiniPlayer above bottom navigation
- **TopBar Modes**: SOLID (padded) vs IMMERSIVE (edge-to-edge)
- **Navigation Model**: `AppScreen` sealed class in `commonMain` used for all screens

## Source Reference (V2 Components)

All paths relative to `../dead/v2/`:

### Core Navigation Files:
- `app/src/main/java/com/deadly/v2/app/MainNavigation.kt` - Main navigation coordinator
- `app/src/main/java/com/deadly/v2/app/navigation/BottomNavDestination.kt` - 5-tab definitions
- `app/src/main/java/com/deadly/v2/app/navigation/BarConfiguration.kt` - Route-based configs
- `core/design/src/main/java/com/deadly/v2/core/design/scaffold/AppScaffold.kt` - Layout manager
- `core/design/src/main/java/com/deadly/v2/core/design/component/topbar/TopBar.kt` - TopBar component

### Dependencies from V2:
- `app/build.gradle.kts`: `androidx.navigation:navigation-compose:2.7.7`

## Implementation Plan - Buildable Checkpoints

Each checkpoint is independently testable on Android and iOS.

### Checkpoint 1: Navigation Foundation (Shared)
**Goal**: Add sealed class to represent all app screens, usable from Android and iOS

**Success Criteria**:
- ✅ AppScreen sealed class compiles on both Android and iOS
- ✅ ViewModels can emit `AppScreen` for navigation events
- ✅ Foundation ready for platform-specific navigation integration

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
       object SearchResults : AppScreen
   }
   ```

2. **Add route conversion extensions (`commonMain/navigation/AppScreenExtensions.kt`)**:
   ```kotlin
   fun AppScreen.route(): String = when (this) {
       AppScreen.Home -> "home"
       AppScreen.Search -> "search"
       AppScreen.Library -> "library"
       AppScreen.Collections -> "collections"
       AppScreen.Settings -> "settings"
       AppScreen.SearchResults -> "search-results"
       is AppScreen.ShowDetail -> "show/${this.id}"
   }
   
   fun String.toAppScreen(): AppScreen? = when {
       this == "home" -> AppScreen.Home
       this == "search" -> AppScreen.Search
       this == "library" -> AppScreen.Library
       this == "collections" -> AppScreen.Collections
       this == "settings" -> AppScreen.Settings
       this == "search-results" -> AppScreen.SearchResults
       this.startsWith("show/") -> AppScreen.ShowDetail(this.removePrefix("show/"))
       else -> null
   }
   ```

3. **Test**: Verify sealed class compiles and route conversion works correctly

---

### Checkpoint 2: ViewModels Emit Navigation Events
**Goal**: Refactor shared ViewModels to return `AppScreen` when navigating

**Success Criteria**:
- ✅ Shared logic triggers navigation without platform-specific references
- ✅ Android and iOS can observe these navigation events
- ✅ SearchViewModel updated to use sealed navigation

**Implementation**:
1. **Update SearchViewModel to emit AppScreen**:
   ```kotlin
   class SearchViewModel {
       fun onSearchResultSelected(showId: String): AppScreen {
           return AppScreen.ShowDetail(showId)
       }

       fun onSearchQuerySubmitted(query: String): AppScreen {
           return AppScreen.SearchResults
       }
       
       fun onNavigateToPlayer(recordingId: String): AppScreen {
           Logger.i("SearchViewModel", "Navigate to player: $recordingId")
           // TODO: Return AppScreen.Player when implemented
           return AppScreen.Home
       }
   }
   ```

2. **Create navigation event system**:
   ```kotlin
   // commonMain/navigation/NavigationEvent.kt
   data class NavigationEvent(
       val screen: AppScreen,
       val clearBackStack: Boolean = false
   )
   ```

3. **Test**: Verify ViewModels can emit navigation events without crashes

---

### Checkpoint 3: Android NavHost Integration
**Goal**: Wire sealed class into Android's Compose Navigation

**Success Criteria**:
- ✅ Navigation triggered by `AppScreen` events
- ✅ Arguments passed correctly for parameterized screens
- ✅ App navigates to search screen via NavHost
- ✅ Search screen looks and functions identically to before

**Implementation**:
1. **Update App.kt to use NavHost with AppScreen**:
   ```kotlin
   @Composable
   fun App() {
       Logger.i("App", "🎵 Deadly app UI starting")
       
       MaterialTheme {
           val navController = rememberNavController()
           val searchViewModel: SearchViewModel = DIHelper.get()
           
           NavHost(
               navController = navController,
               startDestination = AppScreen.Search.route()
           ) {
               composable(AppScreen.Home.route()) {
                   // TODO: HomeScreen placeholder
                   Text("Home Screen")
               }
               
               composable(AppScreen.Search.route()) {
                   SearchScreen(
                       viewModel = searchViewModel,
                       onNavigateToPlayer = { recordingId -> 
                           val screen = searchViewModel.onNavigateToPlayer(recordingId)
                           navController.navigate(screen.route())
                       },
                       onNavigateToShow = { showId ->
                           val screen = searchViewModel.onSearchResultSelected(showId)
                           navController.navigate(screen.route())
                       },
                       onNavigateToSearchResults = {
                           val screen = searchViewModel.onSearchQuerySubmitted("")
                           navController.navigate(screen.route())
                       }
                   )
               }
               
               composable(AppScreen.SearchResults.route()) {
                   // TODO: SearchResultsScreen placeholder
                   Text("Search Results Screen")
               }
               
               composable(AppScreen.Library.route()) {
                   Text("Library Screen")
               }
               
               composable(AppScreen.Collections.route()) {
                   Text("Collections Screen")
               }
               
               composable(AppScreen.Settings.route()) {
                   Text("Settings Screen")
               }
               
               composable(
                   "show/{id}",
                   arguments = listOf(navArgument("id") { type = NavType.StringType })
               ) { backStackEntry ->
                   val id = backStackEntry.arguments?.getString("id") ?: ""
                   Text("Show Detail Screen: $id")
               }
           }
       }
   }
   ```

2. **Test**: Verify search screen appears via NavHost and navigation works

---

### Checkpoint 4: AppScaffold with TopBar
**Goal**: Add AppScaffold wrapper with TopBar support using AppScreen-based configuration

**Success Criteria**:
- ✅ Search screen now has a top bar
- ✅ TopBar shows "Search" title and looks native on both platforms
- ✅ Search content is properly padded below TopBar
- ✅ Bar configuration based on AppScreen type

**Implementation**:
1. **Create TopBar modes (`commonMain/design/topbar/TopBarMode.kt`)**:
   ```kotlin
   sealed class TopBarMode {
       object SOLID : TopBarMode()      // Content padded below status bar
       object IMMERSIVE : TopBarMode()  // Content behind status bar with scrim
   }
   ```

2. **Create BarConfiguration system (`commonMain/navigation/BarConfiguration.kt`)**:
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
       fun getBarConfig(screen: AppScreen): BarConfiguration = when(screen) {
           AppScreen.Home -> BarConfiguration(
               topBar = TopBarConfig(title = "Home"),
               bottomBar = BottomBarConfig(visible = true)
           )
           AppScreen.Search -> BarConfiguration(
               topBar = TopBarConfig(title = "Search"),
               bottomBar = BottomBarConfig(visible = true)
           )
           AppScreen.SearchResults -> BarConfiguration(
               topBar = TopBarConfig(title = "Search Results", showBackButton = true),
               bottomBar = BottomBarConfig(visible = true)
           )
           AppScreen.Library -> BarConfiguration(
               topBar = TopBarConfig(title = "Library"),
               bottomBar = BottomBarConfig(visible = true)
           )
           AppScreen.Collections -> BarConfiguration(
               topBar = TopBarConfig(title = "Collections"),
               bottomBar = BottomBarConfig(visible = true)
           )
           AppScreen.Settings -> BarConfiguration(
               topBar = TopBarConfig(title = "Settings"),
               bottomBar = BottomBarConfig(visible = true)
           )
           is AppScreen.ShowDetail -> BarConfiguration(
               topBar = TopBarConfig(title = "Show Details", showBackButton = true),
               bottomBar = BottomBarConfig(visible = true)
           )
       }
   }
   ```

3. **Create TopBar component (`commonMain/design/topbar/TopBar.kt`)**:
   Port from V2 but simplified for Material3 TopAppBar

4. **Create AppScaffold (`commonMain/design/scaffold/AppScaffold.kt`)**:
   Port from V2 but start with TopBar + Content only (no BottomBar yet)

5. **Update App.kt to use AppScaffold**:
   ```kotlin
   NavHost(...) {
       composable(AppScreen.Search.route()) {
           val barConfig = NavigationBarConfig.getBarConfig(AppScreen.Search)
           AppScaffold(
               topBarConfig = barConfig.topBar,
               onNavigateBack = { navController.popBackStack() }
           ) { paddingValues ->
               SearchScreen(
                   viewModel = searchViewModel,
                   // ... same callbacks
                   modifier = Modifier.padding(paddingValues)
               )
           }
       }
       // ... other routes with AppScaffold
   }
   ```

6. **Test**: Search screen should now have a "Search" top bar

---

### Checkpoint 5: Bottom Navigation Tabs
**Goal**: Connect 5-tab bottom navigation to AppScreen sealed class

**Success Criteria**:
- ✅ Bottom navigation bar appears with 5 tabs: Home, Search, Library, Collections, Settings
- ✅ Tapping tabs navigates between placeholder screens
- ✅ Search tab shows existing SearchScreen
- ✅ Current tab is highlighted correctly

**Implementation**:
1. **Create BottomNavDestination (`commonMain/navigation/BottomNavDestination.kt`)**:
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

3. **Update App.kt with centralized navigation**:
   ```kotlin
   @Composable
   fun App() {
       MaterialTheme {
           val navController = rememberNavController()
           val navBackStackEntry by navController.currentBackStackEntryAsState()
           val currentRoute = navBackStackEntry?.destination?.route
           val currentScreen = currentRoute?.toAppScreen() ?: AppScreen.Home
           val barConfig = NavigationBarConfig.getBarConfig(currentScreen)
           
           AppScaffold(
               topBarConfig = barConfig.topBar,
               bottomBarConfig = barConfig.bottomBar,
               currentScreen = currentScreen,
               onNavigateToTab = { screen -> 
                   navController.navigate(screen.route()) {
                       popUpTo(navController.graph.findStartDestination().id) {
                           saveState = true
                       }
                       launchSingleTop = true
                       restoreState = true
                   }
               },
               onNavigateBack = { navController.popBackStack() }
           ) { paddingValues ->
               NavHost(
                   navController = navController,
                   startDestination = AppScreen.Home.route(),
                   modifier = Modifier.padding(paddingValues)
               ) {
                   // Simplified composable definitions - no repeated AppScaffold
                   composable(AppScreen.Home.route()) {
                       Text("Home Screen")
                   }
                   composable(AppScreen.Search.route()) {
                       SearchScreen(viewModel = searchViewModel, ...)
                   }
                   // ... other routes
               }
           }
       }
   }
   ```

4. **Test**: Verify bottom navigation works and all screens are reachable

---

### Checkpoint 6: SearchResultsScreen Navigation
**Goal**: Create SearchResultsScreen and connect search box navigation using AppScreen

**Success Criteria**:
- ✅ SearchResultsScreen exists and shows placeholder content
- ✅ Tapping search box in SearchScreen navigates to SearchResultsScreen
- ✅ Back navigation from SearchResultsScreen returns to SearchScreen
- ✅ Both screens maintain bottom navigation

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

## Dependencies Required

**⚠️ Cross-Platform Navigation Consideration**: 
`androidx.navigation.compose` is Android-only. Compose Multiplatform provides its own cross-platform `NavHost` API. Verify your Compose Multiplatform version supports NavHost on iOS, or consider creating a `DeadlyNavHost` abstraction for future flexibility.

Already added:
```toml
[versions]
navigation-compose = "2.8.5"

[libraries]
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation-compose" }
```

## Testing Instructions

After each checkpoint:

1. **Android Test**: `make android-run`
   - Verify visual appearance matches expectations
   - Test navigation flows work correctly
   - Check logs with `make logs-android-search`

2. **iOS Test**: `make ios-sim`  
   - Verify visual appearance matches expectations
   - Test navigation flows work correctly
   - Check logs with `make logs-ios-search`

3. **Integration Test**:
   - Navigate through all available screens
   - Verify back navigation works
   - Test bottom navigation tab switching
   - Confirm no crashes or errors

## Key Benefits of Sealed Navigation Model

✅ **Single source of truth** for all routes
✅ **Type-safe navigation** with compile-time route validation  
✅ **Shared navigation model** between Android and iOS
✅ **Future-proof** for native UI migration
✅ **Eliminates string-based route errors**
✅ **Simplified navigation event handling**

## Future Enhancements (Post-Checkpoints)

- **iOS NavigationStack Integration**: Wire AppScreen into SwiftUI NavigationStack
- **MiniPlayer Integration**: Layer above bottom navigation like Spotify
- **Navigation Animations**: Smooth transitions between screens
- **Deep Linking**: Support URL-based navigation with AppScreen conversion
- **Theme Integration**: Port V2's theme system for TopBar styling
- **Advanced TopBar**: IMMERSIVE mode support

## Success Metrics

Upon completion:
- ✅ 5-tab bottom navigation working on both platforms
- ✅ Search → Search Results → Back navigation flow
- ✅ TopBar shows appropriate titles and back buttons
- ✅ Clean, maintainable architecture using sealed classes
- ✅ Foundation ready for additional screens and features
- ✅ Type-safe navigation throughout the app