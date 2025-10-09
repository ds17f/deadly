import Foundation
import SwiftUI
import ComposeApp
import Combine

/**
 * Bridges KMM PlayerViewModel to SwiftUI.
 *
 * Observes Kotlin StateFlow and converts to SwiftUI @Published properties.
 * Provides action methods that delegate to the shared ViewModel.
 */
class PlayerViewModelWrapper: ObservableObject {
    private let viewModel: PlayerViewModel
    private var cancellables = Set<AnyCancellable>()

    // Published playback state
    @Published var currentTrack: Track?
    @Published var currentRecordingId: String?
    @Published var showDate: String?
    @Published var venue: String?
    @Published var location: String?
    @Published var isPlaying: Bool = false
    @Published var currentPositionMs: Int64 = 0
    @Published var durationMs: Int64 = 0
    @Published var isLoading: Bool = false
    @Published var isBuffering: Bool = false
    @Published var error: String?
    @Published var hasNext: Bool = false
    @Published var hasPrevious: Bool = false
    @Published var playlistPosition: Int32 = 0
    @Published var playlistSize: Int32 = 0

    // Navigation info
    @Published var showId: String?
    @Published var recordingIdForNav: String?

    // Computed properties
    var progress: Float {
        guard durationMs > 0 else { return 0 }
        return Float(currentPositionMs) / Float(durationMs)
    }

    var formattedPosition: String {
        formatTime(milliseconds: currentPositionMs)
    }

    var formattedDuration: String {
        formatTime(milliseconds: durationMs)
    }

    init(mediaService: MediaService) {
        self.viewModel = PlayerViewModel(mediaService: mediaService)

        // Observe uiState StateFlow
        viewModel.uiState.watch { [weak self] uiState in
            guard let self = self else { return }
            guard let state = uiState as? PlayerUiState else { return }

            DispatchQueue.main.async {
                let playbackState = state.playbackState
                self.currentTrack = playbackState.currentTrack
                self.currentRecordingId = playbackState.currentRecordingId
                self.showDate = playbackState.showDate
                self.venue = playbackState.venue
                self.location = playbackState.location
                self.isPlaying = playbackState.isPlaying
                self.currentPositionMs = playbackState.currentPositionMs
                self.durationMs = playbackState.durationMs
                self.isLoading = playbackState.isLoading
                self.isBuffering = playbackState.isBuffering
                self.error = playbackState.error
                self.hasNext = playbackState.hasNext
                self.hasPrevious = playbackState.hasPrevious
                self.playlistPosition = playbackState.playlistPosition
                self.playlistSize = playbackState.playlistSize

                // Navigation info
                self.showId = state.navigationInfo.showId
                self.recordingIdForNav = state.navigationInfo.recordingId
            }
        }
    }

    // MARK: - Actions

    func togglePlayPause() {
        Task {
            try? await viewModel.togglePlayPause()
        }
    }

    func nextTrack() {
        Task {
            try? await viewModel.nextTrack()
        }
    }

    func previousTrack() {
        Task {
            try? await viewModel.previousTrack()
        }
    }

    func seekTo(positionMs: Int64) {
        Task {
            try? await viewModel.seekTo(positionMs: positionMs)
        }
    }

    // MARK: - Helpers

    private func formatTime(milliseconds: Int64) -> String {
        let totalSeconds = Int(milliseconds / 1000)
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}
