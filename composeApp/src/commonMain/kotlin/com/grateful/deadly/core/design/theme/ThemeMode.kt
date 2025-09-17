package com.grateful.deadly.core.design.theme

import kotlinx.serialization.Serializable

/**
 * Enum representing theme mode options
 */
@Serializable
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    val displayName: String
        get() = when(this) {
            LIGHT -> "Light"
            DARK -> "Dark"
            SYSTEM -> "System Default"
        }
}