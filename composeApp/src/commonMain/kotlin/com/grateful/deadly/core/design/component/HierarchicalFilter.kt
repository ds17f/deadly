package com.grateful.deadly.core.design.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Data structure for hierarchical filter tree
 */
data class FilterNode(
    val id: String,
    val label: String,
    val children: List<FilterNode> = emptyList()
)

/**
 * Represents the current filter selection path
 */
data class FilterPath(
    val nodes: List<FilterNode> = emptyList()
) {
    val isNotEmpty: Boolean get() = nodes.isNotEmpty()
    val isEmpty: Boolean get() = nodes.isEmpty()

    /**
     * Get display text for the full path (e.g., "[70s] Spring")
     */
    fun getDisplayText(): String {
        return if (nodes.size >= 2) {
            "[${nodes[0].label}] ${nodes[1].label}"
        } else if (nodes.size == 1) {
            nodes[0].label
        } else {
            ""
        }
    }

    /**
     * Get combined ID for the full path (e.g., "70s_summer")
     */
    fun getCombinedId(): String = nodes.joinToString("_") { it.id }
}

/**
 * Spotify-style hierarchical filter component for KMM
 *
 * Features:
 * - Hierarchical navigation through filter tree
 * - Clear button to reset selection
 * - Combined display of selected path with visual separator
 * - Reusable across different screens with different filter trees
 *
 * @param filterTree The root nodes of the filter hierarchy
 * @param selectedPath The current filter selection path
 * @param onSelectionChanged Callback when filter selection changes
 * @param modifier Modifier for the component
 */
@Composable
fun HierarchicalFilter(
    filterTree: List<FilterNode>,
    selectedPath: FilterPath,
    onSelectionChanged: (FilterPath) -> Unit,
    modifier: Modifier = Modifier
) {
    // Current level being displayed (root or children of selected parent)
    val currentLevel = remember(selectedPath) {
        if (selectedPath.isEmpty) {
            filterTree
        } else {
            // Show children of the last selected node if we haven't completed the selection
            if (selectedPath.nodes.size == 1) {
                selectedPath.nodes.lastOrNull()?.children ?: emptyList()
            } else {
                emptyList()
            }
        }
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Clear button - always show first
        item {
            FilterClearButton(
                onClick = { onSelectionChanged(FilterPath()) }
            )
        }

        // Show either the combined selection or individual options
        when {
            // Complete selection (2 levels) - show as single combined chip
            selectedPath.nodes.size >= 2 -> {
                item {
                    CombinedSelectionChip(
                        path = selectedPath,
                        onClick = {
                            // Navigate back one level
                            val newPath = FilterPath(selectedPath.nodes.dropLast(1))
                            onSelectionChanged(newPath)
                        }
                    )
                }
            }

            // Partial selection (1 level) - show selected + options, with selected highlighted
            selectedPath.nodes.size == 1 -> {
                val selectedNode = selectedPath.nodes.first()

                // Show selected node as highlighted
                item {
                    FilterOptionChip(
                        node = selectedNode,
                        isSelected = true,
                        onClick = {
                            // Navigate back to root
                            onSelectionChanged(FilterPath())
                        }
                    )
                }

                // Show child options
                items(currentLevel) { node ->
                    FilterOptionChip(
                        node = node,
                        isSelected = false,
                        onClick = {
                            // Add this node to the path
                            val newPath = FilterPath(selectedPath.nodes + node)
                            onSelectionChanged(newPath)
                        }
                    )
                }
            }

            // No selection - show root level options
            else -> {
                items(filterTree) { node ->
                    FilterOptionChip(
                        node = node,
                        isSelected = false,
                        onClick = {
                            // Start new path with this node
                            val newPath = FilterPath(listOf(node))
                            onSelectionChanged(newPath)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Clear button (X) for resetting filter selection - styled like other filter chips
 */
@Composable
private fun FilterClearButton(
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                text = "✕",
                style = MaterialTheme.typography.labelMedium
            )
        },
        selected = false,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false
        )
    )
}

/**
 * Combined selection chip that shows visual separator between selections (e.g., "[70s] Spring")
 */
@Composable
private fun CombinedSelectionChip(
    path: FilterPath,
    onClick: () -> Unit
) {
    if (path.nodes.size < 2) return

    val firstNode = path.nodes[0]
    val secondNode = path.nodes[1]

    FilterChip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // First part with brackets (e.g., "[70s]")
                Text(
                    text = "[${firstNode.label}]",
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Second part (e.g., "Spring")
                Text(
                    text = secondNode.label,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        },
        selected = true,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color.Red,
            selectedLabelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = true,
            borderColor = Color.Red,
            selectedBorderColor = Color.Red,
            borderWidth = 1.dp
        )
    )
}

/**
 * Individual filter option chip
 */
@Composable
private fun FilterOptionChip(
    node: FilterNode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = { Text(node.label) },
        selected = isSelected,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = Color.Red,
            selectedLabelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            selectedBorderColor = Color.Red
        )
    )
}

/**
 * Utility functions for building common filter trees for KMM Deadly app
 */
object FilterTrees {

    /**
     * Build Grateful Dead era/tour filter tree for library
     */
    fun buildDeadToursTree(): List<FilterNode> {
        return listOf(
            FilterNode(
                id = "60s",
                label = "60s",
                children = listOf(
                    FilterNode("60s_spring", "Spring"),
                    FilterNode("60s_summer", "Summer"),
                    FilterNode("60s_fall", "Fall"),
                    FilterNode("60s_winter", "Winter")
                )
            ),
            FilterNode(
                id = "70s",
                label = "70s",
                children = listOf(
                    FilterNode("70s_spring", "Spring"),
                    FilterNode("70s_summer", "Summer"),
                    FilterNode("70s_fall", "Fall"),
                    FilterNode("70s_winter", "Winter")
                )
            ),
            FilterNode(
                id = "80s",
                label = "80s",
                children = listOf(
                    FilterNode("80s_spring", "Spring"),
                    FilterNode("80s_summer", "Summer"),
                    FilterNode("80s_fall", "Fall"),
                    FilterNode("80s_winter", "Winter")
                )
            ),
            FilterNode(
                id = "90s",
                label = "90s",
                children = listOf(
                    FilterNode("90s_spring", "Spring"),
                    FilterNode("90s_summer", "Summer"),
                    FilterNode("90s_fall", "Fall"),
                    FilterNode("90s_winter", "Winter")
                )
            )
        )
    }

    /**
     * Build venue/location filter tree for browse
     */
    fun buildVenueTree(): List<FilterNode> {
        return listOf(
            FilterNode(
                id = "west_coast",
                label = "West Coast",
                children = listOf(
                    FilterNode("california", "California"),
                    FilterNode("oregon", "Oregon"),
                    FilterNode("washington", "Washington")
                )
            ),
            FilterNode(
                id = "east_coast",
                label = "East Coast",
                children = listOf(
                    FilterNode("new_york", "New York"),
                    FilterNode("massachusetts", "Massachusetts"),
                    FilterNode("pennsylvania", "Pennsylvania")
                )
            ),
            FilterNode(
                id = "midwest",
                label = "Midwest",
                children = listOf(
                    FilterNode("illinois", "Illinois"),
                    FilterNode("ohio", "Ohio"),
                    FilterNode("michigan", "Michigan")
                )
            )
        )
    }

    /**
     * Build simple home filter tree starting with "All"
     */
    fun buildHomeFiltersTree(): List<FilterNode> {
        return listOf(
            FilterNode(
                id = "all",
                label = "All"
            )
        )
    }
}