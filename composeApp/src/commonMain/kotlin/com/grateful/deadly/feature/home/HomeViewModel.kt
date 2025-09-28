package com.grateful.deadly.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grateful.deadly.core.util.Logger
import com.grateful.deadly.domain.home.HomeService
import com.grateful.deadly.domain.home.HomeContent
import com.grateful.deadly.navigation.AppScreen
import com.grateful.deadly.navigation.NavigationEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * HomeViewModel - State management for rich Home screen
 *
 * Following V2 architecture patterns:
 * - Single HomeService dependency for data orchestration
 * - StateFlow for reactive UI updates
 * - Clean separation between UI state and service state
 * - NavigationEvent flow for platform-agnostic navigation
 */
class HomeViewModel(
    private val homeService: HomeService
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState.initial())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Navigation event flow for reactive navigation
    private val _navigation = MutableSharedFlow<NavigationEvent>()
    val navigation: SharedFlow<NavigationEvent> = _navigation

    init {
        Logger.d(TAG, "HomeViewModel initialized")
        observeHomeService()
    }

    /**
     * Observe HomeService content and update UI state
     */
    private fun observeHomeService() {
        viewModelScope.launch {
            homeService.homeContent.collect { homeContent ->
                Logger.d(TAG, "HomeService content updated: ${homeContent.recentShows.size} recent, " +
                        "${homeContent.todayInHistory.size} history, ${homeContent.featuredCollections.size} collections")

                _uiState.value = _uiState.value.copy(
                    homeContent = homeContent,
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    /**
     * Refresh all home content
     */
    fun refresh() {
        Logger.d(TAG, "refresh() called")
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val result = homeService.refreshAll()
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to refresh content"
                    )
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to refresh", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to refresh content"
                )
            }
        }
    }

    /**
     * Navigation methods for platform-agnostic navigation
     */
    suspend fun onNavigateToShow(showId: String) {
        Logger.d(TAG, "Navigate to show: $showId")
        _navigation.emit(NavigationEvent(AppScreen.ShowDetail(showId)))
    }

    suspend fun onNavigateToPlayer(recordingId: String) {
        Logger.d(TAG, "Navigate to player: $recordingId")
        // TODO: Add recording context to player navigation
        _navigation.emit(NavigationEvent(AppScreen.Player))
    }

    suspend fun onNavigateToCollection(collectionId: String) {
        Logger.d(TAG, "Navigate to collection: $collectionId")
        // TODO: Add AppScreen.CollectionDetail when implemented
        Logger.w(TAG, "Collection detail screen not yet implemented")
    }

    suspend fun onNavigateToSearch() {
        Logger.d(TAG, "Navigate to search")
        _navigation.emit(NavigationEvent(AppScreen.Search))
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        Logger.d(TAG, "HomeViewModel cleared")
    }
}

/**
 * UI State for HomeScreen
 *
 * Wraps HomeService content with additional UI concerns
 */
data class HomeUiState(
    val homeContent: HomeContent,
    val isLoading: Boolean,
    val error: String?
) {
    companion object {
        fun initial() = HomeUiState(
            homeContent = HomeContent.initial(),
            isLoading = true,
            error = null
        )
    }

    val hasError: Boolean get() = error != null
}