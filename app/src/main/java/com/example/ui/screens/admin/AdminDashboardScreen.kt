package com.example.ui.screens.admin

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminSubProfile
import com.example.data.model.FeaturedSlide
import com.example.data.model.Song
import com.example.data.model.SongReport
import com.example.data.model.StoredAccount
import com.example.data.model.UploadStatus
import com.example.ui.theme.RjBackground
import com.example.ui.theme.RjBackgroundSecondary
import com.example.ui.theme.RjBorder
import com.example.ui.theme.RjCard
import com.example.ui.theme.RjCardElevated
import com.example.ui.theme.RjSilverAccent
import com.example.ui.theme.RjSilverMuted
import com.example.ui.theme.RjTextMuted
import com.example.ui.theme.RjTextPrimary
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.ui.theme.RjTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    songs: List<Song>,
    registeredAccounts: List<StoredAccount> = emptyList(),
    featuredSlides: List<FeaturedSlide>,
    adminSubProfiles: List<AdminSubProfile>,
    activeProfileId: String?,
    reports: List<SongReport>,
    onNavigateBack: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onApproveSong: (songId: String) -> Unit,
    onRejectSong: (songId: String, reason: String) -> Unit,
    onRequestChanges: (songId: String, notes: String) -> Unit,
    onEditSongMetadata: (songId: String, title: String, genre: String, tags: List<String>, explicit: Boolean) -> Unit,
    onDeleteSongPermanently: (songId: String) -> Unit = {},
    onDeleteUser: (uid: String) -> Unit = {},
    onSetTrendingRank: (songId: String, rank: Int, pin: Boolean) -> Unit,
    onAddFeaturedSlide: (FeaturedSlide) -> Unit,
    onRemoveFeaturedSlide: (slideId: String) -> Unit,
    onToggleSlidePin: (slideId: String) -> Unit,
    onCreateAdminSubProfile: (name: String, user: String, bio: String, insta: String) -> Unit,
    onSwitchSubProfile: (profileId: String) -> Unit,
    onDeleteSubProfile: (profileId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableStateOf(0) }
    var userSearchQuery by remember { mutableStateOf("") }
    var deleteTargetUser by remember { mutableStateOf<StoredAccount?>(null) }

    val pendingSongs = songs.filter { it.uploadStatus == UploadStatus.PENDING_REVIEW || it.uploadStatus == UploadStatus.CHANGES_REQUESTED }
    val publishedSongs = songs.filter { it.uploadStatus == UploadStatus.PUBLISHED }

    val tabs = listOf(
        "User IDs & Delete (${registeredAccounts.size})",
        "Song Approvals (${pendingSongs.size})",
        "All Songs & Delete",
        "Trending Control",
        "Featured Slider",
        "Master Admin ID",
        "Reports"
    )

    // Dialog state for Permanent Deletion
    var deleteTargetSong by remember { mutableStateOf<Song?>(null) }

    // Dialog state for Rejection
    var rejectTargetSongId by remember { mutableStateOf<String?>(null) }
    var rejectReasonText by remember { mutableStateOf("") }

    // Dialog state for Request Changes
    var changeTargetSongId by remember { mutableStateOf<String?>(null) }
    var changeNotesText by remember { mutableStateOf("") }

    // Dialog state for Edit Metadata
    var editTargetSong by remember { mutableStateOf<Song?>(null) }

    // Dialog state for New Slide
    var showAddSlideDialog by remember { mutableStateOf(false) }

    // Dialog state for New Sub Profile
    var showAddSubProfileDialog by remember { mutableStateOf(false) }

    Surface(
        color = RjBackground,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ADMINISTRATOR CONSOLE",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = RjSilverAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Just RJ Musics (Master Admin) • Official",
                        color = RjTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Quick Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard("Pending", "${pendingSongs.size}", Modifier.weight(1f))
                StatCard("Live Tracks", "${publishedSongs.size}", Modifier.weight(1f))
                StatCard("Profiles", "${adminSubProfiles.size}", Modifier.weight(1f))
                StatCard("Reports", "${reports.size}", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Navigation Tabs
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
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: USER IDS & DELETION
                    val filteredAccounts = if (userSearchQuery.isBlank()) {
                        registeredAccounts
                    } else {
                        val query = userSearchQuery.trim().lowercase()
                        registeredAccounts.filter {
                            it.uid.lowercase().contains(query) ||
                            it.username.lowercase().contains(query) ||
                            it.displayName.lowercase().contains(query) ||
                            it.email.lowercase().contains(query)
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            // User Search Input Field
                            OutlinedTextField(
                                value = userSearchQuery,
                                onValueChange = { userSearchQuery = it },
                                placeholder = { Text("Search by User ID, Name, Username or Email...", color = RjTextMuted, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = RjSilverAccent, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    if (userSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { userSearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = RjTextMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = RjBorder,
                                    focusedContainerColor = RjCardElevated,
                                    unfocusedContainerColor = RjCard
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_user_search_input")
                            )
                        }

                        item {
                            // Account Summary Counters
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatCard(title = "Total Users", value = registeredAccounts.size.toString(), modifier = Modifier.weight(1f))
                                StatCard(title = "Standard Users", value = registeredAccounts.count { !it.isAdmin }.toString(), modifier = Modifier.weight(1f))
                                StatCard(title = "Admins", value = registeredAccounts.count { it.isAdmin }.toString(), modifier = Modifier.weight(1f))
                            }
                        }

                        if (filteredAccounts.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No registered user accounts found matching query.", color = RjTextMuted, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(filteredAccounts, key = { it.uid }) { account ->
                                val songsUploaded = songs.count { it.primaryCreatorUid == account.uid }
                                UserAccountCard(
                                    account = account,
                                    songsUploadedCount = songsUploaded,
                                    onCopyUid = {
                                        clipboardManager.setText(AnnotatedString(account.uid))
                                        Toast.makeText(context, "User ID copied: ${account.uid}", Toast.LENGTH_SHORT).show()
                                    },
                                    onDeleteClick = {
                                        deleteTargetUser = account
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: SONG APPROVALS & MODERATION (Confirmation by Admin)
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (pendingSongs.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                    Text("No songs pending moderation. All submissions are up to date.", color = RjTextMuted, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(pendingSongs, key = { it.songId }) { song ->
                                ModerationSongCard(
                                    song = song,
                                    onPlay = { onPlaySong(song) },
                                    onApprove = {
                                        onApproveSong(song.songId)
                                        Toast.makeText(context, "\"${song.title}\" approved and published!", Toast.LENGTH_SHORT).show()
                                    },
                                    onReject = { rejectTargetSongId = song.songId },
                                    onRequestChanges = { changeTargetSongId = song.songId },
                                    onEditMetadata = { editTargetSong = song },
                                    onDeletePermanently = { deleteTargetSong = song },
                                    onCopyCreatorUid = {
                                        clipboardManager.setText(AnnotatedString(song.primaryCreatorUid))
                                        Toast.makeText(context, "Creator User ID copied: ${song.primaryCreatorUid}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: ALL SONGS & PERMANENT DELETION
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                text = "Admin Catalog Management (${songs.size} Total Songs):",
                                color = RjTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(songs) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RjCard)
                                    .border(1.dp, RjBorder, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (song.isLifetimeConfirmed || song.moderationStatus == "approved") {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF1565C0).copy(alpha = 0.4f))
                                                    .border(0.5.dp, Color(0xFF42A5F5), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text("✓ VERIFIED", color = Color(0xFF90CAF9), fontSize = 8.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${song.displayArtists} • ${song.genre} • ${song.uploadStatus.name}",
                                        color = RjTextMuted,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "🎧 ${java.text.NumberFormat.getIntegerInstance().format(song.totalViews)} plays",
                                            color = Color(0xFFCCCCCC),
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "👍 ${java.text.NumberFormat.getIntegerInstance().format(song.likesCount)}",
                                            color = Color(0xFF81C784),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "👎 ${java.text.NumberFormat.getIntegerInstance().format(song.dislikesCount)}",
                                            color = Color(0xFFE57373),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (!song.isLifetimeConfirmed && song.moderationStatus != "approved") {
                                        Button(
                                            onClick = {
                                                onApproveSong(song.songId)
                                                Toast.makeText(context, "\"${song.title}\" approved and published!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("Confirm", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    IconButton(onClick = { onPlaySong(song) }) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White
                                        )
                                    }
                                    IconButton(
                                        onClick = { deleteTargetSong = song }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Song Permanently",
                                            tint = Color(0xFFEF5350)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // TAB 3: TRENDING MANAGEMENT
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                text = "Drag or override rank to curate the official Top 10 Trending chart:",
                                color = RjTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        items(publishedSongs) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RjCard)
                                    .border(1.dp, RjBorder, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "${song.displayArtists} • Score: ${song.automaticTrendingScore}", color = RjTextMuted, fontSize = 11.sp)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = {
                                            onSetTrendingRank(song.songId, 1, !song.isPinnedTrending)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (song.isPinnedTrending) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                            contentDescription = "Pin #1",
                                            tint = if (song.isPinnedTrending) Color.White else RjTextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // TAB 4: FEATURED SLIDER MANAGER
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Button(
                                onClick = { showAddSlideDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add New Featured Slide", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        items(featuredSlides) { slide ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RjCard)
                                    .border(1.dp, RjBorder, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = slide.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = slide.subtitle, color = RjTextMuted, fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { onToggleSlidePin(slide.id) }) {
                                        Icon(
                                            imageVector = if (slide.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                            contentDescription = null,
                                            tint = if (slide.isPinned) Color.White else RjTextMuted
                                        )
                                    }
                                    IconButton(onClick = { onRemoveFeaturedSlide(slide.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF5350))
                                    }
                                }
                            }
                        }
                    }
                }
                5 -> {
                    // TAB 5: MASTER ADMIN PROFILE (Just RJ Musics)
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(RjCard)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Just RJ Musics",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.Check, contentDescription = "Verified Master Admin", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                        Text(text = "@justrjmusics", color = RjSilverAccent, fontSize = 13.sp)
                                        Text(text = "Primary Master Admin • Official", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RjBorder))
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Master Admin ID:", color = RjTextMuted, fontSize = 12.sp)
                                    Text("admin_rj_primary", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Official Email:", color = RjTextMuted, fontSize = 12.sp)
                                    Text("itsrjeditor@gmail.com", color = Color.White, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Instagram:", color = RjTextMuted, fontSize = 12.sp)
                                    Text("@justrjmusics", color = Color.White, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Deletion Status:", color = RjTextMuted, fontSize = 12.sp)
                                    Text("Permanently Protected (Cannot be deleted)", color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        item {
                            Surface(
                                color = Color(0xFF161616),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, RjBorder, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Single Master Admin System",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "All administrative controls and rights belong exclusively to 'Just RJ Musics'. Regular users can be deleted with one click, while this master admin profile remains permanently safeguarded.",
                                        color = RjTextMuted,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
                6 -> {
                    // TAB 6: CONTENT REPORTS
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (reports.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                    Text("No unresolved reports found.", color = RjTextMuted, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(reports) { report ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(RjCard)
                                        .border(1.dp, RjBorder, RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = "Reported: ${report.targetTitle}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "Reason: ${report.reason}", color = Color(0xFFEF5350), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    if (report.description.isNotBlank()) {
                                        Text(text = report.description, color = RjTextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Permanent Delete Confirmation Dialog
    if (deleteTargetSong != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetSong = null },
            containerColor = RjCardElevated,
            title = {
                Text("Delete Song Permanently", color = Color(0xFFEF5350), fontWeight = FontWeight.Black)
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to permanently delete \"${deleteTargetSong?.title}\"?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This action cannot be undone. The song audio file, cover artwork, Firestore metadata records, and playlist references will be permanently deleted from Just RJ Music.",
                        color = RjTextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteTargetSong?.let { s ->
                            onDeleteSongPermanently(s.songId)
                        }
                        deleteTargetSong = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetSong = null }) {
                    Text("Cancel", color = RjTextMuted)
                }
            }
        )
    }

    // Delete User Confirmation Dialog (Admin deletes user; Admin ID is protected)
    if (deleteTargetUser != null) {
        val target = deleteTargetUser!!
        AlertDialog(
            onDismissRequest = { deleteTargetUser = null },
            containerColor = RjCardElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonRemove, contentDescription = null, tint = Color(0xFFEF5350))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete User Account", color = Color(0xFFEF5350), fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to permanently delete \"${target.displayName}\" (@${target.username})?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1A1A1A))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "User ID: ${target.uid}",
                            color = Color(0xFF64B5F6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Confirming this will delete this user and remove all songs uploaded by them from Just RJ Music feeds.",
                        color = RjTextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(target.uid)
                        Toast.makeText(context, "User ${target.username} deleted successfully", Toast.LENGTH_SHORT).show()
                        deleteTargetUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetUser = null }) {
                    Text("Cancel", color = RjTextMuted)
                }
            }
        )
    }

    // Rejection Dialog
    if (rejectTargetSongId != null) {
        AlertDialog(
            onDismissRequest = { rejectTargetSongId = null },
            containerColor = RjCardElevated,
            title = { Text("Reject Submission", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Provide a specific reason for rejection:", color = RjTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReasonText,
                        onValueChange = { rejectReasonText = it },
                        placeholder = { Text("e.g. Inaudible mix, unauthorized sampling") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = RjBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectSong(rejectTargetSongId!!, rejectReasonText.ifBlank { "Does not meet Just RJ Music quality standards." })
                        rejectTargetSongId = null
                        rejectReasonText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Text("Confirm Reject", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectTargetSongId = null }) {
                    Text("Cancel", color = RjTextMuted)
                }
            }
        )
    }

    // Request Changes Dialog
    if (changeTargetSongId != null) {
        AlertDialog(
            onDismissRequest = { changeTargetSongId = null },
            containerColor = RjCardElevated,
            title = { Text("Request Track Updates", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Explain required changes to the creator:", color = RjTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = changeNotesText,
                        onValueChange = { changeNotesText = it },
                        placeholder = { Text("e.g. Please clarify primary genre or re-upload higher bitrate") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = RjBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRequestChanges(changeTargetSongId!!, changeNotesText.ifBlank { "Please update track details." })
                        changeTargetSongId = null
                        changeNotesText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Send Request", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { changeTargetSongId = null }) {
                    Text("Cancel", color = RjTextMuted)
                }
            }
        )
    }

    // Add Sub Profile Dialog
    if (showAddSubProfileDialog) {
        var subName by remember { mutableStateOf("") }
        var subUser by remember { mutableStateOf("") }
        var subBio by remember { mutableStateOf("") }
        var subInsta by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSubProfileDialog = false },
            containerColor = RjCardElevated,
            title = { Text("Create Unlimited Admin Sub-Profile", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subName, onValueChange = { subName = it }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = subUser, onValueChange = { subUser = it }, label = { Text("Unique Username") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = subBio, onValueChange = { subBio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = subInsta, onValueChange = { subInsta = it }, label = { Text("Instagram @handle") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subName.isNotBlank() && subUser.isNotBlank()) {
                            onCreateAdminSubProfile(subName, subUser, subBio, subInsta)
                            showAddSubProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubProfileDialog = false }) { Text("Cancel", color = RjTextMuted) }
            }
        )
    }
}

@Composable
private fun ModerationSongCard(
    song: Song,
    onPlay: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRequestChanges: () -> Unit,
    onEditMetadata: () -> Unit,
    onDeletePermanently: () -> Unit = {},
    onCopyCreatorUid: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RjCardElevated)
            .border(1.dp, RjBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = "By ${song.primaryCreatorName} (@${song.primaryCreatorUsername})", color = RjTextSecondary, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Preview", tint = Color.Black, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onDeletePermanently,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Song Permanently", tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp))
                }
            }
        }

        // Creator User ID Row with Copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141414))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "UPLOADER USER ID", color = RjSilverMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(text = song.primaryCreatorUid, color = Color(0xFF64B5F6), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onCopyCreatorUid,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252525)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy UID", tint = Color.White, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy UID", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Genre: ${song.genre}", color = RjSilverAccent, fontSize = 11.sp)
            Text(text = "• Tempo: ${song.tempo}", color = RjTextMuted, fontSize = 11.sp)
            Text(text = "• Mood: ${song.mood}", color = RjTextMuted, fontSize = 11.sp)
        }

        // Exact Metrics Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "🎧 ${java.text.NumberFormat.getIntegerInstance().format(song.totalViews)} plays",
                color = Color(0xFFCCCCCC),
                fontSize = 11.sp
            )
            Text(
                text = "👍 ${java.text.NumberFormat.getIntegerInstance().format(song.likesCount)} likes",
                color = Color(0xFF81C784),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "👎 ${java.text.NumberFormat.getIntegerInstance().format(song.dislikesCount)} dislikes",
                color = Color(0xFFE57373),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Action Buttons Row (Confirm & Approve button prominent)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onApprove,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1.3f)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Confirm & Publish", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            Button(
                onClick = onRequestChanges,
                colors = ButtonDefaults.buttonColors(containerColor = RjCard),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(0.9f)
            ) {
                Text("Changes", color = RjSilverAccent, fontSize = 11.sp)
            }

            Button(
                onClick = onReject,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF330000)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(0.9f)
            ) {
                Text("Reject", color = Color(0xFFEF5350), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun UserAccountCard(
    account: StoredAccount,
    songsUploadedCount: Int,
    onCopyUid: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RjCardElevated)
            .border(1.dp, RjBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (account.isAdmin) Color(0xFF1565C0) else Color(0xFF333333)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (account.displayName.take(1).ifEmpty { "U" }).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (account.isAdmin) Color(0xFF1565C0) else Color(0xFF2E7D32))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (account.isAdmin) "ADMIN" else "USER",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "@${account.username} • ${account.email}",
                        color = RjTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // If Admin: Protected badge (Admin ID CANNOT be deleted)
            // If Regular User: Delete user button (Admin can delete users)
            if (account.isAdmin) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0D47A1).copy(alpha = 0.4f))
                        .border(1.dp, Color(0xFF2196F3).copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Admin (Protected)",
                        color = Color(0xFF90CAF9),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                IconButton(
                    onClick = { onDeleteClick?.invoke() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF330000))
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonRemove,
                        contentDescription = "Delete User",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Prominent User ID Display with Copy Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141414))
                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "USER ID (UID)",
                    color = RjSilverMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = account.uid,
                    color = Color(0xFF64B5F6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onCopyUid,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252525)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy UID", tint = Color.White, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy ID", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Additional account stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(account.registeredAt))
            Text(text = "Joined: $dateStr", color = RjTextMuted, fontSize = 10.sp)
            Text(
                text = "$songsUploadedCount songs in catalog",
                color = if (songsUploadedCount > 0) Color(0xFF81C784) else RjTextMuted,
                fontSize = 10.sp,
                fontWeight = if (songsUploadedCount > 0) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(RjCard)
            .border(1.dp, RjBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(text = title, color = RjTextMuted, fontSize = 10.sp)
        }
    }
}
