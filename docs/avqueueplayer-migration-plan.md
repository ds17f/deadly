# AVQueuePlayer Migration Plan

## Overview

Replace AVPlayer with AVQueuePlayer for native gapless playback. Clean migration - rip out old, build new.

## Current Architecture (AVPlayer)

**What we're removing:**
- Single AVPlayer with manual track switching
- Manual prefetch mechanism (`nextPlayerItem`)
- Manual track completion handling
- `loadAndPlay(url: String)` single-track approach

**What we're keeping:**
- Kotlin as source of truth (`currentEnrichedTracks` list)
- Index tracking (`currentEnrichedTrackIndex`)
- `PlatformPlaybackState` flow
- **`currentTrackIndex` flow** - critical for UI synchronization
- MPNowPlayingInfoCenter integration
- Remote command setup

## AVQueuePlayer Architecture

### Core Pattern: Kotlin-Managed Queue

**Source of Truth:**
- Kotlin maintains: `currentEnrichedTracks: List<EnrichedTrack>`
- Kotlin maintains: `currentEnrichedTrackIndex: Int`
- AVQueuePlayer queue is a **view** of this data, not the source

**Queue Window Pattern:**
- Build small queue window (current + next 2-3 tracks)
- Rebuild queue when user goes backwards
- Extend queue as we approach the end

**Why Queue Window?**
- Smaller queue = faster rebuilds for previous track
- Less memory usage
- Easier to manage and debug

## UI Synchronization Strategy

### The Critical Flow Chain

Every track change MUST trigger this sequence:

```kotlin
// 1. Update Kotlin source of truth
currentEnrichedTrackIndex = newIndex

// 2. Emit to flow (PlayerViewModel observes this and updates UI)
_currentTrackIndex.value = newIndex

// 3. Update platform metadata
val track = currentEnrichedTracks[newIndex]
setTrackMetadata(track.track, track.recordingId)
updateNowPlayingInfo()
```

### Why This Matters

**Manual navigation (next/previous):**
```
User clicks → Update index → Emit flow → UI updates → Lock screen updates
```

**Auto-advance (track completion):**
```
Track ends → AVQueuePlayer advances → Sync index → Emit flow → UI updates
```

**If we miss the flow emission:** UI shows wrong track while correct track plays = bad UX

## Key Challenges & Solutions

### Challenge 1: Previous Track Behavior (Restart vs Go Back)

**Standard music player behavior:**
- **> 3 seconds into track**: Previous button → restart current track (seek to 0)
- **≤ 3 seconds into track**: Previous button → go to actual previous track

**Implementation:**
```kotlin
actual suspend fun previousTrack(): Result<Unit> = withContext(Dispatchers.Main) {
    try {
        // Check current position
        val currentTime = queuePlayer.currentTime()
        val currentSeconds = CMTimeGetSeconds(currentTime)

        // If more than 3 seconds in, restart current track
        if (currentSeconds > PREVIOUS_TRACK_THRESHOLD_SECONDS) {
            seekTo(0)
            return@withContext Result.success(Unit)
        }

        // Otherwise go to actual previous track
        val currentIndex = currentEnrichedTrackIndex
        if (currentIndex <= 0) {
            // At first track, just restart it
            seekTo(0)
            return@withContext Result.success(Unit)
        }

        // Show loading state
        updatePlaybackState { copy(isLoading = true) }

        // Calculate previous index
        val prevIndex = currentIndex - 1

        // Rebuild queue from previous index
        queuePlayer.removeAllItems()
        val items = buildQueueWindow(fromIndex = prevIndex)
        items.forEach { queuePlayer.insertItem(it, afterItem = null) }

        // CRITICAL: Update index and emit to flow for UI sync
        currentEnrichedTrackIndex = prevIndex
        _currentTrackIndex.value = prevIndex

        // Update metadata for platform integrations
        val prevTrack = currentEnrichedTracks[prevIndex]
        setTrackMetadata(prevTrack.track, prevTrack.recordingId)
        updateNowPlayingInfo()

        // Play
        queuePlayer.play()

        // Clear loading state
        updatePlaybackState { copy(isLoading = false) }

        // Setup observer for new current item
        setupTrackCompletionObserver()

        Result.success(Unit)
    } catch (e: Exception) {
        updatePlaybackState { copy(isLoading = false, error = e.message) }
        Result.failure(e)
    }
}

companion object {
    private const val PREVIOUS_TRACK_THRESHOLD_SECONDS = 3.0
}
```

### Challenge 2: No Previous Track in AVQueuePlayer
**Problem:** AVQueuePlayer removes items after playback, can't go backwards

**Solution:** Rebuild queue from previous index (see above)

### Challenge 3: UI Lockups During Queue Rebuild
**Problem:** Queue operations on main thread can freeze UI

**Solution:**
1. Keep queue window small (fast rebuilds)
2. Show loading state during rebuild
3. All operations in single `withContext(Dispatchers.Main)` block
4. Optimize AVPlayerItem creation

### Challenge 4: State Synchronization
**Problem:** AVQueuePlayer queue state can desync from Kotlin state, UI shows wrong track

**Solution:**
- Kotlin index is authoritative
- **Always emit to `_currentTrackIndex` flow after index change**
- PlayerViewModel observes flow and updates UI
- If desync detected, rebuild from Kotlin index

### Challenge 5: Downloaded Media
**Problem:** Will this work with local files?

**Solution:** ✅ Yes, AVQueuePlayer works with any URL
- Remote: `https://archive.org/.../track.mp3`
- Local: `file:///path/to/downloaded/track.mp3`
- Same code path, no changes needed

## Implementation Plan

### Phase 1: Replace AVPlayer with AVQueuePlayer Core

**Changes:**
1. Replace `private val avPlayer = AVPlayer()` with `private val queuePlayer = AVQueuePlayer()`
2. Remove prefetch mechanism (`nextPlayerItem` and `prefetchNextTrack()`)
3. Update all `avPlayer` references to `queuePlayer`
4. Add `PREVIOUS_TRACK_THRESHOLD_SECONDS` constant

**New Helper Functions:**
```kotlin
private fun buildQueueWindow(fromIndex: Int, windowSize: Int = 3): List<AVPlayerItem> {
    val endIndex = minOf(fromIndex + windowSize, currentEnrichedTracks.size)
    return (fromIndex until endIndex).map { index ->
        val track = currentEnrichedTracks[index]
        val url = NSURL.URLWithString(track.trackUrl)!!
        AVPlayerItem.playerItemWithURL(url)
    }
}

private suspend fun rebuildQueue(fromIndex: Int) = withContext(Dispatchers.Main) {
    queuePlayer.removeAllItems()
    val items = buildQueueWindow(fromIndex)
    items.forEach { item ->
        queuePlayer.insertItem(item, afterItem = null)
    }
}
```

**Test:** Basic playback works

---

### Phase 2: Implement loadAndPlayPlaylist() with Queue

**Replace current implementation:**
```kotlin
actual suspend fun loadAndPlayPlaylist(
    enrichedTracks: List<EnrichedTrack>,
    startIndex: Int
): Result<Unit> = withContext(Dispatchers.Main) {
    try {
        if (enrichedTracks.isEmpty() || startIndex !in enrichedTracks.indices) {
            return@withContext Result.failure(Exception("Invalid playlist"))
        }

        // Store playlist state
        currentEnrichedTracks = enrichedTracks
        currentEnrichedTrackIndex = startIndex

        // CRITICAL: Emit to flow for UI sync
        _currentTrackIndex.value = startIndex

        // Set metadata
        val startTrack = enrichedTracks[startIndex]
        setTrackMetadata(startTrack.track, startTrack.recordingId)

        // Build queue window from start index
        queuePlayer.removeAllItems()
        val items = buildQueueWindow(fromIndex = startIndex)
        items.forEach { queuePlayer.insertItem(it, afterItem = null) }

        // Play
        queuePlayer.play()

        // Setup completion observer for first item
        setupTrackCompletionObserver()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Remove:** `loadAndPlay(url: String)` - no longer needed

**Test:** Playlist loads and plays forward automatically

---

### Phase 3: Implement Next Track

**Replace current implementation:**
```kotlin
actual suspend fun nextTrack(): Result<Unit> = withContext(Dispatchers.Main) {
    try {
        val currentIndex = currentEnrichedTrackIndex
        if (currentIndex >= currentEnrichedTracks.size - 1) {
            return@withContext Result.failure(Exception("No next track"))
        }

        // Advance to next item (AVQueuePlayer handles this natively)
        queuePlayer.advanceToNextItem()

        // CRITICAL: Update index and emit to flow for UI sync
        val nextIndex = currentIndex + 1
        currentEnrichedTrackIndex = nextIndex
        _currentTrackIndex.value = nextIndex

        // Update metadata for platform integrations
        val nextTrack = currentEnrichedTracks[nextIndex]
        setTrackMetadata(nextTrack.track, nextTrack.recordingId)
        updateNowPlayingInfo()

        // Extend queue if needed (approaching end of window)
        extendQueueIfNeeded()

        // Setup observer for new current item
        setupTrackCompletionObserver()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**New Helper:**
```kotlin
private suspend fun extendQueueIfNeeded() = withContext(Dispatchers.Main) {
    val itemsInQueue = queuePlayer.items().size
    val remainingInPlaylist = currentEnrichedTracks.size - currentEnrichedTrackIndex

    // If only 1 item left in queue and more tracks available, add next
    if (itemsInQueue <= 1 && remainingInPlaylist > 1) {
        val nextIndex = currentEnrichedTrackIndex + 1
        val nextTrack = currentEnrichedTracks[nextIndex]
        val url = NSURL.URLWithString(nextTrack.trackUrl)!!
        val item = AVPlayerItem.playerItemWithURL(url)
        queuePlayer.insertItem(item, afterItem = null)
    }
}
```

**Test:** Next track works, queue extends properly, UI updates correctly

---

### Phase 4: Implement Previous Track (High Risk)

**Implementation with restart behavior (see Challenge 1 above)**

**Test:**
- Previous track works without UI lockup
- Restart behavior (>3 sec → restart, ≤3 sec → previous)
- UI updates correctly with new track info

---

### Phase 5: Update Auto-Advance Logic

**Replace `handleTrackCompletion()`:**
```kotlin
private suspend fun handleTrackCompletion() {
    try {
        val currentIndex = currentEnrichedTrackIndex

        // Check if there's a next track
        if (currentIndex < currentEnrichedTracks.size - 1) {
            // AVQueuePlayer automatically advances to next item
            // We need to sync our state and UI

            withContext(Dispatchers.Main) {
                val nextIndex = currentIndex + 1

                // CRITICAL: Update index and emit to flow for UI sync
                currentEnrichedTrackIndex = nextIndex
                _currentTrackIndex.value = nextIndex

                // Update metadata for all platform integrations
                val nextTrack = currentEnrichedTracks[nextIndex]
                setTrackMetadata(nextTrack.track, nextTrack.recordingId)
                updateNowPlayingInfo()

                // Extend queue if needed
                extendQueueIfNeeded()

                // Setup observer for new current item
                setupTrackCompletionObserver()

                Logger.d(TAG, "🎵 Auto-advanced to track $nextIndex: ${nextTrack.displayTitle}")
            }
        } else {
            // End of playlist
            Logger.d(TAG, "🎵 Playlist complete")
            updatePlaybackState { copy(isPlaying = false) }
        }
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to handle track completion", e)
    }
}
```

**Critical:** This is where auto-advance UI sync happens. Missing the flow emission here means UI shows wrong track.

**Test:**
- Automatic progression through playlist
- UI updates correctly on auto-advance
- Lock screen shows correct track info

---

### Phase 6: Update Observers

**Track Completion Observer:**
- Change from observing AVPlayerItem to observing AVQueuePlayer's current item
- Re-register observer when current item changes

```kotlin
private fun setupTrackCompletionObserver() {
    // Clean up old observer
    playerItemEndObserver?.let { observer ->
        NSNotificationCenter.defaultCenter().removeObserver(observer)
        playerItemEndObserver = null
    }

    // Observe current item (queuePlayer.currentItem)
    val currentItem = queuePlayer.currentItem ?: return

    playerItemEndObserver = NSNotificationCenter.defaultCenter().addObserverForName(
        name = AVPlayerItemDidPlayToEndTimeNotification,
        `object` = currentItem,
        queue = NSOperationQueue.mainQueue()
    ) { _ ->
        playerScope.launch {
            handleTrackCompletion()
        }
    }
}
```

---

### Phase 7: Update Other Methods

**Simple updates (replace `avPlayer` with `queuePlayer`):**
- `pause()` - just use `queuePlayer.pause()`
- `resume()` - just use `queuePlayer.play()`
- `seekTo()` - just use `queuePlayer.seekToTime()`
- `stop()` - pause and `queuePlayer.removeAllItems()`
- `release()` - cleanup observers
- `updateNowPlayingInfo()` - use `queuePlayer.currentItem`
- `startPositionUpdates()` - use `queuePlayer.currentItem`

---

## UI Synchronization Validation

### Add Validation Method

```kotlin
private fun validateSync() {
    val ourIndex = currentEnrichedTrackIndex
    val currentItem = queuePlayer.currentItem

    // Detect desync issues
    if (currentItem == null && currentEnrichedTracks.isNotEmpty()) {
        Logger.e(TAG, "⚠️ DESYNC: No current item but we have tracks at index $ourIndex")
        // Could trigger auto-recovery here
    }

    // Validate flow emission
    if (_currentTrackIndex.value != ourIndex) {
        Logger.e(TAG, "⚠️ DESYNC: Flow value ${_currentTrackIndex.value} != actual index $ourIndex")
    }
}
```

### PlayerViewModel Integration

The ViewModel should observe the flow:

```kotlin
// In PlayerViewModel (commonMain)
init {
    viewModelScope.launch {
        platformMediaPlayer.currentTrackIndex.collect { index ->
            // Update UI state with new track info
            val newTrack = enrichedTracks.getOrNull(index)
            _uiState.value = _uiState.value.copy(
                currentTrackIndex = index,
                currentTrack = newTrack,
                currentTrackTitle = newTrack?.displayTitle ?: "",
                currentTrackArtist = newTrack?.displayArtist ?: "",
                // ... other UI updates
            )
            Logger.d(TAG, "UI updated for track $index: ${newTrack?.displayTitle}")
        }
    }
}
```

## Edge Cases to Handle

### 1. First Track Previous
**Behavior:**
- **> 3 seconds:** Restart current track (seek to 0)
- **≤ 3 seconds:** Still restart (no previous track exists)

```kotlin
if (currentIndex == 0) {
    // Always restart when at first track
    seekTo(0)
    return Result.success(Unit)
}
```

### 2. Last Track Next
**Behavior:** Stop playback (don't loop)

```kotlin
if (currentIndex >= currentEnrichedTracks.size - 1) {
    // At last track - stop playback
    stop()
    return Result.failure(Exception("End of playlist"))
}
```

### 3. Single Track Playlist
**Behavior:** Disable next/previous, allow repeat by seeking to 0

```kotlin
if (currentEnrichedTracks.size == 1) {
    // No queue needed, just single item
    // Previous = restart
    // Next = disabled or restart
}
```

### 4. Empty Queue Recovery
**Behavior:** If queue unexpectedly empty, rebuild from Kotlin state

```kotlin
if (queuePlayer.items().isEmpty() && currentEnrichedTracks.isNotEmpty()) {
    Logger.w(TAG, "Queue empty, rebuilding from Kotlin state")
    rebuildQueue(fromIndex = currentEnrichedTrackIndex)
}
```

## Testing Checklist

### Basic Playback
- [ ] Load playlist and play first track
- [ ] Auto-advance to next track (gapless)
- [ ] **UI shows correct track on auto-advance**
- [ ] Manual next track
- [ ] **UI shows correct track on manual next**
- [ ] Manual previous track (restart behavior)
- [ ] **UI shows correct track on manual previous**
- [ ] Seek within track
- [ ] Pause/resume

### Previous Track Restart Behavior
- [ ] >3 seconds into track, press previous → restarts track
- [ ] ≤3 seconds into track, press previous → goes to previous track
- [ ] At first track >3 seconds, press previous → restarts track
- [ ] At first track ≤3 seconds, press previous → restarts track

### Edge Cases
- [ ] First track - previous behavior
- [ ] Last track - next behavior
- [ ] Single track playlist
- [ ] Rapid next/previous navigation
- [ ] Previous at track boundaries

### UI Synchronization
- [ ] **Track title updates on auto-advance**
- [ ] **Track title updates on manual next**
- [ ] **Track title updates on manual previous**
- [ ] **Lock screen shows correct track**
- [ ] **Track index displayed correctly**
- [ ] **No lag between audio and UI update**

### Integration
- [ ] MPNowPlayingInfoCenter updates correctly
- [ ] Remote commands (lock screen) work
- [ ] State synchronization maintained
- [ ] No UI lockups on previous track

### Downloaded Media
- [ ] Remote URLs work (existing)
- [ ] Local file:// URLs work
- [ ] Mixed remote/local playlist works

## Performance Considerations

### Queue Window Size
- **Start with 3** (current + next 2)
- Monitor memory usage
- Adjust if needed

### Previous Track Threshold
- **Start with 3 seconds**
- Configurable via constant
- Consider making user-configurable later

### Rebuild Optimization
- **Keep it simple first** - removeAll + rebuild
- Profile if slow
- Consider item reuse if needed

### Memory Management
- AVPlayerItem auto-released by AVQueuePlayer
- No manual cleanup needed
- Monitor for leaks in testing

## Constants to Add

```kotlin
companion object {
    private const val TAG = "PlatformMediaPlayer"
    private const val POSITION_UPDATE_INTERVAL_MS = 1000L
    private const val MAX_RETRIES = 3
    private const val QUEUE_WINDOW_SIZE = 3
    private const val PREVIOUS_TRACK_THRESHOLD_SECONDS = 3.0
}
```

## Rollback Plan

**If migration fails:**
1. Git revert to previous AVPlayer implementation
2. Document specific issues encountered
3. Reassess approach

**Rollback triggers:**
- Unresolvable UI lockups
- State desync issues that can't be fixed
- UI not updating correctly
- Worse gapless than before
- Memory leaks

## Success Criteria

**Must have:**
- ✅ Gapless forward playback
- ✅ Working previous track (no UI freeze)
- ✅ Restart vs go-back behavior (3 second threshold)
- ✅ **UI updates correctly on all track changes**
- ✅ Correct metadata updates (lock screen, MPNowPlayingInfoCenter)
- ✅ Remote controls functional
- ✅ Works with local files

**Nice to have:**
- ⭐ Better gapless than current
- ⭐ Cleaner code
- ⭐ Easier to maintain

## Questions & Answers

1. **Queue window size:** 3 tracks (current + next 2)

2. **Previous at first track:** Restart current track (seek to 0)

3. **Previous track threshold:** 3 seconds
   - >3 sec = restart
   - ≤3 sec = go to previous

4. **Next at last track:** Stop playback

5. **Loading indicator on previous:** Show brief loading state when rebuilding queue

6. **Empty queue recovery:** Auto-rebuild with logging

## Next Steps

1. ✅ Review and approve this plan
2. Create feature branch: `feat/avqueueplayer-migration`
3. Implement phases 1-7 sequentially
4. Test thoroughly after each phase
5. **Pay special attention to UI sync (flow emissions)**
6. Single commit when complete and tested
7. Create PR with before/after comparison

## Timeline

**Estimated:** 6-8 hours
- Phases 1-3: 2 hours (basic queue setup)
- Phase 4: 2-3 hours (previous track with restart behavior - most critical)
- Phases 5-7: 2-3 hours (auto-advance, UI sync, and polish)

**Strategy:** Implement in one session to maintain context and momentum

## Critical Reminders

🚨 **Never forget to emit to `_currentTrackIndex` flow after changing `currentEnrichedTrackIndex`**

🚨 **UI sync happens via flow - PlayerViewModel observes it and updates UI**

🚨 **Test UI updates after every track change (manual and auto)**
