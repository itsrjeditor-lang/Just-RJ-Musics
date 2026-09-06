package com.example.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.FeaturedSlide
import com.example.data.model.GenreCategory
import com.example.data.model.Song
import com.example.data.model.UploadStatus
import com.example.ui.components.CategoryChipsRow
import com.example.ui.components.FeaturedHeroSlider
import com.example.ui.components.SongCardMedium
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

@Composable
fun HomeScreen(
    featuredSlides: List<FeaturedSlide>,
    songs: List<Song>,
    recommendedSongs: List<Song>,
    trendingSongs: List<Song>,
    categories: List<GenreCategory>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onSlideClick: (FeaturedSlide) -> Unit,
    onSongOptionsClick: (Song) -> Unit,
    onCreatorClick: (username: String) -> Unit,
    onViewAllTrending: () -> Unit,
    modifier: Modifier = Modifier
) {
    val publishedSongs = songs.filter { it.uploadStatus == UploadStatus.PUBLISHED }
    val filteredSongs = if (selectedCategory == "all") {
        publishedSongs
    } else {
        publishedSongs.filter {
            it.genre.equals(selectedCategory, ignoreCase = true) ||
            it.secondaryGenres.any { sec -> sec.equals(selectedCategory, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RjBackground),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // 1. Featured Hero Slider
        item {
            Spacer(modifier = Modifier.height(8.dp))
            FeaturedHeroSlider(
                slides = featuredSlides.filter { it.isEnabled },
                onSlideClick = onSlideClick
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 2. Category Filter Chips
        item {
            CategoryChipsRow(
                categories = categories,
                selectedCategoryId = selectedCategory,
                onCategorySelect = onSelectCategory
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Made For You / Recommendations
        item {
            SectionHeader(
                title = "Made For You",
                subtitle = "Handpicked for your music taste",
                onSeeAll = null
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recommendedSongs.take(6)) { song ->
                    SongCardMedium(
                        song = song,
                        onClick = { onSongClick(song) },
                        onMoreClick = { onSongOptionsClick(song) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }

        // 4. Trending Now Section (With ranking numbers)
        item {
            SectionHeader(
                title = "Trending Now",
                subtitle = "Top charting hits on Just RJ Music",
                onSeeAll = onViewAllTrending
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        itemsIndexed(trendingSongs.take(4)) { index, song ->
            SongListItemRow(
                song = song,
                rank = index + 1,
                onClick = { onSongClick(song) },
                onMoreClick = { onSongOptionsClick(song) }
            )
        }

        // 5. Popular Creators & Producers
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(
                title = "Featured Creators",
                subtitle = "Discover original producers & artists",
                onSeeAll = null
            )
            Spacer(modifier = Modifier.height(10.dp))

            val creators = listOf(
                CreatorSpotlight("Just RJ Musics", "justrjmusics", "Founder & Producer", R.drawable.rj_logo, true)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(creators) { creator ->
                    CreatorCard(
                        creator = creator,
                        onClick = { onCreatorClick(creator.username) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }

        // 6. New Releases / Fresh Hits
        item {
            SectionHeader(
                title = "New Releases",
                subtitle = "Latest original audio drops",
                onSeeAll = null
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredSongs.reversed().take(6)) { song ->
                    SongCardMedium(
                        song = song,
                        onClick = { onSongClick(song) },
                        onMoreClick = { onSongOptionsClick(song) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }

        // 7. Midnight Vibe & Chill Playlist Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(RjCardElevated)
                    .border(1.dp, RjBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OFFICIAL RJ PLAYLISTS",
                            color = RjSilverAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Midnight Aesthetics Mix",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hand-curated dark cinematic hip-hop & lofi tape loops.",
                            color = RjTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val target = publishedSongs.firstOrNull()
                            if (target != null) onSongClick(target)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Play", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    onSeeAll: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    color = Color(0xFF777777),
                    fontSize = 11.sp
                )
            }
        }
        if (onSeeAll != null) {
            TextButton(
                onClick = onSeeAll,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "SEE ALL",
                    color = Color(0xFF777777),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

private data class CreatorSpotlight(
    val name: String,
    val username: String,
    val role: String,
    val imageResId: Int,
    val verified: Boolean
)

@Composable
private fun CreatorCard(
    creator: CreatorSpotlight,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(RjCard)
                .border(1.5.dp, if (creator.verified) Color.White else RjBorder, CircleShape)
        ) {
            Image(
                painter = painterResource(id = creator.imageResId),
                contentDescription = creator.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = creator.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (creator.verified) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Text(
            text = "@${creator.username}",
            color = RjTextMuted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
