package com.grateful.deadly.feature.search.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grateful.deadly.feature.search.models.BrowseAllItem

/**
 * Rows 7 & 8: Browse All section with 2-column grid
 */
@Composable
fun SearchBrowseAllSection(
    onBrowseAllClick: (BrowseAllItem) -> Unit
) {
    val browseAllItems = listOf(
        BrowseAllItem("Popular Shows", "Most listened to concerts", "popular"),
        BrowseAllItem("Recent Uploads", "Latest additions to Archive.org", "recent"),
        BrowseAllItem("Top Rated", "Highest community ratings", "top-rated"),
        BrowseAllItem("Audience Recordings", "Taped from the crowd", "audience"),
        BrowseAllItem("Soundboard", "Direct from the mixing board", "soundboard"),
        BrowseAllItem("Live Albums", "Official releases", "official")
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Browse All",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(400.dp) // Fixed height for demonstration
        ) {
            items(browseAllItems) { item ->
                BrowseAllCard(
                    item = item,
                    onClick = { onBrowseAllClick(item) }
                )
            }
        }
    }
}

/**
 * Individual browse all card component (2x height of browse cards)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseAllCard(
    item: BrowseAllItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp), // 2x the height of decade cards
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}