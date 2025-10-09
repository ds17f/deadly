//
//  LibraryView.swift
//  iosApp
//
//  Native SwiftUI library screen matching LibraryScreen.kt
//

import SwiftUI
import ComposeApp

struct LibraryView: View {
    @StateObject private var viewModel: LibraryViewModelWrapper
    @EnvironmentObject private var coordinator: NavigationCoordinator

    @State private var showSortSheet = false

    private let DeadRed = Color(red: 0xDC/255.0, green: 0x14/255.0, blue: 0x3C/255.0)

    init(libraryViewModel: LibraryViewModel) {
        _viewModel = StateObject(wrappedValue: LibraryViewModelWrapper(libraryViewModel: libraryViewModel))
    }

    var body: some View {
        VStack(spacing: 0) {
            // Sort and display controls
            SortAndDisplayControls()
                .padding(.horizontal, 16)
                .padding(.vertical, 8)

            // Main content
            if viewModel.isLoading {
                LoadingContent()
            } else if let error = viewModel.error {
                ErrorContent(error: error)
            } else if viewModel.shows.isEmpty {
                EmptyLibraryContent()
            } else {
                LibraryContent()
            }
        }
        .navigationTitle("Library")
        .navigationBarTitleDisplayMode(.large)
        .sheet(isPresented: $showSortSheet) {
            SortOptionsSheet()
        }
    }

    // MARK: - Sort and Display Controls

    @ViewBuilder
    private func SortAndDisplayControls() -> some View {
        HStack {
            // Sort button
            Button(action: {
                showSortSheet = true
            }) {
                HStack(spacing: 4) {
                    Image(systemName: "arrow.up.arrow.down")
                    Text(viewModel.selectedSortOption.displayName)
                    Image(systemName: viewModel.selectedSortDirection == .descending ? "chevron.down" : "chevron.up")
                }
                .font(.system(size: 14))
                .foregroundColor(.primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))
                .cornerRadius(8)
            }

            Spacer()

            // Display mode toggle
            HStack(spacing: 8) {
                Button(action: {
                    viewModel.setDisplayMode(.list)
                }) {
                    Image(systemName: "list.bullet")
                        .foregroundColor(viewModel.displayMode == .list ? DeadRed : .secondary)
                }

                Button(action: {
                    viewModel.setDisplayMode(.grid)
                }) {
                    Image(systemName: "square.grid.2x2")
                        .foregroundColor(viewModel.displayMode == .grid ? DeadRed : .secondary)
                }
            }
            .font(.system(size: 20))
        }
    }

    // MARK: - Sort Options Sheet

    @ViewBuilder
    private func SortOptionsSheet() -> some View {
        NavigationView {
            List {
                Section("Sort By") {
                    ForEach([LibrarySortOption.dateAdded, .dateOfShow, .venue, .rating], id: \.self) { option in
                        Button(action: {
                            viewModel.updateSortOption(option)
                        }) {
                            HStack {
                                Text(option.displayName)
                                    .foregroundColor(.primary)
                                Spacer()
                                if viewModel.selectedSortOption == option {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(DeadRed)
                                }
                            }
                        }
                    }
                }

                Section("Direction") {
                    ForEach([LibrarySortDirection.descending, .ascending], id: \.self) { direction in
                        Button(action: {
                            viewModel.updateSortDirection(direction)
                        }) {
                            HStack {
                                Text(direction.displayName)
                                    .foregroundColor(.primary)
                                Spacer()
                                if viewModel.selectedSortDirection == direction {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(DeadRed)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Sort Options")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        showSortSheet = false
                    }
                }
            }
        }
    }

    // MARK: - Library Content

    @ViewBuilder
    private func LibraryContent() -> some View {
        if viewModel.displayMode == .list {
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(viewModel.shows, id: \.showId) { show in
                        LibraryShowListItem(show: show)
                            .onTapGesture {
                                coordinator.path.append(NavigationCoordinator.Destination.showDetail(
                                    showId: show.showId,
                                    recordingId: nil
                                ))
                            }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
        } else {
            ScrollView {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    ForEach(viewModel.shows, id: \.showId) { show in
                        LibraryShowGridItem(show: show)
                            .onTapGesture {
                                coordinator.path.append(NavigationCoordinator.Destination.showDetail(
                                    showId: show.showId,
                                    recordingId: nil
                                ))
                            }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
        }
    }

    // MARK: - Loading Content

    @ViewBuilder
    private func LoadingContent() -> some View {
        VStack {
            Spacer()
            ProgressView()
            Text("Loading your library...")
                .font(.system(size: 16))
                .foregroundColor(.secondary)
                .padding(.top, 16)
            Spacer()
        }
    }

    // MARK: - Error Content

    @ViewBuilder
    private func ErrorContent(error: String) -> some View {
        VStack(spacing: 16) {
            Spacer()

            Text("Error Loading Library")
                .font(.system(size: 24, weight: .bold))

            Text(error)
                .font(.system(size: 16))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Button(action: {
                viewModel.refreshLibrary()
            }) {
                Text("Retry")
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .frame(width: 120, height: 44)
                    .background(DeadRed)
                    .cornerRadius(8)
            }

            Spacer()
        }
    }

    // MARK: - Empty Library Content

    @ViewBuilder
    private func EmptyLibraryContent() -> some View {
        VStack(spacing: 24) {
            Spacer()

            Text("Your Library is Empty")
                .font(.system(size: 24, weight: .bold))

            Text("Add some shows to get started. In development mode, use \"Populate Test Data\" to load realistic test data.")
                .font(.system(size: 16))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Button(action: {
                viewModel.populateTestData()
            }) {
                Text("Populate Test Data")
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(DeadRed)
                    .cornerRadius(8)
            }

            Spacer()
        }
    }
}

// MARK: - Library Show List Item

struct LibraryShowListItem: View {
    let show: LibraryShowViewModel
    private let DeadRed = Color(red: 0xDC/255.0, green: 0x14/255.0, blue: 0x3C/255.0)

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    // Date
                    Text(show.displayDate)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.primary)

                    // Venue
                    Text(show.venue)
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)

                    // Location
                    Text(show.location)
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)

                    // Rating
                    if let rating = show.rating {
                        HStack(spacing: 4) {
                            Image(systemName: "star.fill")
                                .foregroundColor(DeadRed)
                                .font(.system(size: 12))
                            Text(String(format: "%.1f", rating))
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                            Text("(\(show.reviewCount) reviews)")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }
                    }
                }

                Spacer()

                // Pin indicator
                if show.isPinned {
                    Image(systemName: "pin.fill")
                        .foregroundColor(DeadRed)
                        .font(.system(size: 16))
                }
            }
        }
        .padding(16)
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }
}

// MARK: - Library Show Grid Item

struct LibraryShowGridItem: View {
    let show: LibraryShowViewModel
    private let DeadRed = Color(red: 0xDC/255.0, green: 0x14/255.0, blue: 0x3C/255.0)

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Date (larger for grid)
            Text(extractMonthDay(from: show.date))
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(.primary)

            // Year
            Text(extractYear(from: show.date))
                .font(.system(size: 12))
                .foregroundColor(.secondary)

            Spacer()

            // Pin indicator at bottom
            if show.isPinned {
                HStack {
                    Spacer()
                    Image(systemName: "pin.fill")
                        .foregroundColor(DeadRed)
                        .font(.system(size: 12))
                }
            }
        }
        .padding(8)
        .frame(height: 100)
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }

    private func extractMonthDay(from date: String) -> String {
        let components = date.split(separator: "-")
        guard components.count == 3,
              let month = Int(components[1]),
              let day = Int(components[2]) else {
            return date
        }
        let monthNames = ["", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                          "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
        let monthName = month > 0 && month <= 12 ? monthNames[month] : ""
        return "\(monthName) \(day)"
    }

    private func extractYear(from date: String) -> String {
        return String(date.prefix(4))
    }
}
