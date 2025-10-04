package com.grateful.deadly.feature.search

import androidx.compose.runtime.Composable
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

private object IOSDIHelper : KoinComponent

/**
 * iOS wrapper for SearchScreen that handles manual Koin ViewModel injection
 */
@Composable
fun IOSSearchScreen(
    onNavigateToShow: (String) -> Unit,
    onNavigateToSearchResults: () -> Unit,
    initialEra: String? = null
) {
    val viewModel: SearchViewModel = IOSDIHelper.get()
    
    SearchScreen(
        viewModel = viewModel,
        onNavigateToShow = onNavigateToShow,
        onNavigateToSearchResults = onNavigateToSearchResults,
        initialEra = initialEra
    )
}