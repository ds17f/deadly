package com.grateful.deadly.feature.search.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grateful.deadly.feature.search.models.DiscoverItem

/**
 * Rows 5 & 6: Discover section
 */
@Composable
fun SearchDiscoverSection(
    onDiscoverClick: (DiscoverItem) -> Unit
) {
    val discoverItems = listOf(
        DiscoverItem("Discover 1"),
        DiscoverItem("Discover 2"),
        DiscoverItem("Discover 3")
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Discover Something New",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            discoverItems.forEach { item ->
                DiscoverCard(
                    item = item,
                    onClick = { onDiscoverClick(item) },
                    modifier = Modifier.weight(1f) // Each card takes equal width
                )
            }
        }
    }
}

/**
 * Individual discover card component - taller design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverCard(
    item: DiscoverItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp), // Flexible width, tall height
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}