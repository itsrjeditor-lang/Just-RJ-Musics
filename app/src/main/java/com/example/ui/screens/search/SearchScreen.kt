package com.example.ui.screens.search

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.GenreCategory
import com.example.data.model.Song
import com.example.data.model.UploadStatus
import com.example.data.model.UserProfile
import com.example.ui.components.SongListItemRow
import com.example.ui.theme.RjBackground
import com.example.ui.theme.RjBorder
import com.example.ui.theme.RjCard
import com.example.ui.theme.RjCardElevated
import com.example.ui.theme.RjSilverAccent
import com.example.ui.theme.RjSilverMuted
import com.example.ui.theme.RjTextMuted
import com.example.ui.theme.RjTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    selectedFilter: String,
    recentSearches: List<String>,
    categories: List<GenreCategory>,
    allSongs: List<Song>,
    allProfiles: List<UserProfile> = emptyList(),
    onQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onSongOptionsClick: (Song) -> Unit,
    onGenreCardClick: (String) -> Unit,
    onProfileClick: (username: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val filters = listOf("All", "Tracks", "Artists", "Genres")

    val publishedSongs = allSongs.filter { it.uploadStatus == UploadStatus.PUBLISHED }

    val rawQ = searchQuery.trim().lowercase()
    val q = rawQ.removePrefix("@")

    // Filter matching songs
    val searchResults = if (searchQuery.isBlank()) {
        emptyList()
    } else {
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

    // Filter matching user and creator profiles
    val matchingProfiles = remember(allProfiles, q) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allProfiles.filter { profile ->
                profile.displayName.contains(q, ignoreCase = true) ||
                        profile.username.contains(q, ignoreCase = true) ||
                        profile.bio.contains(q, ignoreCase = true)
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
                    placeholder = { Text("Songs, artists, @creators...") },
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
                // When showing Artists or All, display matched Profiles first
                if ((selectedFilter == "All" || selectedFilter == "Artists") && matchingProfiles.isNotEmpty()) {
                    item {
                        Text(
                            text = "Artists & Profiles (${matchingProfiles.size})",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }

                    items(matchingProfiles) { profile ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProfileClick(profile.username) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Avatar
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF2A2A2A), Color(0xFF141414))
                                        )
                                    )
                                    .border(1.dp, RjBorder, CircleShape)
                            ) {
                                if (!profile.profileImageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(profile.profileImageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = profile.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = profile.displayName.take(2).uppercase(),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.displayName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (profile.verified || profile.isAdmin) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Verified",
                                            tint = Color(0xFF3897F0),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "@${profile.username} • Creator",
                                    color = RjSilverMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View Profile",
                                tint = RjTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Show Songs when filter is All, Tracks, or Genres
                if (selectedFilter == "All" || selectedFilter == "Tracks" || selectedFilter == "Genres") {
                    if (searchResults.isNotEmpty()) {
                        item {
                            Text(
                                text = "Songs (${searchResults.size})",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }

                        items(searchResults) { song ->
                            SongListItemRow(
                                song = song,
                                onClick = { onSongClick(song) },
                                onMoreClick = { onSongOptionsClick(song) }
                            )
                        }
                    }
                }

                // Empty state if nothing matched
                val hasProfiles = (selectedFilter == "All" || selectedFilter == "Artists") && matchingProfiles.isNotEmpty()
                val hasSongs = (selectedFilter == "All" || selectedFilter == "Tracks" || selectedFilter == "Genres") && searchResults.isNotEmpty()

                if (!hasProfiles && !hasSongs) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No results found for '$searchQuery'",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Search by song title, artist @username, or genre.",
                                    color = RjTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Default Explore View: Recent Searches, Discover Creators & Browse Genre Grid
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

                // Discover Creators & Profiles Section
                if (allProfiles.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "Discover Creators & Profiles",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(allProfiles) { profile ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable { onProfileClick(profile.username) }
                                            .width(76.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFF2A2A2A), Color(0xFF141414))
                                                    )
                                                )
                                                .border(1.5.dp, RjBorder, CircleShape)
                                        ) {
                                            if (!profile.profileImageUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(profile.profileImageUrl)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = profile.displayName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text(
                                                    text = profile.displayName.take(2).uppercase(),
                                                    color = Color.White,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = profile.displayName,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "@${profile.username}",
                                            color = RjTextMuted,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
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
