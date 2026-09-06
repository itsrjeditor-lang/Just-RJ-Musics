package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AdminSubProfile
import com.example.data.model.UserProfile
import com.example.ui.Screen
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

import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun RjTopHeader(
    currentUser: UserProfile?,
    activeAdminProfile: AdminSubProfile?,
    unreadNotificationCount: Int,
    onLogoClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAdminClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFF181818))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Identity: Compact Official Logo + "JUST RJ MUSIC"
            Row(
                modifier = Modifier
                    .clickable { onLogoClick() }
                    .testTag("app_logo_header"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Official Brand Icon
                Image(
                    painter = painterResource(id = R.drawable.just_rj_logo),
                    contentDescription = "Just RJ Music Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "JUST RJ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "MUSIC",
                        color = Color(0xFFB8B8B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp
                    )
                }
            }

            // Right Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Admin Dashboard Button (if user is Admin)
                if (currentUser?.isAdmin == true) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(20.dp))
                            .clickable { onAdminClick() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("header_admin_badge")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Panel",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "ADMIN",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Notification Bell in Dark Pill
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141414))
                        .border(1.dp, Color(0xFF242424), CircleShape)
                        .clickable { onNotificationsClick() }
                        .testTag("header_notifications_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFFB8B8B8),
                        modifier = Modifier.size(18.dp)
                    )
                    if (unreadNotificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .align(Alignment.TopEnd)
                                .padding(top = 7.dp, end = 7.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color.Black, CircleShape)
                        )
                    }
                }

                // Profile Avatar / Photo / Initials
                val photoUrl = if (currentUser?.isAdmin == true) {
                    activeAdminProfile?.profileImageUrl?.ifBlank { currentUser.profileImageUrl } ?: currentUser.profileImageUrl
                } else {
                    currentUser?.profileImageUrl ?: ""
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF181818))
                        .border(1.5.dp, Color(0xFF444444), CircleShape)
                        .clickable { onProfileClick() }
                        .testTag("header_profile_button"),
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
                        val initial = if (currentUser?.isAdmin == true) {
                            activeAdminProfile?.displayName?.take(2) ?: currentUser.displayName.take(2)
                        } else {
                            currentUser?.displayName?.take(2) ?: "RJ"
                        }
                        Text(
                            text = initial.uppercase(),
                            color = Color.White,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RjBottomNavBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFF181818))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = if (currentScreen is Screen.Home) Icons.Filled.Home else Icons.Outlined.Home,
                label = "Home",
                isSelected = currentScreen is Screen.Home,
                onClick = { onNavigate(Screen.Home) },
                testTag = "nav_home"
            )

            BottomNavItem(
                icon = if (currentScreen is Screen.Search) Icons.Filled.Search else Icons.Outlined.Search,
                label = "Search",
                isSelected = currentScreen is Screen.Search,
                onClick = { onNavigate(Screen.Search) },
                testTag = "nav_search"
            )

            BottomNavItem(
                icon = if (currentScreen is Screen.Library) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                label = "Library",
                isSelected = currentScreen is Screen.Library,
                onClick = { onNavigate(Screen.Library) },
                testTag = "nav_library"
            )

            BottomNavItem(
                icon = if (currentScreen is Screen.Profile) Icons.Filled.Person else Icons.Outlined.Person,
                label = "Profile",
                isSelected = currentScreen is Screen.Profile,
                onClick = { onNavigate(Screen.Profile) },
                testTag = "nav_profile"
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
    }
}
