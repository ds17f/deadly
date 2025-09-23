package com.grateful.deadly.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grateful.deadly.feature.search.components.SearchResultsTopBar
import com.grateful.deadly.feature.search.components.RecentSearchesSection
import com.grateful.deadly.feature.search.components.SearchResultsSection

/**
 * SearchResultsScreen - Full-screen search interface
 *
 * This screen provides a comprehensive search experience with:
 * - Search input with back navigation
 * - Recent searches for quick access
 * - Suggested search terms
 * - Search results similar to LibraryV2 cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    initialQuery: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToShow: (String) -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    viewModel: SearchViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Fixed top bar with back arrow and search input (like Spotify)
        SearchResultsTopBar(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = viewModel::onSearchQueryChanged,
            onNavigateBack = onNavigateBack
        )

        // Search content that scrolls underneath the fixed header
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.searchQuery.isEmpty()) {
                // Show recent searches when no query
                item {
                    RecentSearchesSection(
                        recentSearches = uiState.recentSearches,
                        onSearchSelected = { recentSearch ->
                            viewModel.onRecentSearchSelected(recentSearch)
                        }
                    )
                }
            } else {
                // Show results when typing (suggestions removed for cleaner UI)
                item {
                    SearchResultsSection(
                        searchResults = uiState.searchResults,
                        searchStatus = uiState.searchStatus,
                        searchStats = uiState.searchStats,
                        onShowSelected = onNavigateToShow,
                        onRecordingSelected = onNavigateToPlayer
                    )
                }
            }
        }
    }
}