package com.grateful.deadly.services.recording

import com.grateful.deadly.domain.models.Recording
import com.grateful.deadly.domain.models.RecordingOptionsResult
import com.grateful.deadly.domain.models.RecordingOptionViewModel
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.services.show.ShowService

/**
 * RecordingSelectionService - Universal business logic for recording selection
 *
 * Follows the Universal Platform Pattern and V2's PlaylistService architecture:
 * - Handles recording selection business logic in commonMain
 * - Converts domain Recording → RecordingOptionViewModel for UI
 * - Manages selection state (session-only like V2)
 * - Delegates data operations to existing ShowService
 *
 * This service focuses on recording selection concerns while ShowService handles data access.
 */
class RecordingSelectionService(
    private val showService: ShowService
) {

    // Session-only state (like V2's PlaylistService)
    private var currentShowId: String? = null
    private var currentRecordingId: String? = null
    private var selectedRecordingId: String? = null // Track user selection

    /**
     * Get recording options for a show with UI-friendly formatting
     *
     * Loads all recordings for the show and converts them to ViewModels with:
     * - Current/selected state
     * - Recommended indicator
     * - Match reasons (why this recording is good)
     * - UI-friendly formatting
     */
    suspend fun getRecordingOptions(showId: String, currentRecordingId: String? = null): RecordingOptionsResult {
        return try {
            println("🔴 RecordingSelectionService.getRecordingOptions called with showId: $showId")
            // Update session state
            this.currentShowId = showId
            this.currentRecordingId = currentRecordingId
            this.selectedRecordingId = null // Reset selection when opening modal (V2 pattern)

            // Load show and recordings from ShowService
            val show = showService.getShowById(showId)
                ?: return RecordingOptionsResult.Error("Show not found")
            println("🔴 RecordingSelectionService loaded show: ${show.displayTitle}")

            val recordings = showService.getRecordingsForShow(showId)
            println("🔴 RecordingSelectionService loaded ${recordings.size} recordings for showId: $showId")
            if (recordings.isEmpty()) {
                return RecordingOptionsResult.Error("No recordings found for this show")
            }

            // Determine current recording (fallback to best recording)
            val activeRecordingId = currentRecordingId ?: show.bestRecordingId ?: recordings.first().identifier

            // Convert to ViewModels with UI state
            val recordingViewModels = recordings.map { recording ->
                convertRecordingToViewModel(recording, show, activeRecordingId)
            }

            // Split into current vs alternatives
            val currentRecording = recordingViewModels.find { it.identifier == activeRecordingId }
            val alternativeRecordings = recordingViewModels.filter { it.identifier != activeRecordingId }

            RecordingOptionsResult.Success(
                currentRecording = currentRecording,
                alternativeRecordings = alternativeRecordings,
                hasRecommended = show.bestRecordingId != null
            )

        } catch (e: Exception) {
            RecordingOptionsResult.Error("Failed to load recording options: ${e.message}")
        }
    }

    /**
     * Convert Recording domain model to UI ViewModel
     *
     * Follows V2's conversion pattern with UI-specific formatting and state.
     */
    private fun convertRecordingToViewModel(
        recording: Recording,
        show: Show,
        currentRecordingId: String
    ): RecordingOptionViewModel {
        val isCurrent = recording.identifier == currentRecordingId
        val isRecommended = recording.identifier == show.bestRecordingId

        // Generate match reason (V2 pattern)
        val matchReason = when {
            isRecommended -> "Recommended"
            recording.sourceType.displayName == "SBD" -> "Soundboard Quality"
            recording.rating > 4.0 -> "Highly Rated"
            recording.reviewCount > 10 -> "Popular Choice"
            else -> null
        }

        // Format taper info
        val taperInfo = recording.taper?.let { "Taper: $it" }

        // Format technical details
        val technicalDetails = listOfNotNull(
            recording.source,
            recording.lineage
        ).joinToString(", ").takeIf { it.isNotEmpty() }

        // Determine selection state - only use selectedRecordingId (V2 pattern: preview separate from current)
        // When modal first opens, selectedRecordingId is null, so nothing is selected (just current is highlighted)
        val isSelected = selectedRecordingId == recording.identifier

        return RecordingOptionViewModel(
            identifier = recording.identifier,
            sourceType = recording.sourceType.displayName,
            taperInfo = taperInfo,
            technicalDetails = technicalDetails,
            rating = if (recording.hasRating) recording.rating.toFloat() else null,
            reviewCount = if (recording.reviewCount > 0) recording.reviewCount else null,
            rawSource = recording.source,
            rawLineage = recording.lineage,
            isSelected = isSelected,
            isCurrent = isCurrent,
            isRecommended = isRecommended,
            matchReason = matchReason
        )
    }

    /**
     * Select a recording (session-only like V2)
     *
     * Updates the selected recording ID for this session.
     * This is separate from currentRecordingId until "Set as Default" is called.
     */
    fun selectRecording(recordingId: String) {
        selectedRecordingId = recordingId
        // Note: Don't update currentRecordingId until setRecordingAsDefault is called
        // This matches V2's pattern of temporary selection vs permanent change
    }

    /**
     * Set recording as default
     *
     * Persists the user's preference to settings.
     * Makes the temporary selection permanent.
     */
    suspend fun setRecordingAsDefault(recordingId: String) {
        val showId = currentShowId ?: return
        selectedRecordingId = recordingId
        currentRecordingId = recordingId // Make selection permanent

        // Persist user's default recording preference via ShowService
        showService.setUserRecordingPreference(showId, recordingId)
    }

    /**
     * Reset to recommended recording
     *
     * Clears user's saved preference and switches back to the show's best recording.
     * Makes the change permanent (inverse of setRecordingAsDefault).
     */
    suspend fun resetToRecommended() {
        val showId = currentShowId ?: return
        val show = showService.getShowById(showId) ?: return
        val recommendedId = show.bestRecordingId ?: return

        selectedRecordingId = recommendedId
        currentRecordingId = recommendedId // Make selection permanent

        // Clear user's saved preference (inverse of setRecordingAsDefault)
        showService.clearUserRecordingPreference(showId)
    }

    /**
     * Get currently selected recording ID
     */
    fun getCurrentRecordingId(): String? = currentRecordingId

    /**
     * Clear temporary selection (used when dismissing modal without saving)
     */
    fun clearTemporarySelection() {
        selectedRecordingId = null
    }

    /**
     * Clear all selection state
     */
    fun clearSelection() {
        currentShowId = null
        currentRecordingId = null
        selectedRecordingId = null
    }
}