package com.grateful.deadly.core.design.icons

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import deadly.composeapp.generated.resources.Res
import deadly.composeapp.generated.resources.material_symbols_outlined
import org.jetbrains.compose.resources.Font

/**
 * iOS implementation using Material Design Icons via Compose resources
 * Uses the new Compose Multiplatform resource system for consistent cross-platform icons
 */
@Composable
actual fun AppIcon.Render(size: Dp, tint: Color?) {
    val iconColor = tint ?: MaterialTheme.colorScheme.primary

    // Use the same Material Design Icons codepoints as Android for perfect consistency
    val codepoint = when (this) {
        AppIcon.QrCodeScanner -> "\uE4C6"  // qr_code_scanner
        AppIcon.Home -> "\uE88A"           // home
        AppIcon.Settings -> "\uE8B8"       // settings
        AppIcon.Search -> "\uE8B6"         // search
        AppIcon.ArrowBack -> "\uE5C4"      // arrow_back
        AppIcon.Clear -> "\uE14C"          // clear
        AppIcon.Edit -> "\uE3C9"           // edit
        AppIcon.AlbumArt -> "\uE030"       // album
        AppIcon.CheckCircle -> "\uE86C"    // check_circle
        AppIcon.LibraryMusic -> "\uE030"   // library_music
        AppIcon.Collections -> "\uE0E9"    // collections
    }

    // Create FontFamily using the new Compose Multiplatform resource system
    val materialSymbols = FontFamily(Font(Res.font.material_symbols_outlined))

    Text(
        text = codepoint,
        fontFamily = materialSymbols,
        fontSize = size.value.sp,
        color = iconColor,
        textAlign = TextAlign.Center
    )
}