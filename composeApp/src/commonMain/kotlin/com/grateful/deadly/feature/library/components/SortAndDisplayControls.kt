package com.grateful.deadly.feature.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grateful.deadly.domain.models.LibrarySortOption
import com.grateful.deadly.domain.models.LibrarySortDirection
import com.grateful.deadly.domain.models.LibraryDisplayMode
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

/**
 * KMM Sort and Display Controls Component
 *
 * Focused component for sort controls and grid/list display toggle
 * following KMM component architecture patterns.
 */
@Composable
fun SortAndDisplayControls(
    sortBy: LibrarySortOption,
    sortDirection: LibrarySortDirection,
    displayMode: LibraryDisplayMode,
    onSortSelectorClick: () -> Unit,
    onDisplayModeChanged: (LibraryDisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort selector button
        SortSelectorButton(
            sortBy = sortBy,
            sortDirection = sortDirection,
            onClick = onSortSelectorClick
        )

        // Display mode toggle
        IconButton(
            onClick = {
                onDisplayModeChanged(
                    if (displayMode == LibraryDisplayMode.LIST) {
                        LibraryDisplayMode.GRID
                    } else {
                        LibraryDisplayMode.LIST
                    }
                )
            }
        ) {
            if (displayMode == LibraryDisplayMode.LIST) {
                AppIcon.GridView.Render() // Grid icon when in list mode
            } else {
                AppIcon.FormatListBulleted.Render() // List icon when in grid mode
            }
        }
    }
}

/**
 * Sort selector button component
 */
@Composable
private fun SortSelectorButton(
    sortBy: LibrarySortOption,
    sortDirection: LibrarySortDirection,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon.SwapVert.Render(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = sortBy.displayName)
            Spacer(modifier = Modifier.width(4.dp))
            if (sortDirection == LibrarySortDirection.ASCENDING) {
                AppIcon.KeyboardArrowUp.Render(size = 16.dp)
            } else {
                AppIcon.KeyboardArrowDown.Render(size = 16.dp)
            }
        }
    }
}