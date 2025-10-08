import AVFoundation
import MediaPlayer
import UIKit
import ComposeApp

/// SmartQueuePlayer - A wrapper around AVQueuePlayer that handles all the queue management complexity
/// and provides simple callbacks for track changes, along with production-ready features like
/// Now Playing Info Center and remote control integration.
@objc(SmartQueuePlayer) public class SmartQueuePlayer: NSObject {

    // MARK: - Private Properties

    private let queuePlayer = AVQueuePlayer()
    private var urls: [String] = []
    private var trackMetadata: [[String: Any]] = []
    private var currentIndex = 0
    private var endObserver: NSObjectProtocol?
    private var isObservingRate = false
    private var isObservingCurrentItem = false

    // MARK: - Public Properties

    /// Callback triggered when the current track changes (either manually or auto-advance)
    public var onTrackChanged: ((Int) -> Void)?

    /// Callback triggered when playback reaches the end of the playlist
    public var onPlaylistEnded: (() -> Void)?

    // MARK: - Initialization

    /// Initialize with array of URLs to play
    /// - Parameters:
    ///   - urls: Array of track URLs (can be remote https:// or local file://)
    ///   - startIndex: Index to start playback from (default: 0)
    ///   - metadata: Optional array of track metadata dictionaries
    public init(urls: [String], startIndex: Int = 0, metadata: [[String: Any]] = []) {
        super.init()

        // Store URLs for creating fresh AVPlayerItems as needed
        self.urls = urls
        self.trackMetadata = metadata
        self.currentIndex = max(0, min(startIndex, urls.count - 1))

        // Configure AVQueuePlayer for gapless playback
        queuePlayer.automaticallyWaitsToMinimizeStalling = false

        setupQueue()
        setupEndObserver()
        setupPlaybackStateObserver()
        setupRemoteCommands()
        configureAudioSession()
    }

    deinit {
        cleanup()
    }

    // MARK: - Public Playback Control

    /// Start or resume playback
    public func play() {
        queuePlayer.play()
        updateNowPlayingInfo()
    }

    /// Pause playback
    public func pause() {
        queuePlayer.pause()
        updateNowPlayingInfo()
    }

    /// Skip to next track
    /// - Returns: true if successfully advanced, false if at end of playlist
    @discardableResult
    public func playNext() -> Bool {
        guard currentIndex < urls.count - 1 else {
            // At end of playlist
            NSLog("🎯 🎵 [NEXT|MANUAL] At end of playlist, cannot advance. idx:\(currentIndex)/\(urls.count)")
            return false
        }

        // Log BEFORE changing state
        let oldIndex = currentIndex
        let newIndex = currentIndex + 1
        let trackTitle = extractTitle() ?? "Unknown"
        NSLog("🎯 🎵 [NEXT|MANUAL] Manual next: idx:\(oldIndex)→\(newIndex)/\(urls.count) track:\"\(trackTitle)\"")

        // Go to actual next track using immediate rebuild approach
        currentIndex = newIndex
        rebuildQueue(from: currentIndex)

        // Log callback notification
        NSLog("🎯 🟢 [S→K] Calling onTrackChanged(\(currentIndex))")
        onTrackChanged?(currentIndex)

        NSLog("🎯 🟢 [S→K] Calling AppPlatform.notifyTrackChanged(\(currentIndex))")
        AppPlatform.shared.notifyTrackChanged(newIndex: Int32(currentIndex))
        updateNowPlayingInfo()

        return true
    }

    /// Skip to previous track or restart current track
    /// - Parameter threshold: Time threshold in seconds. If current position > threshold, restart current track.
    ///                       If <= threshold, go to previous track. Default: 3.0 seconds
    /// - Returns: true if action was successful
    @discardableResult
    public func playPrevious(threshold: TimeInterval = 3.0) -> Bool {
        let currentTime = CMTimeGetSeconds(queuePlayer.currentTime())

        // If we're past the threshold, just restart current track
        if currentTime > threshold {
            let trackTitle = extractTitle() ?? "Unknown"
            NSLog("🎯 🎵 [PREV|RESTART] Restarting current track (position > \(threshold)s): idx:\(currentIndex)/\(urls.count) track:\"\(trackTitle)\"")
            seek(to: 0)
            return true
        }

        // If at first track, restart it
        guard currentIndex > 0 else {
            let trackTitle = extractTitle() ?? "Unknown"
            NSLog("🎯 🎵 [PREV|RESTART] At first track, restarting: idx:\(currentIndex)/\(urls.count) track:\"\(trackTitle)\"")
            seek(to: 0)
            return true
        }

        // Log BEFORE changing state
        let oldIndex = currentIndex
        let newIndex = currentIndex - 1
        let trackTitle = extractTitle() ?? "Unknown"
        NSLog("🎯 🎵 [PREV|MANUAL] Manual previous: idx:\(oldIndex)→\(newIndex)/\(urls.count) track:\"\(trackTitle)\"")

        // Go to actual previous track
        currentIndex = newIndex
        rebuildQueue(from: currentIndex)

        // Log callback notification
        NSLog("🎯 🟢 [S→K] Calling onTrackChanged(\(currentIndex))")
        onTrackChanged?(currentIndex)

        NSLog("🎯 🟢 [S→K] Calling AppPlatform.notifyTrackChanged(\(currentIndex))")
        AppPlatform.shared.notifyTrackChanged(newIndex: Int32(currentIndex))
        updateNowPlayingInfo()

        return true
    }

    /// Seek to specific position in current track
    /// - Parameter seconds: Position in seconds
    public func seek(to seconds: TimeInterval) {
        let time = CMTime(seconds: seconds, preferredTimescale: 1000)
        queuePlayer.seek(to: time)
        updateNowPlayingInfo()
    }

    /// Get current playback position in seconds
    public var currentTime: TimeInterval {
        return CMTimeGetSeconds(queuePlayer.currentTime())
    }

    /// Get current track duration in seconds
    public var duration: TimeInterval {
        guard let currentItem = queuePlayer.currentItem else { return 0 }
        let duration = currentItem.duration
        let seconds = CMTimeGetSeconds(duration)
        return seconds.isFinite ? seconds : 0
    }

    /// Check if currently playing
    public var isPlaying: Bool {
        return queuePlayer.rate > 0
    }

    /// Get current track index
    public var trackIndex: Int {
        return currentIndex
    }

    /// Get total number of tracks
    public var trackCount: Int {
        return urls.count
    }

    // MARK: - Private Queue Management

    /// Create a fresh AVPlayerItem for the given index
    /// - Parameter index: Track index
    /// - Returns: New AVPlayerItem or nil if invalid index/URL
    private func createPlayerItem(for index: Int) -> AVPlayerItem? {
        guard index >= 0 && index < urls.count else { return nil }
        guard let url = URL(string: urls[index]) else { return nil }
        return AVPlayerItem(url: url)
    }

    private func setupQueue() {
        queuePlayer.removeAllItems()

        // Add current track + next 2 tracks for gapless playback
        let endIndex = min(currentIndex + 3, urls.count)
        for i in currentIndex..<endIndex {
            if let item = createPlayerItem(for: i) {
                queuePlayer.insert(item, after: nil)
            }
        }
    }

    private func extendQueueIfNeeded() {
        let queueSize = queuePlayer.items().count

        // If only 1 item left in queue and more tracks available, add next track
        if queueSize <= 1 && currentIndex + queueSize < urls.count {
            let nextIndex = currentIndex + queueSize
            if let nextItem = createPlayerItem(for: nextIndex) {
                queuePlayer.insert(nextItem, after: nil)
            }
        }
    }

    private func rebuildQueue(from index: Int) {
        let wasPlaying = isPlaying  // Capture state before pausing
        queuePlayer.pause()
        queuePlayer.removeAllItems()

        // Add up to 3 tracks starting from the new index
        let endIndex = min(index + 3, urls.count)
        for i in index..<endIndex {
            if let item = createPlayerItem(for: i) {
                queuePlayer.insert(item, after: nil)
            }
        }

        // Seek to beginning of track
        queuePlayer.seek(to: CMTime.zero)

        // Only resume playing if we were playing before
        if wasPlaying {
            queuePlayer.play()
        }
    }

    // MARK: - Event Observers

    private func setupEndObserver() {
        endObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            self?.handleItemDidEnd(notification)
        }
    }

    private func setupPlaybackStateObserver() {
        // Observe rate changes to detect play/pause events
        queuePlayer.addObserver(
            self,
            forKeyPath: #keyPath(AVQueuePlayer.rate),
            options: [.new],
            context: nil
        )
        isObservingRate = true

        // Observe currentItem changes to detect unexpected track changes
        queuePlayer.addObserver(
            self,
            forKeyPath: #keyPath(AVQueuePlayer.currentItem),
            options: [.new, .old],
            context: nil
        )
        isObservingCurrentItem = true
    }

    // KVO observer for playback state changes
    public override func observeValue(forKeyPath keyPath: String?,
                                     of object: Any?,
                                     change: [NSKeyValueChangeKey : Any]?,
                                     context: UnsafeMutableRawPointer?) {
        if keyPath == #keyPath(AVQueuePlayer.rate) {
            let isPlaying = queuePlayer.rate > 0
            NSLog("🎯 📱 [PLAYBACK_STATE] Rate changed to \(queuePlayer.rate), isPlaying: \(isPlaying)")

            // Notify Kotlin via AppPlatform
            AppPlatform.shared.notifyPlaybackStateChanged(isPlaying: isPlaying)
            updateNowPlayingInfo()
        } else if keyPath == #keyPath(AVQueuePlayer.currentItem) {
            // Detect when AVQueuePlayer changes items (could be unexpected)
            let queueItems = queuePlayer.items()
            let queueSize = queueItems.count
            NSLog("🎯 🔴 [AVQ_ITEM_CHANGE] AVQueuePlayer currentItem changed. Queue size: \(queueSize), Kotlin thinks idx: \(currentIndex)/\(urls.count)")

            // This could indicate AVQueuePlayer advanced without us knowing
            if let currentItem = queuePlayer.currentItem,
               let oldItem = change?[.oldKey] as? AVPlayerItem,
               currentItem !== oldItem {
                NSLog("🎯 🔴 [AVQ_ITEM_CHANGE] UNEXPECTED: AVQueuePlayer switched items! This may indicate queue desync.")
            }
        }
    }

    private func handleItemDidEnd(_ notification: Notification) {
        guard let endedItem = notification.object as? AVPlayerItem,
              endedItem == queuePlayer.currentItem else {
            return
        }

        // Auto-advance to next track
        if currentIndex < urls.count - 1 {
            let oldIndex = currentIndex
            let newIndex = currentIndex + 1
            let oldTrackTitle = extractTitle() ?? "Unknown"

            NSLog("🎯 🔴 [AUTO-ADVANCE] Track ended, auto-advancing: idx:\(oldIndex)→\(newIndex)/\(urls.count) ended:\"\(oldTrackTitle)\"")

            currentIndex = newIndex
            extendQueueIfNeeded()

            // Get new track title after index change
            let newTrackTitle = extractTitle() ?? "Unknown"
            NSLog("🎯 🔴 [AUTO-ADVANCE] Advanced to: idx:\(currentIndex)/\(urls.count) track:\"\(newTrackTitle)\"")

            NSLog("🎯 🟢 [S→K] Calling onTrackChanged(\(currentIndex)) [AUTO-ADVANCE]")
            onTrackChanged?(currentIndex)

            // Notify Kotlin via AppPlatform
            NSLog("🎯 🟢 [S→K] Calling AppPlatform.notifyTrackChanged(\(currentIndex)) [AUTO-ADVANCE]")
            AppPlatform.shared.notifyTrackChanged(newIndex: Int32(currentIndex))

            updateNowPlayingInfo()
        } else {
            // End of playlist
            NSLog("🎯 🎵 [PLAYLIST-END] Reached end of playlist: idx:\(currentIndex)/\(urls.count)")
            onPlaylistEnded?()
        }
    }

    // MARK: - Audio Session Configuration

    private func configureAudioSession() {
        do {
            let audioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(.playback, mode: .default)
            try audioSession.setActive(true)
        } catch {
            NSLog("Failed to configure audio session: \(error)")
        }
    }

    // MARK: - Remote Command Center

    private func setupRemoteCommands() {
        let commandCenter = MPRemoteCommandCenter.shared()

        // Play command
        commandCenter.playCommand.isEnabled = true
        commandCenter.playCommand.addTarget { [weak self] _ in
            self?.play()
            return .success
        }

        // Pause command
        commandCenter.pauseCommand.isEnabled = true
        commandCenter.pauseCommand.addTarget { [weak self] _ in
            self?.pause()
            return .success
        }

        // Next track command
        commandCenter.nextTrackCommand.isEnabled = true
        commandCenter.nextTrackCommand.addTarget { [weak self] _ in
            if self?.playNext() == true {
                return .success
            } else {
                return .noActionableNowPlayingItem
            }
        }

        // Previous track command
        commandCenter.previousTrackCommand.isEnabled = true
        commandCenter.previousTrackCommand.addTarget { [weak self] _ in
            if self?.playPrevious() == true {
                return .success
            } else {
                return .noActionableNowPlayingItem
            }
        }

        // Seek command
        commandCenter.changePlaybackPositionCommand.isEnabled = true
        commandCenter.changePlaybackPositionCommand.addTarget { [weak self] event in
            guard let positionEvent = event as? MPChangePlaybackPositionCommandEvent else {
                return .commandFailed
            }
            self?.seek(to: positionEvent.positionTime)
            return .success
        }

        // Skip forward/backward commands (15 seconds)
        commandCenter.skipForwardCommand.isEnabled = true
        commandCenter.skipForwardCommand.preferredIntervals = [15]
        commandCenter.skipForwardCommand.addTarget { [weak self] _ in
            let currentTime = self?.currentTime ?? 0
            self?.seek(to: currentTime + 15)
            return .success
        }

        commandCenter.skipBackwardCommand.isEnabled = true
        commandCenter.skipBackwardCommand.preferredIntervals = [15]
        commandCenter.skipBackwardCommand.addTarget { [weak self] _ in
            let currentTime = self?.currentTime ?? 0
            self?.seek(to: max(0, currentTime - 15))
            return .success
        }
    }

    // MARK: - Now Playing Info Center

    private func updateNowPlayingInfo() {
        guard currentIndex < urls.count else { return }

        var nowPlayingInfo: [String: Any] = [:]

        // Rich metadata if available, otherwise fallback to extracted title
        if currentIndex < trackMetadata.count && !trackMetadata[currentIndex].isEmpty {
            let metadata = trackMetadata[currentIndex]

            NSLog("🎯 📱 [NOW_PLAYING] Using rich metadata for track \(currentIndex): \(metadata)")

            // Use rich metadata for professional notifications
            nowPlayingInfo[MPMediaItemPropertyTitle] = metadata["title"] ?? extractTitle() ?? "Unknown Title"
            nowPlayingInfo[MPMediaItemPropertyArtist] = metadata["artist"] ?? "Grateful Dead"

            // Enhanced album title with venue and date for context
            if let venue = metadata["venue"] as? String, let date = metadata["date"] as? String {
                nowPlayingInfo[MPMediaItemPropertyAlbumTitle] = "\(venue) • \(date)"
                NSLog("🎯 📱 [NOW_PLAYING] Set album to: \"\(venue) • \(date)\"")
            } else {
                nowPlayingInfo[MPMediaItemPropertyAlbumTitle] = metadata["album"] ?? "Live Performance"
            }

            // Additional context in comments field
            if let recordingInfo = metadata["recordingInfo"] as? String {
                nowPlayingInfo[MPMediaItemPropertyComments] = recordingInfo
            }
        } else {
            // Fallback to basic info
            NSLog("🎯 📱 [NOW_PLAYING] Using fallback metadata for track \(currentIndex) (metadata count: \(trackMetadata.count))")
            nowPlayingInfo[MPMediaItemPropertyTitle] = extractTitle() ?? "Unknown Title"
            nowPlayingInfo[MPMediaItemPropertyArtist] = "Grateful Dead"
            nowPlayingInfo[MPMediaItemPropertyAlbumTitle] = "Live Performance"
        }

        // Playback info
        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentTime
        nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = duration
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = queuePlayer.rate

        // Track position
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackQueueIndex] = currentIndex
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackQueueCount] = urls.count

        MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
    }

    private func extractTitle() -> String? {
        guard currentIndex < urls.count else { return nil }
        guard let url = URL(string: urls[currentIndex]) else { return nil }

        let filename = url.lastPathComponent
        let nameWithoutExtension = (filename as NSString).deletingPathExtension

        // Clean up common filename patterns for Grateful Dead tracks
        var cleanTitle = nameWithoutExtension
            .replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: "-", with: " ")

        // Extract track title from common patterns like "01 Dark Star" or "gd1977-05-08d1t01"
        if let trackMatch = cleanTitle.range(of: #"\d+\s+(.+)"#, options: .regularExpression) {
            cleanTitle = String(cleanTitle[trackMatch]).trimmingCharacters(in: .whitespaces)
            // Remove leading track number
            cleanTitle = cleanTitle.replacingOccurrences(of: #"^\d+\s+"#, with: "", options: .regularExpression)
        }

        return cleanTitle.isEmpty ? "Unknown Track" : cleanTitle
    }

    // MARK: - Cleanup

    private func cleanup() {
        if let observer = endObserver {
            NotificationCenter.default.removeObserver(observer)
        }

        // Remove KVO observers if they were added
        if isObservingRate {
            queuePlayer.removeObserver(self, forKeyPath: #keyPath(AVQueuePlayer.rate))
            isObservingRate = false
        }
        if isObservingCurrentItem {
            queuePlayer.removeObserver(self, forKeyPath: #keyPath(AVQueuePlayer.currentItem))
            isObservingCurrentItem = false
        }

        queuePlayer.pause()
        queuePlayer.removeAllItems()
    }

    // MARK: - Objective-C Bridge Methods for Kotlin/Native

    /// Create SmartQueuePlayer from array of URL strings
    @objc public static func create(urls: [String], startIndex: Int) -> SmartQueuePlayer {
        return SmartQueuePlayer(urls: urls, startIndex: startIndex)
    }

    /// Create SmartQueuePlayer with rich metadata
    @objc public static func createWithMetadata(urls: [String], startIndex: Int, metadata: [[String: Any]]) -> SmartQueuePlayer {
        return SmartQueuePlayer(urls: urls, startIndex: startIndex, metadata: metadata)
    }

    /// Set track changed callback
    @objc public func setTrackChangedCallback(_ callback: @escaping (Int) -> Void) {
        self.onTrackChanged = callback
    }

    /// Set playlist ended callback
    @objc public func setPlaylistEndedCallback(_ callback: @escaping () -> Void) {
        self.onPlaylistEnded = callback
    }

    /// Seek to position specified as seconds (Double for Kotlin compatibility)
    @objc public func seekToTime(_ seconds: Double) {
        seek(to: seconds)
    }
}