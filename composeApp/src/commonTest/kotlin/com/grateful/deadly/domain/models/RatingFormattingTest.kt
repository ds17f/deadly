package com.grateful.deadly.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals

class RatingFormattingTest {

    @Test
    fun `should format rating correctly`() {
        val recording = RecordingOptionViewModel(
            identifier = "test",
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

        // Test what the actual output is
        println("Actual rating display: '${recording.displayRating}'")

        // This will show us what the formatting actually produces
        val expected = "4.2★ (15)"
        assertEquals(expected, recording.displayRating)
    }
}