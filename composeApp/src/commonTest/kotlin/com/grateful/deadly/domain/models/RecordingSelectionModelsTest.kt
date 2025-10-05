package com.grateful.deadly.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordingSelectionModelsTest {

    // Test RecordingOptionViewModel

    @Test
    fun `RecordingOptionViewModel should format rating correctly with valid data`() {
        val recording = RecordingOptionViewModel(
            identifier = "gd1977-05-08.sbd.hicks",
            sourceType = "SBD",
            taperInfo = null,
            technicalDetails = null,
            rating = 4.2f,
            reviewCount = 15,
            rawSource = null,
            rawLineage = null,
            isSelected = false,
            isCurrent = false,
            isRecommended = false,
            matchReason = null
        )

        assertEquals("4.2★ (15)", recording.displayRating)
        assertTrue(recording.hasRating)
    }

    @Test
    fun `RecordingOptionViewModel should handle null rating`() {
        val recording = RecordingOptionViewModel(
            identifier = "gd1977-05-08.sbd.hicks",
            sourceType = "SBD",
            taperInfo = null,
            technicalDetails = null,
            rating = null,
            reviewCount = 15,
            rawSource = null,
            rawLineage = null,
            isSelected = false,
            isCurrent = false,
            isRecommended = false,
            matchReason = null
        )

        assertNull(recording.displayRating)
        assertFalse(recording.hasRating)
    }

    @Test
    fun `RecordingOptionViewModel should handle zero rating`() {
        val recording = RecordingOptionViewModel(
            identifier = "gd1977-05-08.sbd.hicks",
            sourceType = "SBD",
            taperInfo = null,
            technicalDetails = null,
            rating = 0.0f,
            reviewCount = 0,
            rawSource = null,
            rawLineage = null,
            isSelected = false,
            isCurrent = false,
            isRecommended = false,
            matchReason = null
        )

        assertNull(recording.displayRating)
        assertFalse(recording.hasRating)
    }

    @Test
    fun `RecordingOptionViewModel should format displayIdentifier correctly`() {
        val fullIdentifier = "gd1977-05-08.sbd.hicks.4982.sbeok.shnf"
        val recording = RecordingOptionViewModel(
            identifier = fullIdentifier,
            sourceType = "SBD",
            taperInfo = null,
            technicalDetails = null,
            rating = null,
            reviewCount = null,
            rawSource = null,
            rawLineage = null,
            isSelected = false,
            isCurrent = false,
            isRecommended = false,
            matchReason = null
        )

        assertEquals("shnf", recording.displayIdentifier)
    }

    @Test
    fun `RecordingOptionViewModel should handle simple identifier`() {
        val simpleIdentifier = "simple-id"
        val recording = RecordingOptionViewModel(
            identifier = simpleIdentifier,
            sourceType = "SBD",
            taperInfo = null,
            technicalDetails = null,
            rating = null,
            reviewCount = null,
            rawSource = null,
            rawLineage = null,
            isSelected = false,
            isCurrent = false,
            isRecommended = false,
            matchReason = null
        )

        assertEquals("simple-id", recording.displayIdentifier)
    }

    @Test
    fun `RecordingOptionViewModel displayTitle should return sourceType`() {
        val recording = RecordingOptionViewModel(
            identifier = "test",
            sourceType = "Soundboard",
            taperInfo = null,
            technicalDetails = null,
            rating = null,
            reviewCount = null,
            rawSource = null,
            rawLineage = null,
            isSelected = false,
            isCurrent = false,
            isRecommended = false,
            matchReason = null
        )

        assertEquals("Soundboard", recording.displayTitle)
    }

    // Test RecordingSelectionState

    @Test
    fun `RecordingSelectionState should return all recordings correctly`() {
        val currentRecording = createTestRecording("current", isCurrent = true)
        val alternativeRecording1 = createTestRecording("alt1")
        val alternativeRecording2 = createTestRecording("alt2")

        val state = RecordingSelectionState(
            currentRecording = currentRecording,
            alternativeRecordings = listOf(alternativeRecording1, alternativeRecording2)
        )

        assertEquals(3, state.allRecordings.size)
        assertEquals(currentRecording, state.allRecordings[0])
        assertTrue(state.allRecordings.contains(alternativeRecording1))
        assertTrue(state.allRecordings.contains(alternativeRecording2))
    }

    @Test
    fun `RecordingSelectionState should handle null current recording`() {
        val alternativeRecording = createTestRecording("alt1")

        val state = RecordingSelectionState(
            currentRecording = null,
            alternativeRecordings = listOf(alternativeRecording)
        )

        assertEquals(1, state.allRecordings.size)
        assertEquals(alternativeRecording, state.allRecordings[0])
    }

    @Test
    fun `RecordingSelectionState should detect new selection correctly`() {
        val currentRecording = createTestRecording("current", isCurrent = true, isSelected = false)
        val selectedRecording = createTestRecording("selected", isSelected = true)

        val state = RecordingSelectionState(
            currentRecording = currentRecording,
            alternativeRecordings = listOf(selectedRecording)
        )

        assertTrue(state.hasNewSelection)
        assertEquals(selectedRecording, state.selectedRecording)
    }

    @Test
    fun `RecordingSelectionState should not detect new selection when current is selected`() {
        val currentRecording = createTestRecording("current", isCurrent = true, isSelected = true)
        val alternativeRecording = createTestRecording("alt", isSelected = false)

        val state = RecordingSelectionState(
            currentRecording = currentRecording,
            alternativeRecordings = listOf(alternativeRecording)
        )

        assertFalse(state.hasNewSelection)
        assertEquals(currentRecording, state.selectedRecording)
    }

    @Test
    fun `RecordingSelectionState should detect current is recommended`() {
        val currentRecording = createTestRecording("current", isCurrent = true, isRecommended = true)

        val state = RecordingSelectionState(
            currentRecording = currentRecording,
            hasRecommended = true
        )

        assertTrue(state.isCurrentRecommended)
        assertFalse(state.shouldShowResetToRecommended)
    }

    @Test
    fun `RecordingSelectionState should show reset when current is not recommended`() {
        val currentRecording = createTestRecording("current", isCurrent = true, isRecommended = false)

        val state = RecordingSelectionState(
            currentRecording = currentRecording,
            hasRecommended = true
        )

        assertFalse(state.isCurrentRecommended)
        assertTrue(state.shouldShowResetToRecommended)
    }

    @Test
    fun `RecordingSelectionState should not show reset when no recommended exists`() {
        val currentRecording = createTestRecording("current", isCurrent = true, isRecommended = false)

        val state = RecordingSelectionState(
            currentRecording = currentRecording,
            hasRecommended = false
        )

        assertFalse(state.shouldShowResetToRecommended)
    }

    @Test
    fun `RecordingSelectionState should show set as default when new selection exists`() {
        val currentRecording = createTestRecording("current", isCurrent = true, isSelected = false)
        val selectedRecording = createTestRecording("selected", isSelected = true)

        val state = RecordingSelectionState(
            currentRecording = currentRecording,
            alternativeRecordings = listOf(selectedRecording)
        )

        assertTrue(state.shouldShowSetAsDefault)
    }

    @Test
    fun `RecordingSelectionState should not show set as default when no new selection`() {
        val currentRecording = createTestRecording("current", isCurrent = true, isSelected = true)
        val alternativeRecording = createTestRecording("alt", isSelected = false)

        val state = RecordingSelectionState(
            currentRecording = currentRecording,
            alternativeRecordings = listOf(alternativeRecording)
        )

        assertFalse(state.shouldShowSetAsDefault)
    }

    // Helper function to create test recordings
    private fun createTestRecording(
        identifier: String,
        sourceType: String = "SBD",
        rating: Float? = null,
        reviewCount: Int? = null,
        isSelected: Boolean = false,
        isCurrent: Boolean = false,
        isRecommended: Boolean = false
    ): RecordingOptionViewModel {
        return RecordingOptionViewModel(
            identifier = identifier,
            sourceType = sourceType,
            taperInfo = null,
            technicalDetails = null,
            rating = rating,
            reviewCount = reviewCount,
            rawSource = null,
            rawLineage = null,
            isSelected = isSelected,
            isCurrent = isCurrent,
            isRecommended = isRecommended,
            matchReason = null
        )
    }
}