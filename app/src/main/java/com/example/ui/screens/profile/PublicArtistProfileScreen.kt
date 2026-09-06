package com.example.ui.screens.profile

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.Song
import com.example.data.model.UserProfile
import com.example.service.player.PlayerState
import com.example.ui.theme.RjBackground
import com.example.ui.theme.RjBorder
import com.example.ui.theme.RjCard
import com.example.ui.theme.RjSilverAccent
import com.example.ui.theme.RjSilverMuted
import com.example.ui.theme.RjTextMuted
import com.example.ui.theme.RjTextSecondary

@Composable
fun PublicArtistProfileScreen(
    username: String,
    currentUserId: String?,
    allProfiles: List<UserProfile>,
    allSongs: List<Song>,
    follows: Set<Pair<String, String>>,
    playerState: PlayerState,
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongOptionsClick: (Song) -> Unit,
    onToggleFollow: (targetUid: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cleanUser = username.removePrefix("@").trim().lowercase()

    val profile = remember(allProfiles, cleanUser) {
        allProfiles.firstOrNull { it.username.equals(cleanUser, ignoreCase = true) }
            ?: UserProfile(
                uid = "user_$cleanUser",
                email = "$cleanUser@rjmusics.com",
                displayName = username.removePrefix("@").replaceFirstChar { it.uppercase() },
                username = cleanUser,
                bio = "Creator on Just RJ Musics."
            )
    }

    val userSongs = remember(allSongs, profile.uid, profile.username) {
        allSongs.filter { song ->
            song.primaryCreatorUid == profile.uid ||
                    song.primaryCreatorUsername.equals(profile.username, ignoreCase = true) ||
                    (profile.isAdmin && (song.primaryCreatorUid == "admin_rj_primary" || song.primaryCreatorUsername.equals("justrjmusics", ignoreCase = true)))
        }
    }

    val isSelf = currentUserId == profile.uid
    val isFollowing = remember(follows, currentUserId, profile.uid) {
        currentUserId != null && (follows.contains(currentUserId to profile.uid) || follows.contains(currentUserId to "admin_${profile.username}"))
    }
    val followersCount = remember(follows, profile.uid, profile.followersCount) {
        val liveFollowers = follows.count { pair -> pair.second == profile.uid || pair.second == "admin_${profile.username}" }
        maxOf(profile.followersCount, liveFollowers)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RjBackground)
    ) {
        // Top Navigation Bar
        Surface(
            color = RjBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("profile_back_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "@${profile.username}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out @${profile.username} (${profile.displayName}) on Just RJ Musics! Stream all their songs online."
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Creator Profile"))
                    }
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share Profile",
                        tint = RjSilverAccent
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Profile Header Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1F1F1F),
                                    RjBackground
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Avatar
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF2A2A2A), Color(0xFF141414))
                                    )
                                )
                                .border(2.dp, RjBorder, CircleShape)
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
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Display Name + Verified Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = profile.displayName,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (profile.verified || profile.isAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Verified Artist",
                                    tint = Color(0xFF3897F0),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Username handle
                        Text(
                            text = "@${profile.username}",
                            color = RjSilverMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Bio
                        if (profile.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = profile.bio,
                                color = RjTextSecondary,
                                fontSize = 13.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        // Instagram Tag
                        if (!profile.instagramUsername.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .border(1.dp, RjBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "📸 @${profile.instagramUsername.removePrefix("@")}",
                                    color = Color(0xFFE1306C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Row
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(RjCard)
                                .border(1.dp, RjBorder, RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = followersCount.toString(),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Followers",
                                    color = RjTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = profile.followingCount.toString(),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Following",
                                    color = RjTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = userSongs.size.toString(),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Songs",
                                    color = RjTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons Row (Follow + Play All + Shuffle)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!isSelf) {
                                if (isFollowing) {
                                    OutlinedButton(
                                        onClick = { onToggleFollow(profile.uid) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Following",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Following", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    Button(
                                        onClick = { onToggleFollow(profile.uid) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+ Follow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (userSongs.isNotEmpty()) {
                                Button(
                                    onClick = { onSongClick(userSongs.first(), userSongs) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1DB954),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play All",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = {
                                        val shuffled = userSongs.shuffled()
                                        onSongClick(shuffled.first(), shuffled)
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(RjCard)
                                        .border(1.dp, RjBorder, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = RjSilverAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Songs Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "UPLOADED SONGS (${userSongs.size})",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Online Stream",
                        color = RjSilverMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Songs List
            if (userSongs.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp)
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = RjTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No songs uploaded yet",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Songs uploaded by @${profile.username} will be playable by all users here.",
                            color = RjTextMuted,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                itemsIndexed(userSongs) { index, song ->
                    val isPlayingThis = playerState.currentSong?.songId == song.songId && playerState.isPlaying

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongClick(song, userSongs) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Track Number / Rank
                        Text(
                            text = "${index + 1}",
                            color = if (isPlayingThis) Color(0xFF1DB954) else RjTextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )

                        // Artwork
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RjCard)
                        ) {
                            if (!song.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(song.coverUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (song.coverResId != null) {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(id = song.coverResId),
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = RjSilverAccent,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.Center)
                                )
                            }

                            if (isPlayingThis) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Playing",
                                        tint = Color(0xFF1DB954),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title and Metadata
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = if (isPlayingThis) Color(0xFF1DB954) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${song.genre} • ${song.totalViews} streams",
                                color = RjTextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // More options
                        IconButton(onClick = { onSongOptionsClick(song) }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Song Options",
                                tint = RjTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
