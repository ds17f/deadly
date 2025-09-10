package com.grateful.deadly.feature.search.models

import androidx.compose.ui.graphics.Color

/**
 * UI data models for SearchScreen components
 * These models are used exclusively for UI composition and don't contain business logic
 */

data class DecadeBrowse(
    val title: String,
    val gradient: List<Color>,
    val era: String
)

data class DiscoverItem(
    val title: String,
    val subtitle: String = ""
)

data class BrowseAllItem(
    val title: String,
    val subtitle: String,
    val searchQuery: String
)