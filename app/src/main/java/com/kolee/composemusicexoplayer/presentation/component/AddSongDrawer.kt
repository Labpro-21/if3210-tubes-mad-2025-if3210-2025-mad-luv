package com.kolee.composemusicexoplayer.presentation.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.data.auth.UserPreferences
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerEvent
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AddSongDrawer(
    modifier: Modifier = Modifier,
    visible: Boolean,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onSongAdded: (Boolean, String) -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentUserId by remember { mutableStateOf("1") } // Default user ID

    var isImageUploaded by remember { mutableStateOf(false) }
    var isAudioUploaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val userName = userPreferences.getUserName.first()
            currentUserId = userName ?: "1"
        }
    }

    val launcherAudio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let { uri ->
            audioUri = uri
            isAudioUploaded = true
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""

            val embeddedPicture = retriever.embeddedPicture
            if (embeddedPicture != null) {
                val bitmap = BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
                val imageFile = File(context.cacheDir, "cover.jpg")
                FileOutputStream(imageFile).use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }
                imageUri = imageFile.toUri()
                isImageUploaded = true
            }

            retriever.release()
        }
    }

    val launcherImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
        isImageUploaded = it != null
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300))
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(1f)
                .heightIn(max = 600.dp)
                .offset(y = 250.dp),
            color = MaterialTheme.colors.background,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            elevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Upload Song", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    UploadBox(
                        text = "Upload Photo",
                        onClick = { launcherImage.launch("image/*") },
                        icon = R.drawable.ic_music_unknown,
                        size = 100.dp,
                        isUploaded = isImageUploaded
                    )
                    UploadBox(
                        text = "Upload File",
                        onClick = { launcherAudio.launch("audio/*") },
                        icon = R.drawable.ic_music_unknown,
                        size = 100.dp,
                        isUploaded = isAudioUploaded
                    )
                }

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                errorMessage = "Title cannot be empty."
                                return@Button
                            }
                            if (audioUri == null) {
                                errorMessage = "Please select an audio file."
                                return@Button
                            }

                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(context, audioUri)
                            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                            retriever.release()

                            if (duration <= 0L) {
                                errorMessage = "Invalid audio file."
                                return@Button
                            }

                            val id = System.currentTimeMillis()
                            val path = getRealPathFromURI(context, audioUri!!) ?: copyFileToInternalStorage(context, audioUri!!, "$id.mp3")

                            val music = MusicEntity(
                                audioId = id,
                                title = title,
                                artist = artist.ifBlank { "Unknown Artist" },
                                audioPath = path,
                                albumPath = imageUri.toString(),
                                duration = duration,
                                owner = currentUserId
                            )

                            playerViewModel.onEvent(PlayerEvent.addMusic(music))
                            onSongAdded(true, "Song added successfully")
                            Toast.makeText(context, "Song added successfully", Toast.LENGTH_SHORT).show()

                            title = ""
                            artist = ""
                            audioUri = null
                            imageUri = null
                            isAudioUploaded = false
                            isImageUploaded = false
                            errorMessage = null

                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Save")
                    }
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colors.error)
                }
            }
        }
    }
}

@Composable
fun UploadBox(
    text: String,
    onClick: () -> Unit,
    icon: Int,
    size: Dp = 120.dp,
    isUploaded: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .size(size),
        shape = RoundedCornerShape(12.dp),
        border = ButtonDefaults.outlinedBorder,
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(text, style = MaterialTheme.typography.caption)

            if (isUploaded) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = "Uploaded",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
            }
        }
    }
}

fun getRealPathFromURI(context: Context, uri: Uri): String? {
    val projection = arrayOf(MediaStore.Audio.Media.DATA)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        if (cursor.moveToFirst()) {
            return cursor.getString(columnIndex)
        }
    }
    return null
}

fun copyFileToInternalStorage(context: Context, uri: Uri, filename: String): String {
    val file = File(context.filesDir, filename)
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    return file.absolutePath
}