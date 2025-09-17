package com.grateful.deadly.core.design.theme

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages theme preferences for the application.
 * This is a simplified version that could be enhanced with persistence later.
 */
class ThemeManager {
    private val _currentTheme = MutableStateFlow(ThemeMode.SYSTEM)
    val currentTheme: StateFlow<ThemeMode> = _currentTheme.asStateFlow()

    /**
     * Update the current theme mode
     */
    fun setThemeMode(mode: ThemeMode) {
        _currentTheme.value = mode
    }

    /**
     * Get the current theme mode
     */
    fun getCurrentThemeMode(): ThemeMode {
        return _currentTheme.value
    }
}