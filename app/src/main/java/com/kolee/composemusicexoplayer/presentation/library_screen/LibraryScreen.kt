package com.kolee.composemusicexoplayer.presentation.library

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.presentation.MusicPlayerSheet.MusicPlayerSheet
import com.kolee.composemusicexoplayer.presentation.component.BottomMusicPlayerHeight
import com.kolee.composemusicexoplayer.presentation.component.BottomMusicPlayerImpl
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerEvent
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel
import com.bumptech.glide.Glide
import com.kolee.composemusicexoplayer.data.network.NetworkSensing
import com.kolee.composemusicexoplayer.presentation.component.NetworkSensingScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

class MusicAdapter(
    private var musicList: List<MusicEntity>,
    private val onItemClick: (MusicEntity) -> Unit,
    private val onEditClick: (MusicEntity) -> Unit
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    private var currentlyPlayingId: Long? = null

    inner class MusicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.itemCard)
        val imageCover: ImageView = view.findViewById(R.id.imageCover)
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textArtist: TextView = view.findViewById(R.id.textArtist)
        val imageWave: ImageView = view.findViewById(R.id.imageWave)
        val editButton: ImageView = view.findViewById(R.id.imageButton2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_music, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val music = musicList[position]

        holder.textTitle.text = music.title
        holder.textArtist.text = music.artist

        Glide.with(holder.itemView.context)
            .load(File(music.albumPath))
            .placeholder(R.drawable.ic_music_unknown)
            .error(R.drawable.ic_music_unknown)
            .into(holder.imageCover)

        val isPlaying = music.audioId == currentlyPlayingId
        holder.imageWave.visibility = if (isPlaying) View.VISIBLE else View.GONE

        val primaryColor = holder.itemView.context.getColor(R.color.spotify_green)
        val defaultTitleColor = holder.itemView.context.getColor(android.R.color.white)
        val defaultArtistColor = holder.itemView.context.getColor(android.R.color.darker_gray)

        holder.textTitle.setTextColor(if (isPlaying) primaryColor else defaultTitleColor)
        holder.textArtist.setTextColor(if (isPlaying) primaryColor else defaultArtistColor)

        holder.card.setOnClickListener {
            onItemClick(music)
        }

        Glide.with(holder.itemView.context)
            .load(R.drawable.ic_edit)
            .into(holder.editButton)

        holder.editButton.visibility = View.VISIBLE
        holder.editButton.setOnClickListener {
            onEditClick(music)
        }
    }

    override fun getItemCount(): Int = musicList.size

    fun updatePlayingSong(songId: Long?) {
        currentlyPlayingId = songId
        notifyDataSetChanged()
    }

    fun updateList(newList: List<MusicEntity>) {
        musicList = newList
        notifyDataSetChanged()
    }
}


@Composable
fun ToggleTab(title: String, selected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (selected) Color(0xFF1ED760) else Color.Transparent
    val textColor = if (selected) Color.Black else Color.White

    Box(
        modifier = Modifier
            .height(36.dp)
            .background(backgroundColor, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 20.dp)
            .wrapContentSize(Alignment.Center)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = textColor,
            style = androidx.compose.material.MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
fun LibraryScreen(
    playerVM: PlayerViewModel = hiltViewModel(),
    navController: NavHostController,
    networkSensing: NetworkSensing
) {
    NetworkSensingScreen(
        networkSensing = networkSensing,
        showFallbackPage = false
    ) {
        val musicUiState by playerVM.uiState.collectAsState()
        val allSongs = musicUiState.musicList
        val likedSongs = allSongs.filter { it.loved }

        var selectedTab by remember { mutableStateOf("All") }
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var showAddSongDrawer by remember { mutableStateOf(false) }
        var showEditSongDrawer by remember { mutableStateOf(false) }
        var currentEditingSong by remember { mutableStateOf<MusicEntity?>(null) }

        var showSuccessToast by remember { mutableStateOf(false) }
        var showErrorToast by remember { mutableStateOf(false) }
        var toastMessage by remember { mutableStateOf("") }
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }

        val songsToDisplay = when (selectedTab) {
            "Liked" -> likedSongs
            else -> allSongs
        }

        val recyclerViewRef = remember { mutableStateOf<RecyclerView?>(null) }

        val adapter = remember {
            MusicAdapter(songsToDisplay,
                onItemClick = { music ->
                    playerVM.onEvent(PlayerEvent.Play(music))
                },
                onEditClick = { music ->
                    currentEditingSong = music
                    showEditSongDrawer = true
                }
            )
        }

        LaunchedEffect(songsToDisplay) {
            adapter.updateList(songsToDisplay)
        }

        LaunchedEffect(musicUiState.currentPlayedMusic) {
            adapter.updatePlayingSong(musicUiState.currentPlayedMusic.audioId)
        }

        LaunchedEffect(showSuccessToast, showErrorToast) {
            if (showSuccessToast || showErrorToast) {
                delay(3000)
                showSuccessToast = false
                showErrorToast = false
            }
        }

        val showHeaderAndTab = !musicUiState.isPlayerExpanded || !musicUiState.isBottomPlayerShow

        Box(modifier = Modifier.fillMaxSize()) {

            if (showHeaderAndTab) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .zIndex(1f)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Your Library",
                            style = androidx.compose.material.MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        IconButton(onClick = { showAddSongDrawer = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToggleTab("All", selectedTab == "All") { selectedTab = "All" }
                        ToggleTab("Liked", selectedTab == "Liked") { selectedTab = "Liked" }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            AndroidView(
                factory = { ctx ->
                    RecyclerView(ctx).apply {
                        layoutManager = LinearLayoutManager(ctx)
                        this.adapter = adapter
                        recyclerViewRef.value = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = if (showHeaderAndTab) 104.dp else 0.dp,
                        bottom = BottomMusicPlayerHeight.value
                    )
            )

            if (musicUiState.currentPlayedMusic != MusicEntity.default) {
                if (musicUiState.isPlayerExpanded) {
                    MusicPlayerSheet(
                        playerVM = playerVM,
                        navController = navController,
                        onCollapse = { playerVM.setPlayerExpanded(false) }
                    )
                } else {
                    BottomMusicPlayerImpl(
                        playerVM = playerVM,
                        musicUiState = musicUiState,
                        onPlayPauseClicked = {
                            playerVM.onEvent(PlayerEvent.PlayPause(musicUiState.isPlaying))
                        },
                        onExpand = { playerVM.setPlayerExpanded(true) }
                    )
                }
            }

            AnimatedVisibility(
                visible = showSuccessToast,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .zIndex(10f)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1ED760)),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = toastMessage,
                            color = Color.Black,
                            style = androidx.compose.material.MaterialTheme.typography.body1
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showErrorToast,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .zIndex(10f)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Error",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = toastMessage,
                            color = Color.White,
                            style = androidx.compose.material.MaterialTheme.typography.body1
                        )
                    }
                }
            }

            if (showDeleteConfirmDialog && currentEditingSong != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text("Delete Song", color = Color.White) },
                    text = { Text("Are you sure you want to delete '${currentEditingSong?.title}'?", color = Color.White) },
                    confirmButton = {
                        Button(
                            onClick = {
                                currentEditingSong?.let { song ->
                                    playerVM.onEvent(PlayerEvent.deleteMusic(song))
                                    toastMessage = "Song deleted successfully"
                                    showSuccessToast = true
                                }
                                showDeleteConfirmDialog = false
                                showEditSongDrawer = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { showDeleteConfirmDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Cancel")
                        }
                    },
                    containerColor = Color(0xFF212121),
                )
            }

            if (showAddSongDrawer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000))
                        .clickable(onClick = { showAddSongDrawer = false })
                        .zIndex(100f) // Ensure it's above everything
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        SongDrawer(
                            visible = true,
                            onDismiss = { showAddSongDrawer = false },
                            onSongAdded = { success, message ->
                                toastMessage = message
                                if (success) {
                                    showSuccessToast = true
                                    showAddSongDrawer = false
                                } else {
                                    showErrorToast = true
                                }
                            },
                            playerViewModel = playerVM,
                            existingSong = null,
                            onDeleteRequested = { /* Not used for Add drawer */ }
                        )
                    }
                }
            }
            if (showEditSongDrawer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000))
                        .clickable(onClick = { showEditSongDrawer = false })
                        .zIndex(100f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        SongDrawer(
                            visible = true,
                            onDismiss = { showEditSongDrawer = false },
                            onSongAdded = { success, message ->
                                toastMessage = message
                                if (success) {
                                    showSuccessToast = true
                                    showEditSongDrawer = false
                                } else {
                                    showErrorToast = true
                                }
                            },
                            playerViewModel = playerVM,
                            existingSong = currentEditingSong,
                            onDeleteRequested = {
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSongAdded: (Boolean, String) -> Unit,
    playerViewModel: PlayerViewModel,
    existingSong: MusicEntity?,
    onDeleteRequested: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditMode = existingSong != null

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
            try {
                Log.d("AudioDebug", "Selected audio URI: $uri")

                audioUri = it
                val uriString = it.toString()

                audioFilePath = getRealPathFromURI(context, it) ?: uriString
                Log.d("AudioDebug", "Audio file path: $audioFilePath")

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, it)

                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    duration = durationStr?.toLong() ?: 0
                    Log.d("AudioDebug", "Duration: $duration ms")

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
                    Log.e("AudioDebug", "Error reading metadata: ${e.message}")
                    e.printStackTrace()
                    Toast.makeText(context, "Error reading audio file: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    retriever.release()
                }
            } catch (e: Exception) {
                Log.e("AudioDebug", "Error processing audio: ${e.message}")
                e.printStackTrace()
                Toast.makeText(context, "Error processing audio file: ${e.message}", Toast.LENGTH_SHORT).show()
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

    LaunchedEffect(visible, existingSong) {
        if (visible) {
            if (existingSong != null) {
                title = existingSong.title
                artist = existingSong.artist
                duration = existingSong.duration

                val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(minutes)
                formattedDuration = String.format("%02d:%02d", minutes, seconds)

                audioFilePath = existingSong.audioPath
                imageFilePath = existingSong.albumPath

                if (existingSong.audioPath.isNotEmpty()) {
                    try {
                        audioUri = Uri.parse(existingSong.audioPath)
                    } catch (e: Exception) {
                        Log.e("AudioDebug", "Error parsing audio URI: ${e.message}")
                    }
                }

                if (existingSong.albumPath.isNotEmpty()) {
                    try {
                        val file = File(existingSong.albumPath)
                        if (file.exists()) {
                            imageUri = file.toUri()
                        }
                    } catch (e: Exception) {
                        Log.e("AudioDebug", "Error parsing image URI: ${e.message}")
                    }
                }
            } else {
                title = ""
                artist = ""
                duration = 0
                formattedDuration = "00:00"
                audioUri = null
                imageUri = null
                audioFilePath = ""
                imageFilePath = ""
            }

            when {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED -> {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
    )
    {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditMode) "Edit Song" else "Upload Song",
                    style = androidx.compose.material.MaterialTheme.typography.h6.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                if (isEditMode) {
                    IconButton(
                        onClick = onDeleteRequested,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Song",
                            tint = Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .dashedBorder(
                            color = Color.Gray,
                            cornerRadius = 8f,
                            dashWidth = 10f,
                            dashGap = 10f,
                            strokeWidth = 1f
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
                        style = androidx.compose.material.MaterialTheme.typography.caption
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .dashedBorder(
                            color = Color.Gray,
                            cornerRadius = 8f,
                            dashWidth = 10f,
                            dashGap = 10f,
                            strokeWidth = 1f
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
                        style = androidx.compose.material.MaterialTheme.typography.caption
                    )
                }
            }

            if (duration > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Duration: $formattedDuration",
                    color = Color.White,
                    style = androidx.compose.material.MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Title",
                color = Color.White,
                style = androidx.compose.material.MaterialTheme.typography.subtitle1,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray,
                    focusedContainerColor = Color.DarkGray,
                    unfocusedContainerColor = Color.DarkGray,
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
                style = androidx.compose.material.MaterialTheme.typography.subtitle1,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                placeholder = { Text("Artist") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray,
                    focusedContainerColor = Color.DarkGray,
                    unfocusedContainerColor = Color.DarkGray,
                    cursorColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (title.isBlank()) {
                            onSongAdded(false, "Please enter a title")
                            return@Button
                        }

                        if (audioUri == null && !isEditMode) {
                            onSongAdded(false, "Please select an audio file")
                            return@Button
                        }

                        scope.launch {
                            try {
                                val finalAudioPath = if (audioFilePath.isBlank()) audioUri.toString() else audioFilePath

                                val musicEntity = if (isEditMode) {
                                    existingSong!!.copy(
                                        title = title,
                                        artist = artist,
                                        duration = if (duration > 0) duration else existingSong.duration,
                                        albumPath = if (imageFilePath.isNotEmpty()) imageFilePath else existingSong.albumPath,
                                        audioPath = if (finalAudioPath.isNotEmpty()) finalAudioPath else existingSong.audioPath
                                    )
                                } else {
                                    MusicEntity(
                                        audioId = System.currentTimeMillis().inc(),
                                        title = title,
                                        artist = artist,
                                        duration = duration,
                                        albumPath = imageFilePath,
                                        audioPath = finalAudioPath,
                                        loved = false
                                    )
                                }

                                Log.d("AudioDebug", "Saving music with path: $finalAudioPath")

                                if (isEditMode) {
                                    playerViewModel.onEvent(PlayerEvent.updateMusic(musicEntity))
                                    onSongAdded(true, "Song updated successfully")
                                } else {
                                    playerViewModel.onEvent(PlayerEvent.addMusic(musicEntity))
                                    onSongAdded(true, "Song added successfully")
                                }
                            } catch (e: Exception) {
                                Log.e("AudioDebug", "Error saving music: ${e.message}")
                                e.printStackTrace()
                                onSongAdded(false, "Error saving music: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DB954), // Spotify green
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(if (isEditMode) "Update" else "Save")
                }
            }
        }
    }
}

private fun getRealPathFromURI(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        try {
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            return cursor?.use {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                if (it.moveToFirst()) {
                    it.getString(columnIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()

            return uri.toString()
        }
    }
    else if (uri.scheme == "file") {
        return uri.path
    }


    return uri.toString()
}

fun Modifier.dashedBorder(color: Color, cornerRadius: Float, dashWidth: Float, dashGap: Float, strokeWidth: Float) = composed {
    drawBehind {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
        drawRoundRect(
            color = color,
            style = Stroke(
                width = strokeWidth,
                pathEffect = pathEffect,
                cap = StrokeCap.Round
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
        )
    }
}