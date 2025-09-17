package com.grateful.deadly.feature.search.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grateful.deadly.domain.search.RecentSearch

/**
 * Recent searches section for SearchResultsScreen
 */
@Composable
fun RecentSearchesSection(
    recentSearches: List<RecentSearch>,
    onSearchSelected: (RecentSearch) -> Unit
) {
    if (recentSearches.isNotEmpty()) {
        Column {
            Text(
                text = "Recent Searches",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            recentSearches.forEach { recentSearch ->
                RecentSearchCard(
                    search = recentSearch,
                    onClick = { onSearchSelected(recentSearch) }
                )
            }
        }
    }
}