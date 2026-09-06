package com.example.ui.screens.library

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.DownloadedSongEntity
import com.example.data.model.Playlist
import com.example.data.model.ReactionType
import com.example.data.model.Song
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
fun LibraryScreen(
    playlists: List<Playlist>,
    downloadedSongs: List<DownloadedSongEntity>,
    allSongs: List<Song>,
    userReactions: Map<String, ReactionType>,
    currentUserId: String?,
    onPlaylistClick: (Playlist) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSongOptionsClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Playlists", "Offline Downloads", "Liked Songs")

    val likedSongs = allSongs.filter { song ->
        val key = "${currentUserId}_${song.songId}"
        userReactions[key] == ReactionType.LIKE
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RjBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "YOUR LIBRARY",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            if (selectedTab == 0) {
                Button(
                    onClick = onCreatePlaylistClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Playlist", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Sub Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)),
            containerColor = Color.Transparent,
            contentColor = Color.White,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (selectedTab in tabPositions.indices) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color.White
                    )
                }
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> {
                // Playlists Tab
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (playlists.isEmpty()) {
                        item {
                            EmptyLibraryView("No playlists yet. Create one to organize your favorite music.")
                        }
                    } else {
                        items(playlists) { pl ->
                            PlaylistCardRow(playlist = pl, onClick = { onPlaylistClick(pl) })
                        }
                    }
                }
            }
            1 -> {
                // Offline Downloads Tab
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Surface(
                            color = RjCardElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RjBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF222222)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.DownloadDone,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${downloadedSongs.size} Tracks Downloaded Offline",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "🔒 In-App Private Vault • No external file access",
                                        color = RjSilverAccent,
                                        fontSize = 11.sp
                                    )
                                }
                                if (downloadedSongs.isNotEmpty()) {
                                    val firstMatched = allSongs.firstOrNull { it.songId == downloadedSongs.first().songId } ?: Song(
                                        songId = downloadedSongs.first().songId,
                                        title = downloadedSongs.first().title,
                                        primaryCreatorUid = "offline",
                                        primaryCreatorName = downloadedSongs.first().artistName,
                                        primaryCreatorUsername = "artist",
                                        genre = downloadedSongs.first().genre,
                                        durationSec = downloadedSongs.first().durationSec
                                    )
                                    Button(
                                        onClick = { onSongClick(firstMatched) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Play Offline", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (downloadedSongs.isEmpty()) {
                        item {
                            EmptyLibraryView("No offline downloads yet. Tap the download icon on any track to listen offline without internet.")
                        }
                    } else {
                        items(downloadedSongs) { dl ->
                            val matchedSong = allSongs.firstOrNull { it.songId == dl.songId } ?: Song(
                                songId = dl.songId,
                                title = dl.title,
                                primaryCreatorUid = "offline",
                                primaryCreatorName = dl.artistName,
                                primaryCreatorUsername = "artist",
                                genre = dl.genre,
                                durationSec = dl.durationSec
                            )
                            SongListItemRow(
                                song = matchedSong,
                                onClick = { onSongClick(matchedSong) },
                                onMoreClick = { onSongOptionsClick(matchedSong) }
                            )
                        }
                    }
                }
            }
            2 -> {
                // Liked Songs Tab
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${likedSongs.size} Liked tracks",
                                color = RjSilverAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (likedSongs.isEmpty()) {
                        item {
                            EmptyLibraryView("No liked tracks yet. Hit the like button while listening to build your collection.")
                        }
                    } else {
                        items(likedSongs) { song ->
                            SongListItemRow(
                                song = song,
                                onClick = { onSongClick(song) },
                                onMoreClick = { onSongOptionsClick(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistCardRow(playlist: Playlist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RjCard)
            .border(1.dp, RjBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(RjCardElevated)
                .border(1.dp, RjBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Bookmark, contentDescription = null, tint = RjSilverAccent, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = playlist.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.songIds.size} songs • By ${playlist.ownerName}",
                color = RjTextMuted,
                fontSize = 12.sp
            )
        }

        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun EmptyLibraryView(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = msg,
            color = RjTextMuted,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}
