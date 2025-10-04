package com.grateful.deadly.feature.search

import androidx.compose.runtime.Composable
import org.koin.androidx.compose.koinViewModel

/**
 * Android wrapper for SearchScreen that handles Koin ViewModel injection
 */
@Composable
fun AndroidSearchScreen(
    onNavigateToShow: (String) -> Unit,
    onNavigateToSearchResults: () -> Unit,
    initialEra: String? = null,
    viewModel: SearchViewModel = koinViewModel()
) {
    SearchScreen(
        viewModel = viewModel,
        onNavigateToShow = onNavigateToShow,
        onNavigateToSearchResults = onNavigateToSearchResults,
        initialEra = initialEra
    )
}