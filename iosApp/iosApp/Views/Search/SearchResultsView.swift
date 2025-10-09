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

    private let DeadRed = Color(red: 0xDC/255.0, green: 0x14/255.0, blue: 0x3C/255.0)

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    // Date
                    Text(formatDate(result.show.date))
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.primary)

                    // Venue
                    Text(result.show.venue.name)
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)

                    // Location
                    Text(result.show.location.displayText)
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)

                    // Match type indicator
                    Text(result.matchType.displayName)
                        .font(.system(size: 12))
                        .foregroundColor(DeadRed)
                        .padding(.top, 4)
                }

                Spacer()

                // Has downloads indicator
                if result.hasDownloads {
                    Image(systemName: "arrow.down.circle.fill")
                        .foregroundColor(DeadRed)
                        .font(.system(size: 20))
                }
            }
        }
        .padding(16)
        .background(Color(.systemGray6))
        .cornerRadius(8)
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
    }

    private func formatDate(_ dateString: String) -> String {
        // Format: "1977-05-08" -> "May 8, 1977"
        let components = dateString.split(separator: "-")
        guard components.count == 3,
              let year = components.first,
              let month = Int(components[1]),
              let day = Int(components[2]) else {
            return dateString
        }

        let monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                          "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
        let monthName = month > 0 && month <= 12 ? monthNames[month - 1] : ""

        return "\(monthName) \(day), \(year)"
    }
}
