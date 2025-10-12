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
import com.grateful.deadly.services.data.DataSyncOrchestrator
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
    Logger.i("App", "🔍 🎵 Deadly app UI starting - App composable invoked")

    // Get theme manager and current theme
    val themeManager: ThemeManager = remember { DIHelper.get() }
    val currentTheme by themeManager.currentTheme.collectAsState()

    DeadlyTheme(themeMode = currentTheme) {
        val navigationController = rememberNavigationController()
        // Manual injection using KoinComponent for Koin 4.1.0 - cached to survive recomposition
        val searchViewModel: SearchViewModel = remember { DIHelper.get() }
        val recentShowsService: com.grateful.deadly.services.data.RecentShowsService = remember { DIHelper.get() }
        val coroutineScope = rememberCoroutineScope()

        // Start RecentShowsService tracking on app startup
        LaunchedEffect(Unit) {
            Logger.i("App", "📱 Starting RecentShowsService tracking...")
            recentShowsService.startTracking()
            Logger.i("App", "📱 RecentShowsService tracking started")
        }

        // Collect navigation events from ViewModels
        LaunchedEffect(Unit) {
            searchViewModel.navigation.collect { event ->
                navigationController.navigate(event.screen)
            }
        }
        
        // Track current screen with simple state in App composable
        var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
        val barConfig = NavigationBarConfig.getBarConfig(currentScreen)
        
        // AppScaffold wraps the navigation with consistent TopBar and BottomBar
        AppScaffold(
            topBarConfig = barConfig.topBar,
            bottomBarConfig = barConfig.bottomBar,
            currentScreen = currentScreen,
            onNavigateBack = { navigationController.navigateUp() },
            onNavigateToTab = { screen -> navigationController.navigate(screen) },
            miniPlayerContent = {
                // MiniPlayer integration - shows when audio is playing
                val mediaService: com.grateful.deadly.services.media.MediaService = remember { DIHelper.get() }

                com.grateful.deadly.feature.player.components.MiniPlayer(
                    mediaService = mediaService,
                    onPlayerClick = {
                        // Navigate to full player screen
                        navigationController.navigate(AppScreen.Player)
                    }
                )
            }
        ) { paddingValues ->
            // DeadlyNavHost with cross-platform navigation abstraction
            DeadlyNavHost(
                navigationController = navigationController,
                startDestination = AppScreen.Splash, // Start on Splash screen for initialization
                modifier = Modifier.padding(paddingValues),
                onScreenChanged = { screen -> currentScreen = screen }
            ) {
            // Splash screen for initialization
            composable(AppScreen.Splash) {
                Logger.i("App", "🔍 📱 Splash screen composable invoked")
                val splashViewModel: com.grateful.deadly.feature.splash.SplashViewModel = remember {
                    Logger.i("App", "🔍 🏗️ Creating SplashViewModel from Koin...")
                    DIHelper.get()
                }
                Logger.i("App", "🔍 ✅ SplashViewModel obtained: $splashViewModel")

                com.grateful.deadly.feature.splash.SplashScreen(
                    onSplashComplete = {
                        navigationController.navigate(AppScreen.Home)
                    },
                    viewModel = splashViewModel
                )
            }

            // Main bottom navigation tabs
            composable(AppScreen.Home) {
                // Create HomeViewModel lazily when navigating to Home (after splash completes)
                val homeViewModel: com.grateful.deadly.feature.home.HomeViewModel = remember { DIHelper.get() }

                // Collect navigation events from HomeViewModel
                LaunchedEffect(Unit) {
                    homeViewModel.navigation.collect { event ->
                        navigationController.navigate(event.screen)
                    }
                }

                com.grateful.deadly.feature.home.HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToShow = { showId, recordingId ->
                        coroutineScope.launch {
                            homeViewModel.onNavigateToShow(showId, recordingId)
                        }
                    },
                    onNavigateToSearch = {
                        coroutineScope.launch {
                            homeViewModel.onNavigateToSearch()
                        }
                    },
                    onNavigateToCollection = { collectionId ->
                        coroutineScope.launch {
                            homeViewModel.onNavigateToCollection(collectionId)
                        }
                    }
                )
            }
            
            composable(AppScreen.Search) {
                // Real SearchScreen with ViewModel Flow-based navigation integration
                com.grateful.deadly.feature.search.SearchScreen(
                    viewModel = searchViewModel,
                    // All callbacks now trigger ViewModel navigation methods that emit NavigationEvent
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
            
            // Library screen with full functionality
            composable(AppScreen.Library) {
                val libraryViewModel: com.grateful.deadly.feature.library.LibraryViewModel = remember { DIHelper.get() }

                com.grateful.deadly.feature.library.LibraryScreen(
                    onNavigateToShow = { showId ->
                        navigationController.navigate(AppScreen.ShowDetail(showId))
                    },
                    onNavigateBack = {
                        navigationController.navigateUp()
                    },
                    viewModel = libraryViewModel
                )
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
                )
            }
            
            composable(AppScreen.Player) {
                val playerViewModel: com.grateful.deadly.feature.player.PlayerViewModel = remember { DIHelper.get() }

                com.grateful.deadly.feature.player.PlayerScreen(
                    viewModel = playerViewModel,
                    onNavigateBack = { navigationController.navigateUp() },
                    onNavigateToShowDetail = { showId, recordingId ->
                        // Navigate to ShowDetail with current show/recording like V2
                        navigationController.navigate(AppScreen.ShowDetail(showId, recordingId))
                    }
                )
            }
            
            // Show detail routes with parameters using cross-platform argument handling
            composable("showDetail/{showId}") { args ->
                val showId = args["showId"] ?: ""
                val showDetailViewModel = remember {
                    com.grateful.deadly.feature.showdetail.ShowDetailViewModel(
                        showDetailService = DIHelper.get(),
                        mediaService = DIHelper.get(),
                        libraryService = DIHelper.get()
                    )
                }
                val recordingSelectionViewModel = remember(showDetailViewModel) {
                    com.grateful.deadly.feature.recording.RecordingSelectionViewModel(
                        recordingSelectionService = DIHelper.get(),
                        onRecordingChanged = { recordingId ->
                            showDetailViewModel.selectRecording(recordingId)
                        }
                    )
                }

                // Collect navigation events from ShowDetailViewModel
                LaunchedEffect(showDetailViewModel) {
                    showDetailViewModel.navigation.collect { event ->
                        navigationController.navigate(event.screen)
                    }
                }

                com.grateful.deadly.feature.showdetail.ShowDetailScreen(
                    showId = showId,
                    recordingId = null,
                    onNavigateBack = { navigationController.navigateUp() },
                    viewModel = showDetailViewModel,
                    recordingSelectionViewModel = recordingSelectionViewModel
                )
            }

            composable("showDetail/{showId}/{recordingId}") { args ->
                val showId = args["showId"] ?: ""
                val recordingId = args["recordingId"] ?: ""
                val showDetailViewModel = remember {
                    com.grateful.deadly.feature.showdetail.ShowDetailViewModel(
                        showDetailService = DIHelper.get(),
                        mediaService = DIHelper.get(),
                        libraryService = DIHelper.get()
                    )
                }
                val recordingSelectionViewModel = remember(showDetailViewModel) {
                    com.grateful.deadly.feature.recording.RecordingSelectionViewModel(
                        recordingSelectionService = DIHelper.get(),
                        onRecordingChanged = { recordingId ->
                            showDetailViewModel.selectRecording(recordingId)
                        }
                    )
                }

                // Collect navigation events from ShowDetailViewModel
                LaunchedEffect(showDetailViewModel) {
                    showDetailViewModel.navigation.collect { event ->
                        navigationController.navigate(event.screen)
                    }
                }

                com.grateful.deadly.feature.showdetail.ShowDetailScreen(
                    showId = showId,
                    recordingId = recordingId,
                    onNavigateBack = { navigationController.navigateUp() },
                    viewModel = showDetailViewModel,
                    recordingSelectionViewModel = recordingSelectionViewModel
                )
            }
            }
        }
    }
}