import SwiftUI
import ComposeApp

/**
 * SwiftUI implementation of ShowDetailScreen.
 *
 * Matches the Compose version EXACTLY to avoid iOS gesture duplication bugs.
 * Uses native SwiftUI components with shared KMM ViewModel for business logic.
 *
 * Layout matches Compose ShowDetailScreen.kt:
 * - Album art (220pt size, 8pt corners)
 * - Show info (date, venue, prev/next buttons)
 * - Interactive rating
 * - Action row (library, download, setlist, collections, menu, play)
 * - Track list
 *
 * NOTE: Mini player is handled by the iOS app scaffold, not this screen.
 * The back button matches ShowDetailHeader.kt with semi-transparent surface background.
 *
 * Color Mapping (Material3 → SwiftUI):
 * - onSurface → Color(UIColor.label)
 * - onSurfaceVariant → Color(UIColor.secondaryLabel)
 * - primary → DeadRed (Crimson #DC143C) - NOT blue!
 * - surface → Color(UIColor.systemBackground)
 */

// Grateful Dead color palette matching DeadlyTheme.kt
private let DeadRed = Color(red: 0xDC/255.0, green: 0x14/255.0, blue: 0x3C/255.0) // Crimson red

struct ShowDetailView: View {
    @StateObject private var viewModel: ShowDetailViewModelWrapper
    @StateObject private var recordingSelectionViewModel: RecordingSelectionViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    @State private var showMenu = false
    @State private var showRecordingSelection = false

    let showId: String
    let recordingId: String?

    init(
        showId: String,
        recordingId: String?,
        showDetailService: ShowDetailService,
        mediaService: MediaService,
        libraryService: LibraryService
    ) {
        self.showId = showId
        self.recordingId = recordingId

        _viewModel = StateObject(wrappedValue: ShowDetailViewModelWrapper(
            showId: showId,
            recordingId: recordingId,
            showDetailService: showDetailService,
            mediaService: mediaService,
            libraryService: libraryService
        ))

        // Create RecordingSelectionViewModel directly with services from Koin
        let recordingSelectionService = KoinHelper.shared.getRecordingSelectionService()

        // Callback to reload show with new recording
        let onRecordingChanged: ((String) -> Void) = { newRecordingId in
            print("🎵 Recording changed to: \(newRecordingId)")
            // Will trigger reload via the wrapper
        }

        let recordingSelectionVM = RecordingSelectionViewModel(
            recordingSelectionService: recordingSelectionService,
            onRecordingChanged: onRecordingChanged
        )

        _recordingSelectionViewModel = StateObject(wrappedValue: RecordingSelectionViewModelWrapper(
            recordingSelectionViewModel: recordingSelectionVM
        ))
    }

    var body: some View {
        ZStack(alignment: .topLeading) {
            // Main content
            if viewModel.isLoading {
                loadingView
            } else if let error = viewModel.error {
                errorView(error: error)
            } else {
                mainContent
            }

            // Back button overlay (matches Compose ShowDetailHeader)
            backButton
        }
        .navigationBarHidden(true)
        .background(Color(UIColor.systemBackground))
        .sheet(isPresented: $showMenu) {
            ShowDetailMenuSheet(
                onShareClick: {
                    shareShow()
                },
                onChooseRecordingClick: {
                    openRecordingSelection()
                },
                onDismiss: {
                    showMenu = false
                }
            )
        }
        .sheet(isPresented: $showRecordingSelection) {
            RecordingSelectionSheet(
                viewModel: recordingSelectionViewModel,
                onDismiss: {
                    showRecordingSelection = false
                }
            )
        }
    }

    // MARK: - Loading State

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
            Text("Loading show...")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Error State

    private func errorView(error: String) -> some View {
        VStack(spacing: 16) {
            Text("Error: \(error)")
                .font(.body)
                .foregroundColor(.red)
            Button("Retry") {
                viewModel.clearError()
                viewModel.reload(showId: showId, recordingId: recordingId)
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Main Content

    private var mainContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Album art - matches ShowDetailAlbumArt.kt exactly
                albumArt

                if let showData = viewModel.showData {
                    // Show info - matches ShowDetailShowInfo.kt exactly
                    showInfo(showData: showData)

                    // Interactive rating - matches ShowDetailInteractiveRating.kt
                    interactiveRating(showData: showData)

                    // Action row - matches ShowDetailActionRow.kt exactly
                    actionRow(showData: showData)
                }

                // Track list - matches ShowDetailTrackList.kt exactly
                if viewModel.isTracksLoading {
                    tracksLoadingView
                } else {
                    trackList
                }
            }
        }
    }

    // MARK: - Album Art (matches ShowDetailAlbumArt.kt)

    private var albumArt: some View {
        HStack {
            Spacer()
            // Load deadly_logo from Assets.xcassets or show placeholder
            if let image = UIImage(named: "deadly_logo") {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 220, height: 220)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            } else {
                // Gray placeholder with text if image not found
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.gray.opacity(0.3))
                    .frame(width: 220, height: 220)
                    .overlay(
                        VStack {
                            Image(systemName: "music.note")
                                .font(.system(size: 48))
                                .foregroundColor(.gray)
                            Text("deadly_logo")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                    )
            }
            Spacer()
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 16)
    }

    // MARK: - Show Info (matches ShowDetailShowInfo.kt)

    private func showInfo(showData: Show_) -> some View {
        HStack(alignment: .top) {
            // Left side: Show info
            VStack(alignment: .leading, spacing: 8) {
                // Show date (main title) - matches headlineSmall + Bold
                Text(showData.date)
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(Color(UIColor.label)) // onSurface

                // Venue and location - matches bodyLarge
                Text("\(showData.venue.name), \(showData.location.displayText)")
                    .font(.body)
                    .foregroundColor(Color(UIColor.secondaryLabel)) // onSurfaceVariant
                    .lineLimit(3)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // Right side: Navigation buttons (40pt size)
            HStack(spacing: 8) {
                Button(action: { viewModel.navigateToPreviousShow() }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 24))
                        .foregroundColor(Color(UIColor.secondaryLabel)) // onSurfaceVariant
                        .frame(width: 40, height: 40)
                }

                Button(action: { viewModel.navigateToNextShow() }) {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 24))
                        .foregroundColor(Color(UIColor.secondaryLabel)) // onSurfaceVariant
                        .frame(width: 40, height: 40)
                }
            }
        }
        .padding(.horizontal, 24)
    }

    // MARK: - Interactive Rating (matches ShowDetailInteractiveRating.kt)

    private func interactiveRating(showData: Show_) -> some View {
        Button(action: {
            // TODO: Show reviews modal
            print("Show reviews for: \(showData.id)")
        }) {
            HStack {
                // Left: Star rating and numerical score
                HStack(spacing: 8) {
                    // Compact star rating
                    compactStarRating(rating: showData.averageRating?.floatValue)

                    // Numerical rating
                    Text(ratingText(rating: showData.averageRating?.doubleValue))
                        .font(.body)
                        .fontWeight(.semibold)
                        .foregroundColor(showData.averageRating != nil && showData.averageRating!.floatValue > 0
                            ? Color(UIColor.label)
                            : Color(UIColor.secondaryLabel))
                }

                Spacer()

                // Right: Review count with chevron
                HStack(spacing: 4) {
                    let reviewCount = Int(showData.totalReviews)
                    Text(reviewCount > 0 ? "(\(reviewCount))" : "No reviews")
                        .font(.body)
                        .foregroundColor(Color(UIColor.secondaryLabel))

                    Image(systemName: "chevron.right")
                        .font(.system(size: 14))
                        .foregroundColor(Color(UIColor.secondaryLabel))
                }
            }
            .padding(12)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(UIColor.secondarySystemFill).opacity(0.3))
            )
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 24)
        .padding(.vertical, 8)
    }

    // Star rating display helper
    private func compactStarRating(rating: Float?) -> some View {
        let safeRating = rating ?? 0
        let maxRating = 5

        return HStack(spacing: 2) {
            ForEach(0..<maxRating, id: \.self) { index in
                let starRating = safeRating - Float(index)
                let iconName = starIconName(for: starRating)

                Image(systemName: iconName)
                    .font(.system(size: 16))
                    .foregroundColor(starRating > 0 ? DeadRed : Color(UIColor.label).opacity(0.3))
            }
        }
    }

    // Star icon name helper
    private func starIconName(for rating: Float) -> String {
        if rating >= 1.0 {
            return "star.fill"
        } else if rating >= 0.5 {
            return "star.leadinghalf.filled"
        } else {
            return "star"
        }
    }

    // Rating text helper
    private func ratingText(rating: Double?) -> String {
        guard let rating = rating, rating > 0 else {
            return "N/A"
        }
        let rounded = (rating * 10).rounded() / 10
        return String(format: "%.1f", rounded)
    }

    // MARK: - Action Row (matches ShowDetailActionRow.kt)

    private func actionRow(showData: Show_) -> some View {
        HStack(spacing: 0) {
            // Left side: Grouped action buttons (all 40pt size with 24pt icons)
            HStack(spacing: 8) {
                // Library button - shows DeadRed when in library
                Button(action: { viewModel.toggleLibraryStatus() }) {
                    Image(systemName: viewModel.isInLibrary ? "checkmark.circle.fill" : "plus.circle")
                        .font(.system(size: 24))
                        .foregroundColor(viewModel.isInLibrary ? DeadRed : Color(UIColor.secondaryLabel))
                        .frame(width: 40, height: 40)
                }

                // Download button (placeholder)
                Button(action: {}) {
                    Image(systemName: "arrow.down.circle")
                        .font(.system(size: 24))
                        .foregroundColor(Color(UIColor.secondaryLabel)) // onSurfaceVariant
                        .frame(width: 40, height: 40)
                }

                // Setlist button (placeholder)
                Button(action: {}) {
                    Image(systemName: "list.bullet")
                        .font(.system(size: 24))
                        .foregroundColor(Color(UIColor.secondaryLabel)) // onSurfaceVariant
                        .frame(width: 40, height: 40)
                }

                // Collections button (placeholder)
                Button(action: {}) {
                    Image(systemName: "folder")
                        .font(.system(size: 24))
                        .foregroundColor(Color(UIColor.secondaryLabel)) // onSurfaceVariant
                        .frame(width: 40, height: 40)
                }

                // Menu button - opens ShowDetailMenuSheet
                Button(action: {
                    showMenu = true
                }) {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 24))
                        .foregroundColor(Color(UIColor.secondaryLabel)) // onSurfaceVariant
                        .frame(width: 40, height: 40)
                }
            }

            Spacer()

            // Right side: Large play/pause button (56pt size, 32pt icon) - DeadRed like Android
            Button(action: { viewModel.togglePlayback() }) {
                ZStack {
                    if viewModel.isMediaLoading {
                        // Loading state with circle background
                        Circle()
                            .fill(DeadRed.opacity(0.1)) // primary.copy(alpha = 0.1)
                            .frame(width: 56, height: 56)
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: DeadRed))
                            .frame(width: 24, height: 24)
                    } else {
                        // Play/pause state with circle background
                        Circle()
                            .fill(DeadRed.opacity(0.1)) // primary.copy(alpha = 0.1)
                            .frame(width: 56, height: 56)
                        Image(systemName: viewModel.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                            .font(.system(size: 32))
                            .foregroundColor(DeadRed) // primary = DeadRed (#DC143C)
                    }
                }
            }
            .frame(width: 56, height: 56)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 16)
    }

    // MARK: - Track List (matches ShowDetailTrackList.kt)

    private var tracksLoadingView: some View {
        VStack(spacing: 8) {
            ProgressView()
            Text("Loading tracks...")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(24)
    }

    private var trackList: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Section header - matches V2 exactly
            Text("Tracks (\(viewModel.tracks.count))")
                .font(.headline)
                .fontWeight(.bold)
                .padding(.horizontal, 24)
                .padding(.vertical, 16)

            // Track items
            ForEach(viewModel.tracks, id: \.name) { track in
                trackRow(track: track)
            }

            // Bottom spacing
            Spacer()
                .frame(height: 24)
        }
    }

    // MARK: - Track Row (matches ShowDetailTrackItem in ShowDetailTrackList.kt)

    private func trackRow(track: Track) -> some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 4) {
                // Track title - bodyLarge
                Text(track.title ?? track.name)
                    .font(.body)
                    .foregroundColor(Color(UIColor.label)) // onSurface
                    .lineLimit(1)

                // Format and duration - bodySmall - matches V2 format exactly
                Text("\(track.format) • \(track.duration ?? "N/A")")
                    .font(.caption)
                    .foregroundColor(Color(UIColor.secondaryLabel)) // onSurfaceVariant
            }

            Spacer()
        }
        .contentShape(Rectangle())
        .padding(.horizontal, 24)
        .padding(.vertical, 12)
        .onTapGesture {
            print("🎵 [SWIFTUI] Track tapped: \(track.title ?? track.name)")
            viewModel.playTrack(track)
        }
    }

    // MARK: - Back Button (matches ShowDetailHeader.kt exactly - 40pt size, semi-transparent surface)

    private var backButton: some View {
        Button(action: { dismiss() }) {
            Image(systemName: "chevron.left")
                .font(.system(size: 24))
                .foregroundColor(Color(UIColor.label)) // onSurface
                .frame(width: 40, height: 40)
                .background(
                    Color(UIColor.systemBackground).opacity(0.9) // surface.copy(alpha = 0.9f)
                )
                .clipShape(Circle())
        }
        .padding(16)
    }

    // MARK: - Helper Functions

    private func shareShow() {
        guard let showData = viewModel.showData else { return }

        let shareText = "Check out this Grateful Dead show: \(showData.date) at \(showData.venue.name), \(showData.location.displayText)"

        let activityVC = UIActivityViewController(
            activityItems: [shareText],
            applicationActivities: nil
        )

        // Present the share sheet
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let rootVC = windowScene.windows.first?.rootViewController {
            rootVC.present(activityVC, animated: true)
        }
    }

    private func openRecordingSelection() {
        guard let showData = viewModel.showData else { return }

        recordingSelectionViewModel.showRecordingSelection(
            showId: showData.id,
            showTitle: showData.displayTitle,
            showDate: showData.date,
            currentRecordingId: viewModel.currentRecordingId
        )

        showRecordingSelection = true
    }
}

// MARK: - Preview

#Preview {
    // Preview disabled - requires Koin initialization
    Text("ShowDetailView Preview")
}
