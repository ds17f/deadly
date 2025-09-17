package com.grateful.deadly.feature.search.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

/**
 * Top bar with back arrow and search input for SearchResultsScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Manage TextFieldValue state for proper cursor positioning
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }

    // Update textFieldValue when searchQuery changes from external sources (like suggestions)
    LaunchedEffect(searchQuery) {
        if (textFieldValue.text != searchQuery) {
            textFieldValue = TextFieldValue(
                text = searchQuery,
                selection = TextRange(searchQuery.length) // Place cursor at end
            )
        }
    }

    // Auto-focus when this composable is first shown
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Back arrow
            IconButton(onClick = onNavigateBack) {
                AppIcon.ArrowBack.Render(size = 24.dp)
            }

            // Search input - transparent background, compact vertical design with clear button
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onSearchQueryChange(newValue.text)
                },
                placeholder = {
                    Text(
                        text = "What do you want to listen to?",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = if (textFieldValue.text.isNotEmpty()) {
                    @Composable {
                        IconButton(
                            onClick = {
                                textFieldValue = TextFieldValue()
                                onSearchQueryChange("")
                            }
                        ) {
                            AppIcon.Clear.Render(size = 20.dp)
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = (-8).dp) // Move the entire text field 8dp to the left
                    .focusRequester(focusRequester), // Auto-focus support
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    // Transparent/invisible background - user types directly into the interface
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    // Invisible borders
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    // Visible text
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text
                )
            )
        }
    }
}