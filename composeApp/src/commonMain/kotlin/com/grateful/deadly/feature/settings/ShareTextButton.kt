package com.grateful.deadly.feature.settings

import androidx.compose.runtime.Composable

/**
 * Platform-specific composable that shows a button triggering the native share sheet
 * with the given text content.
 */
@Composable
expect fun ShareTextButton(
    label: String,
    content: String,
    filename: String,
    enabled: Boolean = true
)
