package com.grateful.deadly.feature.search

import androidx.compose.runtime.Composable
import org.koin.androidx.compose.koinViewModel

/**
 * Android wrapper for SearchScreen that handles Koin ViewModel injection
 */
@Composable
fun AndroidSearchScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (String) -> Unit,
    onNavigateToSearchResults: () -> Unit,
    initialEra: String? = null,
    viewModel: SearchViewModel = koinViewModel()
) {
    SearchScreen(
        viewModel = viewModel,
        onNavigateToPlayer = onNavigateToPlayer,
        onNavigateToShow = onNavigateToShow,
        onNavigateToSearchResults = onNavigateToSearchResults,
        initialEra = initialEra
    )
}