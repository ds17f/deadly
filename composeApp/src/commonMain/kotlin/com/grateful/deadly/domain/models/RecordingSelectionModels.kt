package com.grateful.deadly.domain.models

import kotlinx.serialization.Serializable

/**
 * RecordingSelection - UI state container for recording selection modal
 *
 * Follows the Universal Platform Pattern: contains UI state logic in commonMain,
 * while actual data loading is delegated to platform-specific repositories.
 */
@Serializable
data class RecordingSelectionState(
    val isVisible: Boolean = false,
    val showTitle: String = "",
    val showDate: String = "",
    val currentRecording: RecordingOptionViewModel? = null,
    val alternativeRecordings: List<RecordingOptionViewModel> = emptyList(),
    val hasRecommended: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * All recordings (current + alternatives) for easy iteration
     */
    val allRecordings: List<RecordingOptionViewModel>
        get() = listOfNotNull(currentRecording) + alternativeRecordings

    /**
     * Whether any recording is selected that's different from current
     */
    val hasNewSelection: Boolean
        get() = allRecordings.any { it.isSelected && !it.isCurrent }

    /**
     * The selected recording (either current or newly selected)
     */
    val selectedRecording: RecordingOptionViewModel?
        get() = allRecordings.find { it.isSelected }

    /**
     * Whether the current recording is the recommended one
     */
    val isCurrentRecommended: Boolean
        get() = currentRecording?.isRecommended == true

    /**
     * Whether reset to recommended should be shown
     */
    val shouldShowResetToRecommended: Boolean
        get() = hasRecommended && !isCurrentRecommended

    /**
     * Whether set as default button should be shown
     */
    val shouldShowSetAsDefault: Boolean
        get() = hasNewSelection
}

/**
 * RecordingOptionViewModel - UI-specific view model for individual recording display
 *
 * Flattens Recording domain model into UI-friendly properties with display formatting.
 * Follows V2 pattern with clear separation of domain data and UI presentation.
 */
@Serializable
data class RecordingOptionViewModel(
    val identifier: String,              // Archive.org identifier
    val sourceType: String,              // "SBD", "AUD", "FM", etc.
    val taperInfo: String?,              // "Taper: Betty Cantor" (formatted)
    val technicalDetails: String?,       // "Soundboard, DAT → CD → FLAC" (formatted)
    val rating: Float?,                  // 0.0-5.0 or null if no rating
    val reviewCount: Int?,               // Number of reviews or null if none
    val rawSource: String?,              // Raw source string
    val rawLineage: String?,             // Raw lineage string
    val isSelected: Boolean,             // Currently selected in modal
    val isCurrent: Boolean,              // The recording currently being used
    val isRecommended: Boolean,          // This is the show's best recording
    val matchReason: String?             // "Recommended", "Soundboard Quality", etc.
) {
    /**
     * Display title for the recording
     */
    val displayTitle: String
        get() = sourceType

    /**
     * Display rating with format "4.2★ (15)"
     */
    val displayRating: String?
        get() = if (rating != null && reviewCount != null && rating > 0f) {
            val formattedRating = (kotlin.math.round(rating * 10) / 10.0).toString()
            "${formattedRating}★ ($reviewCount)"
        } else null

    /**
     * Whether this recording has a meaningful rating
     */
    val hasRating: Boolean
        get() = rating != null && rating > 0f && reviewCount != null && reviewCount > 0

    /**
     * Formatted identifier for display (shorter version)
     */
    val displayIdentifier: String
        get() = identifier.takeLastWhile { it != '.' }.takeIf { it != identifier } ?: identifier
}

/**
 * RecordingOptionsResult - Result of loading recording options
 *
 * Encapsulates the result of fetching recording options for a show,
 * following the Universal Platform Pattern for clean error handling.
 */
sealed class RecordingOptionsResult {
    data class Success(
        val currentRecording: RecordingOptionViewModel?,
        val alternativeRecordings: List<RecordingOptionViewModel>,
        val hasRecommended: Boolean
    ) : RecordingOptionsResult()

    data class Error(val message: String) : RecordingOptionsResult()
    object Loading : RecordingOptionsResult()
}

/**
 * RecordingSelectionAction - Actions that can be performed on recordings
 *
 * Encapsulates the different actions users can take when selecting recordings.
 */
sealed class RecordingSelectionAction {
    data class SelectRecording(val recordingId: String) : RecordingSelectionAction()
    data class SetAsDefault(val recordingId: String) : RecordingSelectionAction()
    object ResetToRecommended : RecordingSelectionAction()
    object DismissSelection : RecordingSelectionAction()
}

/**
 * RecordingMetadata - Extended metadata for recordings
 *
 * Contains additional metadata that might be loaded from Archive.org API
 * when detailed recording information is needed.
 */
@Serializable
data class RecordingMetadata(
    val identifier: String,
    val title: String? = null,
    val description: String? = null,
    val creator: String? = null,
    val date: String? = null,
    val venue: String? = null,
    val collection: String? = null,
    val format: List<String> = emptyList(),
    val trackCount: Int? = null,
    val duration: String? = null,
    val fileSize: Long? = null
)