package com.grateful.deadly

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.feature.search.SearchViewModel
import com.grateful.deadly.navigation.AppScreen
import com.grateful.deadly.navigation.DeadlyNavHost
import com.grateful.deadly.navigation.NavigationBarConfig
import com.grateful.deadly.navigation.rememberNavigationController
import com.grateful.deadly.core.design.scaffold.AppScaffold
import com.grateful.deadly.core.design.theme.DeadlyTheme
import com.grateful.deadly.core.design.theme.ThemeManager
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object DIHelper : KoinComponent

/**
 * Main App composable - Entry point for the Deadly KMM app
 * 
 * This is the root navigation controller that manages all app screens using a sealed class
 * architecture for type-safe navigation. The AppScreen sealed interface provides compile-time
 * route validation and shared navigation logic between Android and iOS platforms.
 * 
 * Architecture:
 * - Uses AppScreen sealed interface for type-safe navigation 
 * - ViewModels emit NavigationEvent via Flow for reactive navigation
 * - DeadlyNavHost abstracts platform-specific navigation (NavHost on Android, NavigationStack on iOS)
 * - Supports parameterized routes with cross-platform argument handling
 * 
 * Navigation Flow:
 * - SearchScreen callbacks trigger ViewModel navigation methods
 * - ViewModels emit NavigationEvent to Flow  
 * - App.kt collects navigation events and triggers navigation via NavigationController
 * - All screens accessible via cross-platform navigation abstraction
 */
@Composable
@Preview
fun App() {
    Logger.i("App", "🎵 Deadly app UI starting")

    // Get theme manager and current theme
    val themeManager: ThemeManager = remember { DIHelper.get() }
    val currentTheme by themeManager.currentTheme.collectAsState()

    DeadlyTheme(themeMode = currentTheme) {
        val navigationController = rememberNavigationController()
        // Manual injection using KoinComponent for Koin 4.1.0 - cached to survive recomposition
        val searchViewModel: SearchViewModel = remember { DIHelper.get() }
        val coroutineScope = rememberCoroutineScope()
        
        // Collect navigation events from ViewModels
        LaunchedEffect(Unit) {
            searchViewModel.navigation.collect { event ->
                navigationController.navigate(event.screen)
            }
        }
        
        // Track current screen with simple state in App composable
        var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Search) }
        val barConfig = NavigationBarConfig.getBarConfig(currentScreen)
        
        // AppScaffold wraps the navigation with consistent TopBar and BottomBar
        AppScaffold(
            topBarConfig = barConfig.topBar,
            bottomBarConfig = barConfig.bottomBar,
            currentScreen = currentScreen,
            onNavigateBack = { navigationController.navigateUp() },
            onNavigateToTab = { screen -> navigationController.navigate(screen) }
        ) { paddingValues ->
            // DeadlyNavHost with cross-platform navigation abstraction
            DeadlyNavHost(
                navigationController = navigationController,
                startDestination = AppScreen.Search, // Start on Search for development
                modifier = Modifier.padding(paddingValues),
                onScreenChanged = { screen -> currentScreen = screen }
            ) {
            // Main bottom navigation tabs
            composable(AppScreen.Home) {
                Text("Home Screen")
            }
            
            composable(AppScreen.Search) {
                // Real SearchScreen with ViewModel Flow-based navigation integration
                com.grateful.deadly.feature.search.SearchScreen(
                    viewModel = searchViewModel,
                    // All callbacks now trigger ViewModel navigation methods that emit NavigationEvent
                    onNavigateToPlayer = { recordingId -> 
                        coroutineScope.launch {
                            searchViewModel.onNavigateToPlayer(recordingId)
                        }
                    },
                    onNavigateToShow = { showId ->
                        coroutineScope.launch {
                            searchViewModel.onSearchResultSelected(showId)
                        }
                    },
                    onNavigateToSearchResults = {
                        coroutineScope.launch {
                            searchViewModel.onSearchQuerySubmitted("")
                        }
                    }
                )
            }
            
            // Placeholder screens for remaining bottom nav tabs
            composable(AppScreen.Library) {
                Text("Library Screen")
            }
            
            composable(AppScreen.Collections) {
                Text("Collections Screen")
            }
            
            composable(AppScreen.Settings) {
                com.grateful.deadly.feature.settings.SettingsScreen()
            }
            
            // Secondary screens
            composable(AppScreen.SearchResults) {
                // SearchResultsScreen shares the same ViewModel instance for state continuity
                com.grateful.deadly.feature.search.SearchResultsScreen(
                    viewModel = searchViewModel,
                    onNavigateBack = {
                        navigationController.navigateUp()
                    },
                    onNavigateToShow = { showId ->
                        coroutineScope.launch {
                            searchViewModel.onSearchResultSelected(showId)
                        }
                    },
                    onNavigateToPlayer = { recordingId ->
                        coroutineScope.launch {
                            searchViewModel.onNavigateToPlayer(recordingId)
                        }
                    }
                )
            }
            
            composable(AppScreen.Player) {
                Text("Player Screen")
            }
            
            // Show detail routes with parameters using cross-platform argument handling
            composable("showDetail/{showId}") { args ->
                val showId = args["showId"] ?: ""
                Text("Show Detail Screen: $showId")
            }
            
            composable("showDetail/{showId}/{recordingId}") { args ->
                val showId = args["showId"] ?: ""
                val recordingId = args["recordingId"] ?: ""
                Text("Show Detail Screen: $showId, Recording: $recordingId")
            }
            }
        }
    }
}