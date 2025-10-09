//
//  SearchResultsView.swift
//  iosApp
//
//  Native SwiftUI full-screen search results matching SearchResultsScreen.kt
//

import SwiftUI
import ComposeApp

struct SearchResultsView: View {
    @ObservedObject var viewModel: SearchViewModelWrapper
    @EnvironmentObject private var coordinator: NavigationCoordinator
    @State private var searchText: String = ""
    @FocusState private var isSearchFocused: Bool

    var body: some View {
        VStack(spacing: 0) {
            // Top bar with back button and search field
            SearchTopBar()

            // Search content
            ScrollView {
                if searchText.isEmpty {
                    // Show recent searches when no query
                    RecentSearchesSection()
                } else {
                    // Show search results
                    SearchResultsSection()
                }
            }
        }
        .navigationBarHidden(true)
        .onAppear {
            isSearchFocused = true
            searchText = viewModel.searchQuery
        }
    }

    // MARK: - Top Bar

    @ViewBuilder
    private func SearchTopBar() -> some View {
        HStack(spacing: 0) {
            // Back button
            Button(action: {
                coordinator.path.removeLast()
            }) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 24))
                    .foregroundColor(.primary)
            }
            .padding(.leading, 8)

            // Search text field
            TextField("What do you want to listen to?", text: $searchText)
                .focused($isSearchFocused)
                .textFieldStyle(PlainTextFieldStyle())
                .font(.system(size: 16))
                .padding(.horizontal, 8)
                .onChange(of: searchText) { oldValue, newValue in
                    viewModel.onSearchQueryChanged(newValue)
                }
                .overlay(
                    HStack {
                        Spacer()
                        if !searchText.isEmpty {
                            Button(action: {
                                searchText = ""
                                viewModel.onClearSearch()
                            }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.secondary)
                            }
                            .padding(.trailing, 8)
                        }
                    }
                )
        }
        .padding(.vertical, 12)
        .background(Color(.systemBackground))
        .shadow(color: Color.black.opacity(0.1), radius: 2, x: 0, y: 2)
    }

    // MARK: - Recent Searches Section

    @ViewBuilder
    private func RecentSearchesSection() -> some View {
        VStack(alignment: .leading, spacing: 12) {
            if !viewModel.recentSearches.isEmpty {
                HStack {
                    Text("Recent Searches")
                        .font(.system(size: 20, weight: .bold))
                        .padding(.horizontal, 16)
                        .padding(.top, 16)

                    Spacer()

                    Button(action: {
                        viewModel.onClearRecentSearches()
                    }) {
                        Text("Clear")
                            .font(.system(size: 14))
                            .foregroundColor(.blue)
                    }
                    .padding(.trailing, 16)
                    .padding(.top, 16)
                }

                ForEach(viewModel.recentSearches, id: \.query) { recentSearch in
                    Button(action: {
                        searchText = recentSearch.query
                        viewModel.onRecentSearchSelected(recentSearch)
                    }) {
                        HStack {
                            Image(systemName: "clock")
                                .foregroundColor(.secondary)

                            Text(recentSearch.query)
                                .foregroundColor(.primary)

                            Spacer()

                            Image(systemName: "arrow.up.left")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                    }
                }
            } else {
                Text("No recent searches")
                    .foregroundColor(.secondary)
                    .padding(16)
            }
        }
    }

    // MARK: - Search Results Section

    @ViewBuilder
    private func SearchResultsSection() -> some View {
        VStack(alignment: .leading, spacing: 8) {
            // Search stats
            if viewModel.searchStatus == .success {
                Text("\(viewModel.searchStats.totalResults) results")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 16)
                    .padding(.top, 16)
            }

            // Loading indicator
            if viewModel.isLoading {
                HStack {
                    Spacer()
                    ProgressView()
                        .padding()
                    Spacer()
                }
            }

            // Results
            if !viewModel.searchResults.isEmpty {
                ForEach(viewModel.searchResults, id: \.show.id) { result in
                    SearchResultCard(result: result)
                        .onTapGesture {
                            coordinator.path.append(NavigationCoordinator.Destination.showDetail(
                                showId: result.show.id,
                                recordingId: nil
                            ))
                        }
                }
            } else if viewModel.searchStatus == .noResults {
                VStack(spacing: 8) {
                    Text("No results found")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.primary)

                    Text("Try a different search term")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 40)
            }
        }
    }
}

// MARK: - Search Result Card

struct SearchResultCard: View {
    let result: SearchResultShow

    var body: some View {
        HStack(spacing: 12) {
            // Album art placeholder (60pt square)
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(.systemGray5))
                    .frame(width: 60, height: 60)

                Image(systemName: "music.note")
                    .font(.system(size: 24))
                    .foregroundColor(.secondary)
            }

            // Text content
            VStack(alignment: .leading, spacing: 2) {
                // Line 1: Date • Location
                Text("\(result.show.date) • \(result.show.location.displayText)")
                    .font(.system(size: 15))
                    .foregroundColor(.primary)
                    .lineLimit(1)

                // Line 2: Venue
                Text(result.show.venue.name)
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }

            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
    }
}
