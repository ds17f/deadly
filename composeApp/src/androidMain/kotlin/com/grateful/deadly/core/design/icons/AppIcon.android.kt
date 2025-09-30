package com.grateful.deadly.core.design.icons

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
        // App branding
        AppIcon.Logo -> "\uE405"           // music_note (representing Grateful Dead music)
        AppIcon.Error -> "\uE000"          // error

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

        // Player UI icons
        AppIcon.Pause -> "\uE034"          // pause
        AppIcon.SkipPrevious -> "\uE045"   // skip_previous
        AppIcon.SkipNext -> "\uE044"       // skip_next
        AppIcon.MusicNote -> "\uE405"      // music_note
        AppIcon.PlaylistAdd -> "\uE03B"    // playlist_add
        AppIcon.KeyboardArrowDown -> "\uE313" // keyboard_arrow_down
        AppIcon.Shuffle -> "\uE043"        // shuffle
        AppIcon.Repeat -> "\uE040"         // repeat
        AppIcon.Cast -> "\uE307"           // cast
        AppIcon.Share -> "\uE80D"          // share
        AppIcon.QueueMusic -> "\uE03C"     // queue_music

        // Library UI icons
        AppIcon.PushPin -> "\uE8D4"        // push_pin
        AppIcon.Delete -> "\uE872"         // delete
        AppIcon.Close -> "\uE5CD"          // close
        AppIcon.GridView -> "\uE9B0"       // grid_view
        AppIcon.SwapVert -> "\uE8D5"       // swap_vert
        AppIcon.KeyboardArrowUp -> "\uE316" // keyboard_arrow_up
        AppIcon.Add -> "\uE145"            // add
    }

    Text(
        text = codepoint,
        fontFamily = MaterialSymbols,
        fontSize = size.value.sp,
        color = tint ?: MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .then(
                // For small icons, provide extra height to accommodate the upward offset
                if (size <= 12.dp) {
                    Modifier.sizeIn(
                        minWidth = size,
                        maxWidth = size,
                        minHeight = size + 6.dp,
                        maxHeight = size + 6.dp
                    )
                } else {
                    Modifier.size(size)
                }
            )
            .universalIconAlignment()
    )
}