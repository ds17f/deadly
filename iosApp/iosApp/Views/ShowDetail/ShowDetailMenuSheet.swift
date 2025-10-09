import SwiftUI
import ComposeApp

/**
 * ShowDetailMenuSheet - Simple menu bottom sheet for show detail actions
 *
 * Matches Compose ShowDetailMenuSheet.kt exactly:
 * - Share option (share icon)
 * - Choose Recording option (library_music icon)
 * - Both dismiss sheet and trigger respective actions
 */

struct ShowDetailMenuSheet: View {
    let onShareClick: () -> Void
    let onChooseRecordingClick: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Share option
            Button(action: {
                onShareClick()
                onDismiss()
            }) {
                HStack(spacing: 16) {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 24))
                        .foregroundColor(.primary)
                        .frame(width: 24, height: 24)

                    Text("Share")
                        .font(.body)
                        .foregroundColor(.primary)

                    Spacer()
                }
                .padding(.vertical, 16)
                .padding(.horizontal, 16)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            Divider()
                .padding(.leading, 56) // Align with text, not icon

            // Choose Recording option
            Button(action: {
                onChooseRecordingClick()
                onDismiss()
            }) {
                HStack(spacing: 16) {
                    Image(systemName: "music.note.list")
                        .font(.system(size: 24))
                        .foregroundColor(.primary)
                        .frame(width: 24, height: 24)

                    Text("Choose Recording")
                        .font(.body)
                        .foregroundColor(.primary)

                    Spacer()
                }
                .padding(.vertical, 16)
                .padding(.horizontal, 16)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            // Bottom spacing
            Spacer()
                .frame(height: 16)
        }
        .presentationDetents([.height(160)])
        .presentationDragIndicator(.visible)
    }
}

// MARK: - Preview

#Preview {
    ShowDetailMenuSheet(
        onShareClick: { print("Share clicked") },
        onChooseRecordingClick: { print("Choose Recording clicked") },
        onDismiss: { print("Dismissed") }
    )
}
