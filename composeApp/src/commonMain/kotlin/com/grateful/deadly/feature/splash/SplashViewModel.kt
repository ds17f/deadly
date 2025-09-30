package com.grateful.deadly.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grateful.deadly.feature.splash.model.SplashUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Splash screen following V2 pattern.
 *
 * Delegates all business logic to SplashService (Universal Service).
 * Provides clean interface between UI and service layer.
 */
class SplashViewModel(
    private val splashService: SplashService
) : ViewModel() {

    val uiState: StateFlow<SplashUiState> = splashService.uiState

    init {
        // Start initialization on ViewModel creation
        viewModelScope.launch {
            splashService.initializeData()
        }
    }

    /**
     * Retry initialization after error.
     */
    fun retry() {
        viewModelScope.launch {
            splashService.retry()
        }
    }

    /**
     * Skip initialization and proceed to app.
     */
    fun skip() {
        splashService.skip()
    }
}