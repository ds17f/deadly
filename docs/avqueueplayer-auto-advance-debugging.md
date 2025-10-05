# AVQueuePlayer Auto-Advance Bug Investigation

## Problem Description

**Bug**: When using AVQueuePlayer for gapless playback, auto-advance between tracks works initially but eventually stalls:
1. Track 1 → Track 2: ✅ Auto-advances, UI updates
2. Track 2 → Track 3: ❌ Auto-advances but UI doesn't update
3. Track 3 → End: ❌ Stops playing, UI doesn't show stopped state

**Root Cause**: The queue extension logic was not being triggered on auto-advance, causing the queue to eventually run out of tracks.

## Approaches Attempted

### 1. Queue Extension Calculation Fix

**Theory**: The `extendQueueWindow()` calculation was wrong after `advanceToNextItem()`.

**Changes Made**:
- Fixed calculation from `currentIndex + currentQueueSize + (i - 1)` to `currentIndex + 1 + currentQueueSize + (i - 1)`
- Added detailed logging to track queue states
- Simplified to just ensure queue has `QUEUE_WINDOW_SIZE - 1` items

**Results**: ❌ **Failed** - Still had incorrect queue extension behavior

**Files**: `PlatformMediaPlayer.kt` - `extendQueueWindow()` function

### 2. End-of-Playlist Detection

**Theory**: When the last track finishes, `currentItem` becomes null but UI doesn't update to stopped state.

**Changes Made**:
- Added detection in position updates when `currentItem == null`
- Set `isPlaying = false` when at end of playlist

**Results**: ❌ **Failed** - Didn't address the core queue extension issue

**Files**: `PlatformMediaPlayer.kt` - `startPositionUpdates()` function

### 3. Crash Fixes (Null Safety)

**Problem**: App was crashing on startup due to null reference exceptions.

**Changes Made**:
- Added null safety to `queuePlayer.items()?.size ?: 0`
- Fixed `currentEnrichedTracks?.isNotEmpty() == true` for Kotlin/Native initialization race conditions

**Results**: ✅ **Success** - Fixed crashes, but didn't solve the queue bug

### 4. Track Completion Observer Investigation

**Theory**: `setupTrackCompletionObserver()` wasn't firing for subsequent tracks after the first auto-advance.

**Root Issue Identified**:
- Observer was bound to specific `AVPlayerItem` objects
- After auto-advance, observer was still watching the OLD item, not the NEW current item
- This created a chicken-and-egg problem where we needed the observer to fire to set up the next observer

**Changes Made**:
- Added diagnostic logging to verify `handleTrackCompletion()` was never being called
- Confirmed that queue extension was never happening

**Results**: ❌ **Failed** - Identified the problem but notifications approach was fundamentally broken

### 5. KVO (Key-Value Observing) Approach

**Theory**: Use KVO to observe `queuePlayer.currentItem` changes instead of relying on end-of-track notifications.

**Attempts**:

#### Attempt 5a: Modern KVO API
```kotlin
currentItemObserver = queuePlayer.observeValueForKeyPath(
    keyPath = "currentItem",
    options = NSKeyValueObservingOptionNew or NSKeyValueObservingOptionOld
) { _, _ ->
    playerScope.launch {
        handleCurrentItemChange()
    }
}
```

**Results**: ❌ **Failed** - Compilation errors, `NSKeyValueObservation` not available

#### Attempt 5b: Traditional KVO API
```kotlin
queuePlayer.addObserver(
    observer = queuePlayer,
    forKeyPath = "currentItem",
    options = NSKeyValueObservingOptionNew or NSKeyValueObservingOptionOld,
    context = null
)
```

**Results**: ❌ **Failed** - Requires implementing `observeValueForKeyPath` which needs subclassing

**Conclusion**: KVO in Kotlin/Native is complex and requires Objective-C bridging patterns that are difficult to implement correctly.

### 6. Polling-Based Detection

**Theory**: Poll `queuePlayer.currentItem` in the position updates loop to detect when AVQueuePlayer auto-advances.

**Implementation**:
```kotlin
// Track last seen item
private var lastSeenCurrentItem: AVPlayerItem? = null

// In startPositionUpdates():
if (currentItem !== lastSeenCurrentItem) {
    val oldItem = lastSeenCurrentItem
    lastSeenCurrentItem = currentItem

    if (oldItem != null && currentItem != null) {
        handleCurrentItemChange() // Detected auto-advance
    }
}
```

**Problem Encountered**: Double-handling during manual navigation
- User presses Next → `nextTrack()` calls `advanceToNextItem()` → `currentItem` changes
- Polling detects change → calls `handleCurrentItemChange()` → tries to advance again

#### Attempt 6a: Manual Navigation Flags
```kotlin
private var isManualNavigation = false

// Set flag before manual operations
isManualNavigation = true
queuePlayer.advanceToNextItem()
// ...
isManualNavigation = false

// In polling:
if (!isManualNavigation) {
    handleCurrentItemChange()
}
```

**Results**: ❌ **Failed** - Race conditions and timing issues with async operations

#### Attempt 6b: Sync lastSeenCurrentItem After Manual Nav
```kotlin
// In nextTrack() after advanceToNextItem():
lastSeenCurrentItem = queuePlayer.currentItem

// In previousTrack() after loading new item:
lastSeenCurrentItem = queuePlayer.currentItem
```

**Results**: ❌ **Failed** - Still had edge cases and unreliable behavior

## Key Insights Discovered

1. **AVPlayerItem Notifications Are Fragile**: The `AVPlayerItemDidPlayToEndTimeNotification` approach breaks after the first auto-advance because observers are bound to specific item instances.

2. **Queue Extension Logic Is Complex**: Calculating which tracks to add to maintain the window requires careful tracking of what's actually in the AVQueuePlayer queue vs. our Kotlin state.

3. **Kotlin/Native KVO Is Difficult**: Modern KVO patterns available in Swift/Objective-C don't translate well to Kotlin/Native without significant complexity.

4. **Polling Has Race Conditions**: While conceptually simple, polling `currentItem` changes creates timing issues with manual navigation that are hard to resolve cleanly.

5. **Auto-Advance Detection Is The Core Issue**: All other fixes (queue calculation, end-of-playlist detection) were treating symptoms. The fundamental problem is reliably detecting when AVQueuePlayer auto-advances.

## Recommended Next Steps

### Option 1: Notification Center Approach
Instead of observing individual `AVPlayerItem` objects, observe queue-level changes:
```kotlin
NSNotificationCenter.defaultCenter().addObserverForName(
    name = AVPlayerItemDidPlayToEndTimeNotification,
    object = null, // Observe ALL items
    queue = NSOperationQueue.mainQueue()
) { notification ->
    // Check if the item that ended belongs to our queue
    if (notification.object == lastKnownCurrentItem) {
        handleAutoAdvance()
    }
}
```

### Option 2: Custom Player Wrapper
Create a wrapper around AVQueuePlayer that maintains its own queue state and provides reliable callbacks:
```kotlin
class ReliableQueuePlayer {
    private val avQueue = AVQueuePlayer()
    private val items = mutableListOf<AVPlayerItem>()
    private var currentIndex = 0

    fun onTrackComplete() {
        currentIndex++
        onAutoAdvance?.invoke(currentIndex)
    }
}
```

### Option 3: Periodic State Reconciliation
Instead of trying to detect the exact moment of auto-advance, periodically reconcile the player state:
```kotlin
// Every 2-3 seconds, check if player state matches our expectations
private fun reconcilePlayerState() {
    val expectedURL = currentEnrichedTracks[currentIndex].trackUrl
    val actualURL = (queuePlayer.currentItem?.asset as? AVURLAsset)?.URL?.absoluteString

    if (actualURL != expectedURL) {
        // Player diverged from our state - resync
        findAndSyncToActualTrack(actualURL)
    }
}
```

### Option 4: Hybrid Approach
Combine multiple detection methods for redundancy:
- Primary: Notification observers (if they work)
- Fallback: Polling every 1-2 seconds
- Backup: Periodic state reconciliation every 5-10 seconds

## Technical Debt Created

- **Diagnostic Logging**: Added extensive logging that should be cleaned up
- **Unused Functions**: Several helper functions were created and abandoned
- **Complexity**: Multiple failed approaches left the codebase with complex logic that doesn't work

## Files Modified

- `composeApp/src/iosMain/kotlin/com/grateful/deadly/services/media/platform/PlatformMediaPlayer.kt`
  - Added `logQueueState()` diagnostic function
  - Modified `extendQueueWindow()` multiple times
  - Added/removed KVO observer setup
  - Added/removed polling detection logic
  - Added/removed manual navigation flags

All changes have been reverted by resetting the branch.