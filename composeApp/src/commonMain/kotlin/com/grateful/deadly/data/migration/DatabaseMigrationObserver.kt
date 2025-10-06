package com.grateful.deadly.data.migration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observable database migration state for UI feedback.
 *
 * Provides real-time migration progress to the splash screen so users
 * can see their data is being safely migrated.
 */
object DatabaseMigrationObserver {

    private val _migrationState = MutableStateFlow<MigrationState>(MigrationState.Idle)
    val migrationState: StateFlow<MigrationState> = _migrationState.asStateFlow()

    /**
     * Called when migration starts
     */
    fun onMigrationStart(oldVersion: Long, newVersion: Long) {
        _migrationState.value = MigrationState.InProgress(
            fromVersion = oldVersion,
            toVersion = newVersion,
            message = "Upgrading database from v$oldVersion to v$newVersion..."
        )
    }

    /**
     * Called when migration completes successfully
     */
    fun onMigrationSuccess(oldVersion: Long, newVersion: Long) {
        _migrationState.value = MigrationState.Success(
            fromVersion = oldVersion,
            toVersion = newVersion,
            message = "Database upgraded successfully from v$oldVersion to v$newVersion"
        )
    }

    /**
     * Called when migration fails
     */
    fun onMigrationError(oldVersion: Long, newVersion: Long, error: Throwable) {
        _migrationState.value = MigrationState.Error(
            fromVersion = oldVersion,
            toVersion = newVersion,
            message = "Migration failed: ${error.message}",
            error = error
        )
    }

    /**
     * Reset migration state (called after splash screen completes)
     */
    fun reset() {
        _migrationState.value = MigrationState.Idle
    }
}

/**
 * Migration state for UI display
 */
sealed class MigrationState {
    /**
     * No migration in progress
     */
    object Idle : MigrationState()

    /**
     * Migration in progress
     */
    data class InProgress(
        val fromVersion: Long,
        val toVersion: Long,
        val message: String
    ) : MigrationState()

    /**
     * Migration completed successfully
     */
    data class Success(
        val fromVersion: Long,
        val toVersion: Long,
        val message: String
    ) : MigrationState()

    /**
     * Migration failed
     */
    data class Error(
        val fromVersion: Long,
        val toVersion: Long,
        val message: String,
        val error: Throwable
    ) : MigrationState()
}
