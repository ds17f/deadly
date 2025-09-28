package com.grateful.deadly.domain.models

import kotlinx.serialization.Serializable

/**
 * Collection domain model - represents a curated collection of Grateful Dead shows.
 *
 * Collections are thematic groupings like "Europe '72", "Best of 1977", etc.
 * Used for home screen featured collections and browse organization.
 */
@Serializable
data class Collection(
    val id: String,
    val name: String,
    val description: String?,
    val showIds: List<String>,
    val showCount: Int,
    val coverImageUrl: String?,
    val isFeatured: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
) {
    /**
     * Display text for show count
     */
    val showCountText: String
        get() = when (showCount) {
            0 -> "No shows"
            1 -> "1 show"
            else -> "$showCount shows"
        }

    /**
     * Whether this collection has shows
     */
    val hasShows: Boolean
        get() = showCount > 0

    /**
     * Short description for UI display
     */
    val displayDescription: String
        get() = description?.take(100)?.let {
            if (description.length > 100) "$it..." else it
        } ?: "Collection of $showCountText"
}