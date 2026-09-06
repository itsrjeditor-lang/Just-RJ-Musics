package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DownloadedSongEntity
import com.example.data.model.AdminSubProfile
import com.example.data.model.CollaborationInvite
import com.example.data.model.CreatorAnalytics
import com.example.data.model.FeaturedSlide
import com.example.data.model.GenreCategory
import com.example.data.model.NotificationItem
import com.example.data.model.Playlist
import com.example.data.model.ReactionType
import com.example.data.model.Song
import com.example.data.model.SongReport
import com.example.data.model.StoredAccount
import com.example.data.model.UploadStatus
import com.example.data.model.UserProfile
import com.example.data.repository.AppRepository
import com.example.service.player.MusicPlayerManager
import com.example.service.player.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    data object Splash : Screen()
    data object Login : Screen()
    data object SignUp : Screen()
    data object ForgotPassword : Screen()
    data object Home : Screen()
    data object Search : Screen()
    data object Library : Screen()
    data object Profile : Screen()
    data class UserProfileView(val username: String) : Screen()
    data object UploadSong : Screen()
    data object Notifications : Screen()
    data object Settings : Screen()
    data object AdminDashboard : Screen()
    data class PlaylistDetail(val playlistId: String) : Screen()
    data class GenreDetail(val genreName: String) : Screen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = AppRepository(application.applicationContext)
    val playerManager = MusicPlayerManager(application.applicationContext)

    // Current Navigation Screen
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Dialog & overlay states
    private val _isFullPlayerVisible = MutableStateFlow(false)
    val isFullPlayerVisible: StateFlow<Boolean> = _isFullPlayerVisible.asStateFlow()

    private val _isQueueVisible = MutableStateFlow(false)
    val isQueueVisible: StateFlow<Boolean> = _isQueueVisible.asStateFlow()

    private val _selectedHomeCategory = MutableStateFlow("all")
    val selectedHomeCategory: StateFlow<String> = _selectedHomeCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSearchFilter = MutableStateFlow("All")
    val selectedSearchFilter: StateFlow<String> = _selectedSearchFilter.asStateFlow()

    private val _activeAdminProfileId = MutableStateFlow<String?>("prof_rj_editor")
    val activeAdminProfileId: StateFlow<String?> = _activeAdminProfileId.asStateFlow()

    // State bindings from repository & player manager
    val currentUser: StateFlow<UserProfile?> = repository.currentUser
    val registeredAccounts: StateFlow<List<StoredAccount>> = repository.registeredAccounts
    val activeAdminProfile: StateFlow<AdminSubProfile?> = repository.activeAdminProfile
    val adminSubProfiles: StateFlow<List<AdminSubProfile>> = repository.adminSubProfiles
    val songs: StateFlow<List<Song>> = repository.songs
    val featuredSlides: StateFlow<List<FeaturedSlide>> = repository.featuredSlides
    val playlists: StateFlow<List<Playlist>> = repository.playlists
    val collaborationInvites: StateFlow<List<CollaborationInvite>> = repository.collaborationInvites
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications
    val contentReports: StateFlow<List<SongReport>> = repository.reports
    val recentSearches: StateFlow<List<String>> = repository.recentSearches
    val userReactions: StateFlow<Map<String, ReactionType>> = repository.userReactions
    val follows: StateFlow<Set<Pair<String, String>>> = repository.follows
    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val genreCategories: List<GenreCategory> = repository.categories

    val downloadedSongs: StateFlow<List<DownloadedSongEntity>> = repository.getDownloadedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        playerManager.onTrackValidView = { songId ->
            repository.recordValidView(songId)
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectCategory(categoryId: String) {
        _selectedHomeCategory.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchFilter(filter: String) {
        _selectedSearchFilter.value = filter
    }

    // Authentication Actions
    fun loginUser(email: String, pass: String): Result<UserProfile> {
        return repository.login(email, pass)
    }

    fun signUpUser(name: String, username: String, email: String, pass: String, bio: String, instagram: String): Result<UserProfile> {
        return repository.signUp(name, username, email, pass, bio, instagram)
    }

    fun sendPasswordReset(email: String): Result<String> {
        return repository.sendPasswordReset(email)
    }

    fun resetUserPassword(email: String, newPass: String): Result<String> {
        return repository.resetPassword(email, newPass)
    }

    fun logout() {
        repository.logout()
    }

    fun updateUserProfile(name: String, bio: String, instagram: String, website: String) {
        repository.updateProfile(name, bio, instagram, website)
    }

    fun updateProfilePhoto(photoUri: String) {
        repository.updateProfileImage(photoUri)
    }

    // Sub-Profile Management
    fun createAdminSubProfile(name: String, username: String, bio: String, instagram: String) {
        val created = repository.createAdminSubProfile(name, username, bio, instagram)
        _activeAdminProfileId.value = created.profileId
    }

    fun switchActiveAdminProfile(profileId: String) {
        _activeAdminProfileId.value = profileId
        repository.switchActiveProfile(profileId)
    }

    fun deleteAdminSubProfile(profileId: String) {
        repository.deleteAdminSubProfile(profileId)
        if (_activeAdminProfileId.value == profileId) {
            _activeAdminProfileId.value = adminSubProfiles.value.firstOrNull()?.profileId
        }
    }

    // Song Operations
    suspend fun uploadSong(
        title: String,
        desc: String,
        lyrics: String,
        explicit: Boolean,
        allowDownload: Boolean,
        collabs: List<String>,
        audioUri: String = "",
        coverUri: String = "",
        genre: String = "Pop"
    ): Song {
        return repository.uploadSong(title, desc, lyrics, explicit, allowDownload, collabs, audioUri, coverUri, genre)
    }

    fun approveSong(songId: String, note: String = "") {
        repository.approveSong(songId, note)
    }

    fun rejectSong(songId: String, reason: String) {
        repository.rejectSong(songId, reason)
    }

    fun requestSongChanges(songId: String, notes: String) {
        repository.requestSongChanges(songId, notes)
    }

    fun editSongMetadata(songId: String, title: String, genre: String, tags: List<String>, explicit: Boolean) {
        repository.editSongMetadata(songId, title, genre, tags, explicit)
    }

    fun deleteSongPermanently(songId: String) {
        repository.deleteSong(songId)
        playerManager.removeFromQueue(songId)
    }

    fun setTrendingRank(songId: String, rank: Int, pin: Boolean = false) {
        repository.setSongTrendingRank(songId, rank, pin)
    }

    fun addFeaturedSlide(slide: FeaturedSlide) {
        repository.addFeaturedSlide(slide)
    }

    fun removeFeaturedSlide(slideId: String) {
        repository.removeFeaturedSlide(slideId)
    }

    fun toggleSlidePin(slideId: String) {
        repository.toggleSlidePin(slideId)
    }

    fun setSongReaction(songId: String, reaction: ReactionType) {
        repository.toggleReaction(songId, reaction)
    }

    fun downloadSongForOffline(song: Song) {
        repository.toggleDownload(song)
    }

    fun reportSong(songId: String, title: String, reason: String, details: String) {
        repository.submitReport("SONG", songId, title, reason, details)
    }

    fun respondToCollabInvite(inviteId: String, accept: Boolean) {
        repository.respondToCollaboration(inviteId, accept)
    }

    // Playlists
    fun createPlaylist(name: String, songIds: List<String> = emptyList()): Playlist {
        val pl = repository.createPlaylist(name, "Curated on RJ Musics", true)
        songIds.forEach { songId ->
            repository.addSongToPlaylist(pl.playlistId, songId)
        }
        return pl
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        repository.addSongToPlaylist(playlistId, songId)
    }

    fun deletePlaylist(playlistId: String) {
        repository.deletePlaylist(playlistId)
    }

    // Notifications
    fun markNotificationRead(id: String) {
        repository.markNotificationRead(id)
    }

    fun markAllNotificationsRead() {
        repository.markAllNotificationsRead()
    }

    // Playback Helpers
    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        playerManager.playSong(song, queue)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun skipToNext() {
        playerManager.playNext(repository.getRecommendedSongs())
    }

    fun skipToPrevious() {
        playerManager.playPrevious()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playerManager.toggleRepeat()
    }

    fun toggleAutoplay() {
        playerManager.toggleAutoplay()
    }

    fun clearQueue() {
        playerManager.clearQueue()
    }

    fun addToQueue(song: Song) {
        playerManager.addToQueue(song)
    }

    fun removeFromQueue(songId: String) {
        playerManager.removeFromQueue(songId)
    }

    fun stopPlayback() {
        playerManager.release()
    }

    fun getTrendingSongs(): List<Song> {
        return repository.getTrendingSongs()
    }

    fun getCreatorAnalytics(creatorUid: String): CreatorAnalytics {
        return repository.getCreatorAnalytics(creatorUid)
    }

    fun deleteUser(uid: String): Result<String> {
        return repository.deleteUser(uid)
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val app = context.applicationContext as Application
            return MainViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
