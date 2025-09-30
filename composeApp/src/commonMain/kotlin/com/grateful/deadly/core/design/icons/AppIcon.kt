package com.grateful.deadly.core.design.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cross-platform icon abstraction for KMM using native rendering approaches
 * 
 * This enum represents semantic icons that get rendered natively on each platform:
 * - Android: Material Symbols font glyphs
 * - iOS: SF Symbols via SwiftUI
 * 
 * Usage: AppIcon.QrCodeScanner.Render(size = 24.dp)
 */
enum class AppIcon {
    // App branding
    Logo,
    Error,

    QrCodeScanner,
    Home,
    Settings,
    Search,
    ArrowBack,
    Clear,
    Edit,
    AlbumArt,
    CheckCircle,
    LibraryMusic,
    Collections,

    // ShowDetail UI icons
    FileDownload,
    FormatListBulleted,
    MoreVertical,
    PlayCircleFilled,
    PlayArrow,
    ArrowLeft,
    ArrowRight,
    Star,
    StarHalf,
    StarBorder,
    ChevronRight,
    LibraryAdd,
    LibraryAddCheck,
    PauseCircleFilled,

    // Player UI icons
    Pause,
    SkipPrevious,
    SkipNext,
    MusicNote,
    PlaylistAdd,
    KeyboardArrowDown,
    Shuffle,
    Repeat,
    Cast,
    Share,
    QueueMusic,

    // Add more icons as needed for the search feature and beyond
}

/**
 * Platform-specific rendering function
 * - Android: Renders Material Symbols font glyph as Text
 * - iOS: Renders SF Symbol as SwiftUI Image
 */
@Composable
expect fun AppIcon.Render(size: Dp = 24.dp, tint: androidx.compose.ui.graphics.Color? = null)