package com.grateful.deadly.core.design.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.grateful.deadly.R

private val MaterialSymbols = FontFamily(
    Font(R.font.material_symbols_outlined)
)

/**
 * Android implementation using Material Symbols font
 */
@Composable
actual fun AppIcon.Render(size: Dp) {
    val codepoint = when (this) {
        AppIcon.QrCodeScanner -> "\uE4C6"  // qr_code_scanner
        AppIcon.Home -> "\uE88A"           // home
        AppIcon.Settings -> "\uE8B8"       // settings
        AppIcon.Search -> "\uE8B6"         // search
    }

    Text(
        text = codepoint,
        fontFamily = MaterialSymbols,
        fontSize = size.value.sp,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.size(size)
    )
}