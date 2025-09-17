package com.grateful.deadly.feature.search.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grateful.deadly.domain.search.*

/**
 * Search results section with LibraryV2-style cards for SearchResultsScreen
 */
@Composable
fun SearchResultsSection(
    searchResults: List<SearchResultShow>,
    searchStatus: SearchStatus,
    searchStats: SearchStats,
    onShowSelected: (String) -> Unit,
    onRecordingSelected: (String) -> Unit
) {
    Column {
        // Results header with stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Search Results",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (searchResults.isNotEmpty()) {
                Text(
                    text = "${searchStats.totalResults} results (${searchStats.searchDuration}ms)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (searchStatus) {
            SearchStatus.SEARCHING -> {
                Text(
                    text = "Searching...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            SearchStatus.NO_RESULTS -> {
                Text(
                    text = "No results found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            SearchStatus.ERROR -> {
                Text(
                    text = "Search failed. Please try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            SearchStatus.SUCCESS -> {
                // Search results using LibraryV2-style cards
                searchResults.forEach { result ->
                    SearchResultCard(
                        searchResult = result,
                        onShowSelected = onShowSelected,
                        onShowLongPress = { show ->
                            // TODO: Implement show actions bottom sheet
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            SearchStatus.IDLE -> {
                // Show nothing when idle
            }
        }
    }
}