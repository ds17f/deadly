package com.grateful.deadly.core.design.component.bottombar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
import com.grateful.deadly.navigation.AppScreen

/**
 * Bottom navigation tab configuration
 */
data class BottomNavTab(
    val screen: AppScreen,
    val label: String,
    val icon: AppIcon
)

/**
 * List of bottom navigation tabs
 */
val bottomNavTabs = listOf(
    BottomNavTab(AppScreen.Home, "Home", AppIcon.Home),
    BottomNavTab(AppScreen.Search, "Search", AppIcon.Search),
    BottomNavTab(AppScreen.Library, "Library", AppIcon.LibraryMusic),
    BottomNavTab(AppScreen.Collections, "Collections", AppIcon.Collections),
    BottomNavTab(AppScreen.Settings, "Settings", AppIcon.Settings)
)

/**
 * Bottom navigation bar with Material3 styling
 */
@Composable
fun BottomNavigationBar(
    currentScreen: AppScreen,
    onNavigateToTab: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavTabs.forEach { tab ->
                BottomNavItem(
                    tab = tab,
                    isSelected = currentScreen == tab.screen,
                    onClick = { onNavigateToTab(tab.screen) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Individual bottom navigation item using clickable surface
 */
@Composable
private fun BottomNavItem(
    tab: BottomNavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        tab.icon.Render(size = 24.dp, tint = contentColor)

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = tab.label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}