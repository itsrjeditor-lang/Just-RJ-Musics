package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.DownloadedSongEntity
import com.example.data.local.ListeningHistoryEntity
import com.example.data.local.MusicDatabase
import com.example.data.model.AdminSubProfile
import com.example.data.model.CollaborationInvite
import com.example.data.model.CollaborationStatus
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
import com.example.service.ai.AudioAnalysisResult
import com.example.service.ai.GenreDetectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AppRepository(private val context: Context) {
    private val db = MusicDatabase.getDatabase(context)
    private val dao = db.musicDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    // Current Auth & Profile State
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _adminSubProfiles = MutableStateFlow<List<AdminSubProfile>>(emptyList())
    val adminSubProfiles: StateFlow<List<AdminSubProfile>> = _adminSubProfiles.asStateFlow()

    private val _activeAdminProfile = MutableStateFlow<AdminSubProfile?>(null)
    val activeAdminProfile: StateFlow<AdminSubProfile?> = _activeAdminProfile.asStateFlow()

    // Songs Collection
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    // Featured Slides
    private val _featuredSlides = MutableStateFlow<List<FeaturedSlide>>(emptyList())
    val featuredSlides: StateFlow<List<FeaturedSlide>> = _featuredSlides.asStateFlow()

    // Playlists
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // Collaboration Invites
    private val _collaborationInvites = MutableStateFlow<List<CollaborationInvite>>(emptyList())
    val collaborationInvites: StateFlow<List<CollaborationInvite>> = _collaborationInvites.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // Follows Set: (followerUid to followingUid)
    private val _follows = MutableStateFlow<Set<Pair<String, String>>>(emptySet())
    val follows: StateFlow<Set<Pair<String, String>>> = _follows.asStateFlow()

    // User Reactions: (userId_songId to ReactionType)
    private val _userReactions = MutableStateFlow<Map<String, ReactionType>>(emptyMap())
    val userReactions: StateFlow<Map<String, ReactionType>> = _userReactions.asStateFlow()

    // Reports
    private val _reports = MutableStateFlow<List<SongReport>>(emptyList())
    val reports: StateFlow<List<SongReport>> = _reports.asStateFlow()

    // Search History
    private val _recentSearches = MutableStateFlow<List<String>>(listOf("Lo-Fi Beats", "Midnight Drive", "Just RJ Musics", "Punjabi Pop", "Acoustic Chill"))
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // User Taste Map: (genre to score)
    private val _genreTasteWeights = MutableStateFlow<Map<String, Int>>(mapOf("Lo-Fi" to 5, "Hip-Hop" to 4, "Punjabi" to 3))

    // Categories
    val categories: List<GenreCategory> = listOf(
        GenreCategory("all", "All", "Explore All Music"),
        GenreCategory("pop", "Pop", "Chart Toppers & Hits", R.drawable.cover_night_drive),
        GenreCategory("hiphop", "Hip-Hop", "Beats, Bars & Energy", R.drawable.cover_night_drive),
        GenreCategory("rap", "Rap", "Hard-Hitting Flows", R.drawable.cover_night_drive),
        GenreCategory("lofi", "Lo-Fi", "Late Night & Study Chill", R.drawable.cover_lofi),
        GenreCategory("chill", "Chill", "Relax & Unwind", R.drawable.cover_lofi),
        GenreCategory("punjabi", "Punjabi", "Desi Urban & Banger Beats", R.drawable.cover_night_drive),
        GenreCategory("bollywood", "Bollywood", "Melodies & Romance", R.drawable.cover_night_drive),
        GenreCategory("electronic", "Electronic", "EDM & Club Rhythms", R.drawable.cover_night_drive),
        GenreCategory("rock", "Rock", "Guitars & Heavy Anthems", R.drawable.cover_night_drive),
        GenreCategory("acoustic", "Acoustic", "Raw Vocals & Strings", R.drawable.cover_lofi),
        GenreCategory("sad", "Sad", "Heartbreak & Deep Feelings", R.drawable.cover_lofi),
        GenreCategory("romantic", "Romantic", "Love & Soulful Tracks", R.drawable.cover_lofi),
        GenreCategory("workout", "Workout", "High Energy & Cardio", R.drawable.cover_night_drive),
        GenreCategory("focus", "Focus", "Deep Concentration", R.drawable.cover_lofi),
        GenreCategory("devotional", "Devotional", "Spiritual & Sacred Mantras", R.drawable.cover_lofi),
        GenreCategory("instrumental", "Instrumental", "Pure Melodic Waves", R.drawable.cover_lofi),
        GenreCategory("indie", "Indie", "Underground & Authentic", R.drawable.cover_lofi),
        GenreCategory("classical", "Classical", "Timeless Compositions", R.drawable.cover_lofi)
    )

    private val prefs = context.getSharedPreferences("rj_musics_auth_prefs", Context.MODE_PRIVATE)

    // Permanent Deletion Tracking
    fun getDeletedUserUids(): Set<String> {
        return prefs.getStringSet("permanently_deleted_user_uids_v5", emptySet()) ?: emptySet()
    }

    fun markUserPermanentlyDeleted(uid: String) {
        val currentSet = getDeletedUserUids().toMutableSet()
        currentSet.add(uid)
        prefs.edit().putStringSet("permanently_deleted_user_uids_v5", currentSet).commit()
    }

    fun getDeletedSongIds(): Set<String> {
        return prefs.getStringSet("permanently_deleted_song_ids_v5", emptySet()) ?: emptySet()
    }

    fun markSongPermanentlyDeleted(songId: String) {
        val currentSet = getDeletedSongIds().toMutableSet()
        currentSet.add(songId)
        prefs.edit().putStringSet("permanently_deleted_song_ids_v5", currentSet).commit()
    }

    // Persistent Registered User Accounts (Strict Account Requirement)
    private val _registeredAccounts = mutableListOf<StoredAccount>()
    private val _registeredAccountsFlow = MutableStateFlow<List<StoredAccount>>(emptyList())
    val registeredAccounts: StateFlow<List<StoredAccount>> = _registeredAccountsFlow.asStateFlow()

    init {
        loadRegisteredAccounts()
        initializeInitialData()
        loadSavedData()
    }

    private fun loadRegisteredAccounts() {
        _registeredAccounts.clear()
        val deletedUids = getDeletedUserUids().toMutableSet()
        val dummyUids = setOf("user_rahul_9214", "user_simran_4012", "user_kabir_7731")
        val dummyUsernames = setOf("rahulmusic", "simranbeats", "djkabir", "rjlofi", "rjbeats")

        // Permanently record dummy sample IDs in deleted set so they never resurface
        deletedUids.addAll(dummyUids)
        prefs.edit().putStringSet("admin_deleted_user_uids", deletedUids).commit()

        // Sole Master Admin ID is always registered and lifetime protected
        val defaultAdmin = StoredAccount(
            uid = "admin_rj_primary",
            email = "itsrjeditor@gmail.com",
            password = "admin123",
            displayName = "Just RJ Musics",
            username = "justrjmusics",
            bio = "Official Just RJ Musics Founder & Producer.",
            instagramUsername = "justrjmusics",
            instagramUrl = "https://instagram.com/justrjmusics",
            isAdmin = true
        )
        _registeredAccounts.add(defaultAdmin)

        val jsonString = prefs.getString("registered_accounts_json_v2", null)
        if (!jsonString.isNullOrBlank()) {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val email = obj.optString("email").trim().lowercase()
                    val uid = obj.optString("uid")
                    val uName = obj.optString("username", "").trim().lowercase()

                    // Skip any user permanently deleted by admin or dummy accounts
                    if (deletedUids.contains(uid) || dummyUids.contains(uid) || dummyUsernames.contains(uName)) {
                        continue
                    }

                    if (uid == "admin_rj_primary" || email == "itsrjeditor@gmail.com") {
                        val idx = _registeredAccounts.indexOfFirst { it.uid == "admin_rj_primary" }
                        if (idx >= 0) {
                            _registeredAccounts[idx] = _registeredAccounts[idx].copy(
                                displayName = "Just RJ Musics",
                                username = "justrjmusics",
                                profileImageUrl = obj.optString("profileImageUrl", _registeredAccounts[idx].profileImageUrl),
                                bio = "Official Just RJ Musics Founder & Producer.",
                                instagramUsername = "justrjmusics",
                                instagramUrl = "https://instagram.com/justrjmusics",
                                password = obj.optString("password", _registeredAccounts[idx].password)
                            )
                        }
                    } else if (_registeredAccounts.none { it.email.equals(email, ignoreCase = true) || it.uid == uid }) {
                        _registeredAccounts.add(
                            StoredAccount(
                                uid = obj.optString("uid", "user_" + UUID.randomUUID().toString().take(8)),
                                email = email,
                                password = obj.optString("password", ""),
                                displayName = obj.optString("displayName", email.substringBefore("@")),
                                username = obj.optString("username", email.substringBefore("@").replace("[^a-z0-9_.]".toRegex(), "_")),
                                profileImageUrl = obj.optString("profileImageUrl", ""),
                                bio = obj.optString("bio", "Music listener and creator on Just RJ Music."),
                                instagramUsername = obj.optString("instagramUsername", ""),
                                instagramUrl = obj.optString("instagramUrl", ""),
                                isAdmin = obj.optBoolean("isAdmin", false),
                                registeredAt = obj.optLong("registeredAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Clean any potential dummy accounts and persist clean list
        _registeredAccounts.removeAll { deletedUids.contains(it.uid) || dummyUids.contains(it.uid) || dummyUsernames.contains(it.username.lowercase()) }
        _registeredAccountsFlow.value = _registeredAccounts.toList()
        saveRegisteredAccounts()
    }

    private fun saveRegisteredAccounts() {
        try {
            val jsonArray = JSONArray()
            for (acc in _registeredAccounts) {
                val obj = JSONObject().apply {
                    put("uid", acc.uid)
                    put("email", acc.email)
                    put("password", acc.password)
                    put("displayName", acc.displayName)
                    put("username", acc.username)
                    put("profileImageUrl", acc.profileImageUrl)
                    put("bio", acc.bio)
                    put("instagramUsername", acc.instagramUsername)
                    put("instagramUrl", acc.instagramUrl)
                    put("isAdmin", acc.isAdmin)
                    put("registeredAt", acc.registeredAt)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("registered_accounts_json_v2", jsonArray.toString()).commit()
            _registeredAccountsFlow.value = _registeredAccounts.toList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun savePlaylists() {
        try {
            val array = JSONArray()
            _playlists.value.forEach { pl ->
                val obj = JSONObject().apply {
                    put("playlistId", pl.playlistId)
                    put("title", pl.title)
                    put("description", pl.description)
                    put("ownerUid", pl.ownerUid)
                    put("ownerName", pl.ownerName)
                    put("coverUrl", pl.coverUrl)
                    put("isPublic", pl.isPublic)
                    put("createdAt", pl.createdAt)
                    put("updatedAt", pl.updatedAt)
                    val sArr = JSONArray()
                    pl.songIds.forEach { sArr.put(it) }
                    put("songIds", sArr)
                }
                array.put(obj)
            }
            prefs.edit().putString("user_playlists_json_v2", array.toString()).commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveUserReactions() {
        try {
            val obj = JSONObject()
            _userReactions.value.forEach { (k, v) ->
                obj.put(k, v.name)
            }
            prefs.edit().putString("user_reactions_json_v2", obj.toString()).commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveAdminSubProfiles() {
        try {
            val array = JSONArray()
            _adminSubProfiles.value.forEach { sub ->
                val obj = JSONObject().apply {
                    put("profileId", sub.profileId)
                    put("ownerUid", sub.ownerUid)
                    put("displayName", sub.displayName)
                    put("username", sub.username)
                    put("profileImageUrl", sub.profileImageUrl)
                    put("bio", sub.bio)
                    put("instagramUsername", sub.instagramUsername)
                    put("instagramUrl", sub.instagramUrl)
                    put("isDefault", sub.isDefault)
                    put("isPublic", sub.isPublic)
                    put("followersCount", sub.followersCount)
                    put("followingCount", sub.followingCount)
                    put("monthlyListeners", sub.monthlyListeners)
                    put("createdAt", sub.createdAt)
                }
                array.put(obj)
            }
            prefs.edit().putString("admin_sub_profiles_json_v1", array.toString()).commit()
            _activeAdminProfile.value?.let {
                prefs.edit().putString("active_admin_profile_id_v1", it.profileId).commit()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadAdminSubProfiles(): List<AdminSubProfile> {
        val jsonStr = prefs.getString("admin_sub_profiles_json_v1", null)
        val defaultProfile = AdminSubProfile(
            profileId = "prof_rj_musics",
            ownerUid = "admin_rj_primary",
            displayName = "Just RJ Musics",
            username = "justrjmusics",
            bio = "Official Just RJ Musics Founder & Producer.",
            instagramUsername = "justrjmusics",
            instagramUrl = "https://instagram.com/justrjmusics",
            isDefault = true,
            followersCount = 0,
            followingCount = 0,
            monthlyListeners = 0
        )
        if (jsonStr.isNullOrBlank()) {
            return listOf(defaultProfile)
        }
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<AdminSubProfile>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AdminSubProfile(
                        profileId = obj.optString("profileId", UUID.randomUUID().toString()),
                        ownerUid = obj.optString("ownerUid", "admin_rj_primary"),
                        displayName = obj.optString("displayName", "Just RJ Musics"),
                        username = obj.optString("username", "justrjmusics"),
                        profileImageUrl = obj.optString("profileImageUrl", ""),
                        bio = obj.optString("bio", ""),
                        instagramUsername = obj.optString("instagramUsername", ""),
                        instagramUrl = obj.optString("instagramUrl", ""),
                        isDefault = obj.optBoolean("isDefault", i == 0),
                        isPublic = obj.optBoolean("isPublic", true),
                        followersCount = obj.optInt("followersCount", 0),
                        followingCount = obj.optInt("followingCount", 0),
                        monthlyListeners = obj.optInt("monthlyListeners", 0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            if (list.isEmpty()) listOf(defaultProfile) else list
        } catch (e: Exception) {
            listOf(defaultProfile)
        }
    }

    fun getDefaultStreamUrl(genre: String): String {
        return when (genre.lowercase()) {
            "hip-hop", "rap" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            "lo-fi", "chill" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
            "romantic", "acoustic" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
            "punjabi", "dance" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
            "electronic", "edm" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"
            "rock" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3"
            "pop" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3"
            else -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3"
        }
    }

    fun getLifetimeConfirmedSongIds(): Set<String> {
        return prefs.getStringSet("lifetime_confirmed_song_ids_v5", emptySet()) ?: emptySet()
    }

    fun markSongLifetimeConfirmed(songId: String) {
        val currentSet = getLifetimeConfirmedSongIds().toMutableSet()
        currentSet.add(songId)
        prefs.edit().putStringSet("lifetime_confirmed_song_ids_v5", currentSet).commit()
    }

    fun isSongLifetimeConfirmed(songId: String): Boolean {
        return getLifetimeConfirmedSongIds().contains(songId)
    }

    private fun saveCustomSongs() {
        try {
            val array = JSONArray()
            val lifetimeIds = getLifetimeConfirmedSongIds()
            _songs.value.filter {
                it.songId.startsWith("song_upload_") ||
                it.primaryCreatorUid != "admin_rj_primary" ||
                it.isLifetimeConfirmed ||
                it.moderationStatus == "approved" ||
                lifetimeIds.contains(it.songId)
            }.forEach { song ->
                val isLifetime = song.isLifetimeConfirmed || song.moderationStatus == "approved" || lifetimeIds.contains(song.songId)
                val obj = JSONObject().apply {
                    put("songId", song.songId)
                    put("title", song.title)
                    put("description", song.description)
                    put("primaryCreatorUid", song.primaryCreatorUid)
                    put("primaryCreatorName", song.primaryCreatorName)
                    put("primaryCreatorUsername", song.primaryCreatorUsername)
                    put("genre", song.genre)
                    put("mood", song.mood)
                    put("energy", song.energy)
                    put("tempo", song.tempo)
                    put("language", song.language)
                    put("lyrics", song.lyrics)
                    put("coverUrl", song.coverUrl)
                    put("audioUrl", song.audioUrl)
                    put("explicit", song.explicit)
                    put("allowDownload", song.allowDownload)
                    put("uploadStatus", song.uploadStatus.name)
                    put("moderationStatus", song.moderationStatus)
                    put("moderationNote", song.moderationNote)
                    put("createdAt", song.createdAt)
                    put("publishedAt", song.publishedAt)
                    put("totalViews", song.totalViews)
                    put("uniqueListeners", song.uniqueListeners)
                    put("likesCount", song.likesCount)
                    put("dislikesCount", song.dislikesCount)
                    put("downloadsCount", song.downloadsCount)
                    put("isLifetimeConfirmed", isLifetime)
                    val tagArr = JSONArray()
                    song.tags.forEach { tagArr.put(it) }
                    put("tags", tagArr)
                    val mentionArr = JSONArray()
                    song.mentionedUsernames.forEach { mentionArr.put(it) }
                    put("mentionedUsernames", mentionArr)
                }
                array.put(obj)
            }
            prefs.edit().putString("custom_songs_json_v2", array.toString()).commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveUserFollows() {
        try {
            val array = JSONArray()
            _follows.value.forEach { (f, t) ->
                array.put("${f}__SPLIT__${t}")
            }
            prefs.edit().putString("user_follows_json_v2", array.toString()).commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSavedData() {
        // Load Saved User Reactions
        val reactJson = prefs.getString("user_reactions_json_v2", null)
        if (!reactJson.isNullOrBlank()) {
            try {
                val obj = JSONObject(reactJson)
                val map = mutableMapOf<String, ReactionType>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = try { ReactionType.valueOf(obj.getString(key)) } catch (e: Exception) { ReactionType.NONE }
                }
                _userReactions.value = map
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Load Saved Follows
        val followJson = prefs.getString("user_follows_json_v2", null)
        if (!followJson.isNullOrBlank()) {
            try {
                val array = JSONArray(followJson)
                val set = mutableSetOf<Pair<String, String>>()
                for (i in 0 until array.length()) {
                    val str = array.getString(i)
                    val parts = str.split("__SPLIT__")
                    if (parts.size == 2) {
                        set.add(parts[0] to parts[1])
                    }
                }
                _follows.value = set
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Load Saved Playlists
        val playlistJson = prefs.getString("user_playlists_json_v2", null)
        if (!playlistJson.isNullOrBlank()) {
            try {
                val array = JSONArray(playlistJson)
                val list = mutableListOf<Playlist>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val sArr = obj.optJSONArray("songIds")
                    val songIds = mutableListOf<String>()
                    if (sArr != null) {
                        for (j in 0 until sArr.length()) songIds.add(sArr.getString(j))
                    }
                    list.add(
                        Playlist(
                            playlistId = obj.optString("playlistId", UUID.randomUUID().toString()),
                            title = obj.optString("title", "My Playlist"),
                            description = obj.optString("description", ""),
                            ownerUid = obj.optString("ownerUid", ""),
                            ownerName = obj.optString("ownerName", ""),
                            coverUrl = obj.optString("coverUrl", ""),
                            isPublic = obj.optBoolean("isPublic", true),
                            songIds = songIds,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    _playlists.value = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Load Saved Custom Songs
        val songJson = prefs.getString("custom_songs_json_v2", null)
        if (!songJson.isNullOrBlank()) {
            try {
                val array = JSONArray(songJson)
                val list = mutableListOf<Song>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val tagArr = obj.optJSONArray("tags")
                    val tags = mutableListOf<String>()
                    if (tagArr != null) {
                        for (j in 0 until tagArr.length()) tags.add(tagArr.getString(j))
                    }
                    val mentionArr = obj.optJSONArray("mentionedUsernames")
                    val mentions = mutableListOf<String>()
                    if (mentionArr != null) {
                        for (j in 0 until mentionArr.length()) mentions.add(mentionArr.getString(j))
                    }
                    val uploadStatus = try {
                        UploadStatus.valueOf(obj.optString("uploadStatus", "PUBLISHED"))
                    } catch (e: Exception) {
                        UploadStatus.PUBLISHED
                    }
                    val coverUrl = obj.optString("coverUrl", "")
                    val audioUrl = obj.optString("audioUrl", "")

                    val sId = obj.optString("songId")
                    if (sId == "song_pending_sufi_moonlight") continue

                    val rawViews = obj.optLong("totalViews", 0)
                    val rawLikes = obj.optLong("likesCount", 0)
                    val rawDislikes = obj.optLong("dislikesCount", 0)
                    val isDemoSong = sId in listOf("song_night_drive", "song_midnight_rain", "song_meri_kahani", "song_urban_punjabi", "song_synth_horizon")
                    val totalViews = if (isDemoSong && (rawViews == 124500L || rawViews == 248000L || rawViews == 58200L || rawViews == 89400L || rawViews == 72100L)) 0L else rawViews
                    val likesCount = if (isDemoSong && (rawLikes == 14200L || rawLikes == 31200L || rawLikes == 7400L || rawLikes == 11800L || rawLikes == 9200L)) 0L else rawLikes
                    val dislikesCount = if (isDemoSong && (rawDislikes == 120L || rawDislikes == 85L || rawDislikes == 42L || rawDislikes == 95L || rawDislikes == 60L)) 0L else rawDislikes

                    val dummyCreatorUids = setOf("user_rahul_9214", "user_simran_4012", "user_kabir_7731")
                    val dummyCreatorUsernames = setOf("rahulmusic", "simranbeats", "djkabir", "rjlofi", "rjbeats")
                    val currentUid = obj.optString("primaryCreatorUid")
                    val currentUsername = obj.optString("primaryCreatorUsername")
                    if (sId != "song_meri_kahani" && (currentUid in dummyCreatorUids || currentUsername.lowercase() in dummyCreatorUsernames)) {
                        continue
                    }

                    val finalCreatorUid = if (sId == "song_meri_kahani") "admin_rj_primary" else currentUid
                    val finalCreatorName = if (sId == "song_meri_kahani") "Just RJ Musics" else obj.optString("primaryCreatorName")
                    val finalCreatorUsername = if (sId == "song_meri_kahani") "justrjmusics" else currentUsername
                    val finalMentions = if (sId == "song_meri_kahani") emptyList() else mentions

                    list.add(
                        Song(
                            songId = sId,
                            title = obj.optString("title"),
                            description = obj.optString("description"),
                            primaryCreatorUid = finalCreatorUid,
                            primaryCreatorName = finalCreatorName,
                            primaryCreatorUsername = finalCreatorUsername,
                            mentionedUsernames = finalMentions,
                            coverUrl = coverUrl,
                            coverResId = if (coverUrl.isBlank()) R.drawable.cover_night_drive else null,
                            audioUrl = if (audioUrl.isNotBlank()) audioUrl else getDefaultStreamUrl(obj.optString("genre", "Pop")),
                            genre = obj.optString("genre", "Pop"),
                            tags = tags,
                            mood = obj.optString("mood", "Chill"),
                            energy = obj.optString("energy", "Medium"),
                            tempo = obj.optString("tempo", "120 BPM"),
                            language = obj.optString("language", "Hindi"),
                            lyrics = obj.optString("lyrics", ""),
                            explicit = obj.optBoolean("explicit", false),
                            allowDownload = obj.optBoolean("allowDownload", true),
                            uploadStatus = uploadStatus,
                            moderationStatus = obj.optString("moderationStatus", "approved"),
                            moderationNote = obj.optString("moderationNote", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            publishedAt = obj.optLong("publishedAt", System.currentTimeMillis()),
                            totalViews = totalViews,
                            uniqueListeners = if (isDemoSong && totalViews == 0L) 0L else obj.optLong("uniqueListeners", 0),
                            likesCount = likesCount,
                            dislikesCount = dislikesCount,
                            downloadsCount = if (isDemoSong && totalViews == 0L) 0L else obj.optLong("downloadsCount", 0),
                            isLifetimeConfirmed = obj.optBoolean("isLifetimeConfirmed", false) || obj.optString("moderationStatus") == "approved" || isSongLifetimeConfirmed(obj.optString("songId"))
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    list.filter { it.isLifetimeConfirmed }.forEach { markSongLifetimeConfirmed(it.songId) }
                    val deletedSongs = getDeletedSongIds()
                    val activeList = list.filterNot { it.songId in deletedSongs }
                    val existingIds = activeList.map { it.songId }.toSet()
                    _songs.value = activeList + _songs.value.filterNot { existingIds.contains(it.songId) || it.songId in deletedSongs }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initializeInitialData() {
        val subProfiles = loadAdminSubProfiles()
        val savedActiveId = prefs.getString("active_admin_profile_id_v1", "prof_rj_musics")
        val activeProfile = subProfiles.firstOrNull { it.profileId == savedActiveId } ?: subProfiles.first()
        _adminSubProfiles.value = subProfiles
        _activeAdminProfile.value = activeProfile

        val admin = UserProfile(
            uid = "admin_rj_primary",
            email = "itsrjeditor@gmail.com",
            displayName = activeProfile.displayName,
            username = activeProfile.username,
            bio = activeProfile.bio.ifBlank { "Official Just RJ Musics Founder & Producer. Crafting premium luxury soundscapes." },
            instagramUsername = activeProfile.instagramUsername,
            instagramUrl = activeProfile.instagramUrl,
            followersCount = activeProfile.followersCount,
            followingCount = activeProfile.followingCount,
            monthlyListeners = activeProfile.monthlyListeners,
            verified = true,
            isAdmin = true,
            adminProfileIds = subProfiles.map { it.profileId },
            activeProfileId = activeProfile.profileId
        )

        // Demo Songs with rich data and online streaming URLs
        val sampleSongs = listOf(
            Song(
                songId = "song_night_drive",
                title = "Night Drive",
                description = "Cinematic midnight hip-hop drive with deep bass and atmospheric synths.",
                primaryCreatorUid = admin.uid,
                primaryCreatorName = "Just RJ Musics",
                primaryCreatorUsername = "justrjmusics",
                mentionedUsernames = emptyList(),
                mentionedUserIds = emptyList(),
                acceptedCollaboratorUids = emptyList(),
                coverResId = R.drawable.cover_night_drive,
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                genre = "Hip-Hop",
                secondaryGenres = listOf("Rap", "Trap"),
                tags = listOf("Night Vibe", "Bass", "Cruising", "Melodic"),
                mood = "Confident & Dark",
                energy = "High",
                tempo = "132 BPM",
                durationSec = 208,
                lyrics = "[Intro]\nMidnight lights flickering down the highway...\nBass kicking through the floorboards...\n[Chorus]\nNight drive, chasing the moonlight glow\nCity asleep, but the music flows\nJust RJ Musics, we never slow down.",
                totalViews = 0,
                uniqueListeners = 0,
                likesCount = 0,
                dislikesCount = 0,
                downloadsCount = 0,
                shareCount = 0,
                playlistAddCount = 0,
                automaticTrendingScore = 0.0,
                trendingPosition = 1,
                isPinnedTrending = true,
                uploadStatus = UploadStatus.PUBLISHED,
                moderationStatus = "approved"
            ),
            Song(
                songId = "song_midnight_rain",
                title = "Midnight Rain (Lo-Fi Study)",
                description = "Cozy vinyl tape noise with warm piano chords and soft rain textures.",
                primaryCreatorUid = admin.uid,
                primaryCreatorName = "Just RJ Musics",
                primaryCreatorUsername = "justrjmusics",
                coverResId = R.drawable.cover_lofi,
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                genre = "Lo-Fi",
                secondaryGenres = listOf("Chill", "Acoustic"),
                tags = listOf("Study", "Rain", "Relax", "Late Night", "Vinyl"),
                mood = "Peaceful",
                energy = "Low",
                tempo = "84 BPM",
                durationSec = 175,
                lyrics = "[Instrumental with soft atmospheric whispers and raindrops]",
                totalViews = 0,
                uniqueListeners = 0,
                likesCount = 0,
                dislikesCount = 0,
                downloadsCount = 0,
                shareCount = 0,
                playlistAddCount = 0,
                automaticTrendingScore = 0.0,
                trendingPosition = 2,
                uploadStatus = UploadStatus.PUBLISHED,
                moderationStatus = "approved"
            ),
            Song(
                songId = "song_meri_kahani",
                title = "Meri Kahani",
                description = "Heartfelt soulful melody with acoustic guitar and expressive vocals.",
                primaryCreatorUid = admin.uid,
                primaryCreatorName = "Just RJ Musics",
                primaryCreatorUsername = "justrjmusics",
                mentionedUsernames = emptyList(),
                mentionedUserIds = emptyList(),
                acceptedCollaboratorUids = emptyList(),
                coverResId = R.drawable.cover_lofi,
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                genre = "Romantic",
                secondaryGenres = listOf("Bollywood", "Acoustic"),
                tags = listOf("Heartfelt", "Soul", "Melody", "Guitar"),
                mood = "Soulful",
                energy = "Medium",
                tempo = "92 BPM",
                durationSec = 224,
                lyrics = "Tere bina jeena lage adhura sa\nHar lamha dhoondhe bas tera nishaan...",
                totalViews = 0,
                uniqueListeners = 0,
                likesCount = 0,
                dislikesCount = 0,
                downloadsCount = 0,
                shareCount = 0,
                playlistAddCount = 0,
                automaticTrendingScore = 0.0,
                trendingPosition = 3,
                uploadStatus = UploadStatus.PUBLISHED,
                moderationStatus = "approved"
            ),
            Song(
                songId = "song_urban_punjabi",
                title = "Desi Banger",
                description = "Heavy bass modern Punjabi urban hit featuring hard synth drops.",
                primaryCreatorUid = admin.uid,
                primaryCreatorName = "Just RJ Musics",
                primaryCreatorUsername = "justrjmusics",
                coverResId = R.drawable.cover_night_drive,
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                genre = "Punjabi",
                secondaryGenres = listOf("Hip-Hop", "Party"),
                tags = listOf("Club", "Banger", "Desi", "Bass Boost"),
                mood = "Energetic",
                energy = "High",
                tempo = "105 BPM",
                durationSec = 196,
                totalViews = 0,
                uniqueListeners = 0,
                likesCount = 0,
                dislikesCount = 0,
                downloadsCount = 0,
                shareCount = 0,
                playlistAddCount = 0,
                automaticTrendingScore = 0.0,
                trendingPosition = 4,
                uploadStatus = UploadStatus.PUBLISHED,
                moderationStatus = "approved"
            ),
            Song(
                songId = "song_synth_horizon",
                title = "Monochrome Horizons",
                description = "Futuristic analog synth journey with silver textures.",
                primaryCreatorUid = admin.uid,
                primaryCreatorName = "Just RJ Musics",
                primaryCreatorUsername = "justrjmusics",
                coverResId = R.drawable.rj_logo,
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                genre = "Electronic",
                secondaryGenres = listOf("Pop", "Workout"),
                tags = listOf("Cyberpunk", "Silver", "Groove", "Synth"),
                mood = "Uplifting",
                energy = "High",
                tempo = "126 BPM",
                durationSec = 240,
                totalViews = 0,
                uniqueListeners = 0,
                likesCount = 0,
                dislikesCount = 0,
                downloadsCount = 0,
                shareCount = 0,
                playlistAddCount = 0,
                automaticTrendingScore = 0.0,
                trendingPosition = 5,
                uploadStatus = UploadStatus.PUBLISHED,
                moderationStatus = "approved"
            )
        )

        val deletedSongs = getDeletedSongIds()
        _songs.value = sampleSongs.filterNot { it.songId in deletedSongs }

        // Featured Hero Slider
        _featuredSlides.value = listOf(
            FeaturedSlide(
                id = "slide_1",
                title = "Night Drive - Official Release",
                subtitle = "Just RJ Musics • The dark cinematic anthem",
                creatorName = "Just RJ Musics",
                artworkResId = R.drawable.cover_night_drive,
                destinationType = "SONG",
                destinationId = "song_night_drive",
                isPinned = true,
                sortOrder = 1
            ),
            FeaturedSlide(
                id = "slide_2",
                title = "Midnight Lo-Fi Sessions",
                subtitle = "Just RJ Musics • Pure relaxing rain & study tape loops",
                creatorName = "Just RJ Musics",
                artworkResId = R.drawable.cover_lofi,
                destinationType = "SONG",
                destinationId = "song_midnight_rain",
                isPinned = false,
                sortOrder = 2
            ),
            FeaturedSlide(
                id = "slide_3",
                title = "Just RJ Musics • Signature Sound",
                subtitle = "Discover high-fidelity original streaming hits",
                creatorName = "Just RJ Musics",
                artworkResId = R.drawable.rj_logo,
                destinationType = "SONG",
                destinationId = "song_synth_horizon",
                isPinned = false,
                sortOrder = 3
            )
        ).filterNot { it.destinationId in deletedSongs }

        // Initial Playlists
        _playlists.value = listOf(
            Playlist(
                playlistId = "pl_midnight_vibes",
                title = "Midnight Aesthetics",
                description = "Curated dark melodies, lo-fi chords & midnight cruising beats.",
                ownerUid = admin.uid,
                ownerName = "Just RJ Musics",
                isPublic = true,
                songIds = listOf("song_night_drive", "song_midnight_rain", "song_synth_horizon").filterNot { it in deletedSongs }
            ),
            Playlist(
                playlistId = "pl_desi_workout",
                title = "Urban Desi & Workout",
                description = "High energy Punjabi hip-hop and high tempo bangers.",
                ownerUid = admin.uid,
                ownerName = "Just RJ Musics",
                isPublic = true,
                songIds = listOf("song_urban_punjabi", "song_night_drive").filterNot { it in deletedSongs }
            )
        )

        // Initial Notifications
        _notifications.value = listOf(
            NotificationItem(
                recipientUid = admin.uid,
                title = "Welcome to Just RJ Musics",
                message = "Your official creator & administrator platform is fully initialized with luxury monochrome branding.",
                type = "alert"
            ),
            NotificationItem(
                recipientUid = admin.uid,
                title = "Song Milestone Reached",
                message = "'Night Drive' crossed 100K streams! Trending score updated to #1.",
                type = "trending",
                actionTargetId = "song_night_drive"
            )
        )

        // Check if there is an existing authenticated session saved
        val savedEmail = prefs.getString("saved_user_email", null)?.trim()?.lowercase()
        if (savedEmail != null) {
            val account = _registeredAccounts.firstOrNull { it.email.equals(savedEmail, ignoreCase = true) }
            if (account != null) {
                if (account.isAdmin) {
                    val currentSubs = _adminSubProfiles.value
                    val currentActive = _activeAdminProfile.value ?: currentSubs.first()
                    val adminUser = UserProfile(
                        uid = account.uid,
                        email = "itsrjeditor@gmail.com",
                        displayName = currentActive.displayName,
                        username = currentActive.username,
                        profileImageUrl = currentActive.profileImageUrl.ifBlank { account.profileImageUrl },
                        bio = currentActive.bio.ifBlank { "Official Just RJ Musics Founder & Producer." },
                        instagramUsername = currentActive.instagramUsername,
                        instagramUrl = currentActive.instagramUrl,
                        followersCount = currentActive.followersCount,
                        followingCount = currentActive.followingCount,
                        monthlyListeners = currentActive.monthlyListeners,
                        verified = true,
                        isAdmin = true,
                        adminProfileIds = currentSubs.map { it.profileId },
                        activeProfileId = currentActive.profileId
                    )
                    _currentUser.value = adminUser
                    _activeAdminProfile.value = currentActive
                } else {
                    val user = UserProfile(
                        uid = account.uid,
                        email = account.email,
                        displayName = account.displayName,
                        username = account.username,
                        profileImageUrl = account.profileImageUrl,
                        bio = account.bio.ifBlank { "Music listener and creator on Just RJ Music." },
                        instagramUsername = account.instagramUsername,
                        instagramUrl = account.instagramUrl,
                        isAdmin = false,
                        verified = false
                    )
                    _currentUser.value = user
                }
            } else {
                _currentUser.value = null
            }
        } else {
            // Fresh installation on any phone: App starts unauthenticated so Login screen is shown first!
            _currentUser.value = null
        }
    }

    // ==========================================
    // AUTHENTICATION & PROFILES
    // ==========================================

    fun login(email: String, pass: String): Result<UserProfile> {
        val cleanInput = email.trim().lowercase()
        val cleanUser = cleanInput.removePrefix("@")
        val cleanPass = pass.trim()

        if (cleanInput.isBlank()) {
            return Result.failure(Exception("Please enter your Gmail / Email address or Username."))
        }

        // Strict Requirement: User MUST have signed up first!
        val account = _registeredAccounts.firstOrNull {
            it.email.equals(cleanInput, ignoreCase = true) ||
            it.username.equals(cleanUser, ignoreCase = true)
        }
        if (account == null) {
            return Result.failure(
                Exception("No account found with '$cleanInput'. Please create a new account first using 'Create Account'.")
            )
        }

        // Verify password
        if (cleanPass.isBlank()) {
            return Result.failure(Exception("Please enter your password."))
        }
        if (account.password.isNotBlank() && account.password != cleanPass) {
            return Result.failure(Exception("Incorrect password for '$cleanInput'. Please try again."))
        }

        if (account.isAdmin) {
            val currentSubs = _adminSubProfiles.value
            val currentActive = _activeAdminProfile.value ?: currentSubs.first()
            val adminUser = UserProfile(
                uid = "admin_rj_primary",
                email = "itsrjeditor@gmail.com",
                displayName = currentActive.displayName,
                username = currentActive.username,
                profileImageUrl = currentActive.profileImageUrl.ifBlank { account.profileImageUrl },
                bio = currentActive.bio.ifBlank { "Official Just RJ Musics Founder & Producer." },
                instagramUsername = currentActive.instagramUsername,
                instagramUrl = currentActive.instagramUrl,
                followersCount = currentActive.followersCount,
                followingCount = currentActive.followingCount,
                monthlyListeners = currentActive.monthlyListeners,
                verified = true,
                isAdmin = true,
                adminProfileIds = currentSubs.map { it.profileId },
                activeProfileId = currentActive.profileId
            )
            _currentUser.value = adminUser
            _activeAdminProfile.value = currentActive
            prefs.edit()
                .putString("saved_user_email", adminUser.email)
                .putString("saved_user_name", adminUser.displayName)
                .putString("saved_user_username", adminUser.username)
                .putString("saved_user_uid", adminUser.uid)
                .apply()
            return Result.success(adminUser)
        }

        // Standard registered user login
        val user = UserProfile(
            uid = account.uid,
            email = account.email,
            displayName = account.displayName,
            username = account.username,
            profileImageUrl = account.profileImageUrl,
            bio = account.bio.ifBlank { "Music listener and creator on Just RJ Music." },
            instagramUsername = account.instagramUsername,
            instagramUrl = account.instagramUrl,
            isAdmin = false,
            verified = false
        )
        _currentUser.value = user
        _activeAdminProfile.value = null
        prefs.edit()
            .putString("saved_user_email", user.email)
            .putString("saved_user_name", user.displayName)
            .putString("saved_user_username", user.username)
            .putString("saved_user_uid", user.uid)
            .apply()
        return Result.success(user)
    }

    fun signUp(name: String, username: String, email: String, pass: String, bio: String = "", instagram: String = ""): Result<UserProfile> {
        val cleanEmail = email.trim().lowercase()
        val cleanUser = username.trim().lowercase().removePrefix("@").replace("[^a-z0-9_.]".toRegex(), "_")
        val cleanName = name.trim().ifBlank { cleanUser }
        val cleanPass = pass.trim()

        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(Exception("Please enter a valid Gmail / Email address (e.g. yourname@gmail.com)."))
        }

        if (cleanPass.length < 6) {
            return Result.failure(Exception("Password must be at least 6 characters long."))
        }

        if (cleanUser.length < 3) {
            return Result.failure(Exception("Username must be at least 3 characters long."))
        }

        // Check if email is already registered
        if (_registeredAccounts.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
            return Result.failure(Exception("An account with '$cleanEmail' already exists. Please tap 'Sign In' to log in."))
        }

        // Check if username is already registered
        if (_registeredAccounts.any { it.username.equals(cleanUser, ignoreCase = true) }) {
            return Result.failure(Exception("Username '@$cleanUser' is already taken. Please choose another one."))
        }

        val isAdmin = cleanEmail == "itsrjeditor@gmail.com"
        val newUid = if (isAdmin) "admin_rj_primary" else "user_" + UUID.randomUUID().toString().take(8)

        val cleanInsta = instagram.trim().removePrefix("@")
        val newStoredAccount = StoredAccount(
            uid = newUid,
            email = cleanEmail,
            password = cleanPass,
            displayName = cleanName,
            username = cleanUser,
            bio = bio.trim().ifBlank { "Music listener and creator on Just RJ Music." },
            instagramUsername = cleanInsta,
            instagramUrl = if (cleanInsta.isNotBlank()) "https://instagram.com/$cleanInsta" else "",
            isAdmin = isAdmin
        )

        _registeredAccounts.add(newStoredAccount)
        saveRegisteredAccounts()

        val newUser = UserProfile(
            uid = newUid,
            email = cleanEmail,
            displayName = cleanName,
            username = cleanUser,
            bio = newStoredAccount.bio,
            instagramUsername = newStoredAccount.instagramUsername,
            instagramUrl = newStoredAccount.instagramUrl,
            isAdmin = isAdmin,
            verified = isAdmin,
            adminProfileIds = if (isAdmin) listOf("prof_rj_musics") else emptyList(),
            activeProfileId = if (isAdmin) "prof_rj_musics" else null
        )

        _currentUser.value = newUser
        if (isAdmin) {
            _activeAdminProfile.value = _adminSubProfiles.value.firstOrNull()
        } else {
            _activeAdminProfile.value = null
        }
        prefs.edit()
            .putString("saved_user_email", newUser.email)
            .putString("saved_user_name", newUser.displayName)
            .putString("saved_user_username", newUser.username)
            .putString("saved_user_uid", newUser.uid)
            .apply()
        return Result.success(newUser)
    }

    fun logout() {
        _currentUser.value = null
        _activeAdminProfile.value = null
        prefs.edit()
            .remove("saved_user_email")
            .remove("saved_user_name")
            .remove("saved_user_username")
            .remove("saved_user_uid")
            .remove("saved_user_bio")
            .remove("saved_user_instagram")
            .apply()
    }

    fun sendPasswordReset(email: String): Result<String> {
        val cleanInput = email.trim().lowercase()
        val cleanUser = cleanInput.removePrefix("@")
        val exists = _registeredAccounts.any {
            it.email.equals(cleanInput, ignoreCase = true) || it.username.equals(cleanUser, ignoreCase = true)
        }
        return if (exists) {
            Result.success("Password reset link and instructions have been processed for $cleanInput.")
        } else {
            Result.failure(Exception("No account registered with '$cleanInput'. Please check the email/username or sign up."))
        }
    }

    fun resetPassword(email: String, newPass: String): Result<String> {
        val cleanInput = email.trim().lowercase()
        val cleanUser = cleanInput.removePrefix("@")
        val cleanPass = newPass.trim()

        if (cleanInput.isBlank()) {
            return Result.failure(Exception("Please enter your registered email address or username."))
        }
        if (cleanPass.length < 6) {
            return Result.failure(Exception("New password must be at least 6 characters long."))
        }

        val accIndex = _registeredAccounts.indexOfFirst {
            it.email.equals(cleanInput, ignoreCase = true) || it.username.equals(cleanUser, ignoreCase = true)
        }
        if (accIndex < 0) {
            return Result.failure(Exception("No account found with '$cleanInput'. Please check the email/username or create a new account."))
        }

        val old = _registeredAccounts[accIndex]
        _registeredAccounts[accIndex] = old.copy(password = cleanPass)
        saveRegisteredAccounts()
        return Result.success("Password for ${old.displayName} has been reset successfully! You can now sign in.")
    }

    // User Management: Admin can delete regular users; Admin accounts are permanently protected for lifetime
    fun deleteUser(uid: String): Result<String> {
        val target = _registeredAccounts.firstOrNull { it.uid == uid }
            ?: return Result.failure(Exception("User with ID '$uid' was not found."))

        // Strictly protect Admin accounts from deletion
        if (target.isAdmin || target.uid == "admin_rj_primary" || target.email.equals("itsrjeditor@gmail.com", ignoreCase = true)) {
            return Result.failure(Exception("Admin ID cannot be deleted. Master Admin is protected."))
        }

        // Record permanent deletion so this user can NEVER re-seed or reappear
        markUserPermanentlyDeleted(uid)

        _registeredAccounts.removeAll { it.uid == uid }
        saveRegisteredAccounts()

        // Clean up songs created by this user from feed and catalog
        val userSongs = _songs.value.filter { it.primaryCreatorUid == uid }
        userSongs.forEach { markSongPermanentlyDeleted(it.songId) }
        _songs.value = _songs.value.filterNot { it.primaryCreatorUid == uid }
        saveCustomSongs()

        // Clean up collaboration invites and follows
        _collaborationInvites.value = _collaborationInvites.value.filterNot { it.inviterUid == uid || it.mentionedUid == target.username }
        _follows.value = _follows.value.filterNot { it.first == uid || it.second == uid }.toSet()
        saveUserFollows()

        return Result.success("User '${target.displayName}' (@${target.username}, ID: $uid) has been permanently deleted.")
    }

    // Admin Sub-Profile Operations
    fun switchActiveProfile(profileId: String) {
        val profile = _adminSubProfiles.value.firstOrNull { it.profileId == profileId }
        if (profile != null) {
            _activeAdminProfile.value = profile
            prefs.edit().putString("active_admin_profile_id_v1", profile.profileId).commit()
            _currentUser.value = _currentUser.value?.copy(
                displayName = profile.displayName,
                username = profile.username,
                bio = profile.bio,
                instagramUsername = profile.instagramUsername,
                instagramUrl = profile.instagramUrl,
                activeProfileId = profile.profileId
            )
            saveAdminSubProfiles()
        }
    }

    fun createAdminSubProfile(displayName: String, username: String, bio: String, instagram: String): AdminSubProfile {
        val current = _currentUser.value ?: return AdminSubProfile(ownerUid = "anon", displayName = displayName, username = username)
        val cleanInsta = instagram.trim().removePrefix("@")
        val newSub = AdminSubProfile(
            ownerUid = current.uid,
            displayName = displayName.trim(),
            username = username.trim().lowercase().replace("[^a-z0-9_.]".toRegex(), "_"),
            bio = bio.trim(),
            instagramUsername = cleanInsta,
            instagramUrl = if (cleanInsta.isNotBlank()) "https://instagram.com/$cleanInsta" else "",
            followersCount = 0,
            followingCount = 0,
            monthlyListeners = 0
        )
        _adminSubProfiles.value = _adminSubProfiles.value + newSub
        saveAdminSubProfiles()
        if (_currentUser.value?.isAdmin == true) {
            _currentUser.value = _currentUser.value?.copy(
                adminProfileIds = _adminSubProfiles.value.map { it.profileId }
            )
        }
        return newSub
    }

    fun deleteAdminSubProfile(profileId: String) {
        if (_adminSubProfiles.value.size <= 1) return // Keep at least one
        _adminSubProfiles.value = _adminSubProfiles.value.filterNot { it.profileId == profileId }
        if (_activeAdminProfile.value?.profileId == profileId) {
            _activeAdminProfile.value = _adminSubProfiles.value.firstOrNull()
        }
        saveAdminSubProfiles()
        if (_currentUser.value?.isAdmin == true) {
            _currentUser.value = _currentUser.value?.copy(
                adminProfileIds = _adminSubProfiles.value.map { it.profileId },
                activeProfileId = _activeAdminProfile.value?.profileId
            )
        }
    }

    fun updateProfile(displayName: String, bio: String, instagram: String, website: String) {
        val current = _currentUser.value ?: return
        val cleanInsta = instagram.trim().removePrefix("@")
        val cleanName = displayName.trim().ifBlank { current.displayName }
        val cleanBio = bio.trim()
        val cleanInstaUrl = if (cleanInsta.isNotBlank()) "https://instagram.com/$cleanInsta" else ""
        val updated = current.copy(
            displayName = cleanName,
            bio = cleanBio,
            instagramUsername = cleanInsta,
            instagramUrl = cleanInstaUrl,
            websiteUrl = website.trim()
        )
        _currentUser.value = updated

        // Update stored accounts list and save permanently
        val accIndex = _registeredAccounts.indexOfFirst { it.uid == current.uid || it.email.equals(current.email, ignoreCase = true) }
        if (accIndex >= 0) {
            val old = _registeredAccounts[accIndex]
            _registeredAccounts[accIndex] = old.copy(
                displayName = cleanName,
                bio = cleanBio,
                instagramUsername = cleanInsta,
                instagramUrl = cleanInstaUrl
            )
            saveRegisteredAccounts()
        }

        // Update preferences for session restore
        prefs.edit()
            .putString("saved_user_name", cleanName)
            .putString("saved_user_bio", cleanBio)
            .putString("saved_user_instagram", cleanInsta)
            .apply()

        // Also update primary creator display name on songs created by this user
        _songs.value = _songs.value.map { song ->
            if (song.primaryCreatorUid == current.uid) {
                song.copy(primaryCreatorName = cleanName)
            } else song
        }
        saveCustomSongs()

        // If admin has an active subprofile, update it as well
        if (current.isAdmin) {
            val active = _activeAdminProfile.value
            if (active != null) {
                val updatedSub = active.copy(
                    displayName = cleanName,
                    bio = cleanBio,
                    instagramUsername = cleanInsta,
                    instagramUrl = cleanInstaUrl
                )
                _activeAdminProfile.value = updatedSub
                _adminSubProfiles.value = _adminSubProfiles.value.map {
                    if (it.profileId == active.profileId) updatedSub else it
                }
            }
        }
    }

    fun updateProfileImage(photoUri: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(profileImageUrl = photoUri)
        _currentUser.value = updated

        // Update stored accounts list and persist
        val accIndex = _registeredAccounts.indexOfFirst { it.uid == current.uid || it.email.equals(current.email, ignoreCase = true) }
        if (accIndex >= 0) {
            val old = _registeredAccounts[accIndex]
            _registeredAccounts[accIndex] = old.copy(profileImageUrl = photoUri)
            saveRegisteredAccounts()
        }

        // If admin has an active subprofile, update it as well
        if (current.isAdmin) {
            val active = _activeAdminProfile.value
            if (active != null) {
                val updatedSub = active.copy(profileImageUrl = photoUri)
                _activeAdminProfile.value = updatedSub
                _adminSubProfiles.value = _adminSubProfiles.value.map {
                    if (it.profileId == active.profileId) updatedSub else it
                }
            }
        }
    }

    // ==========================================
    // SONG UPLOAD & AI DETECTION WORKFLOW
    // ==========================================

    suspend fun uploadSong(
        title: String,
        description: String,
        lyrics: String,
        explicit: Boolean,
        allowDownload: Boolean,
        mentionedUsernames: List<String>,
        audioUri: String = "",
        coverUri: String = "",
        genre: String = "Pop"
    ): Song {
        val user = _currentUser.value ?: throw IllegalStateException("Not authenticated")

        val cleanTitle = title.trim()
        val cleanDesc = description.trim()
        val cleanGenre = if (genre.isNotBlank()) genre.trim() else "Pop"

        val newSong = Song(
            songId = "song_upload_" + UUID.randomUUID().toString().take(8),
            title = cleanTitle,
            description = cleanDesc,
            primaryCreatorUid = user.uid,
            primaryCreatorName = user.displayName,
            primaryCreatorUsername = user.username,
            creatorUids = listOf(user.uid),
            mentionedUsernames = mentionedUsernames,
            mentionedUserIds = emptyList(),
            acceptedCollaboratorUids = emptyList(),
            pendingCollaboratorUids = mentionedUsernames,
            coverUrl = coverUri,
            coverResId = if (coverUri.isBlank()) R.drawable.cover_night_drive else null,
            audioUrl = if (audioUri.isNotBlank()) audioUri else getDefaultStreamUrl(cleanGenre),
            genre = cleanGenre,
            secondaryGenres = emptyList(),
            tags = listOf(cleanGenre, "Original"),
            mood = "Chill",
            energy = "Medium",
            tempo = "120 BPM",
            language = "Hindi / English",
            lyrics = lyrics,
            explicit = explicit,
            allowDownload = allowDownload,
            uploadStatus = if (user.isAdmin) UploadStatus.PUBLISHED else UploadStatus.PENDING_REVIEW,
            moderationStatus = if (user.isAdmin) "approved" else "pending",
            isLifetimeConfirmed = user.isAdmin,
            publishedAt = System.currentTimeMillis(),
            totalViews = 0,
            likesCount = 0,
            dislikesCount = 0,
            downloadsCount = 0
        )

        _songs.value = listOf(newSong) + _songs.value
        saveCustomSongs()

        // Create collaboration invites for mentioned users
        mentionedUsernames.forEach { username ->
            val invite = CollaborationInvite(
                songId = newSong.songId,
                songTitle = newSong.title,
                inviterUid = user.uid,
                inviterName = user.displayName,
                inviterUsername = user.username,
                mentionedUid = username
            )
            _collaborationInvites.value = _collaborationInvites.value + invite
        }

        // Send notification to Admin moderation queue for non-admin submissions
        if (!user.isAdmin) {
            val adminAlert = NotificationItem(
                recipientUid = "admin_rj_primary",
                title = "New Song Pending Confirmation",
                message = "${user.displayName} (@${user.username}) submitted '${newSong.title}' for review.",
                type = "alert",
                actionTargetId = newSong.songId
            )
            _notifications.value = listOf(adminAlert) + _notifications.value
        }

        return newSong
    }

    // ==========================================
    // ADMIN MODERATION & CONTROLS
    // ==========================================

    fun approveSong(songId: String, note: String = "") {
        markSongLifetimeConfirmed(songId)
        _songs.value = _songs.value.map { song ->
            if (song.songId == songId) {
                // Notify uploader
                val notif = NotificationItem(
                    recipientUid = song.primaryCreatorUid,
                    title = "Song Confirmed & Published",
                    message = "Your song '${song.title}' has been approved by Admin and is now officially live!",
                    type = "song_approved",
                    actionTargetId = song.songId
                )
                _notifications.value = listOf(notif) + _notifications.value

                song.copy(
                    uploadStatus = UploadStatus.PUBLISHED,
                    moderationStatus = "approved",
                    moderationNote = if (note.isNotBlank()) note else "Approved by Admin",
                    publishedAt = System.currentTimeMillis(),
                    isLifetimeConfirmed = true
                )
            } else song
        }
        saveCustomSongs()
    }

    fun rejectSong(songId: String, reason: String) {
        _songs.value = _songs.value.map { song ->
            if (song.songId == songId) {
                val notif = NotificationItem(
                    recipientUid = song.primaryCreatorUid,
                    title = "Song Submission Rejected",
                    message = "Your song '${song.title}' was rejected: $reason",
                    type = "song_rejected",
                    actionTargetId = song.songId
                )
                _notifications.value = listOf(notif) + _notifications.value

                song.copy(
                    uploadStatus = UploadStatus.REJECTED,
                    moderationStatus = "rejected",
                    moderationNote = reason
                )
            } else song
        }
        saveCustomSongs()
    }

    fun requestSongChanges(songId: String, notes: String) {
        _songs.value = _songs.value.map { song ->
            if (song.songId == songId) {
                val notif = NotificationItem(
                    recipientUid = song.primaryCreatorUid,
                    title = "Changes Requested on Song",
                    message = "Admin requested updates for '${song.title}': $notes",
                    type = "changes_requested",
                    actionTargetId = song.songId
                )
                _notifications.value = listOf(notif) + _notifications.value

                song.copy(
                    uploadStatus = UploadStatus.CHANGES_REQUESTED,
                    moderationStatus = "changes_requested",
                    moderationNote = notes
                )
            } else song
        }
        saveCustomSongs()
    }

    fun editSongMetadata(songId: String, title: String, genre: String, tags: List<String>, explicit: Boolean) {
        _songs.value = _songs.value.map { song ->
            if (song.songId == songId) {
                song.copy(
                    title = title,
                    genre = genre,
                    tags = tags,
                    explicit = explicit
                )
            } else song
        }
        saveCustomSongs()
    }

    fun deleteSong(songId: String) {
        markSongPermanentlyDeleted(songId)
        _songs.value = _songs.value.filterNot { it.songId == songId }
        _playlists.value = _playlists.value.map { pl ->
            pl.copy(songIds = pl.songIds.filterNot { it == songId })
        }
        _featuredSlides.value = _featuredSlides.value.filterNot { it.destinationId == songId }
        saveCustomSongs()
        savePlaylists()
    }

    // Featured Slider Manager
    fun addFeaturedSlide(slide: FeaturedSlide) {
        _featuredSlides.value = _featuredSlides.value + slide
    }

    fun removeFeaturedSlide(slideId: String) {
        _featuredSlides.value = _featuredSlides.value.filterNot { it.id == slideId }
    }

    fun toggleSlidePin(slideId: String) {
        _featuredSlides.value = _featuredSlides.value.map {
            if (it.id == slideId) it.copy(isPinned = !it.isPinned) else it
        }
    }

    // Trending Management
    fun setSongTrendingRank(songId: String, rank: Int, pin: Boolean = false) {
        _songs.value = _songs.value.map { song ->
            if (song.songId == songId) {
                song.copy(
                    trendingPosition = rank,
                    adminTrendingOverride = rank,
                    isPinnedTrending = pin
                )
            } else song
        }
    }

    fun removeSongFromTrending(songId: String) {
        _songs.value = _songs.value.map { song ->
            if (song.songId == songId) {
                song.copy(trendingPosition = null, adminTrendingOverride = null, isPinnedTrending = false)
            } else song
        }
    }

    // ==========================================
    // REACTIONS (LIKE / DISLIKE) & VIEWS
    // ==========================================

    fun toggleReaction(songId: String, reaction: ReactionType) {
        val user = _currentUser.value ?: return
        val key = "${user.uid}_$songId"
        val currentReaction = _userReactions.value[key] ?: ReactionType.NONE

        val nextReaction = if (currentReaction == reaction) ReactionType.NONE else reaction
        _userReactions.value = _userReactions.value + (key to nextReaction)
        saveUserReactions()

        _songs.value = _songs.value.map { song ->
            if (song.songId == songId) {
                var likes = song.likesCount
                var dislikes = song.dislikesCount

                if (currentReaction == ReactionType.LIKE) likes--
                if (currentReaction == ReactionType.DISLIKE) dislikes--

                if (nextReaction == ReactionType.LIKE) likes++
                if (nextReaction == ReactionType.DISLIKE) dislikes++

                song.copy(
                    likesCount = likes.coerceAtLeast(0),
                    dislikesCount = dislikes.coerceAtLeast(0)
                )
            } else song
        }
        saveCustomSongs()
    }

    fun recordValidView(songId: String) {
        val user = _currentUser.value
        _songs.value = _songs.value.map { song ->
            if (song.songId == songId) {
                // Update score dynamically
                val newViews = song.totalViews + 1
                val newListeners = song.uniqueListeners + 1
                val newScore = (newViews * 1.0) + (song.likesCount * 3.0) - (song.dislikesCount * 2.0) + (song.shareCount * 4.0)
                song.copy(
                    totalViews = newViews,
                    uniqueListeners = newListeners,
                    automaticTrendingScore = newScore
                )
            } else song
        }

        // Record into Room Database listening history
        scope.launch {
            val song = _songs.value.firstOrNull { it.songId == songId }
            if (song != null) {
                dao.insertHistory(
                    ListeningHistoryEntity(
                        songId = song.songId,
                        songTitle = song.title,
                        artistName = song.displayArtists,
                        genre = song.genre,
                        durationPlayedSec = 30
                    )
                )
                // Boost genre taste
                val currentTaste = _genreTasteWeights.value.toMutableMap()
                currentTaste[song.genre] = (currentTaste[song.genre] ?: 0) + 1
                _genreTasteWeights.value = currentTaste
            }
        }
    }

    // ==========================================
    // DOWNLOADS (OFFLINE ROOM STORAGE & APP-PRIVATE VAULT)
    // ==========================================

    private fun getOfflineVaultDir(): java.io.File {
        val vault = java.io.File(context.filesDir, "offline_vault")
        if (!vault.exists()) {
            vault.mkdirs()
        }
        return vault
    }

    private fun generateOfflineWav(file: java.io.File, song: Song) {
        val sampleRate = 22050
        val durationSec = song.durationSec.coerceIn(15, 60)
        val numSamples = sampleRate * durationSec
        val dataSize = numSamples * 2 // 16-bit mono = 2 bytes per sample
        val totalSize = 36 + dataSize

        java.io.FileOutputStream(file).use { out ->
            val buf = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            buf.put("RIFF".toByteArray())
            buf.putInt(totalSize)
            buf.put("WAVE".toByteArray())
            buf.put("fmt ".toByteArray())
            buf.putInt(16) // Subchunk1Size
            buf.putShort(1.toShort()) // PCM format
            buf.putShort(1.toShort()) // Mono
            buf.putInt(sampleRate)
            buf.putInt(sampleRate * 2) // ByteRate
            buf.putShort(2.toShort()) // BlockAlign
            buf.putShort(16.toShort()) // BitsPerSample
            buf.put("data".toByteArray())
            buf.putInt(dataSize)
            out.write(buf.array())

            // Write PCM melodic progression based on genre
            val baseFreq = when (song.genre.lowercase()) {
                "hip-hop", "rap" -> 164.81
                "pop" -> 261.63
                "lo-fi", "chill" -> 220.00
                "rock" -> 196.00
                "electronic", "edm" -> 293.66
                else -> 261.63
            }
            val chordOffsets = doubleArrayOf(1.0, 1.25, 1.5, 1.33)
            val pcmBuf = java.nio.ByteBuffer.allocate(sampleRate * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)

            for (sec in 0 until durationSec) {
                pcmBuf.clear()
                val chordRatio = chordOffsets[(sec / 4) % chordOffsets.size]
                val freq = baseFreq * chordRatio
                for (i in 0 until sampleRate) {
                    val time = i.toDouble() / sampleRate
                    val sample = (kotlin.math.sin(2.0 * Math.PI * freq * time) * 0.4 +
                            kotlin.math.sin(2.0 * Math.PI * (freq * 1.5) * time) * 0.2) * 32767.0
                    pcmBuf.putShort(sample.toInt().coerceIn(-32768, 32767).toShort())
                }
                out.write(pcmBuf.array())
            }
        }
    }

    fun toggleDownload(song: Song) {
        scope.launch {
            val isDownloaded = dao.isDownloaded(song.songId)
            val vaultDir = getOfflineVaultDir()
            val vaultFile = java.io.File(vaultDir, "${song.songId}.rjtrack")

            if (isDownloaded) {
                // Remove from internal private vault
                if (vaultFile.exists()) {
                    vaultFile.delete()
                }
                dao.deleteDownload(song.songId)
                _songs.value = _songs.value.map {
                    if (it.songId == song.songId) it.copy(downloadsCount = (it.downloadsCount - 1).coerceAtLeast(0)) else it
                }
            } else {
                // Save into internal private vault only (hidden from Android gallery, music, and public file manager)
                var savedAudioBytes = false
                try {
                    if (song.audioUrl.isNotBlank()) {
                        val uri = android.net.Uri.parse(song.audioUrl)
                        if (uri.scheme == "content" || uri.scheme == "file") {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                java.io.FileOutputStream(vaultFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            savedAudioBytes = vaultFile.exists() && vaultFile.length() > 1000
                        } else if (uri.scheme == "http" || uri.scheme == "https") {
                            kotlinx.coroutines.withContext(Dispatchers.IO) {
                                val conn = java.net.URL(song.audioUrl).openConnection() as java.net.HttpURLConnection
                                conn.connectTimeout = 5000
                                conn.readTimeout = 5000
                                conn.inputStream.use { input ->
                                    java.io.FileOutputStream(vaultFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                            savedAudioBytes = vaultFile.exists() && vaultFile.length() > 1000
                        }
                    }
                } catch (_: Exception) {
                    savedAudioBytes = false
                }

                // If remote audio was not reachable or is placeholder, synthesize offline audio track in the private vault
                if (!savedAudioBytes || !vaultFile.exists() || vaultFile.length() < 100) {
                    try {
                        generateOfflineWav(vaultFile, song)
                    } catch (_: Exception) {
                        try { vaultFile.writeText("RJ_TRACK_${song.songId}") } catch (_: Exception) {}
                    }
                }

                dao.insertDownload(
                    DownloadedSongEntity(
                        songId = song.songId,
                        title = song.title,
                        artistName = song.displayArtists,
                        coverUrl = song.coverUrl,
                        localFilePath = vaultFile.absolutePath,
                        durationSec = song.durationSec,
                        genre = song.genre
                    )
                )
                _songs.value = _songs.value.map {
                    if (it.songId == song.songId) it.copy(downloadsCount = it.downloadsCount + 1) else it
                }
            }
        }
    }

    fun getDownloadedSongs(): Flow<List<DownloadedSongEntity>> = dao.getAllDownloads()

    // ==========================================
    // COLLABORATION INVITATIONS
    // ==========================================

    fun respondToCollaboration(inviteId: String, accept: Boolean) {
        val invite = _collaborationInvites.value.firstOrNull { it.id == inviteId } ?: return
        val status = if (accept) CollaborationStatus.ACCEPTED else CollaborationStatus.DECLINED

        _collaborationInvites.value = _collaborationInvites.value.map {
            if (it.id == inviteId) it.copy(status = status, respondedAt = System.currentTimeMillis()) else it
        }

        if (accept) {
            _songs.value = _songs.value.map { song ->
                if (song.songId == invite.songId) {
                    val accepted = (song.acceptedCollaboratorUids + invite.mentionedUid).distinct()
                    song.copy(acceptedCollaboratorUids = accepted)
                } else song
            }

            // Notify inviter
            val notif = NotificationItem(
                recipientUid = invite.inviterUid,
                title = "Collaboration Accepted",
                message = "${invite.mentionedUid} accepted collaboration on '${invite.songTitle}'.",
                type = "collab_accepted",
                actionTargetId = invite.songId
            )
            _notifications.value = listOf(notif) + _notifications.value
        }
    }

    // ==========================================
    // PLAYLISTS
    // ==========================================

    fun createPlaylist(name: String, desc: String, isPublic: Boolean): Playlist {
        val user = _currentUser.value ?: throw IllegalStateException("Not authenticated")
        val pl = Playlist(
            title = name.trim(),
            description = desc.trim(),
            ownerUid = user.uid,
            ownerName = user.displayName,
            isPublic = isPublic
        )
        _playlists.value = _playlists.value + pl
        savePlaylists()
        return pl
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        _playlists.value = _playlists.value.map { pl ->
            if (pl.playlistId == playlistId && !pl.songIds.contains(songId)) {
                pl.copy(songIds = pl.songIds + songId, updatedAt = System.currentTimeMillis())
            } else pl
        }
        _songs.value = _songs.value.map { song ->
            if (song.songId == songId) song.copy(playlistAddCount = song.playlistAddCount + 1) else song
        }
        savePlaylists()
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        _playlists.value = _playlists.value.map { pl ->
            if (pl.playlistId == playlistId) {
                pl.copy(songIds = pl.songIds.filterNot { it == songId }, updatedAt = System.currentTimeMillis())
            } else pl
        }
        savePlaylists()
    }

    fun deletePlaylist(playlistId: String) {
        _playlists.value = _playlists.value.filterNot { it.playlistId == playlistId }
        savePlaylists()
    }

    // ==========================================
    // FOLLOW SYSTEM
    // ==========================================

    fun toggleFollow(targetUid: String) {
        val user = _currentUser.value ?: return
        val pair = user.uid to targetUid
        val currentSet = _follows.value
        if (currentSet.contains(pair)) {
            _follows.value = currentSet - pair
        } else {
            _follows.value = currentSet + pair
            val notif = NotificationItem(
                recipientUid = targetUid,
                title = "New Follower",
                message = "${user.displayName} (@${user.username}) started following you.",
                type = "follower"
            )
            _notifications.value = listOf(notif) + _notifications.value
        }
        saveUserFollows()
    }

    fun isFollowing(targetUid: String): Boolean {
        val user = _currentUser.value ?: return false
        return _follows.value.contains(user.uid to targetUid)
    }

    // ==========================================
    // REPORTS
    // ==========================================

    fun submitReport(targetType: String, targetId: String, targetTitle: String, reason: String, description: String) {
        val user = _currentUser.value ?: return
        val report = SongReport(
            reporterUid = user.uid,
            targetType = targetType,
            targetId = targetId,
            targetTitle = targetTitle,
            reason = reason,
            description = description
        )
        _reports.value = listOf(report) + _reports.value
    }

    // ==========================================
    // NOTIFICATIONS
    // ==========================================

    fun markNotificationRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    // ==========================================
    // ANALYTICS CALCULATION FOR CREATOR DASHBOARD
    // ==========================================

    fun getCreatorAnalytics(creatorUid: String): CreatorAnalytics {
        val creatorSongs = _songs.value.filter {
            it.primaryCreatorUid == creatorUid || it.acceptedCollaboratorUids.contains(creatorUid)
        }

        val totalViews = creatorSongs.sumOf { it.totalViews }
        val uniqueListeners = creatorSongs.sumOf { it.uniqueListeners }
        val likes = creatorSongs.sumOf { it.likesCount }
        val dislikes = creatorSongs.sumOf { it.dislikesCount }
        val downloads = creatorSongs.sumOf { it.downloadsCount }
        val shares = creatorSongs.sumOf { it.shareCount }
        val playlistAdds = creatorSongs.sumOf { it.playlistAddCount }

        val genreMap = mutableMapOf<String, Int>()
        creatorSongs.forEach {
            genreMap[it.genre] = (genreMap[it.genre] ?: 0) + 1
        }

        return CreatorAnalytics(
            totalViews = totalViews,
            uniqueListeners = uniqueListeners,
            likesCount = likes,
            dislikesCount = dislikes,
            downloadsCount = downloads,
            sharesCount = shares,
            playlistAddsCount = playlistAdds,
            avgListenDurationSec = 168,
            completionRate = 78.4f,
            replayRate = 24.2f,
            dailyViews = listOf(
                "Mon" to (totalViews * 0.12).toLong(),
                "Tue" to (totalViews * 0.14).toLong(),
                "Wed" to (totalViews * 0.11).toLong(),
                "Thu" to (totalViews * 0.15).toLong(),
                "Fri" to (totalViews * 0.18).toLong(),
                "Sat" to (totalViews * 0.16).toLong(),
                "Sun" to (totalViews * 0.14).toLong()
            ),
            topGenres = genreMap.toList().sortedByDescending { it.second },
            topSongs = creatorSongs.sortedByDescending { it.totalViews }
        )
    }

    // ==========================================
    // PERSONALIZED RECOMMENDATIONS ENGINE
    // ==========================================

    fun getRecommendedSongs(): List<Song> {
        val published = _songs.value.filter { it.uploadStatus == UploadStatus.PUBLISHED }
        val weights = _genreTasteWeights.value

        return published.sortedByDescending { song ->
            val genreWeight = weights[song.genre] ?: 0
            val viewsWeight = song.totalViews / 10000.0
            val likeRatio = if (song.likesCount + song.dislikesCount > 0) {
                song.likesCount.toDouble() / (song.likesCount + song.dislikesCount)
            } else 1.0

            (genreWeight * 10) + viewsWeight + (likeRatio * 5)
        }
    }

    fun getTrendingSongs(): List<Song> {
        val published = _songs.value.filter { it.uploadStatus == UploadStatus.PUBLISHED }
        return published.sortedWith(
            compareByDescending<Song> { it.isPinnedTrending }
                .thenBy { it.trendingPosition ?: Int.MAX_VALUE }
                .thenByDescending { it.automaticTrendingScore }
        )
    }
}
