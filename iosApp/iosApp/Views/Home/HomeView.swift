import SwiftUI
import ComposeApp

/**
 * SwiftUI implementation of HomeScreen.
 *
 * Matches the Compose version EXACTLY - rich home interface with content discovery.
 * Uses native SwiftUI components with shared KMM HomeViewModel for business logic.
 *
 * Layout matches Compose HomeScreen.kt:
 * - Recently Played Grid (2-column, max 8 shows, 64pt card height)
 * - Today In Grateful Dead History (horizontal scroll, 160pt square cards)
 * - Featured Collections (horizontal scroll, 160pt square cards)
 *
 * Color Mapping (Material3 → SwiftUI):
 * - onSurface → Color(UIColor.label)
 * - onSurfaceVariant → Color(UIColor.secondaryLabel)
 * - surface → Color(UIColor.systemBackground)
 * - surfaceVariant → Color(UIColor.secondarySystemBackground)
 * - error → Color.red
 */

// Grateful Dead color for primary elements
private let DeadRed = Color(red: 0xDC/255.0, green: 0x14/255.0, blue: 0x3C/255.0)

struct HomeView: View {
    @StateObject private var viewModel: HomeViewModelWrapper

    init(homeService: HomeService) {
        _viewModel = StateObject(wrappedValue: HomeViewModelWrapper(homeService: homeService))
    }

    var body: some View {
        ZStack {
            // Error state
            if let error = viewModel.error {
                errorView(error: error)
            }
            // Loading state
            else if viewModel.isLoading && !viewModel.hasContent {
                loadingView
            }
            // Main content
            else {
                mainContent
            }
        }
        .background(Color(UIColor.systemBackground))
    }

    // MARK: - Error State

    private func errorView(error: String) -> some View {
        VStack(spacing: 16) {
            Text(error)
                .font(.body)
                .foregroundColor(.red)
            Button("Retry") {
                viewModel.refresh()
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Loading State

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Main Content

    private var mainContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                // Recent Shows Grid - only show if there are recent shows
                if !viewModel.recentShows.isEmpty {
                    recentShowsGrid
                }

                // Today In Grateful Dead History - only show if there are shows
                if !viewModel.todayInHistory.isEmpty {
                    todayInHistorySection
                }

                // Featured Collections - only show if there are collections
                if !viewModel.featuredCollections.isEmpty {
                    featuredCollectionsSection
                }

                // Empty state if no content
                if !viewModel.hasContent && !viewModel.isLoading {
                    emptyState
                }
            }
            .padding(16)
        }
    }

    // MARK: - Recent Shows Grid (matches RecentShowsGrid.kt)

    private var recentShowsGrid: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Section title - matches headlineSmall + Bold
            Text("Recently Played")
                .font(.title2)
                .fontWeight(.bold)

            // 2-column grid, max 8 shows
            let displayShows = Array(viewModel.recentShows.prefix(8))
            let columns = [
                GridItem(.flexible(), spacing: 8),
                GridItem(.flexible(), spacing: 8)
            ]

            LazyVGrid(columns: columns, spacing: 4) {
                ForEach(displayShows, id: \.id) { show in
                    recentShowCard(show: show)
                }
            }
        }
    }

    // Recent show card - 64pt height, matches RecentShowCard.kt
    private func recentShowCard(show: Show_) -> some View {
        HStack(spacing: 6) {
            // Album cover placeholder - 56pt size, 6pt corners
            RoundedRectangle(cornerRadius: 6)
                .fill(Color(UIColor.secondarySystemBackground))
                .frame(width: 56, height: 56)
                .overlay(
                    Image(systemName: "music.note")
                        .font(.system(size: 24))
                        .foregroundColor(Color(UIColor.secondaryLabel))
                )

            // Show metadata
            VStack(alignment: .leading, spacing: 2) {
                // Date - bodyMedium + SemiBold
                Text(show.date)
                    .font(.callout)
                    .fontWeight(.semibold)
                    .foregroundColor(Color(UIColor.label))
                    .lineLimit(1)

                // Location - bodySmall
                Text(show.location.displayText)
                    .font(.caption)
                    .foregroundColor(Color(UIColor.secondaryLabel))
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(4)
        .frame(height: 64)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(UIColor.systemBackground))
                .shadow(color: Color.black.opacity(0.1), radius: 2, x: 0, y: 1)
        )
        .contentShape(Rectangle())
        .onTapGesture {
            viewModel.navigateToShow(showId: show.id, recordingId: show.lastPlayedRecordingId)
        }
    }

    // MARK: - Today In Grateful Dead History (matches HorizontalCollection.kt)

    private var todayInHistorySection: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Section title - headlineSmall + Bold
            Text("Today In Grateful Dead History")
                .font(.title2)
                .fontWeight(.bold)

            // Horizontal scrolling row
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(viewModel.todayInHistory, id: \.id) { show in
                        horizontalCollectionItem(
                            displayText: "\(show.date)\n\(show.venue.name)\n\(show.location.displayText)",
                            icon: "music.note",
                            onTap: {
                                viewModel.navigateToShow(showId: show.id, recordingId: nil)
                            }
                        )
                    }
                }
            }
        }
    }

    // MARK: - Featured Collections (matches HorizontalCollection.kt)

    private var featuredCollectionsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Section title - headlineSmall + Bold
            Text("Featured Collections")
                .font(.title2)
                .fontWeight(.bold)

            // Horizontal scrolling row
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(viewModel.featuredCollections, id: \.id) { collection in
                        horizontalCollectionItem(
                            displayText: "\(collection.name)\n\(collection.showCountText)",
                            icon: "folder",
                            onTap: {
                                viewModel.navigateToCollection(collectionId: collection.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Horizontal collection item - 160pt square, matches CollectionItemCard.kt
    private func horizontalCollectionItem(displayText: String, icon: String, onTap: @escaping () -> Void) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            // Large square image - 160pt, 12pt corners
            RoundedRectangle(cornerRadius: 12)
                .fill(DeadRed.opacity(0.1))
                .frame(width: 160, height: 160)
                .overlay(
                    Image(systemName: icon)
                        .font(.system(size: 64))
                        .foregroundColor(DeadRed.opacity(0.6))
                )

            // Descriptive text - 3 lines max
            VStack(alignment: .leading, spacing: 2) {
                let lines = displayText.split(separator: "\n").map(String.init)
                ForEach(Array(lines.prefix(3).enumerated()), id: \.offset) { _, line in
                    Text(line)
                        .font(.callout)
                        .fontWeight(.medium)
                        .foregroundColor(Color(UIColor.label))
                        .lineLimit(1)
                        .frame(maxWidth: 160, alignment: .leading)
                }
            }
        }
        .frame(width: 160)
        .contentShape(Rectangle())
        .onTapGesture {
            onTap()
        }
    }

    // MARK: - Empty State (matches HomeScreen.kt empty state)

    private var emptyState: some View {
        VStack(spacing: 16) {
            Text("Welcome to Deadly")
                .font(.title)
                .fontWeight(.bold)

            Text("Start exploring Grateful Dead shows")
                .font(.body)
                .foregroundColor(Color(UIColor.secondaryLabel))

            Button("Browse Shows") {
                viewModel.navigateToSearch()
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity)
        .padding(32)
    }
}

// MARK: - Preview

#Preview {
    // Preview disabled - requires Koin initialization
    Text("HomeView Preview")
}
