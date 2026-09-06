package com.example.ui.screens.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwitchAccount
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AdminSubProfile
import com.example.data.model.CollaborationInvite
import com.example.data.model.CollaborationStatus
import com.example.data.model.CreatorAnalytics
import com.example.data.model.Song
import com.example.data.model.UploadStatus
import com.example.data.model.UserProfile
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
fun ProfileScreen(
    currentUser: UserProfile?,
    activeAdminProfile: AdminSubProfile?,
    allSongs: List<Song>,
    collaborationInvites: List<CollaborationInvite>,
    analytics: CreatorAnalytics,
    follows: Set<Pair<String, String>> = emptySet(),
    onSwitchProfileClick: () -> Unit,
    onUpdateProfile: (name: String, bio: String, insta: String, web: String) -> Unit,
    onUpdateProfilePhoto: (photoUri: String) -> Unit,
    onRespondCollabInvite: (inviteId: String, accept: Boolean) -> Unit,
    onSongClick: (Song) -> Unit,
    onSongOptionsClick: (Song) -> Unit,
    onUploadSongClick: () -> Unit,
    onLogout: () -> Unit,
    onApproveSong: (songId: String) -> Unit = {},
    onRejectSong: (songId: String, reason: String) -> Unit = { _, _ -> },
    onOpenAdminDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableStateOf(0) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val user = currentUser ?: return
    val displayName = if (user.isAdmin) (activeAdminProfile?.displayName ?: user.displayName) else user.displayName
    val username = if (user.isAdmin) (activeAdminProfile?.username ?: user.username) else user.username
    val bio = if (user.isAdmin) (activeAdminProfile?.bio ?: user.bio) else user.bio
    val instagram = if (user.isAdmin) (activeAdminProfile?.instagramUsername ?: user.instagramUsername) else user.instagramUsername
    val photoUrl = if (user.isAdmin) (activeAdminProfile?.profileImageUrl?.ifBlank { user.profileImageUrl } ?: user.profileImageUrl) else user.profileImageUrl

    // Zero-permission Android Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateProfilePhoto(uri.toString())
        }
    }

    val myUploads = allSongs.filter {
        it.primaryCreatorUid == user.uid ||
        it.primaryCreatorUsername.equals(username, ignoreCase = true) ||
        (user.isAdmin && (it.primaryCreatorUid == "admin_rj_primary" || it.primaryCreatorUsername.equals("justrjmusics", ignoreCase = true)))
    }
    val myCollabs = allSongs.filter { it.acceptedCollaboratorUids.contains(user.uid) || it.mentionedUsernames.contains(username) }
    val pendingSongs = allSongs.filter { it.uploadStatus == UploadStatus.PENDING_REVIEW || it.uploadStatus == UploadStatus.CHANGES_REQUESTED }

    // Accurate calculation of user's songs
    val exactSongCount = remember(allSongs, user.uid, username, displayName, user.isAdmin) {
        allSongs.count { song ->
            if (user.isAdmin) {
                song.primaryCreatorUid == user.uid ||
                song.primaryCreatorUsername.equals(username, ignoreCase = true) ||
                song.primaryCreatorUsername.equals("justrjmusics", ignoreCase = true) ||
                song.primaryCreatorName.equals("Just RJ Musics", ignoreCase = true) ||
                song.primaryCreatorUid == "admin_rj_primary"
            } else {
                song.primaryCreatorUid == user.uid ||
                song.primaryCreatorUsername.equals(username, ignoreCase = true) ||
                song.primaryCreatorUsername.equals(user.username, ignoreCase = true)
            }
        }
    }

    // Accurate calculation of real followers (users following this creator)
    val exactFollowersCount = remember(follows, user.uid) {
        follows.count { it.second == user.uid }
    }

    // Accurate calculation of real following (accounts this user follows)
    val exactFollowingCount = remember(follows, user.uid) {
        follows.count { it.first == user.uid }
    }

    // Accurate calculation of real total streams across user's songs
    val exactStreamsCount = remember(allSongs, user.uid, username, displayName, user.isAdmin) {
        allSongs.filter { song ->
            if (user.isAdmin) {
                song.primaryCreatorUid == user.uid ||
                song.primaryCreatorUsername.equals(username, ignoreCase = true) ||
                song.primaryCreatorUsername.equals("justrjmusics", ignoreCase = true) ||
                song.primaryCreatorName.equals("Just RJ Musics", ignoreCase = true) ||
                song.primaryCreatorUid == "admin_rj_primary"
            } else {
                song.primaryCreatorUid == user.uid ||
                song.primaryCreatorUsername.equals(username, ignoreCase = true) ||
                song.primaryCreatorUsername.equals(user.username, ignoreCase = true)
            }
        }.sumOf { it.totalViews }
    }

    val tabs = if (user.isAdmin) {
        listOf("Pending Approvals (${pendingSongs.size})", "My Uploads", "Collaborations", "Analytics")
    } else {
        listOf("My Uploads", "Collaborations", "Analytics")
    }

    val pendingInvites = collaborationInvites.filter {
        it.mentionedUid == username && it.status == CollaborationStatus.PENDING
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RjBackground),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Profile Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top control bar (Switch profile if admin, Edit profile, Logout)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (user.isAdmin) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Dedicated Admin Dashboard Option at the top of Profile
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF1565C0))
                                    .border(1.dp, Color(0xFF64B5F6), RoundedCornerShape(20.dp))
                                    .clickable { onOpenAdminDashboard() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("profile_open_admin_dashboard_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (pendingSongs.isNotEmpty()) "Admin Panel (${pendingSongs.size})" else "Admin Panel",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(RjCardElevated)
                                    .border(1.dp, RjSilverAccent, RoundedCornerShape(20.dp))
                                    .clickable { onSwitchProfileClick() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("profile_switch_admin_chip")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SwitchAccount, contentDescription = null, tint = RjSilverAccent, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Switch Profile",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .clickable { onUploadSongClick() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("profile_top_upload_song_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Upload Song",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(onClick = { showEditProfileDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = RjSilverMuted)
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color(0xFFEF5350))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Avatar with Photo Display & Clickable Photo Picker Launcher
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("profile_avatar_box"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(2.dp, if (user.verified || user.isAdmin) Color.White else RjBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = displayName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Camera Icon Badge Overlay
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Upload Photo",
                            tint = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Explicit "Add / Change Profile Photo" button for clear affordance
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF161616))
                        .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(16.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("profile_change_photo_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = RjSilverAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (photoUrl.isNotBlank()) "Change Photo" else "Add Profile Photo",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Name & Verified Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (user.verified || user.isAdmin) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Creator",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "@$username",
                    color = RjSilverAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = bio,
                        color = RjTextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // Instagram Link Button
                if (instagram.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(RjCard)
                            .border(1.dp, RjBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                val url = if (user.instagramUrl.isNotBlank()) user.instagramUrl else "https://instagram.com/$instagram"
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (ignored: Exception) {}
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("profile_instagram_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📸 Instagram: @$instagram", color = RjSilverAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open Instagram Profile", tint = RjSilverAccent, modifier = Modifier.size(13.dp))
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { showEditProfileDialog = true }
                    ) {
                        Text(text = "+ Add Instagram Handle", color = RjSilverAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row (Songs, Followers, Following, Monthly Streams)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RjCard)
                        .border(1.dp, RjBorder, RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatColumn(
                        value = java.text.NumberFormat.getIntegerInstance().format(exactSongCount),
                        label = "Songs"
                    )
                    StatColumn(
                        value = java.text.NumberFormat.getIntegerInstance().format(exactFollowersCount),
                        label = "Followers"
                    )
                    StatColumn(
                        value = java.text.NumberFormat.getIntegerInstance().format(exactFollowingCount),
                        label = "Following"
                    )
                    StatColumn(
                        value = java.text.NumberFormat.getIntegerInstance().format(exactStreamsCount),
                        label = "Streams"
                    )
                }
            }
        }

        // Pending Collaboration Invites Notification Banner
        if (pendingInvites.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Collaboration Invites (${pendingInvites.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    pendingInvites.forEach { invite ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(RjCardElevated)
                                .border(1.dp, RjSilverAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = invite.songTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "Invited by @${invite.inviterUsername}", color = RjTextMuted, fontSize = 11.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onRespondCollabInvite(invite.id, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Accept", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onRespondCollabInvite(invite.id, false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RjCard),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Decline", color = RjTextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Sub Tabs
        item {
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
        }

        val activeTabType = if (user.isAdmin) {
            when (selectedTab) {
                0 -> "PENDING_APPROVALS"
                1 -> "MY_UPLOADS"
                2 -> "COLLABORATIONS"
                else -> "ANALYTICS"
            }
        } else {
            when (selectedTab) {
                0 -> "MY_UPLOADS"
                1 -> "COLLABORATIONS"
                else -> "ANALYTICS"
            }
        }

        when (activeTabType) {
            "PENDING_APPROVALS" -> {
                if (pendingSongs.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "All Caught Up!",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No songs currently pending admin confirmation. Newly uploaded tracks will appear here for your review.",
                                color = RjTextMuted,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "SONG CONFIRMATION QUEUE (${pendingSongs.size})",
                                color = RjSilverAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Songs are hidden from the app feed until you confirm and approve them below.",
                                color = RjTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    items(pendingSongs) { song ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RjCard)
                                .border(1.dp, Color(0xFFE65100).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Cover Image
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(RjCardElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (song.coverUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = song.coverUrl,
                                                contentDescription = song.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(id = song.coverResId ?: R.drawable.cover_night_drive),
                                                contentDescription = song.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "By ${song.primaryCreatorName} (@${song.primaryCreatorUsername})",
                                            color = RjSilverAccent,
                                            fontSize = 12.sp
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "User ID: ${song.primaryCreatorUid}",
                                                color = RjSilverMuted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Creator User ID",
                                                tint = RjSilverAccent,
                                                modifier = Modifier
                                                    .size(13.dp)
                                                    .clickable {
                                                        clipboardManager.setText(AnnotatedString(song.primaryCreatorUid))
                                                        Toast.makeText(context, "User ID copied: ${song.primaryCreatorUid}", Toast.LENGTH_SHORT).show()
                                                    }
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(RjCardElevated)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = song.genre,
                                                    color = RjSilverMuted,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            if (song.audioUrl.isNotBlank()) {
                                                Text(
                                                    text = "• Audio Attached",
                                                    color = Color(0xFF81C784),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Preview Play Button
                                    IconButton(
                                        onClick = { onSongClick(song) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(RjCardElevated)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play Preview",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (song.description.isNotBlank()) {
                                    Text(
                                        text = song.description,
                                        color = RjTextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 2
                                    )
                                }

                                // Confirmation Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onApproveSong(song.songId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Confirm & Approve",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Button(
                                        onClick = { onRejectSong(song.songId, "Rejected by Admin") },
                                        colors = ButtonDefaults.buttonColors(containerColor = RjCardElevated),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Reject",
                                            color = Color(0xFFEF5350),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "MY_UPLOADS" -> {
                // My Uploads
                if (myUploads.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No uploaded tracks yet.",
                                color = RjTextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(myUploads) { song ->
                        Column {
                            // Status badge
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when (song.uploadStatus) {
                                                UploadStatus.PUBLISHED -> Color(0xFF1B5E20)
                                                UploadStatus.PENDING_REVIEW -> Color(0xFFE65100)
                                                UploadStatus.CHANGES_REQUESTED -> Color(0xFFB71C1C)
                                                else -> RjCard
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when (song.uploadStatus) {
                                            UploadStatus.PENDING_REVIEW -> "PENDING ADMIN CONFIRMATION"
                                            else -> song.uploadStatus.label.uppercase()
                                        },
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${song.totalViews} views • ${song.likesCount} likes",
                                    color = RjTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            SongListItemRow(
                                song = song,
                                onClick = { onSongClick(song) },
                                onMoreClick = { onSongOptionsClick(song) }
                            )
                        }
                    }
                }
            }
            "COLLABORATIONS" -> {
                // Collaborations / Featured On
                if (myCollabs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(text = "No collaborative tracks yet. When other creators tag you, they appear here.", color = RjTextMuted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    items(myCollabs) { song ->
                        SongListItemRow(
                            song = song,
                            onClick = { onSongClick(song) },
                            onMoreClick = { onSongOptionsClick(song) }
                        )
                    }
                }
            }
            "ANALYTICS" -> {
                // Creator Analytics Overview
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "OVERALL PERFORMANCE", color = RjSilverAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AnalyticsTile(title = "Total Views", value = "${analytics.totalViews}", modifier = Modifier.weight(1f))
                            AnalyticsTile(title = "Unique Listeners", value = "${analytics.uniqueListeners}", modifier = Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AnalyticsTile(title = "Total Likes", value = "${analytics.likesCount}", modifier = Modifier.weight(1f))
                            AnalyticsTile(title = "Downloads", value = "${analytics.downloadsCount}", modifier = Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AnalyticsTile(title = "Completion Rate", value = "${analytics.completionRate}%", modifier = Modifier.weight(1f))
                            AnalyticsTile(title = "Replay Rate", value = "${analytics.replayRate}%", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var editName by remember(displayName) { mutableStateOf(displayName) }
        var editBio by remember(bio) { mutableStateOf(bio) }
        var editInsta by remember(instagram) { mutableStateOf(instagram) }
        var editWeb by remember(user.websiteUrl) { mutableStateOf(user.websiteUrl) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = RjCardElevated,
            title = { Text("Edit Creator Profile", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Photo selector row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RjCard)
                            .border(1.dp, RjBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(1.dp, RjBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Profile Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = displayName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (photoUrl.isNotBlank()) "Change Profile Picture" else "Upload Profile Picture",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Tap to choose from gallery",
                                color = RjTextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Pick Photo",
                            tint = RjSilverAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = RjBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio / Producer Tag") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = RjBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editInsta,
                        onValueChange = { editInsta = it },
                        label = { Text("Instagram @handle") },
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
                        onUpdateProfile(editName, editBio, editInsta, editWeb)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = RjTextMuted)
                }
            }
        )
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text(text = label, color = RjTextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun AnalyticsTile(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(RjCard)
            .border(1.dp, RjBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(text = title, color = RjTextMuted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}
