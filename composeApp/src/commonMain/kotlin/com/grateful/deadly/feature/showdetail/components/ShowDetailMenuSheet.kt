package com.grateful.deadly.feature.showdetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

/**
 * ShowDetailMenuSheet - Menu bottom sheet for show detail actions
 *
 * Follows v2's PlaylistMenuSheet pattern with:
 * - Share option
 * - Choose Recording option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailMenuSheet(
    onShareClick: () -> Unit,
    onChooseRecordingClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Share option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onShareClick()
                        onDismiss()
                    }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon.Share.Render(
                    size = 24.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Share",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Choose Recording option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onChooseRecordingClick()
                        onDismiss()
                    }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon.LibraryMusic.Render(
                    size = 24.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Choose Recording",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Bottom spacing
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
