package com.grateful.deadly.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * DensityDebugger - Debug utility for cross-platform density differences
 *
 * Displays density, font scale, and pixel conversion values to help identify
 * platform-specific rendering differences between Android and iOS.
 */
@Composable
fun DensityDebugger(
    modifier: Modifier = Modifier,
    visible: Boolean = false
) {
    if (!visible) return

    val density = LocalDensity.current

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(AppDimens.S)
    ) {
        Text(
            text = "DENSITY DEBUG",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(AppDimens.XS))

        Text(
            text = "Density: ${density.density}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "Font Scale: ${density.fontScale}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "24dp = ${with(density) { 24.dp.toPx() }}px",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "16dp = ${with(density) { 16.dp.toPx() }}px",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "12dp = ${with(density) { AppDimens.M.toPx() }}px",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * VisualSpacingDebugger - Shows spacing bounds visually
 *
 * Adds colored backgrounds to spacing elements to debug layout issues
 */
@Composable
fun Modifier.debugSpacing(
    color: Color = Color.Red.copy(alpha = 0.3f),
    enabled: Boolean = false
): Modifier {
    return if (enabled) {
        this.background(color)
    } else {
        this
    }
}