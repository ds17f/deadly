package com.grateful.deadly.feature.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grateful.deadly.feature.search.models.DecadeBrowse

/**
 * Rows 3 & 4: Browse section with 2x2 decade grid
 */
@Composable
fun SearchBrowseSection(
    onDecadeClick: (String) -> Unit
) {
    val decades = listOf(
        DecadeBrowse("1960s", listOf(Color(0xFF1976D2), Color(0xFF42A5F5)), "1960s"),
        DecadeBrowse("1970s", listOf(Color(0xFF388E3C), Color(0xFF66BB6A)), "1970s"),
        DecadeBrowse("1980s", listOf(Color(0xFFD32F2F), Color(0xFFEF5350)), "1980s"),
        DecadeBrowse("1990s", listOf(Color(0xFF7B1FA2), Color(0xFFAB47BC)), "1990s")
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Start Browsing",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(180.dp) // Fixed height for 2x2 grid
        ) {
            items(decades) { decade ->
                DecadeCard(
                    decade = decade,
                    onClick = { onDecadeClick(decade.era) }
                )
            }
        }
    }
}

/**
 * Individual decade card component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecadeCard(
    decade: DecadeBrowse,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(decade.gradient),
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
        ) {
            // Background alt logo image (see docs/TODO.md for resource setup)
            // Original V2: painterResource(com.deadly.v2.core.design.R.drawable.alt_logo)
            
            // Decade text
            Text(
                text = decade.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}