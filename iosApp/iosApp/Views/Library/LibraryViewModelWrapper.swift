//
//  LibraryViewModelWrapper.swift
//  iosApp
//
//  Bridges KMM LibraryViewModel to SwiftUI
//

import Foundation
import SwiftUI
import ComposeApp

/// SwiftUI wrapper for KMM LibraryViewModel
/// Bridges Kotlin StateFlow to SwiftUI @Published properties
class LibraryViewModelWrapper: ObservableObject {
    private let viewModel: LibraryViewModel

    // Published UI State
    @Published var isLoading: Bool = false
    @Published var error: String? = nil
    @Published var shows: [LibraryShowViewModel] = []
    @Published var stats: LibraryStats = LibraryStats(totalShows: 0, totalPinned: 0, totalDownloaded: 0, totalStorageUsed: 0)
    @Published var selectedSortOption: LibrarySortOption = .dateAdded
    @Published var selectedSortDirection: LibrarySortDirection = .descending
    @Published var displayMode: LibraryDisplayMode = .list

    init(libraryViewModel: LibraryViewModel) {
        self.viewModel = libraryViewModel

        // Observe uiState StateFlow
        viewModel.uiState.watch { [weak self] uiState in
            guard let self = self else { return }
            guard let state = uiState as? LibraryUiState else { return }

            DispatchQueue.main.async {
                self.isLoading = state.isLoading
                self.error = state.error
                self.shows = state.shows
                self.stats = state.stats
                self.selectedSortOption = state.selectedSortOption
                self.selectedSortDirection = state.selectedSortDirection
                self.displayMode = state.displayMode
            }
        }
    }

    // MARK: - ViewModel Methods

    func refreshLibrary() {
        viewModel.refreshLibrary()
    }

    func populateTestData() {
        viewModel.populateTestData()
    }

    func removeFromLibrary(showId: String) {
        viewModel.removeFromLibrary(showId: showId)
    }

    func pinShow(showId: String) {
        viewModel.pinShow(showId: showId)
    }

    func unpinShow(showId: String) {
        viewModel.unpinShow(showId: showId)
    }

    func updateSortOption(_ sortOption: LibrarySortOption) {
        viewModel.updateSortOption(sortOption: sortOption)
    }

    func updateSortDirection(_ sortDirection: LibrarySortDirection) {
        viewModel.updateSortDirection(sortDirection: sortDirection)
    }

    func toggleDisplayMode() {
        viewModel.toggleDisplayMode()
    }

    func setDisplayMode(_ displayMode: LibraryDisplayMode) {
        viewModel.setDisplayMode(displayMode: displayMode)
    }
}
