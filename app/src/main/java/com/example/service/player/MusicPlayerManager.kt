package com.example.service.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import com.example.data.model.ReactionType
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val totalDurationMs: Long = 0,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val userReaction: ReactionType = ReactionType.NONE,
    val isAutoplayEnabled: Boolean = true
)

class MusicPlayerManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var playbackJob: Job? = null
    private var audioSynthesisJob: Job? = null
    private var isToneRunning = false

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // View tracking listener callback (e.g. called after 30s playback)
    var onTrackValidView: ((songId: String) -> Unit)? = null
    private var accumulatedPlaybackTimeMs = 0L
    private var viewRecordedForCurrentSong = false

    private var mediaPlayer: MediaPlayer? = null

    fun playSong(song: Song, newQueue: List<Song> = emptyList()) {
        val queue = if (newQueue.isNotEmpty()) newQueue else if (_playerState.value.queue.isEmpty()) listOf(song) else _playerState.value.queue
        val index = queue.indexOfFirst { it.songId == song.songId }.let { if (it == -1) 0 else it }

        accumulatedPlaybackTimeMs = 0L
        viewRecordedForCurrentSong = false

        val durationMs = (song.durationSec * 1000L).coerceAtLeast(10000L)
        _playerState.value = _playerState.value.copy(
            currentSong = song,
            isPlaying = true,
            currentPositionMs = 0,
            totalDurationMs = durationMs,
            queue = queue,
            queueIndex = index
        )

        stopAudioPlayback()

        // Check if track is available in private offline vault or direct audioUri
        val vaultFile = java.io.File(context.filesDir, "offline_vault/${song.songId}.rjtrack")
        var startedMedia = false

        if (vaultFile.exists() && vaultFile.length() > 500) {
            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(vaultFile.absolutePath)
                    setOnPreparedListener { player ->
                        player.start()
                        val mediaDuration = player.duration.toLong().coerceAtLeast(10000L)
                        _playerState.value = _playerState.value.copy(
                            isPlaying = true,
                            totalDurationMs = mediaDuration
                        )
                    }
                    setOnCompletionListener {
                        playNext()
                    }
                    setOnErrorListener { _, _, _ ->
                        startAudioToneSimulation(song)
                        true
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
                startedMedia = true
            } catch (e: Exception) {
                startedMedia = false
            }
        } else if (song.audioUrl.isNotBlank()) {
            try {
                val cleanUrl = song.audioUrl.trim()
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                        setDataSource(cleanUrl)
                    } else {
                        val uri = android.net.Uri.parse(cleanUrl)
                        setDataSource(context, uri)
                    }
                    setOnPreparedListener { player ->
                        player.start()
                        val mediaDuration = player.duration.toLong().coerceAtLeast(10000L)
                        _playerState.value = _playerState.value.copy(
                            isPlaying = true,
                            totalDurationMs = mediaDuration
                        )
                    }
                    setOnCompletionListener {
                        playNext()
                    }
                    setOnErrorListener { _, _, _ ->
                        startAudioToneSimulation(song)
                        true
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
                startedMedia = true
            } catch (e: Exception) {
                startedMedia = false
            }
        }

        if (!startedMedia) {
            startAudioToneSimulation(song)
        }

        startPlaybackProgress()
    }

    fun togglePlayPause() {
        val current = _playerState.value
        if (current.currentSong == null) return

        val nextPlaying = !current.isPlaying
        _playerState.value = current.copy(isPlaying = nextPlaying)

        if (mediaPlayer != null) {
            try {
                if (nextPlaying) {
                    mediaPlayer?.start()
                } else {
                    mediaPlayer?.pause()
                }
            } catch (_: Exception) {}
        }

        if (nextPlaying) {
            startPlaybackProgress()
            if (mediaPlayer == null) {
                startAudioToneSimulation(current.currentSong)
            }
        } else {
            stopAudioToneSimulation()
        }
    }

    private var lastPlayNextTime = 0L

    fun playNext(availableSongsForAutoplay: List<Song> = emptyList()) {
        val now = System.currentTimeMillis()
        if (now - lastPlayNextTime < 600L) return
        lastPlayNextTime = now

        val current = _playerState.value
        if (current.queue.isEmpty()) return

        if (current.isRepeat && current.currentSong != null) {
            playSong(current.currentSong, current.queue)
            return
        }

        val nextIndex = current.queueIndex + 1
        if (nextIndex < current.queue.size) {
            val nextSong = current.queue[nextIndex]
            playSong(nextSong, current.queue)
        } else if (current.isAutoplayEnabled && availableSongsForAutoplay.isNotEmpty()) {
            // Recommendation autoplay based on current genre/artist
            val currentGenre = current.currentSong?.genre ?: ""
            val nextSong = availableSongsForAutoplay
                .filter { it.songId != current.currentSong?.songId }
                .sortedByDescending { if (it.genre.equals(currentGenre, ignoreCase = true)) 2 else 0 }
                .firstOrNull() ?: availableSongsForAutoplay.random()

            val updatedQueue = current.queue + nextSong
            playSong(nextSong, updatedQueue)
        } else {
            // Loop to beginning if shuffle or reached end
            val nextSong = current.queue.first()
            playSong(nextSong, current.queue)
        }
    }

    fun playPrevious() {
        val current = _playerState.value
        if (current.queue.isEmpty()) return

        if (current.currentPositionMs > 3000L) {
            // If already played more than 3s, seek to start
            seekTo(0)
            return
        }

        val prevIndex = current.queueIndex - 1
        if (prevIndex >= 0) {
            playSong(current.queue[prevIndex], current.queue)
        } else {
            playSong(current.queue.last(), current.queue)
        }
    }

    fun seekTo(positionMs: Long) {
        val current = _playerState.value
        val clamped = positionMs.coerceIn(0L, current.totalDurationMs.coerceAtLeast(0L))
        _playerState.value = current.copy(currentPositionMs = clamped)
        try {
            mediaPlayer?.seekTo(clamped.toInt())
        } catch (_: Exception) {}
    }

    fun toggleShuffle() {
        _playerState.value = _playerState.value.copy(isShuffle = !_playerState.value.isShuffle)
    }

    fun toggleRepeat() {
        _playerState.value = _playerState.value.copy(isRepeat = !_playerState.value.isRepeat)
    }

    fun toggleAutoplay() {
        _playerState.value = _playerState.value.copy(isAutoplayEnabled = !_playerState.value.isAutoplayEnabled)
    }

    fun addToQueue(song: Song) {
        val current = _playerState.value
        if (current.queue.none { it.songId == song.songId }) {
            _playerState.value = current.copy(queue = current.queue + song)
        }
    }

    fun playNextInQueue(song: Song) {
        val current = _playerState.value
        val newQueue = current.queue.toMutableList()
        newQueue.removeAll { it.songId == song.songId }
        val insertIndex = (current.queueIndex + 1).coerceAtMost(newQueue.size)
        newQueue.add(insertIndex, song)
        _playerState.value = current.copy(queue = newQueue)
    }

    fun removeFromQueue(songId: String) {
        val current = _playerState.value
        val newQueue = current.queue.filterNot { it.songId == songId }
        val newIndex = if (current.currentSong?.songId == songId) {
            current.queueIndex.coerceAtMost(newQueue.size - 1)
        } else {
            newQueue.indexOfFirst { it.songId == current.currentSong?.songId }
        }
        _playerState.value = current.copy(queue = newQueue, queueIndex = newIndex)
    }

    fun clearQueue() {
        val current = _playerState.value
        val currentSong = current.currentSong
        _playerState.value = current.copy(
            queue = if (currentSong != null) listOf(currentSong) else emptyList(),
            queueIndex = 0
        )
    }

    private fun stopAudioPlayback() {
        stopAudioToneSimulation()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
    }

    private fun startPlaybackProgress() {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (isActive && _playerState.value.isPlaying) {
                delay(250)
                val state = _playerState.value
                val mpPos = try {
                    if (mediaPlayer?.isPlaying == true) mediaPlayer?.currentPosition?.toLong() else null
                } catch (_: Exception) { null }

                val newPos = mpPos ?: (state.currentPositionMs + 250)
                accumulatedPlaybackTimeMs += 250

                // Valid view check: count view after 30s playback or 30% duration
                if (!viewRecordedForCurrentSong && state.currentSong != null) {
                    val duration = state.totalDurationMs
                    val thirtySec = 30000L
                    val thirtyPercent = duration * 0.3
                    if (accumulatedPlaybackTimeMs >= thirtySec || accumulatedPlaybackTimeMs >= thirtyPercent) {
                        viewRecordedForCurrentSong = true
                        onTrackValidView?.invoke(state.currentSong.songId)
                    }
                }

                if (newPos >= state.totalDurationMs && state.totalDurationMs > 0) {
                    _playerState.value = state.copy(currentPositionMs = state.totalDurationMs)
                    val isMediaHandling = try { mediaPlayer?.isPlaying == true } catch (_: Exception) { false }
                    if (!isMediaHandling) {
                        playNext()
                    }
                    break
                } else {
                    _playerState.value = state.copy(currentPositionMs = newPos)
                }
            }
        }
    }

    // Melodic ambient synthesizer for seamless offline/in-app audio generation
    private fun startAudioToneSimulation(song: Song) {
        stopAudioToneSimulation()
        isToneRunning = true
        audioSynthesisJob = scope.launch(Dispatchers.Default) {
            var audioTrack: AudioTrack? = null
            try {
                val sampleRate = 22050
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                audioTrack = track

                track.play()

                // Base frequency depending on song genre
                val baseFreq = when (song.genre.lowercase()) {
                    "lo-fi", "chill" -> 220.0 // A3 warm lofi chord
                    "hip-hop", "rap", "trap" -> 110.0 // A2 deep bass
                    "punjabi", "party" -> 293.66 // D4 bouncy
                    "sad", "romantic" -> 261.63 // C4 melancholic
                    "electronic", "workout" -> 329.63 // E4 synth
                    else -> 220.0
                }

                val bufferSize = sampleRate / 10
                val buffer = ShortArray(bufferSize)
                var phase = 0.0

                while (isActive && isToneRunning && _playerState.value.isPlaying) {
                    val pos = _playerState.value.currentPositionMs / 1000.0
                    // Gentle chord progression variation every 4 seconds
                    val noteFactor = when (((pos / 4.0).toInt()) % 4) {
                        0 -> 1.0
                        1 -> 1.25 // Major 3rd
                        2 -> 1.5  // 5th
                        else -> 1.33 // 4th
                    }
                    val currentFreq = baseFreq * noteFactor

                    for (i in buffer.indices) {
                        // Soft warm synth harmonics
                        val sample1 = sin(2 * Math.PI * currentFreq * phase)
                        val sample2 = 0.4 * sin(2 * Math.PI * currentFreq * 2 * phase)
                        val sample3 = 0.2 * sin(2 * Math.PI * currentFreq * 3 * phase)
                        val combined = (sample1 + sample2 + sample3) / 1.6
                        // Low volume for pleasant ambient tone
                        buffer[i] = (combined * Short.MAX_VALUE * 0.15).toInt().toShort()
                        phase += 1.0 / sampleRate
                        if (phase > 1.0) phase -= 1.0
                    }
                    track.write(buffer, 0, buffer.size)
                }
            } catch (ignored: Exception) {
                // Audio synthesis not critical if device audio output unavailable
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun stopAudioToneSimulation() {
        isToneRunning = false
        audioSynthesisJob?.cancel()
        audioSynthesisJob = null
    }

    fun release() {
        stopAudioPlayback()
        playbackJob?.cancel()
    }
}
