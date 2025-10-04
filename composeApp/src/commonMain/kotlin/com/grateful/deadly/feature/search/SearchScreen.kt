package com.grateful.deadly.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grateful.deadly.feature.search.components.SearchSearchBox
import com.grateful.deadly.feature.search.components.SearchBrowseSection
import com.grateful.deadly.feature.search.components.SearchDiscoverSection
import com.grateful.deadly.feature.search.components.SearchBrowseAllSection
import com.grateful.deadly.feature.search.components.QrScannerComingSoonDialog

/**
 * SearchScreen - Next-generation search and discovery interface
 * 
 * This is the KMM implementation of the search/browse experience following
 * the V2 architecture pattern. Built using UI-first development methodology
 * where the UI drives the discovery of service requirements.
 * 
 * Architecture:
 * - Material3 design system with Search-specific enhancements
 * - Clean navigation callbacks matching V2 interface
 * - Manual dependency injection (ViewModel passed from platform code)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToShow: (String) -> Unit,
    onNavigateToSearchResults: () -> Unit,
    initialEra: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // QR Scanner coming soon dialog state
    var showQrComingSoonDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Row 2: Search box
        item {
            SearchSearchBox(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChanged,
                onFocusReceived = onNavigateToSearchResults,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Row 3 & 4: Browse by decades
        item {
            SearchBrowseSection(
                onDecadeClick = { era -> /* TODO: Handle decade browse */ }
            )
        }
        
        // Row 5 & 6: Discover section
        item {
            SearchDiscoverSection(
                onDiscoverClick = { item -> /* TODO: Handle discover */ }
            )
        }
        
        // Row 7 & 8: Browse All section
        item {
            SearchBrowseAllSection(
                onBrowseAllClick = { item -> /* TODO: Handle browse all */ }
            )
        }
    }
    
    // QR Scanner coming soon dialog
    if (showQrComingSoonDialog) {
        QrScannerComingSoonDialog(
            onDismiss = { showQrComingSoonDialog = false }
        )
    }
}