package com.kolee.composemusicexoplayer.presentation.create_song

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun CreateSongScreen(
    navController: NavHostController,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf<Long>(0) }
    var formattedDuration by remember { mutableStateOf("00:00") }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var audioFilePath by remember { mutableStateOf("") }
    var imageFilePath by remember { mutableStateOf("") }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Storage permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            audioUri = it
            audioFilePath = getRealPathFromURI(context, it) ?: ""

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, it)

                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                duration = durationStr?.toLong() ?: 0

                val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(minutes)
                formattedDuration = String.format("%02d:%02d", minutes, seconds)

                val metadataTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                if (!metadataTitle.isNullOrEmpty()) {
                    title = metadataTitle
                }

                val metadataArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                if (!metadataArtist.isNullOrEmpty()) {
                    artist = metadataArtist
                }

                val embeddedArt = retriever.embeddedPicture
                if (embeddedArt != null) {
                    val tempFile = File(context.cacheDir, "temp_artwork.jpg")
                    tempFile.writeBytes(embeddedArt)
                    imageUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        tempFile
                    )
                    imageFilePath = tempFile.absolutePath
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageUri = it
            imageFilePath = getRealPathFromURI(context, it) ?: ""
        }
    }

    LaunchedEffect(Unit) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Upload Song",
                style = MaterialTheme.typography.h5.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .border(
                            border = BorderStroke(1.dp, Color.Gray),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { imagePickerLauncher.launch("image/*") }
                        .padding(16.dp)
                ) {
                    if (imageUri != null) {
                        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.bg),
                            contentDescription = "Upload Photo",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Upload Photo",
                        color = Color.Gray,
                        style = MaterialTheme.typography.caption
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .border(
                            border = BorderStroke(1.dp, Color.Gray),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { audioPickerLauncher.launch("audio/*") }
                        .padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_music_unknown),
                        contentDescription = "Upload File",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Upload File",
                        color = Color.Gray,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
            if (duration > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Duration: $formattedDuration",
                    color = Color.White,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Title",
                color = Color.White,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    placeholderColor = Color.Gray,
                    backgroundColor = Color.DarkGray,
                    cursorColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Artist",
                color = Color.White,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                placeholder = { Text("Artist") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    placeholderColor = Color.Gray,
                    backgroundColor = Color.DarkGray,
                    cursorColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.DarkGray,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        // Validate inputs
                        if (title.isBlank()) {
                            Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (audioUri == null) {
                            Toast.makeText(context, "Please select an audio file", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        scope.launch {
                            val musicEntity = MusicEntity(
                                audioId = System.currentTimeMillis().inc(),
                                title = title,
                                artist = artist,
                                duration = duration,
                                albumPath = imageFilePath,
                                audioPath = audioFilePath,
                                loved = false
                            )
                            playerViewModel.addMusic(musicEntity)

                            navController.popBackStack()

                            Toast.makeText(context, "Song added successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1DB954), // Spotify green
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

private fun getRealPathFromURI(context: Context, uri: Uri): String? {
    val projection = arrayOf(MediaStore.Audio.Media.DATA)
    val cursor = context.contentResolver.query(uri, projection, null, null, null)
    return cursor?.use {
        val columnIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        it.moveToFirst()
        it.getString(columnIndex)
    }
}