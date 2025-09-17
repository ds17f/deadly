package com.grateful.deadly.feature.search.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grateful.deadly.domain.search.SuggestedSearch

/**
 * Suggested searches section for SearchResultsScreen
 */
@Composable
fun SuggestedSearchesSection(
    suggestedSearches: List<SuggestedSearch>,
    onSuggestionSelected: (SuggestedSearch) -> Unit
) {
    if (suggestedSearches.isNotEmpty()) {
        Column {
            Text(
                text = "Suggested Searches",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            suggestedSearches.forEach { suggestion ->
                SuggestedSearchCard(
                    suggestion = suggestion,
                    onClick = { onSuggestionSelected(suggestion) }
                )
            }
        }
    }
}