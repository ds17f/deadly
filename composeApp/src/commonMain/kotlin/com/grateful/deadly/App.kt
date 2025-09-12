package com.grateful.deadly

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.feature.search.SearchViewModel
import com.grateful.deadly.navigation.AppScreen
import com.grateful.deadly.navigation.route
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
 * - ViewModels return AppScreen objects instead of performing navigation directly
 * - NavHost handles route conversion via AppScreen.route() extension
 * - Supports parameterized routes (showDetail with showId/recordingId)
 * 
 * Navigation Flow:
 * - SearchScreen callbacks return AppScreen objects  
 * - NavController navigates using sealed class routes
 * - All screens accessible via bottom navigation or deep navigation
 */
@Composable
@Preview
fun App() {
    Logger.i("App", "🎵 Deadly app UI starting")
    
    MaterialTheme {
        val navController = rememberNavController()
        // Manual injection using KoinComponent for Koin 4.1.0
        val searchViewModel: SearchViewModel = DIHelper.get()
        
        // NavHost with AppScreen sealed class integration
        NavHost(
            navController = navController,
            startDestination = AppScreen.Search.route() // Start on Search for development
        ) {
            // Main bottom navigation tabs
            composable(AppScreen.Home.route()) {
                Text("Home Screen")
            }
            
            composable(AppScreen.Search.route()) {
                // Real SearchScreen with ViewModel navigation integration
                com.grateful.deadly.feature.search.SearchScreen(
                    viewModel = searchViewModel,
                    // All callbacks now use AppScreen sealed class for type-safe navigation
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
            
            // Placeholder screens for remaining bottom nav tabs
            composable(AppScreen.Library.route()) {
                Text("Library Screen")
            }
            
            composable(AppScreen.Collections.route()) {
                Text("Collections Screen")
            }
            
            composable(AppScreen.Settings.route()) {
                Text("Settings Screen")
            }
            
            // Secondary screens
            composable(AppScreen.SearchResults.route()) {
                Text("Search Results Screen")
            }
            
            composable(AppScreen.Player.route()) {
                Text("Player Screen")
            }
            
            // Show detail routes with parameters
            composable(
                "showDetail/{showId}",
                arguments = listOf(navArgument("showId") { type = NavType.StringType })
            ) { backStackEntry ->
                val showId = backStackEntry.arguments?.getString("showId") ?: ""
                Text("Show Detail Screen: $showId")
            }
            
            composable(
                "showDetail/{showId}/{recordingId}",
                arguments = listOf(
                    navArgument("showId") { type = NavType.StringType },
                    navArgument("recordingId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val showId = backStackEntry.arguments?.getString("showId") ?: ""
                val recordingId = backStackEntry.arguments?.getString("recordingId") ?: ""
                Text("Show Detail Screen: $showId, Recording: $recordingId")
            }
        }
    }
}