package com.grateful.deadly.core.design.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.grateful.deadly.R
import com.grateful.deadly.core.design.universalIconAlignment

private val MaterialSymbols = FontFamily(
    Font(R.font.material_symbols_outlined)
)

/**
 * Android implementation using Material Symbols font
 */
@Composable
actual fun AppIcon.Render(size: Dp, tint: Color?) {
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

        // ShowDetail UI icons
        AppIcon.FileDownload -> "\uE2C4"   // file_download
        AppIcon.FormatListBulleted -> "\uE241" // format_list_bulleted
        AppIcon.MoreVertical -> "\uE5D4"   // more_vert
        AppIcon.PlayCircleFilled -> "\uE037" // play_circle_filled
        AppIcon.PlayArrow -> "\uE037"      // play_arrow
        AppIcon.ArrowLeft -> "\uE314"      // keyboard_arrow_left
        AppIcon.ArrowRight -> "\uE315"     // keyboard_arrow_right
        AppIcon.Star -> "\uE838"           // star
        AppIcon.StarHalf -> "\uE839"       // star_half
        AppIcon.StarBorder -> "\uE83A"     // star_border
        AppIcon.ChevronRight -> "\uE5CC"   // chevron_right
        AppIcon.LibraryAdd -> "\uE02E"     // library_add
        AppIcon.LibraryAddCheck -> "\uE02F" // library_add_check
        AppIcon.PauseCircleFilled -> "\uE035" // pause_circle_filled
    }

    Text(
        text = codepoint,
        fontFamily = MaterialSymbols,
        fontSize = size.value.sp,
        color = tint ?: MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .size(size)
            .universalIconAlignment()
    )
}