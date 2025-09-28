package com.grateful.deadly.domain.home

import kotlinx.coroutines.flow.Flow

/**
 * Clean API interface for Home screen operations.
 *
 * Orchestrates data from multiple sources to provide unified home experience:
 * - Recent shows from user activity/player history
 * - Today in Grateful Dead History from date-based queries
 * - Featured collections from curated data
 *
 * Follows established patterns from SearchService with reactive StateFlow for UI updates.
 */
interface HomeService {

    /**
     * Reactive home content state for UI consumption
     * Combines data from recent shows, history, and collections services
     */
    val homeContent: Flow<HomeContent>

    /**
     * Refresh all home content from underlying services
     * @return Result indicating success or failure with error details
     */
    suspend fun refreshAll(): Result<Unit>
}