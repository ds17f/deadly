# Event-Driven Media Player Architecture

## Overview

This document outlines the design for an event-driven iOS media player system that eliminates polling-based state synchronization and provides instant, reliable communication between Swift and Kotlin layers.

## Current Problems

### Polling-Based Architecture Issues
- **Performance**: 1000ms polling for track changes + 100ms time observer = excessive overhead
- **Latency**: Up to 1-second delays for UI updates on track changes
- **Reliability**: Missed updates when polling intervals don't align with state changes
- **Battery**: Continuous polling drains battery with unnecessary wake-ups
- **Complexity**: Multiple timing mechanisms create race conditions

### Broken Callback System
- Swift callbacks print to console but never notify Kotlin
- Track updates fail silently, leaving UI in stale state
- No reliable Swift → Kotlin event communication

## Event-Driven Solution

### Architecture Overview

```
iOS Media Events → Swift Callbacks → AppPlatform Bridge → Kotlin Handlers → UI Updates
```

**Flow:**
1. **iOS AVQueuePlayer** fires native events (track change, playback state change)
2. **SmartQueuePlayer** receives events via delegates/observers
3. **AppPlatform** provides typed event bridge to Kotlin
4. **PlatformMediaPlayer** handles events and updates state immediately
5. **UI** receives instant updates via reactive flows

### Core Components

#### 1. AppPlatform Event Bridge
```kotlin
// AppPlatform.kt
object AppPlatform {
    private var trackChangeHandler: ((Int) -> Unit)? = null
    private var playbackStateHandler: ((Boolean) -> Unit)? = null

    fun registerPlayerEventHandlers(
        onTrackChanged: (Int) -> Unit,
        onPlaybackStateChanged: (Boolean) -> Unit
    ) {
        trackChangeHandler = onTrackChanged
        playbackStateHandler = onPlaybackStateChanged
    }

    // Called from Swift
    fun notifyTrackChanged(newIndex: Int) {
        trackChangeHandler?.invoke(newIndex)
    }

    fun notifyPlaybackStateChanged(isPlaying: Boolean) {
        playbackStateHandler?.invoke(isPlaying)
    }
}
```

#### 2. Swift Event Sources
```swift
// SmartQueuePlayer.swift
private func setupEventObservers() {
    // Track change events
    NotificationCenter.default.addObserver(
        forName: .AVPlayerItemDidPlayToEndTime,
        object: nil,
        queue: .main
    ) { [weak self] _ in
        self?.handleTrackAdvance()
    }

    // Playback state observers
    queuePlayer.addObserver(
        self,
        forKeyPath: #keyPath(AVQueuePlayer.rate),
        options: [.new],
        context: nil
    )
}

private func handleTrackAdvance() {
    currentIndex += 1
    AppPlatform.shared.notifyTrackChanged(currentIndex)
}

override func observeValue(forKeyPath keyPath: String?,
                          of object: Any?,
                          change: [NSKeyValueChangeKey : Any]?,
                          context: UnsafeMutableRawPointer?) {
    if keyPath == #keyPath(AVQueuePlayer.rate) {
        let isPlaying = queuePlayer.rate > 0
        AppPlatform.shared.notifyPlaybackStateChanged(isPlaying)
    }
}
```

#### 3. Kotlin Event Handling
```kotlin
// PlatformMediaPlayer.kt
init {
    AppPlatform.registerPlayerEventHandlers(
        onTrackChanged = { newIndex ->
            handleTrackChanged(newIndex)
        },
        onPlaybackStateChanged = { isPlaying ->
            updatePlaybackState {
                copy(isPlaying = isPlaying)
            }
        }
    )
}

private fun handleTrackChanged(newIndex: Int) {
    playerScope.launch {
        currentEnrichedTrackIndex = newIndex
        _currentTrackIndex.value = newIndex

        // Update metadata
        if (newIndex >= 0 && newIndex < currentEnrichedTracks.size) {
            val track = currentEnrichedTracks[newIndex]
            setTrackMetadata(track.track, track.recordingId)
        }
    }
}
```

## Event Types

### Primary Events (Phase 1)
1. **Track Changed** - User navigation or auto-advance
   - Payload: `newIndex: Int`
   - Trigger: Manual navigation, track end, queue advance

2. **Playback State Changed** - Play/pause state
   - Payload: `isPlaying: Boolean`
   - Trigger: Play/pause button, remote control, interruptions

### Secondary Events (Future Phases)
3. **Seek Completed** - Scrubbing finished
4. **Buffer State Changed** - Loading/buffering status
5. **Error Occurred** - Playback errors
6. **Queue Updated** - Playlist changes

## Implementation Phases

### Phase 1: Core Event System
- Implement AppPlatform event bridge
- Add track change and playback state events
- Remove polling from PlatformMediaPlayer
- Test event reliability and performance

### Phase 2: Enhanced Events
- Add seek, buffer, and error events
- Implement event queuing for reliability
- Add event timestamps and sequencing

### Phase 3: Optimization
- Batch related events to reduce bridge calls
- Add event filtering to prevent spam
- Implement event replay for state recovery

## Benefits

### Performance
- **10x reduction** in unnecessary updates (eliminate 100ms observer)
- **Zero polling overhead** - events fire only when needed
- **Instant UI response** - no 1-second delays
- **Better battery life** - no background polling timers

### Reliability
- **Guaranteed delivery** - direct function calls, no timing dependencies
- **Type safety** - Kotlin type system prevents errors
- **Consistency** - single source of truth for state changes
- **Debuggability** - clear event flow for troubleshooting

### Maintainability
- **Simpler architecture** - eliminate multiple timing mechanisms
- **Consistent patterns** - follows existing AppPlatform design
- **Testability** - events can be triggered programmatically
- **Extensibility** - easy to add new event types

## Migration Strategy

### Step 1: Preserve Rich Metadata
- Commit existing TrackMetadata improvements
- Keep enhanced Now Playing Info with venue/date
- Maintain professional notification quality

### Step 2: Implement Event Bridge
- Add AppPlatform event handlers
- Wire Swift callbacks to bridge
- Register handlers in PlatformMediaPlayer

### Step 3: Remove Polling
- Delete 100ms time observer
- Remove track change polling logic
- Keep minimal position polling for UI scrubber

### Step 4: Test and Validate
- Verify instant track updates
- Confirm performance improvements
- Test edge cases (rapid navigation, background/foreground)

## Success Criteria

### Functional
- ✅ Track changes update UI instantly (< 50ms)
- ✅ Playback state changes reflected immediately
- ✅ No missed events during rapid navigation
- ✅ Reliable operation across app lifecycle states

### Performance
- ✅ Zero unnecessary polling overhead
- ✅ < 100ms response time for all events
- ✅ No background timer battery drain
- ✅ Smooth UI responsiveness

### Compatibility
- ✅ Maintains rich metadata notifications
- ✅ Preserves existing track navigation UX
- ✅ Works with iOS control center and lock screen
- ✅ Handles audio interruptions gracefully

## Future Enhancements

### Advanced Event Handling
- Event batching for performance
- Event persistence for crash recovery
- Event analytics for debugging
- Custom event types for app-specific needs

### Cross-Platform Consistency
- Align iOS events with Android MediaSession callbacks
- Standardize event payloads across platforms
- Share event handling logic in common code

### Integration Opportunities
- CarPlay event forwarding
- AirPlay state synchronization
- Remote control command events
- Audio route change notifications

---

**Author**: Development Team
**Date**: October 5, 2025
**Version**: 1.0
**Status**: Design Phase