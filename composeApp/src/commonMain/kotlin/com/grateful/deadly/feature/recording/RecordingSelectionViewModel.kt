package com.grateful.deadly.feature.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grateful.deadly.domain.models.RecordingOptionsResult
import com.grateful.deadly.domain.models.RecordingSelectionAction
import com.grateful.deadly.domain.models.RecordingSelectionState
import com.grateful.deadly.services.recording.RecordingSelectionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * RecordingSelectionViewModel - ViewModel for recording selection modal
 *
 * Follows V2's MVVM pattern with clear separation of concerns:
 * - UI state management with StateFlow
 * - Business logic delegation to RecordingSelectionService
 * - Action-based state updates for testability
 */
class RecordingSelectionViewModel(
    private val recordingSelectionService: RecordingSelectionService
) : ViewModel() {

    private val _state = MutableStateFlow(RecordingSelectionState())
    val state: StateFlow<RecordingSelectionState> = _state.asStateFlow()

    // Track current show ID for operations
    private var currentShowId: String? = null

    /**
     * Shows the recording selection modal for the given show
     */
    fun showRecordingSelection(showId: String, showTitle: String, showDate: String) {
        currentShowId = showId
        _state.value = _state.value.copy(
            isVisible = true,
            showTitle = showTitle,
            showDate = showDate,
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            when (val result = recordingSelectionService.getRecordingOptions(showId)) {
                is RecordingOptionsResult.Success -> {
                    _state.value = _state.value.copy(
                        currentRecording = result.currentRecording,
                        alternativeRecordings = result.alternativeRecordings,
                        hasRecommended = result.hasRecommended,
                        isLoading = false,
                        errorMessage = null
                    )
                }

                is RecordingOptionsResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }

                is RecordingOptionsResult.Loading -> {
                    // Already in loading state
                }
            }
        }
    }

    /**
     * Handles user actions in the recording selection modal
     */
    fun handleAction(action: RecordingSelectionAction) {
        when (action) {
            is RecordingSelectionAction.SelectRecording -> {
                selectRecording(action.recordingId)
            }

            is RecordingSelectionAction.SetAsDefault -> {
                setRecordingAsDefault(action.recordingId)
            }

            is RecordingSelectionAction.ResetToRecommended -> {
                resetToRecommended()
            }

            is RecordingSelectionAction.DismissSelection -> {
                dismissSelection()
            }
        }
    }

    /**
     * Dismisses the recording selection modal
     */
    fun dismissSelection() {
        _state.value = RecordingSelectionState()
    }

    private fun selectRecording(recordingId: String) {
        val currentState = _state.value

        // Update selection state immediately (V2 pattern)
        val updatedCurrentRecording = currentState.currentRecording?.copy(
            isSelected = currentState.currentRecording.identifier == recordingId
        )

        val updatedAlternatives = currentState.alternativeRecordings.map { recording ->
            recording.copy(isSelected = recording.identifier == recordingId)
        }

        _state.value = currentState.copy(
            currentRecording = updatedCurrentRecording,
            alternativeRecordings = updatedAlternatives
        )

        // Also update service state for persistence
        viewModelScope.launch {
            try {
                recordingSelectionService.selectRecording(recordingId)
            } catch (e: Exception) {
                // Log error but don't revert UI state - user should see selection
                println("Error selecting recording in service: ${e.message}")
            }
        }
    }

    private fun setRecordingAsDefault(recordingId: String) {
        viewModelScope.launch {
            try {
                recordingSelectionService.setRecordingAsDefault(recordingId)
                // Refresh the recording options to reflect the change
                val currentState = _state.value
                refreshRecordingOptions()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = "Failed to set recording as default: ${e.message}"
                )
            }
        }
    }

    private fun resetToRecommended() {
        viewModelScope.launch {
            try {
                recordingSelectionService.resetToRecommended()
                // Refresh the recording options to reflect the change
                refreshRecordingOptions()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = "Failed to reset to recommended: ${e.message}"
                )
            }
        }
    }

    private suspend fun refreshRecordingOptions() {
        val showId = currentShowId ?: return
        when (val result = recordingSelectionService.getRecordingOptions(showId)) {
            is RecordingOptionsResult.Success -> {
                _state.value = _state.value.copy(
                    currentRecording = result.currentRecording,
                    alternativeRecordings = result.alternativeRecordings,
                    hasRecommended = result.hasRecommended,
                    errorMessage = null
                )
            }

            is RecordingOptionsResult.Error -> {
                _state.value = _state.value.copy(
                    errorMessage = result.message
                )
            }

            is RecordingOptionsResult.Loading -> {
                // Keep current state
            }
        }
    }

    /**
     * Clears any error state
     */
    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    /**
     * Returns whether the modal is currently visible
     */
    val isVisible: Boolean
        get() = _state.value.isVisible

    /**
     * Returns the current selected recording ID, if any
     */
    val selectedRecordingId: String?
        get() = _state.value.selectedRecording?.identifier
}