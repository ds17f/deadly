import SwiftUI
import ComposeApp

/**
 * RecordingSelectionViewModelWrapper - SwiftUI wrapper for KMM RecordingSelectionViewModel
 *
 * Bridges the Kotlin StateFlow-based ViewModel to SwiftUI's @Published properties.
 * Watches the KMM state and syncs it to SwiftUI reactive properties.
 */
class RecordingSelectionViewModelWrapper: ObservableObject {
    private let viewModel: RecordingSelectionViewModel

    // Published properties matching RecordingSelectionState
    @Published var isVisible: Bool = false
    @Published var showTitle: String = ""
    @Published var showDate: String = ""
    @Published var currentRecording: RecordingOptionViewModel? = nil
    @Published var alternativeRecordings: [RecordingOptionViewModel] = []
    @Published var hasRecommended: Bool = false
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil

    // Computed properties from state
    var allRecordings: [RecordingOptionViewModel] {
        var recordings: [RecordingOptionViewModel] = []
        if let current = currentRecording {
            recordings.append(current)
        }
        recordings.append(contentsOf: alternativeRecordings)
        return recordings
    }

    var selectedRecording: RecordingOptionViewModel? {
        allRecordings.first { $0.isSelected }
    }

    var hasNewSelection: Bool {
        allRecordings.contains { $0.isSelected && !$0.isCurrent }
    }

    var isCurrentRecommended: Bool {
        currentRecording?.isRecommended == true
    }

    var shouldShowResetToRecommended: Bool {
        hasRecommended && !isCurrentRecommended
    }

    var shouldShowSetAsDefault: Bool {
        hasNewSelection
    }

    init(recordingSelectionViewModel: RecordingSelectionViewModel) {
        self.viewModel = recordingSelectionViewModel

        // Watch the state flow from KMM
        viewModel.state.watch { [weak self] state in
            guard let self = self else { return }
            guard let recordingState = state as? RecordingSelectionState else { return }

            DispatchQueue.main.async {
                self.isVisible = recordingState.isVisible
                self.showTitle = recordingState.showTitle
                self.showDate = recordingState.showDate
                self.currentRecording = recordingState.currentRecording
                self.alternativeRecordings = recordingState.alternativeRecordings
                self.hasRecommended = recordingState.hasRecommended
                self.isLoading = recordingState.isLoading
                self.errorMessage = recordingState.errorMessage
            }
        }
    }

    // MARK: - Actions

    func showRecordingSelection(
        showId: String,
        showTitle: String,
        showDate: String,
        currentRecordingId: String?
    ) {
        viewModel.showRecordingSelection(
            showId: showId,
            showTitle: showTitle,
            showDate: showDate,
            currentRecordingId: currentRecordingId
        )
    }

    func selectRecording(_ recordingId: String) {
        let action = RecordingSelectionAction.SelectRecording(recordingId: recordingId)
        viewModel.handleAction(action: action)
    }

    func setAsDefault(_ recordingId: String) {
        let action = RecordingSelectionAction.SetAsDefault(recordingId: recordingId)
        viewModel.handleAction(action: action)
    }

    func resetToRecommended() {
        let action = RecordingSelectionAction.ResetToRecommended()
        viewModel.handleAction(action: action)
    }

    func dismissSelection() {
        viewModel.dismissSelection()
    }

    func clearError() {
        viewModel.clearError()
    }
}
