package com.example.ui.screens.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.theme.RjBackground
import com.example.ui.theme.RjBorder
import com.example.ui.theme.RjCard
import com.example.ui.theme.RjCardElevated
import com.example.ui.theme.RjSilverAccent
import com.example.ui.theme.RjSilverMuted
import com.example.ui.theme.RjTextMuted
import com.example.ui.theme.RjTextSecondary
import kotlinx.coroutines.launch

val POPULAR_GENRES = listOf(
    "Pop", "Lo-Fi", "Punjabi", "Hip-Hop", "Bollywood", "Sad",
    "Romantic", "Electronic", "Rock", "Acoustic", "Rap", "Devotional", "Workout", "Chill"
)

fun detectAutomaticGenre(title: String, description: String, lyrics: String): String {
    val text = "$title $description $lyrics".lowercase()
    return when {
        text.contains("punjabi") || text.contains("jatt") || text.contains("pind") || text.contains("yaari") || text.contains("nach") || text.contains("bhangra") -> "Punjabi"
        text.contains("lofi") || text.contains("lo-fi") || text.contains("chill") || text.contains("vibes") || text.contains("rain") || text.contains("aesthetic") -> "Lo-Fi"
        text.contains("sad") || text.contains("dard") || text.contains("judai") || text.contains("alone") || text.contains("crying") || text.contains("heartbreak") || text.contains("yaad") -> "Sad"
        text.contains("love") || text.contains("pyar") || text.contains("ishq") || text.contains("romantic") || text.contains("dil") || text.contains("sanam") || text.contains("tum") -> "Romantic"
        text.contains("rap") || text.contains("hip hop") || text.contains("hiphop") || text.contains("cypher") || text.contains("flow") || text.contains("rhyme") -> "Hip-Hop"
        text.contains("rock") || text.contains("guitar") || text.contains("metal") || text.contains("band") -> "Rock"
        text.contains("bhajan") || text.contains("aarti") || text.contains("shree") || text.contains("krishna") || text.contains("shiva") || text.contains("ram") || text.contains("devotional") || text.contains("god") -> "Devotional"
        text.contains("edm") || text.contains("dj") || text.contains("dance") || text.contains("club") || text.contains("party") || text.contains("remix") -> "Electronic"
        text.contains("acoustic") || text.contains("unplugged") || text.contains("piano") -> "Acoustic"
        text.contains("bollywood") || text.contains("filmi") || text.contains("hindi") -> "Bollywood"
        text.contains("workout") || text.contains("gym") || text.contains("pump") || text.contains("fitness") -> "Workout"
        else -> "Pop"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadSongScreen(
    onNavigateBack: () -> Unit,
    onSubmitUpload: suspend (
        title: String,
        desc: String,
        lyrics: String,
        explicit: Boolean,
        allowDl: Boolean,
        collabs: List<String>,
        audioUri: String,
        coverUri: String,
        genre: String
    ) -> Song,
    onUploadSuccess: (Song) -> Unit,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var selectedAudioUri by remember { mutableStateOf<String>("") }
    var selectedAudioFileName by remember { mutableStateOf<String>("") }

    var selectedCoverUri by remember { mutableStateOf<String>("") }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var lyrics by remember { mutableStateOf("") }
    var collabInput by remember { mutableStateOf("") }
    var collaborators by remember { mutableStateOf<List<String>>(emptyList()) }
    var allowDownload by remember { mutableStateOf(true) }
    var copyrightAffirmed by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Audio File Picker (MIME: audio/*)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAudioUri = uri.toString()
            val path = uri.path ?: "song_audio.mp3"
            selectedAudioFileName = path.substringAfterLast("/").ifBlank { "song_track.mp3" }
        }
    }

    // Song Cover Photo Picker (Image)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedCoverUri = uri.toString()
        }
    }

    Surface(
        color = RjBackground,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAdmin) "UPLOAD & PUBLISH SONG" else "UPLOAD NEW SONG",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ==========================================
                // 1. SELECT AUDIO FILE (AT THE VERY TOP)
                // ==========================================
                Column {
                    Text(
                        text = "1. Select Song Audio File *",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RjCard)
                            .border(
                                1.dp,
                                if (selectedAudioUri.isNotBlank()) Color(0xFF4CAF50) else RjBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { audioPickerLauncher.launch("audio/*") }
                            .padding(16.dp)
                            .testTag("upload_audio_picker_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedAudioUri.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1B5E20)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Audiotrack,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Audio File Attached",
                                                color = Color(0xFF81C784),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = selectedAudioFileName.ifBlank { "audio_track.mp3" },
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Button(
                                    onClick = { audioPickerLauncher.launch("audio/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = RjCardElevated),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Change", color = RjSilverAccent, fontSize = 11.sp)
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = RjSilverAccent,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Choose Audio / Song File",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to select MP3, WAV, AAC, or M4A from your device",
                                    color = RjTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 2. SONG COVER PHOTO / ARTWORK
                // ==========================================
                Column {
                    Text(
                        text = "2. Song Cover Photo / Artwork",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

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
                            .padding(12.dp)
                            .testTag("upload_cover_picker_box"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Artwork Preview Box
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RjCardElevated)
                                .border(1.dp, RjBorder, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedCoverUri.isNotBlank()) {
                                AsyncImage(
                                    model = selectedCoverUri,
                                    contentDescription = "Cover Artwork Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = RjSilverAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedCoverUri.isNotBlank()) "Cover Photo Selected" else "Upload Song Artwork",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (selectedCoverUri.isNotBlank()) "Tap to change image" else "Choose JPG or PNG image from device gallery",
                                color = RjTextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (selectedCoverUri.isNotBlank()) "Change" else "Browse",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ==========================================
                // 3. SONG DETAILS
                // ==========================================
                Text(
                    text = "3. Song Details",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Song Title Input
                Column {
                    Text(
                        text = "Song Title *",
                        color = RjTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. Midnight Drive, Tere Bina") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = RjBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = RjTextMuted,
                            unfocusedPlaceholderColor = RjTextMuted
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_title_input")
                    )
                }

                // Description
                Column {
                    Text(
                        text = "Description / Story behind the track",
                        color = RjTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Tell listeners about this song, mood, or journey...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = RjBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = RjTextMuted,
                            unfocusedPlaceholderColor = RjTextMuted
                        ),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Lyrics
                Column {
                    Text(
                        text = "Lyrics / Verses (Optional)",
                        color = RjTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = lyrics,
                        onValueChange = { lyrics = it },
                        placeholder = { Text("Paste song lyrics for listeners to follow along...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = RjBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = RjTextMuted,
                            unfocusedPlaceholderColor = RjTextMuted
                        ),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Mention Collaborators
                Column {
                    Text(
                        text = "Mention Collaborators / Featured Artists",
                        color = RjTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = collabInput,
                            onValueChange = { collabInput = it },
                            placeholder = { Text("username without @ (e.g. justrjmusics)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = RjBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedPlaceholderColor = RjTextMuted,
                                unfocusedPlaceholderColor = RjTextMuted
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val clean = collabInput.trim().removePrefix("@")
                                if (clean.isNotBlank() && !collaborators.contains(clean)) {
                                    collaborators = collaborators + clean
                                    collabInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (collaborators.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            collaborators.forEach { collab ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(RjCardElevated)
                                        .border(1.dp, RjBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "@$collab", color = RjSilverAccent, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = RjTextMuted,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { collaborators = collaborators - collab }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Toggles: Allow Downloads
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Allow Offline Streaming Downloads", color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = allowDownload,
                        onCheckedChange = { allowDownload = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF444444)
                        )
                    )
                }

                // Admin Moderation Notice Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RjCardElevated)
                        .border(1.dp, RjSilverAccent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = RjSilverAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isAdmin) "Instant Admin Publishing" else "Admin Confirmation Flow",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAdmin) {
                                    "As the Admin, your track will be published immediately to the public feed."
                                } else {
                                    "All uploaded songs are sent to the Admin queue. The track will be visible in the main feed once confirmed by the Admin."
                                },
                                color = RjTextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Copyright Affirmation Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { copyrightAffirmed = !copyrightAffirmed },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = copyrightAffirmed,
                        onCheckedChange = { copyrightAffirmed = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.White,
                            checkmarkColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "I affirm that I own 100% intellectual rights or have explicit permission to distribute this audio track on Just RJ Music.",
                        color = RjTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFEF5350),
                        fontSize = 12.sp
                    )
                }

                // Submit Upload Button
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            errorMessage = "Please enter a song title."
                            return@Button
                        }
                        if (!copyrightAffirmed) {
                            errorMessage = "Please confirm the copyright ownership declaration."
                            return@Button
                        }
                        isSubmitting = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val autoGenre = detectAutomaticGenre(title, description, lyrics)
                                val created = onSubmitUpload(
                                    title,
                                    description,
                                    lyrics,
                                    false,
                                    allowDownload,
                                    collaborators,
                                    selectedAudioUri,
                                    selectedCoverUri,
                                    autoGenre
                                )
                                onUploadSuccess(created)
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Upload failed. Please try again."
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("upload_submit_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAdmin) "Publish Song Instantly" else "Submit Song for Admin Review",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
