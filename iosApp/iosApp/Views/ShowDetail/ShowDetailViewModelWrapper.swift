import Foundation
import SwiftUI
import ComposeApp
import Combine

/**
 * SwiftUI-compatible wrapper for KMM ShowDetailViewModel.
 *
 * Converts Kotlin StateFlow to SwiftUI @Published properties,
 * making the shared ViewModel reactive in SwiftUI.
 */
class ShowDetailViewModelWrapper: ObservableObject {
    private let viewModel: ShowDetailViewModel
    private var cancellables = Set<AnyCancellable>()

    // Published state that SwiftUI can observe
    @Published var showData: Show_?
    @Published var tracks: [Track] = []
    @Published var isLoading: Bool = true
    @Published var isTracksLoading: Bool = false
    @Published var error: String?
    @Published var isPlaying: Bool = false
    @Published var isMediaLoading: Bool = false
    @Published var isInLibrary: Bool = false

    init(
        showId: String,
        recordingId: String?,
        showDetailService: ShowDetailService,
        mediaService: MediaService,
        libraryService: LibraryService
    ) {
        // Create KMM ViewModel
        self.viewModel = ShowDetailViewModel(
            showDetailService: showDetailService,
            mediaService: mediaService,
            libraryService: libraryService
        )

        // Observe enhancedUiState StateFlow and update @Published properties
        viewModel.enhancedUiState.watch { [weak self] uiState in
            guard let self = self else { return }
            guard let state = uiState as? ShowDetailUiState else { return }

            DispatchQueue.main.async {
                self.showData = state.showData
                self.tracks = state.tracks
                self.isLoading = state.isLoading
                self.isTracksLoading = state.isTracksLoading
                self.error = state.error
                self.isPlaying = state.isPlaying
                self.isMediaLoading = state.isMediaLoading
                self.isInLibrary = state.showData?.isInLibrary ?? false
            }
        }

        // Trigger initial load
        viewModel.loadShow(showId: showId, recordingId: recordingId)
    }

    // MARK: - Actions

    func playTrack(_ track: Track) {
        viewModel.playTrack(track: track)
    }

    func togglePlayback() {
        viewModel.togglePlayback()
    }

    func toggleLibraryStatus() {
        viewModel.toggleLibraryStatus()
    }

    func navigateToPreviousShow() {
        viewModel.navigateToPreviousShow()
    }

    func navigateToNextShow() {
        viewModel.navigateToNextShow()
    }

    func clearError() {
        viewModel.clearError()
    }

    func reload(showId: String?, recordingId: String?) {
        viewModel.loadShow(showId: showId, recordingId: recordingId)
    }
}
