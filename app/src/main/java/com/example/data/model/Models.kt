package com.example.data.model

import java.util.UUID

enum class UploadStatus(val label: String) {
    DRAFT("Draft"),
    UPLOADING("Uploading"),
    PROCESSING("Processing"),
    GENRE_ANALYSIS("AI Genre Analysis"),
    PENDING_COLLABORATION("Pending Collaborators"),
    PENDING_REVIEW("Pending Admin Review"),
    APPROVED("Approved"),
    PUBLISHED("Published"),
    CHANGES_REQUESTED("Changes Requested"),
    REJECTED("Rejected"),
    HIDDEN("Hidden"),
    REMOVED("Removed")
}

enum class CollaborationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    REMOVED
}

enum class ReactionType {
    NONE,
    LIKE,
    DISLIKE
}

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val username: String,
    val profileImageUrl: String = "",
    val bio: String = "",
    val instagramUsername: String = "",
    val instagramUrl: String = "",
    val youtubeUrl: String = "",
    val twitterUrl: String = "",
    val websiteUrl: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val monthlyListeners: Int = 0,
    val verified: Boolean = false,
    val isAdmin: Boolean = false,
    val isSuspended: Boolean = false,
    val adminProfileIds: List<String> = emptyList(),
    val activeProfileId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class StoredAccount(
    val uid: String,
    val email: String,
    val password: String,
    val displayName: String,
    val username: String,
    val profileImageUrl: String = "",
    val bio: String = "",
    val instagramUsername: String = "",
    val instagramUrl: String = "",
    val isAdmin: Boolean = false,
    val registeredAt: Long = System.currentTimeMillis()
)

data class AdminSubProfile(
    val profileId: String = UUID.randomUUID().toString(),
    val ownerUid: String,
    val displayName: String,
    val username: String,
    val profileImageUrl: String = "",
    val bio: String = "",
    val instagramUsername: String = "",
    val instagramUrl: String = "",
    val isDefault: Boolean = false,
    val isPublic: Boolean = true,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val monthlyListeners: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Song(
    val songId: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val primaryCreatorUid: String,
    val primaryCreatorName: String,
    val primaryCreatorUsername: String,
    val creatorUids: List<String> = listOf(primaryCreatorUid),
    val mentionedUsernames: List<String> = emptyList(),
    val mentionedUserIds: List<String> = emptyList(),
    val acceptedCollaboratorUids: List<String> = emptyList(),
    val pendingCollaboratorUids: List<String> = emptyList(),
    val coverUrl: String = "",
    val coverResId: Int? = null,
    val audioUrl: String = "",
    val audioResId: Int? = null,
    val genre: String = "Pop",
    val secondaryGenres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val mood: String = "Chill",
    val energy: String = "Medium",
    val tempo: String = "Mid-Tempo (110 BPM)",
    val language: String = "Hindi / English",
    val durationSec: Int = 214,
    val lyrics: String = "",
    val explicit: Boolean = false,
    val allowDownload: Boolean = true,
    val uploadStatus: UploadStatus = UploadStatus.PUBLISHED,
    val moderationStatus: String = "approved",
    val moderationNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val publishedAt: Long = System.currentTimeMillis(),
    val totalViews: Long = 0,
    val uniqueListeners: Long = 0,
    val likesCount: Long = 0,
    val dislikesCount: Long = 0,
    val downloadsCount: Long = 0,
    val shareCount: Long = 0,
    val playlistAddCount: Long = 0,
    val automaticTrendingScore: Double = 0.0,
    val adminTrendingOverride: Int? = null,
    val trendingPosition: Int? = null,
    val featuredOrder: Int? = null,
    val isPinnedTrending: Boolean = false,
    val isLifetimeConfirmed: Boolean = false
) {
    val displayArtists: String
        get() {
            return if (mentionedUsernames.isNotEmpty()) {
                "$primaryCreatorName ft. ${mentionedUsernames.joinToString(", ")}"
            } else {
                primaryCreatorName
            }
        }

    val formattedDuration: String
        get() {
            val minutes = durationSec / 60
            val seconds = durationSec % 60
            return String.format("%d:%02d", minutes, seconds)
        }
}

data class CollaborationInvite(
    val id: String = UUID.randomUUID().toString(),
    val songId: String,
    val songTitle: String,
    val songCoverUrl: String = "",
    val inviterUid: String,
    val inviterName: String,
    val inviterUsername: String,
    val mentionedUid: String,
    val status: CollaborationStatus = CollaborationStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val respondedAt: Long? = null
)

data class Playlist(
    val playlistId: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val coverUrl: String = "",
    val ownerUid: String,
    val ownerName: String,
    val isPublic: Boolean = true,
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class FeaturedSlide(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String,
    val creatorName: String,
    val artworkUrl: String = "",
    val artworkResId: Int? = null,
    val destinationType: String = "SONG", // SONG, PLAYLIST, USER
    val destinationId: String = "",
    val isEnabled: Boolean = true,
    val isPinned: Boolean = false,
    val sortOrder: Int = 0
)

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val recipientUid: String,
    val title: String,
    val message: String,
    val type: String, // song_approved, collab_invite, mention, trending, follower, alert
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionTargetId: String? = null
)

data class CreatorAnalytics(
    val totalViews: Long,
    val uniqueListeners: Long,
    val likesCount: Long,
    val dislikesCount: Long,
    val downloadsCount: Long,
    val sharesCount: Long,
    val playlistAddsCount: Long,
    val avgListenDurationSec: Int,
    val completionRate: Float,
    val replayRate: Float,
    val dailyViews: List<Pair<String, Long>>,
    val topGenres: List<Pair<String, Int>>,
    val topSongs: List<Song>
)

data class SongReport(
    val id: String = UUID.randomUUID().toString(),
    val reporterUid: String,
    val targetType: String, // SONG, USER, PLAYLIST
    val targetId: String,
    val targetTitle: String,
    val reason: String,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val resolved: Boolean = false
)

data class GenreCategory(
    val id: String,
    val name: String,
    val subtitle: String,
    val imageResId: Int? = null
)
