package com.grateful.deadly.data.search

import com.grateful.deadly.domain.models.Show
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for search-related data operations.
 * Abstracts the data layer from the service layer.
 */
interface SearchRepository {

    /**
     * Search shows by query string
     */
    suspend fun searchShows(query: String): List<Show>

    /**
     * Get show by ID
     */
    suspend fun getShowById(id: String): Show?

    /**
     * Get all shows
     */
    suspend fun getAllShows(): List<Show>

    /**
     * Get all shows as reactive Flow
     */
    fun getAllShowsFlow(): Flow<List<Show>>

    /**
     * Get total number of shows in database
     */
    suspend fun getShowCount(): Long
}