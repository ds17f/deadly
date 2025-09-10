package com.grateful.deadly

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.grateful.deadly.feature.search.SearchViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object DIHelper : KoinComponent

@Composable
@Preview
fun App() {
    MaterialTheme {
        // Manual injection using KoinComponent for Koin 4.1.0
        val searchViewModel: SearchViewModel = DIHelper.get()
        
        com.grateful.deadly.feature.search.SearchScreen(
            viewModel = searchViewModel,
            onNavigateToPlayer = { recordingId -> 
                // TODO: Handle navigation to player
                println("Navigate to player: $recordingId")
            },
            onNavigateToShow = { showId ->
                // TODO: Handle navigation to show
                println("Navigate to show: $showId") 
            },
            onNavigateToSearchResults = {
                // TODO: Handle navigation to search results
                println("Navigate to search results")
            }
        )
    }
}