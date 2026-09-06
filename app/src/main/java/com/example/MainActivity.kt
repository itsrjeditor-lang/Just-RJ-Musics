package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Playlist
import com.example.data.model.ReactionType
import com.example.data.model.Song
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.Screen
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AdminProfileSwitcherDialog
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.components.QueueSheet
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.RjBottomNavBar
import com.example.ui.components.RjTopHeader
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.SignUpScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.library.PlaylistDetailScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.upload.UploadSongScreen
import com.example.ui.theme.RjBackground
import com.example.ui.theme.RjMusicsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RjMusicsTheme {
                RjMusicsApp()
            }
        }
    }
}

@Composable
fun RjMusicsApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(context.applicationContext)
    )

    // State Collection
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeAdminProfile by viewModel.activeAdminProfile.collectAsState()
    val adminSubProfiles by viewModel.adminSubProfiles.collectAsState()
    val activeAdminProfileId by viewModel.activeAdminProfileId.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val featuredSlides by viewModel.featuredSlides.collectAsState()
    val genreCategories = viewModel.genreCategories
    val selectedCategory by viewModel.selectedHomeCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchFilter by viewModel.selectedSearchFilter.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val collaborationInvites by viewModel.collaborationInvites.collectAsState()
    val contentReports by viewModel.contentReports.collectAsState()
    val userReactions by viewModel.userReactions.collectAsState()
    val registeredAccounts by viewModel.registeredAccounts.collectAsState()
    val follows by viewModel.follows.collectAsState()

    // Player State
    val playerState by viewModel.playerState.collectAsState()

    // Dialog & Overlay States
    var showFullPlayer by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var targetSongForPlaylist by remember { mutableStateOf<Song?>(null) }
    var targetSongForReport by remember { mutableStateOf<Song?>(null) }
    var showAdminProfileSwitcher by remember { mutableStateOf(false) }
    var activePlaylistForDetail by remember { mutableStateOf<Playlist?>(null) }

    // Intercept Back Press if Full Player or Queue is open or in sub-screen
    BackHandler(enabled = showQueueSheet || showFullPlayer || currentScreen != Screen.Home && currentScreen != Screen.Splash && currentScreen != Screen.Login) {
        if (showQueueSheet) {
            showQueueSheet = false
        } else if (showFullPlayer) {
            showFullPlayer = false
        } else if (currentScreen is Screen.PlaylistDetail) {
            viewModel.navigateTo(Screen.Library)
        } else if (currentScreen is Screen.UploadSong || currentScreen is Screen.AdminDashboard) {
            viewModel.navigateTo(Screen.Profile)
        } else if (currentScreen is Screen.Notifications) {
            viewModel.navigateTo(Screen.Home)
        } else if (currentScreen is Screen.SignUp || currentScreen is Screen.ForgotPassword) {
            viewModel.navigateTo(Screen.Login)
        } else {
            viewModel.navigateTo(Screen.Home)
        }
    }

    val isAuthScreen = currentScreen is Screen.Splash || currentScreen is Screen.Login || currentScreen is Screen.SignUp || currentScreen is Screen.ForgotPassword
    val isFullScreenModal = currentScreen is Screen.UploadSong || currentScreen is Screen.AdminDashboard || currentScreen is Screen.Notifications || currentScreen is Screen.PlaylistDetail

    val unreadNotificationsCount = notifications.count { !it.isRead }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (!isAuthScreen && !isFullScreenModal) {
                RjTopHeader(
                    currentUser = currentUser,
                    activeAdminProfile = activeAdminProfile,
                    unreadNotificationCount = unreadNotificationsCount,
                    onLogoClick = { viewModel.navigateTo(Screen.Home) },
                    onNotificationsClick = { viewModel.navigateTo(Screen.Notifications) },
                    onAdminClick = { viewModel.navigateTo(Screen.AdminDashboard) },
                    onProfileClick = { viewModel.navigateTo(Screen.Profile) }
                )
            }
        },
        bottomBar = {
            if (!isAuthScreen && !isFullScreenModal) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .navigationBarsPadding()
                ) {
                    // Mini Player (Docked directly above Bottom Navigation Bar)
                    if (playerState.currentSong != null && !showFullPlayer) {
                        MiniPlayer(
                            playerState = playerState,
                            onExpand = { showFullPlayer = true },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onPrevious = { viewModel.skipToPrevious() },
                            onNext = { viewModel.skipToNext() },
                            onOpenQueue = { showQueueSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    RjBottomNavBar(
                        currentScreen = currentScreen,
                        onNavigate = { screen ->
                            viewModel.navigateTo(screen)
                        }
                    )
                }
            } else if (!isAuthScreen && isFullScreenModal && playerState.currentSong != null && !showFullPlayer && currentScreen !is Screen.AdminDashboard && currentScreen !is Screen.UploadSong) {
                // Dock mini player at bottom of sub-screens like PlaylistDetail or Notifications
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .navigationBarsPadding()
                ) {
                    MiniPlayer(
                        playerState = playerState,
                        onExpand = { showFullPlayer = true },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onPrevious = { viewModel.skipToPrevious() },
                        onNext = { viewModel.skipToNext() },
                        onOpenQueue = { showQueueSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        },
        containerColor = RjBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(RjBackground)
        ) {
            // Main Screen Routing
            when (val screen = currentScreen) {
                is Screen.Splash -> {
                    SplashScreen(
                        onSplashFinished = {
                            if (currentUser != null) {
                                viewModel.navigateTo(Screen.Home)
                            } else {
                                viewModel.navigateTo(Screen.Login)
                            }
                        }
                    )
                }
                is Screen.Login -> {
                    LoginScreen(
                        onLogin = { email, pass, onError ->
                            val result = viewModel.loginUser(email, pass)
                            if (result.isSuccess) {
                                val user = result.getOrNull()
                                viewModel.navigateTo(Screen.Home)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Welcome back, ${user?.displayName}!")
                                }
                            } else {
                                val errorMsg = result.exceptionOrNull()?.message ?: "Login failed. Please create a new account first."
                                onError(errorMsg)
                                scope.launch {
                                    snackbarHostState.showSnackbar(errorMsg)
                                }
                            }
                        },
                        onNavigateSignUp = { viewModel.navigateTo(Screen.SignUp) },
                        onNavigateForgotPassword = { viewModel.navigateTo(Screen.ForgotPassword) },
                        onQuickAdminLogin = {
                            val result = viewModel.loginUser("itsrjeditor@gmail.com", "admin123")
                            if (result.isSuccess) {
                                viewModel.navigateTo(Screen.Home)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Logged in as Verified Just RJ Music Administrator")
                                }
                            }
                        }
                    )
                }
                is Screen.SignUp -> {
                    SignUpScreen(
                        onSignUp = { name, username, email, pass, bio, insta, onError ->
                            val result = viewModel.signUpUser(name, username, email, pass, bio, insta)
                            if (result.isSuccess) {
                                val user = result.getOrNull()
                                viewModel.navigateTo(Screen.Home)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Account created! Welcome to Just RJ Music, ${user?.displayName}.")
                                }
                            } else {
                                val errorMsg = result.exceptionOrNull()?.message ?: "Could not create account."
                                onError(errorMsg)
                                scope.launch {
                                    snackbarHostState.showSnackbar(errorMsg)
                                }
                            }
                        },
                        onNavigateLogin = { viewModel.navigateTo(Screen.Login) }
                    )
                }
                is Screen.ForgotPassword -> {
                    ForgotPasswordScreen(
                        onResetPassword = { email, newPass, onError, onSuccess ->
                            val result = viewModel.resetUserPassword(email, newPass)
                            if (result.isSuccess) {
                                val msg = result.getOrNull() ?: "Password reset successfully!"
                                onSuccess(msg)
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            } else {
                                val err = result.exceptionOrNull()?.message ?: "Failed to reset password."
                                onError(err)
                                scope.launch {
                                    snackbarHostState.showSnackbar(err)
                                }
                            }
                        },
                        onNavigateBack = { viewModel.navigateTo(Screen.Login) }
                    )
                }
                is Screen.Home -> {
                    HomeScreen(
                        featuredSlides = featuredSlides,
                        songs = songs,
                        recommendedSongs = songs.shuffled(),
                        trendingSongs = viewModel.getTrendingSongs(),
                        categories = genreCategories,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { viewModel.selectCategory(it) },
                        onSongClick = { song -> viewModel.playSong(song, songs) },
                        onSlideClick = { slide ->
                            val s = songs.firstOrNull { it.songId == slide.destinationId }
                            if (s != null) viewModel.playSong(s, songs)
                        },
                        onSongOptionsClick = { song -> targetSongForPlaylist = song },
                        onCreatorClick = { username ->
                            viewModel.setSearchQuery("@$username")
                            viewModel.navigateTo(Screen.Search)
                        },
                        onViewAllTrending = {
                            viewModel.setSearchQuery("trending")
                            viewModel.navigateTo(Screen.Search)
                        }
                    )
                }
                is Screen.Search -> {
                    SearchScreen(
                        searchQuery = searchQuery,
                        selectedFilter = searchFilter,
                        recentSearches = recentSearches,
                        categories = genreCategories,
                        allSongs = songs,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        onFilterChange = { viewModel.setSearchFilter(it) },
                        onSongClick = { song -> viewModel.playSong(song, songs) },
                        onSongOptionsClick = { song -> targetSongForPlaylist = song },
                        onGenreCardClick = { genre ->
                            viewModel.setSearchQuery(genre)
                        }
                    )
                }
                is Screen.Library -> {
                    LibraryScreen(
                        playlists = playlists,
                        downloadedSongs = downloadedSongs,
                        allSongs = songs,
                        userReactions = userReactions,
                        currentUserId = currentUser?.uid,
                        onPlaylistClick = { pl ->
                            activePlaylistForDetail = pl
                            viewModel.navigateTo(Screen.PlaylistDetail(pl.playlistId))
                        },
                        onCreatePlaylistClick = {
                            viewModel.createPlaylist("My Favorite Vibes", emptyList())
                            scope.launch { snackbarHostState.showSnackbar("Created new playlist!") }
                        },
                        onSongClick = { song -> viewModel.playSong(song, songs) },
                        onSongOptionsClick = { song -> targetSongForPlaylist = song }
                    )
                }
                is Screen.PlaylistDetail -> {
                    val pl = activePlaylistForDetail ?: playlists.firstOrNull { it.playlistId == screen.playlistId }
                    if (pl != null) {
                        PlaylistDetailScreen(
                            playlist = pl,
                            allSongs = songs,
                            onNavigateBack = { viewModel.navigateTo(Screen.Library) },
                            onPlayAll = { list -> if (list.isNotEmpty()) viewModel.playSong(list.first(), list) },
                            onShufflePlay = { list -> if (list.isNotEmpty()) viewModel.playSong(list.shuffled().first(), list.shuffled()) },
                            onSongClick = { song -> viewModel.playSong(song, songs) },
                            onSongOptionsClick = { song -> targetSongForPlaylist = song },
                            onDeletePlaylist = { id ->
                                viewModel.deletePlaylist(id)
                                viewModel.navigateTo(Screen.Library)
                            }
                        )
                    }
                }
                is Screen.Profile -> {
                    val activeProfile = adminSubProfiles.firstOrNull { it.profileId == activeAdminProfileId }
                    val creatorAnalytics = viewModel.getCreatorAnalytics(currentUser?.uid ?: "")
                    ProfileScreen(
                        currentUser = currentUser,
                        activeAdminProfile = activeProfile,
                        allSongs = songs,
                        collaborationInvites = collaborationInvites,
                        analytics = creatorAnalytics,
                        follows = follows,
                        onSwitchProfileClick = { showAdminProfileSwitcher = true },
                        onUpdateProfile = { name, bio, insta, web ->
                            viewModel.updateUserProfile(name, bio, insta, web)
                            scope.launch { snackbarHostState.showSnackbar("Profile updated successfully") }
                        },
                        onUpdateProfilePhoto = { photoUri ->
                            viewModel.updateProfilePhoto(photoUri)
                            scope.launch { snackbarHostState.showSnackbar("Profile photo updated successfully!") }
                        },
                        onRespondCollabInvite = { inviteId, accept ->
                            viewModel.respondToCollabInvite(inviteId, accept)
                            scope.launch {
                                snackbarHostState.showSnackbar(if (accept) "Collaboration accepted!" else "Invitation declined.")
                            }
                        },
                        onSongClick = { song -> viewModel.playSong(song, songs) },
                        onSongOptionsClick = { song -> targetSongForPlaylist = song },
                        onUploadSongClick = { viewModel.navigateTo(Screen.UploadSong) },
                        onLogout = {
                            viewModel.logout()
                            viewModel.navigateTo(Screen.Login)
                        },
                        onApproveSong = { songId ->
                            viewModel.approveSong(songId)
                            scope.launch { snackbarHostState.showSnackbar("Song confirmed & published to app feed!") }
                        },
                        onRejectSong = { songId, reason ->
                            viewModel.rejectSong(songId, reason)
                            scope.launch { snackbarHostState.showSnackbar("Song rejected.") }
                        },
                        onOpenAdminDashboard = { viewModel.navigateTo(Screen.AdminDashboard) }
                    )
                }
                is Screen.UploadSong -> {
                    UploadSongScreen(
                        onNavigateBack = { viewModel.navigateTo(Screen.Profile) },
                        onSubmitUpload = { title, desc, lyrics, explicit, allowDl, collabs, audioUri, coverUri, genre ->
                            viewModel.uploadSong(title, desc, lyrics, explicit, allowDl, collabs, audioUri, coverUri, genre)
                        },
                        onUploadSuccess = { createdSong ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (currentUser?.isAdmin == true) "Song published live to catalog!"
                                    else "Song uploaded! It is now pending Admin approval before appearing in the feed."
                                )
                            }
                            viewModel.navigateTo(Screen.Profile)
                        },
                        isAdmin = currentUser?.isAdmin == true
                    )
                }
                is Screen.AdminDashboard -> {
                    AdminDashboardScreen(
                        songs = songs,
                        featuredSlides = featuredSlides,
                        adminSubProfiles = adminSubProfiles,
                        activeProfileId = activeAdminProfileId,
                        reports = contentReports,
                        registeredAccounts = registeredAccounts,
                        onNavigateBack = { viewModel.navigateTo(Screen.Home) },
                        onPlaySong = { song -> viewModel.playSong(song, songs) },
                        onApproveSong = { songId ->
                            viewModel.approveSong(songId)
                            scope.launch { snackbarHostState.showSnackbar("Song approved & published!") }
                        },
                        onRejectSong = { songId, reason ->
                            viewModel.rejectSong(songId, reason)
                            scope.launch { snackbarHostState.showSnackbar("Song rejected.") }
                        },
                        onRequestChanges = { songId, notes ->
                            viewModel.requestSongChanges(songId, notes)
                            scope.launch { snackbarHostState.showSnackbar("Changes requested from creator.") }
                        },
                        onEditSongMetadata = { songId, title, genre, tags, explicit ->
                            viewModel.editSongMetadata(songId, title, genre, tags, explicit)
                        },
                        onDeleteSongPermanently = { songId ->
                            viewModel.deleteSongPermanently(songId)
                            scope.launch { snackbarHostState.showSnackbar("Song permanently deleted from RJ Musics.") }
                        },
                        onDeleteUser = { uid ->
                            val result = viewModel.deleteUser(uid)
                            scope.launch {
                                snackbarHostState.showSnackbar(result.getOrDefault("User account and uploaded content deleted."))
                            }
                        },
                        onSetTrendingRank = { songId, rank, pin ->
                            viewModel.setTrendingRank(songId, rank, pin)
                            scope.launch { snackbarHostState.showSnackbar("Trending updated!") }
                        },
                        onAddFeaturedSlide = { slide -> viewModel.addFeaturedSlide(slide) },
                        onRemoveFeaturedSlide = { slideId -> viewModel.removeFeaturedSlide(slideId) },
                        onToggleSlidePin = { slideId -> viewModel.toggleSlidePin(slideId) },
                        onCreateAdminSubProfile = { name, user, bio, insta ->
                            viewModel.createAdminSubProfile(name, user, bio, insta)
                            scope.launch { snackbarHostState.showSnackbar("Admin Sub-Profile created!") }
                        },
                        onSwitchSubProfile = { profileId ->
                            viewModel.switchActiveAdminProfile(profileId)
                            scope.launch { snackbarHostState.showSnackbar("Switched active profile") }
                        },
                        onDeleteSubProfile = { profileId ->
                            viewModel.deleteAdminSubProfile(profileId)
                        }
                    )
                }
                is Screen.Notifications -> {
                    NotificationsScreen(
                        notifications = notifications,
                        onNavigateBack = { viewModel.navigateTo(Screen.Home) },
                        onMarkAllRead = { viewModel.markAllNotificationsRead() },
                        onNotificationClick = { notification ->
                            viewModel.markNotificationRead(notification.id)
                            if (notification.actionTargetId != null) {
                                val s = songs.firstOrNull { it.songId == notification.actionTargetId }
                                if (s != null) viewModel.playSong(s, songs)
                            }
                        }
                    )
                }
                is Screen.UserProfileView -> {
                    viewModel.setSearchQuery("@${screen.username}")
                    viewModel.navigateTo(Screen.Search)
                }
                is Screen.GenreDetail -> {
                    viewModel.setSearchQuery(screen.genreName)
                    viewModel.navigateTo(Screen.Search)
                }
                is Screen.Settings -> {
                    viewModel.navigateTo(Screen.Profile)
                }
            }

            // Full Screen Player Sheet (Animated slide up from bottom)
            AnimatedVisibility(
                visible = showFullPlayer && playerState.currentSong != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                playerState.currentSong?.let { song ->
                    val userReactionKey = "${currentUser?.uid}_${song.songId}"
                    val currentReaction = userReactions[userReactionKey] ?: ReactionType.NONE
                    val isDownloaded = downloadedSongs.any { it.songId == song.songId }

                    FullPlayerSheet(
                        playerState = playerState,
                        userReaction = currentReaction,
                        isDownloaded = isDownloaded,
                        onDismiss = { showFullPlayer = false },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.skipToNext() },
                        onPrevious = { viewModel.skipToPrevious() },
                        onSeekTo = { pos -> viewModel.seekTo(pos) },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleRepeat = { viewModel.toggleRepeat() },
                        onToggleAutoplay = { viewModel.toggleAutoplay() },
                        onLikeClick = { viewModel.setSongReaction(song.songId, ReactionType.LIKE) },
                        onDislikeClick = { viewModel.setSongReaction(song.songId, ReactionType.DISLIKE) },
                        onDownloadClick = {
                            viewModel.downloadSongForOffline(song)
                            scope.launch {
                                if (isDownloaded) {
                                    snackbarHostState.showSnackbar("Removed '${song.title}' from Offline Downloads.")
                                } else {
                                    snackbarHostState.showSnackbar("Downloaded '${song.title}' to In-App Vault! (App-Only Offline Access)")
                                }
                            }
                        },
                        onAddToPlaylistClick = {
                            targetSongForPlaylist = song
                        },
                        onShareClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Listen to ${song.title} on RJ Musics")
                                putExtra(Intent.EXTRA_TEXT, "Stream '${song.title}' by ${song.displayArtists} on RJ Musics Official Platform! https://rjmusics.app/track/${song.songId}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                        },
                        onReportClick = {
                            targetSongForReport = song
                        },
                        onOpenQueue = {
                            showQueueSheet = true
                        }
                    )
                }
            }

            // Queue Modal Sheet
            if (showQueueSheet) {
                QueueSheet(
                    queue = playerState.queue,
                    currentSong = playerState.currentSong,
                    isPlaying = playerState.isPlaying,
                    onDismiss = { showQueueSheet = false },
                    onSongSelect = { track ->
                        viewModel.playSong(track, playerState.queue)
                    },
                    onClearQueue = {
                        viewModel.clearQueue()
                    }
                )
            }

            // Dialog: Add To Playlist
            if (targetSongForPlaylist != null) {
                AddToPlaylistDialog(
                    song = targetSongForPlaylist!!,
                    playlists = playlists,
                    onDismiss = { targetSongForPlaylist = null },
                    onSelectPlaylist = { pl ->
                        viewModel.addSongToPlaylist(pl.playlistId, targetSongForPlaylist!!.songId)
                        scope.launch { snackbarHostState.showSnackbar("Added to '${pl.title}'") }
                        targetSongForPlaylist = null
                    },
                    onCreateNewPlaylist = { name ->
                        val created = viewModel.createPlaylist(name, listOf(targetSongForPlaylist!!.songId))
                        scope.launch { snackbarHostState.showSnackbar("Created playlist '$name' with song!") }
                        targetSongForPlaylist = null
                    }
                )
            }

            // Dialog: Report Content
            if (targetSongForReport != null) {
                ReportContentDialog(
                    targetTitle = targetSongForReport!!.title,
                    onDismiss = { targetSongForReport = null },
                    onSubmitReport = { reason, details ->
                        viewModel.reportSong(targetSongForReport!!.songId, targetSongForReport!!.title, reason, details)
                        scope.launch { snackbarHostState.showSnackbar("Report submitted. Thank you for keeping RJ Musics safe.") }
                        targetSongForReport = null
                    }
                )
            }

            // Dialog: Admin Profile Switcher
            if (showAdminProfileSwitcher) {
                AdminProfileSwitcherDialog(
                    subProfiles = adminSubProfiles,
                    activeProfileId = activeAdminProfileId,
                    onDismiss = { showAdminProfileSwitcher = false },
                    onSelectProfile = { profId ->
                        viewModel.switchActiveAdminProfile(profId)
                        showAdminProfileSwitcher = false
                    },
                    onCreateNewProfile = { name, user, bio, insta ->
                        viewModel.createAdminSubProfile(name, user, bio, insta)
                    }
                )
            }
        }
    }
}
