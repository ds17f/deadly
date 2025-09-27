package com.grateful.deadly.core.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * AppTypography - Unified typography system for cross-platform consistency
 *
 * Ensures consistent text rendering, baseline alignment, and line height behavior
 * across Android and iOS platforms. Uses explicit baseline shift and line height
 * settings to normalize platform differences.
 */
object AppTypography {

    /**
     * Baseline shift for consistent text alignment across platforms
     * Helps normalize differences between Material Symbols and SF Symbols
     */
    private val UnifiedBaselineShift = BaselineShift(0.0f)

    /**
     * Line height style for consistent text layout
     * Uses trimmed line height to avoid platform-specific spacing differences
     */
    private val UnifiedLineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )

    /**
     * Cross-platform typography definition
     * Based on Material Design with explicit baseline and line height control
     */
    val Typography = Typography(
        // Display styles
        displayLarge = Typography().displayLarge.copy(
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        ),
        displayMedium = Typography().displayMedium.copy(
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        ),
        displaySmall = Typography().displaySmall.copy(
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        ),

        // Headline styles
        headlineLarge = Typography().headlineLarge.copy(
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        ),
        headlineMedium = Typography().headlineMedium.copy(
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        ),
        headlineSmall = Typography().headlineSmall.copy(
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        ),

        // Title styles (most commonly used in our app)
        titleLarge = Typography().titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        ),
        titleMedium = Typography().titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        ),
        titleSmall = Typography().titleSmall.copy(
            fontWeight = FontWeight.Medium,
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        ),

        // Body styles
        bodyLarge = Typography().bodyLarge.copy(
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        ),
        bodyMedium = Typography().bodyMedium.copy(
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        ),
        bodySmall = Typography().bodySmall.copy(
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        ),

        // Label styles
        labelLarge = Typography().labelLarge.copy(
            fontWeight = FontWeight.Medium,
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        ),
        labelMedium = Typography().labelMedium.copy(
            fontWeight = FontWeight.Medium,
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        ),
        labelSmall = Typography().labelSmall.copy(
            fontWeight = FontWeight.Medium,
            baselineShift = UnifiedBaselineShift,
            lineHeightStyle = UnifiedLineHeightStyle
        )
    )

    /**
     * Semantic typography styles for common use cases
     * Maps design intent to actual typography styles
     */
    object Semantic {
        val ShowTitle = Typography.titleLarge
        val ShowSubtitle = Typography.titleMedium
        val CardTitle = Typography.titleMedium
        val CardSubtitle = Typography.bodyMedium
        val Rating = Typography.titleMedium  // For rating numbers
        val Button = Typography.labelLarge
        val Caption = Typography.bodySmall
    }
}