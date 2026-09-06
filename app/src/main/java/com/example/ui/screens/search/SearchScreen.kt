package com.example.ui.screens.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GenreCategory
import com.example.data.model.Song
import com.example.data.model.UploadStatus
import com.example.ui.components.SongListItemRow
import com.example.ui.theme.RjBackground
import com.example.ui.theme.RjBackgroundSecondary
import com.example.ui.theme.RjBorder
import com.example.ui.theme.RjCard
import com.example.ui.theme.RjCardElevated
import com.example.ui.theme.RjSilverAccent
import com.example.ui.theme.RjSilverMuted
import com.example.ui.theme.RjTextMuted
import com.example.ui.theme.RjTextPrimary
import com.example.ui.theme.RjTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    selectedFilter: String,
    recentSearches: List<String>,
    categories: List<GenreCategory>,
    allSongs: List<Song>,
    onQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onSongOptionsClick: (Song) -> Unit,
    onGenreCardClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf("All", "Tracks", "Artists", "Genres")

    val publishedSongs = allSongs.filter { it.uploadStatus == UploadStatus.PUBLISHED }
    val searchResults = if (searchQuery.isBlank()) {
        emptyList()
    } else {
        val rawQ = searchQuery.trim().lowercase()
        val q = rawQ.removePrefix("@")
        if (rawQ == "trending" || rawQ == "#trending") {
            publishedSongs.sortedBy { it.trendingPosition ?: 999 }
        } else {
            publishedSongs.filter { song ->
                when (selectedFilter) {
                    "Tracks" -> song.title.lowercase().contains(rawQ) || song.title.lowercase().contains(q)
                    "Artists" -> song.primaryCreatorName.lowercase().contains(rawQ) ||
                            song.primaryCreatorUsername.lowercase().contains(q) ||
                            song.mentionedUsernames.any { it.lowercase().contains(q) }
                    "Genres" -> song.genre.lowercase().contains(rawQ) ||
                            song.secondaryGenres.any { it.lowercase().contains(rawQ) } ||
                            song.tags.any { it.lowercase().contains(rawQ) }
                    else -> song.title.lowercase().contains(rawQ) ||
                            song.primaryCreatorName.lowercase().contains(rawQ) ||
                            song.primaryCreatorUsername.lowercase().contains(q) ||
                            song.mentionedUsernames.any { it.lowercase().contains(q) } ||
                            song.genre.lowercase().contains(rawQ) ||
                            song.tags.any { it.lowercase().contains(rawQ) }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RjBackground)
    ) {
        // Search Input Bar
        Surface(
            color = RjBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "SEARCH",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Songs, artists, genres, @creators...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = RjSilverAccent)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = RjTextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = RjBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = RjCard,
                        unfocusedContainerColor = RjCard
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_field")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter tabs
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { filter ->
                        val isSelected = filter == selectedFilter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color.White else RjCard)
                                .border(1.dp, if (isSelected) Color.White else RjBorder, RoundedCornerShape(16.dp))
                                .clickable { onFilterChange(filter) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Color.Black else RjTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        if (searchQuery.isNotBlank()) {
            // Live Search Results
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = "Results (${searchResults.size})",
                        color = RjSilverMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (searchResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No tracks found for '$searchQuery'",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try searching by genre, mood or artist name.",
                                    color = RjTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    items(searchResults) { song ->
                        SongListItemRow(
                            song = song,
                            onClick = { onSongClick(song) },
                            onMoreClick = { onSongOptionsClick(song) }
                        )
                    }
                }
            }
        } else {
            // Default Explore View: Recent Searches & Browse Genre Grid
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Recent Searches
                if (recentSearches.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentSearches.forEach { term ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(RjCard)
                                            .border(1.dp, RjBorder, RoundedCornerShape(8.dp))
                                            .clickable { onQueryChange(term) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = RjTextMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = term, color = RjTextSecondary, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Browse All Genres Grid
                item {
                    Text(
                        text = "Browse All Genres & Moods",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                val browseCategories = categories.filter { it.id != "all" }
                val chunked = browseCategories.chunked(2)

                items(chunked) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RjCardElevated)
                                    .border(1.dp, RjBorder, RoundedCornerShape(12.dp))
                                    .clickable { onGenreCardClick(cat.name) }
                                    .padding(12.dp)
                            ) {
                                Column(modifier = Modifier.align(Alignment.TopStart)) {
                                    Text(
                                        text = cat.name,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = cat.subtitle,
                                        color = RjTextMuted,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
