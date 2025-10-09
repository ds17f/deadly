import Foundation
import SwiftUI
import ComposeApp
import Combine

/**
 * SwiftUI-compatible wrapper for KMM HomeViewModel.
 *
 * Converts Kotlin StateFlow to SwiftUI @Published properties,
 * making the shared ViewModel reactive in SwiftUI.
 *
 * Observes HomeUiState which contains:
 * - homeContent (recentShows, todayInHistory, featuredCollections)
 * - isLoading
 * - error
 */
class HomeViewModelWrapper: ObservableObject {
    private let viewModel: HomeViewModel
    private var cancellables = Set<AnyCancellable>()

    // Published state that SwiftUI can observe
    @Published var recentShows: [Show_] = []
    @Published var todayInHistory: [Show_] = []
    @Published var featuredCollections: [Collection] = []
    @Published var isLoading: Bool = true
    @Published var error: String?
    @Published var hasContent: Bool = false
    @Published var lastRefresh: Int64 = 0

    init(homeService: HomeService) {
        // Create KMM ViewModel
        self.viewModel = HomeViewModel(homeService: homeService)

        // Observe uiState StateFlow and update @Published properties
        viewModel.uiState.watch { [weak self] uiState in
            guard let self = self else { return }
            guard let state = uiState as? HomeUiState else { return }

            DispatchQueue.main.async {
                let homeContent = state.homeContent
                self.recentShows = homeContent.recentShows
                self.todayInHistory = homeContent.todayInHistory
                self.featuredCollections = homeContent.featuredCollections
                self.isLoading = state.isLoading
                self.error = state.error
                self.hasContent = homeContent.hasContent
                self.lastRefresh = homeContent.lastRefresh
            }
        }
    }

    // MARK: - Actions

    func refresh() {
        viewModel.refresh()
    }

    func clearError() {
        viewModel.clearError()
    }

    func navigateToShow(showId: String, recordingId: String? = nil) {
        Task {
            try? await viewModel.onNavigateToShow(showId: showId, recordingId: recordingId)
        }
    }

    func navigateToCollection(collectionId: String) {
        Task {
            try? await viewModel.onNavigateToCollection(collectionId: collectionId)
        }
    }

    func navigateToSearch() {
        Task {
            try? await viewModel.onNavigateToSearch()
        }
    }
}
