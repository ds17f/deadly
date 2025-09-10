package com.grateful.deadly.feature.search.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

/**
 * Row 2: Search box with search icon and placeholder text
 */
@Composable
fun SearchSearchBox(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFocusReceived: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    // Navigate to search results when field receives focus
    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocusReceived()
        }
    }
    
    OutlinedTextField(
        value = "", // Always empty so placeholder always shows
        onValueChange = { /* No-op - this is just a button */ },
        interactionSource = interactionSource,
        readOnly = true, // Make it clear this isn't for typing
        placeholder = { 
            Text(
                text = "What do you want to listen to?",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        },
        leadingIcon = {
            AppIcon.Search.Render(size = 28.dp)
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}