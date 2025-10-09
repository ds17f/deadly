import SwiftUI
import ComposeApp

/**
 * RecordingSelectionSheet - Modal bottom sheet for recording selection
 *
 * Matches Compose RecordingSelectionSheet.kt exactly:
 * - Header with title, show info, and close button
 * - Scrollable list of RecordingOptionCardView items
 * - Loading and error states
 * - Dynamic action buttons:
 *   - "Reset to Recommended" (outlined) - only when not using recommended
 *   - "Set Recording for Show" (filled) - only when different recording selected
 */

private let DeadRed = Color(red: 0xDC/255.0, green: 0x14/255.0, blue: 0x3C/255.0)

struct RecordingSelectionSheet: View {
    @ObservedObject var viewModel: RecordingSelectionViewModelWrapper
    let onDismiss: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Header with close button and show info
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Choose Recording")
                        .font(.title2)
                        .fontWeight(.bold)

                    if !viewModel.showTitle.isEmpty {
                        Text(viewModel.showTitle)
                            .font(.body)
                            .foregroundColor(.secondary)
                    }

                    if !viewModel.showDate.isEmpty {
                        Text(viewModel.showDate)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .font(.system(size: 16))
                        .foregroundColor(.secondary)
                        .frame(width: 32, height: 32)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)

            // Content based on state
            if viewModel.isLoading {
                loadingContent
            } else if let errorMessage = viewModel.errorMessage {
                errorContent(message: errorMessage)
            } else {
                recordingListContent
            }

            // Action buttons - always visible at bottom
            actionButtons
                .padding(.horizontal, 16)
                .padding(.bottom, 32) // Extra bottom padding for safe area
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    // MARK: - Loading State

    private var loadingContent: some View {
        VStack(spacing: 16) {
            ProgressView()
            Text("Loading recordings...")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 200)
    }

    // MARK: - Error State

    private func errorContent(message: String) -> some View {
        VStack(spacing: 16) {
            Text("Error loading recordings")
                .font(.headline)
                .foregroundColor(.red)

            Text(message)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            Button("Retry") {
                // TODO: Add retry action
            }
            .buttonStyle(.bordered)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 200)
        .padding(.horizontal, 16)
    }

    // MARK: - Recording List Content

    private var recordingListContent: some View {
        ScrollView {
            VStack(spacing: 8) {
                // Current recording first (if exists)
                if let currentRecording = viewModel.currentRecording {
                    RecordingOptionCardView(
                        recording: currentRecording,
                        onClick: {
                            viewModel.selectRecording(currentRecording.identifier)
                        }
                    )
                }

                // Alternative recordings
                ForEach(viewModel.alternativeRecordings, id: \.identifier) { recording in
                    RecordingOptionCardView(
                        recording: recording,
                        onClick: {
                            viewModel.selectRecording(recording.identifier)
                        }
                    )
                }

                // Empty state
                if viewModel.allRecordings.isEmpty {
                    Text("No recordings available")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 100)
                }
            }
            .padding(.horizontal, 16)
        }
        .frame(maxHeight: 400) // Max height constraint like Compose
    }

    // MARK: - Action Buttons

    private var actionButtons: some View {
        VStack(spacing: 12) {
            // Reset to Recommended button (only when not using recommended)
            if viewModel.shouldShowResetToRecommended {
                Button(action: {
                    viewModel.resetToRecommended()
                    onDismiss()
                }) {
                    HStack(spacing: 8) {
                        Image(systemName: "star")
                            .font(.system(size: 16))
                        Text("Reset to Recommended")
                            .font(.body)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                }
                .buttonStyle(.bordered)
            }

            // Set as Default button (only when different recording selected)
            if viewModel.shouldShowSetAsDefault,
               let selectedRecording = viewModel.selectedRecording {
                Button(action: {
                    viewModel.setAsDefault(selectedRecording.identifier)
                    onDismiss()
                }) {
                    HStack(spacing: 8) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 20))
                        Text("Set Recording for Show")
                            .font(.body)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                }
                .buttonStyle(.borderedProminent)
                .tint(DeadRed)
            }
        }
    }
}

// MARK: - Preview

#Preview {
    // Preview disabled - requires Koin initialization
    Text("RecordingSelectionSheet Preview")
}
