package com.grateful.deadly.core.design.component.topbar

import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grateful.deadly.navigation.TopBarConfig

/**
 * TopBar component for the Deadly app
 * 
 * Provides consistent top bar styling across all screens.
 * Supports back navigation and different display modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    config: TopBarConfig,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!config.visible) return
    
    TopAppBar(
        title = {
            Text(
                text = config.title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        navigationIcon = {
            if (config.showBackButton) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            }
        },
        modifier = modifier
    )
}