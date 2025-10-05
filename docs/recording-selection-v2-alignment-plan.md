# Recording Selection V2 Alignment Plan

## Overview

The recording selection feature has been implemented and is functional, but needs alignment with the exact UI/UX patterns from `../dead/v2` to achieve parity. This document outlines the specific differences and implementation plan.

## Current State ✅

### What's Working:
- ✅ **Basic Modal**: RecordingSelectionSheet opens from "..." menu button
- ✅ **Cross-Platform**: Builds and runs on both Android and iOS
- ✅ **Service Layer**: RecordingSelectionService integrates with ShowService
- ✅ **State Management**: RecordingSelectionViewModel with StateFlow
- ✅ **Basic UI**: Cards display with source type, rating, technical details
- ✅ **Loading States**: Proper loading/error handling
- ✅ **AppIcon System**: Cross-platform icons (CheckCircle, Close)

### What's Not Working:
- ❌ **Selection**: Clicking cards doesn't update selection state
- ❌ **Action Buttons**: Button show/hide logic differs from V2
- ❌ **Radio Behavior**: Missing selectableGroup for proper radio-button UX
- ❌ **State Updates**: iOS not showing correct selected recording

## V2 Implementation Analysis

### Key V2 Files Analyzed:
- `/v2/feature/playlist/components/PlaylistRecordingSelectionSheet.kt`
- `/v2/feature/playlist/components/PlaylistRecordingOptionCard.kt`
- `/v2/core/model/PlaylistModels.kt`
- `/v2/feature/playlist/models/PlaylistViewModel.kt`

### V2 Patterns:

#### 1. Selection State Management
```kotlin
// V2 immediately updates UI state when recording clicked
fun selectRecording(recordingId: String) {
    // Updates isSelected flags in current state
    val updatedAlternatives = currentSelection.alternativeRecordings.map { option ->
        option.copy(isSelected = option.identifier == recordingId)
    }
    // Triggers track reload with new recording
}
```

#### 2. Modal Layout (V2 PlaylistRecordingSelectionSheet)
```kotlin
LazyColumn(
    modifier = Modifier
        .selectableGroup()  // <- MISSING: Radio button behavior
        .weight(1f, fill = false)
) {
    // Current recording first, then alternatives
}
```

#### 3. Action Button Logic (V2)
```kotlin
// Reset to Recommended: Show only when current != recommended
if (recommendedRecording != null && !currentIsRecommended && onResetToRecommended != null)

// Set as Default: Show only when different recording selected
selectedRecording?.let { selected ->
    if (selected.identifier != state.currentRecording?.identifier)
}
```

#### 4. Card Layout (V2 PlaylistRecordingOptionCard)
- Uses `CompactStarRating` component (custom 12.dp stars)
- `Icons.Default.Check` with 24.dp size
- Error color for archive ID: `MaterialTheme.colorScheme.error.copy(alpha = 0.7f)`
- Different padding and spacing patterns

## Gap Analysis

### 1. Selection Behavior Issues

**Problem**: Cards don't update selection state when clicked

**V2 Pattern**:
```kotlin
// V2 ViewModel selectRecording method
playlistService.selectRecording(recordingId)
// Immediately updates UI state with new selection
```

**Our Current Issue**:
- RecordingSelectionViewModel.selectRecording() doesn't update UI state
- Cards don't show visual selection feedback
- Missing proper state flow from service → ViewModel → UI

### 2. Missing selectableGroup

**Problem**: No radio-button group behavior

**V2 Solution**:
```kotlin
LazyColumn(
    modifier = Modifier.selectableGroup()
)
```

**Impact**: Users can't properly understand single-selection behavior

### 3. Action Button Logic Mismatch

**Problem**: Button show/hide logic differs from V2

**V2 Logic**:
- Reset: Show when `hasRecommended && !isCurrentRecommended`
- Set Default: Show when `selectedRecording != currentRecording`

**Our Current Logic**: Different conditions and button management

### 4. CompactStarRating Missing

**Problem**: Using text rating instead of V2's visual star component

**V2 Component**: `CompactStarRating(rating = rating, starSize = 12.dp)`

**Our Current**: Text-based rating display

## Implementation Plan

### Phase 1: Fix Selection State Management 🔧

#### 1.1 Update RecordingSelectionViewModel
```kotlin
fun handleAction(action: RecordingSelectionAction.SelectRecording) {
    // Immediately update state like V2
    val currentState = _state.value
    val updatedRecordings = currentState.allRecordings.map { recording ->
        recording.copy(isSelected = recording.identifier == action.recordingId)
    }
    // Update state and trigger service
}
```

#### 1.2 Fix Service State Updates
- Ensure getRecordingOptions returns proper isSelected states
- Add current recording ID tracking and comparison logic

### Phase 2: Add selectableGroup + V2 Layout 🎨

#### 2.1 Update RecordingSelectionSheet
```kotlin
LazyColumn(
    modifier = Modifier
        .selectableGroup()  // Add this
        .weight(1f, fill = false)
)
```

#### 2.2 Fix Action Button Logic
```kotlin
// Reset to Recommended
if (state.hasRecommended && !state.isCurrentRecommended) {
    OutlinedButton(onClick = {
        onAction(RecordingSelectionAction.ResetToRecommended)
        onDismiss() // V2 pattern
    })
}

// Set as Default
state.selectedRecording?.let { selected ->
    if (selected.identifier != state.currentRecording?.identifier) {
        Button(onClick = {
            onAction(RecordingSelectionAction.SetAsDefault(selected.identifier))
            onDismiss() // V2 pattern
        })
    }
}
```

### Phase 3: Update RecordingOptionCard to V2 Specs 🎨

#### 3.1 Create CompactStarRating Component
```kotlin
@Composable
fun CompactStarRating(
    rating: Float,
    starSize: Dp = 12.dp
) {
    // Implementation matching V2's visual stars
}
```

#### 3.2 Fix Card Layout
- Update padding to match V2
- Use Material Icons.Default.Check (24.dp)
- Apply error color for archive ID
- Match V2 spacing and typography

### Phase 4: Cross-Platform State Consistency 🔧

#### 4.1 Test iOS State Updates
- Verify selection highlighting works on iOS
- Test action button functionality
- Ensure proper modal dismissal

#### 4.2 Add State Debugging
- Add logging for state changes
- Verify proper StateFlow updates
- Test cross-platform icon rendering

### Phase 5: Integration Testing ✅

#### 5.1 End-to-End Selection Flow
- [ ] Click card → see selection highlight
- [ ] Select different recording → see state update
- [ ] Click "Set as Default" → modal dismisses, recording changes
- [ ] Test "Reset to Recommended" functionality

#### 5.2 Cross-Platform Testing
- [ ] Android: Full flow works
- [ ] iOS: Full flow works
- [ ] State consistency between platforms
- [ ] Icon rendering consistency

## Success Criteria

### UI/UX Parity with V2:
- ✅ Cards show immediate selection feedback when clicked
- ✅ Radio-button group behavior with selectableGroup
- ✅ Action buttons show/hide with V2-exact logic
- ✅ Visual design matches V2 (stars, spacing, colors)
- ✅ Cross-platform state consistency

### Technical Requirements:
- ✅ Proper StateFlow updates for reactive UI
- ✅ Service layer maintains session-only state like V2
- ✅ Clean separation of concerns (UI ↔ ViewModel ↔ Service)
- ✅ Cross-platform icon compatibility maintained

## Files to Modify

### Phase 1 - State Management:
- `RecordingSelectionViewModel.kt` - Fix selection action handling
- `RecordingSelectionService.kt` - Add proper state tracking

### Phase 2 - UI Layout:
- `RecordingSelectionSheet.kt` - Add selectableGroup, fix button logic
- Create `CompactStarRating.kt` - V2-style star component

### Phase 3 - Card Updates:
- `RecordingOptionCard.kt` - Match V2 layout, colors, and icons

### Phase 4 - Testing:
- Build and test on both Android and iOS
- Verify full selection flow works end-to-end

## Timeline Estimate

- **Phase 1**: 2-3 hours (state management fixes)
- **Phase 2**: 2-3 hours (UI layout updates)
- **Phase 3**: 1-2 hours (card styling)
- **Phase 4**: 1-2 hours (cross-platform testing)

**Total**: ~6-10 hours for complete V2 parity

## Notes

- Current implementation provides solid foundation
- Main issues are in state management and UI details
- V2 analysis shows clear patterns to follow
- Cross-platform architecture is sound, just needs state flow fixes