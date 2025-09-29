package com.grateful.deadly.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.util.Logger
import com.grateful.deadly.feature.home.components.HorizontalCollection
import com.grateful.deadly.feature.home.components.HorizontalCollectionItem
import com.grateful.deadly.feature.home.components.CollectionItemType
import com.grateful.deadly.feature.home.components.RecentShowsGrid

/**
 * HomeScreen - Rich home interface with content discovery
 *
 * V2 implementation featuring:
 * - Recent Shows Grid (2x4 layout)
 * - Today In Grateful Dead History (horizontal scroll)
 * - Featured Collections (horizontal scroll)
 *
 * Scaffold-free content designed for use within AppScaffold.
 * Follows V2 architecture with single HomeService dependency and cross-platform compatibility.
 */
@Composable
fun HomeScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCollection: (String) -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.hasError) {
        // Error state - show error message with retry
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = uiState.error ?: "Unknown error",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = { viewModel.refresh() }) {
                    Text("Retry")
                }
            }
        }
        return
    }

    if (uiState.isLoading && !uiState.homeContent.hasContent) {
        // Loading state - show loading indicator
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Main content
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Recent Shows Grid Section - only show if there are recent shows
        if (uiState.homeContent.recentShows.isNotEmpty()) {
            item {
                RecentShowsGrid(
                    shows = uiState.homeContent.recentShows,
                    onShowClick = onNavigateToShow,
                    onShowLongPress = { show ->
                        // TODO: Implement context menu (library add/remove, etc.)
                        Logger.d("HomeScreen", "Long pressed show: ${show.id}")
                    }
                )
            }
        }

        // Today In Grateful Dead History Section
        if (uiState.homeContent.todayInHistory.isNotEmpty()) {
            item {
                val todayItems = uiState.homeContent.todayInHistory.map { show ->
                    HorizontalCollectionItem(
                        id = show.id,
                        displayText = "${show.date}\n${show.venue.name}\n${show.location.displayText}",
                        type = CollectionItemType.SHOW
                    )
                }

                HorizontalCollection(
                    title = "Today In Grateful Dead History",
                    items = todayItems,
                    onItemClick = { item ->
                        // Find the show and navigate
                        val show = uiState.homeContent.todayInHistory.find { it.id == item.id }
                        show?.let {
                            Logger.d("HomeScreen", "🗓️ Navigating to TIGDH show: ${it.id} (${it.date})")
                            onNavigateToShow(it.id)
                        }
                    }
                )
            }
        }

        // Featured Collections Section
        if (uiState.homeContent.featuredCollections.isNotEmpty()) {
            item {
                val collectionItems = uiState.homeContent.featuredCollections.map { collection ->
                    HorizontalCollectionItem(
                        id = collection.id,
                        displayText = "${collection.name}\n${collection.showCountText}",
                        type = CollectionItemType.COLLECTION
                    )
                }

                HorizontalCollection(
                    title = "Featured Collections",
                    items = collectionItems,
                    onItemClick = { item ->
                        onNavigateToCollection(item.id)
                    }
                )
            }
        }

        // Empty state if no content
        if (!uiState.homeContent.hasContent && !uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Welcome to Deadly",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Start exploring Grateful Dead shows",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onNavigateToSearch) {
                            Text("Browse Shows")
                        }
                    }
                }
            }
        }
    }
}