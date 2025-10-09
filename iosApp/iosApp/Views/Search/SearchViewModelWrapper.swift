//
//  SearchViewModelWrapper.swift
//  iosApp
//
//  Bridges KMM SearchViewModel to SwiftUI
//

import Foundation
import SwiftUI
import ComposeApp

/// SwiftUI wrapper for KMM SearchViewModel
/// Bridges Kotlin StateFlow to SwiftUI @Published properties
class SearchViewModelWrapper: ObservableObject {
    private let viewModel: SearchViewModel

    // Published UI State
    @Published var searchQuery: String = ""
    @Published var searchResults: [SearchResultShow] = []
    @Published var searchStatus: SearchStatus = .idle
    @Published var recentSearches: [RecentSearch] = []
    @Published var suggestedSearches: [SuggestedSearch] = []
    @Published var searchStats: SearchStats = SearchStats(totalResults: 0, searchDuration: 0, appliedFilters: [])
    @Published var isLoading: Bool = false
    @Published var error: String? = nil

    init(searchViewModel: SearchViewModel) {
        self.viewModel = searchViewModel

        // Observe uiState StateFlow
        viewModel.uiState.watch { [weak self] uiState in
            guard let self = self else { return }
            guard let state = uiState as? SearchUiState else { return }

            DispatchQueue.main.async {
                self.searchQuery = state.searchQuery
                self.searchResults = state.searchResults
                self.searchStatus = state.searchStatus
                self.recentSearches = state.recentSearches
                self.suggestedSearches = state.suggestedSearches
                self.searchStats = state.searchStats
                self.isLoading = state.isLoading
                self.error = state.error
            }
        }
    }

    // MARK: - ViewModel Methods

    func onSearchQueryChanged(_ query: String) {
        viewModel.onSearchQueryChanged(query: query)
    }

    func onRecentSearchSelected(_ recentSearch: RecentSearch) {
        viewModel.onRecentSearchSelected(recentSearch: recentSearch)
    }

    func onSuggestionSelected(_ suggestion: SuggestedSearch) {
        viewModel.onSuggestionSelected(suggestion: suggestion)
    }

    func onClearSearch() {
        viewModel.onClearSearch()
    }

    func onClearRecentSearches() {
        viewModel.onClearRecentSearches()
    }
}
