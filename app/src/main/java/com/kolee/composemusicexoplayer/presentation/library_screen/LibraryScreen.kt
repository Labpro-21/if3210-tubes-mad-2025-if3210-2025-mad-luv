package com.kolee.composemusicexoplayer.presentation.library

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kolee.composemusicexoplayer.data.auth.UserPreferences
import com.kolee.composemusicexoplayer.data.network.NetworkSensing
import com.kolee.composemusicexoplayer.presentation.component.AddSongDrawer
import com.kolee.composemusicexoplayer.presentation.component.NetworkSensingScreen
import kotlinx.coroutines.delay

class MusicAdapter(
    private var musicList: List<MusicEntity>,
    private val onItemClick: (MusicEntity) -> Unit,
    private val onEdit: (MusicEntity) -> Unit,
    private val onDelete: (MusicEntity) -> Unit
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    private var currentlyPlayingId: Long? = null

    inner class MusicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.itemCard)
        val imageCover: ImageView = view.findViewById(R.id.imageCover)
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textArtist: TextView = view.findViewById(R.id.textArtist)
        val editButton: ImageView = view.findViewById(R.id.imageEdit)
        val deleteButton: ImageView = view.findViewById(R.id.imageDelete)
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
        val albumUri = music.albumPath.toUri()
        Log.d("MusicAdapter", "Album URI: $albumUri")
        // Load image with Glide
        Glide.with(holder.itemView.context)
            .load(music.albumPath.toUri())
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .placeholder(R.drawable.ic_music_unknown)
            .error(R.drawable.ic_music_unknown)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(holder.imageCover)

        val showEditDelete = music.isDownloaded
        holder.editButton.visibility = if (showEditDelete) View.GONE else View.VISIBLE
        holder.deleteButton.visibility = if (showEditDelete) View.GONE else View.VISIBLE


        val isPlaying = music.audioId == currentlyPlayingId

        val primaryColor = holder.itemView.context.getColor(R.color.spotify_green)
        val defaultTitleColor = holder.itemView.context.getColor(android.R.color.white)
        val defaultArtistColor = holder.itemView.context.getColor(android.R.color.darker_gray)

        holder.textTitle.setTextColor(if (isPlaying) primaryColor else defaultTitleColor)
        holder.textArtist.setTextColor(if (isPlaying) primaryColor else defaultArtistColor)

        holder.card.setOnClickListener {
            onItemClick(music)
        }

        holder.editButton.setOnClickListener {
            onEdit(music)
        }
        holder.deleteButton.setOnClickListener {
            onDelete(music)
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
            style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Medium)
        )
    }
}


fun onEditSong(context: Context, playerVM: PlayerViewModel, music: MusicEntity) {
    Toast.makeText(context, "Edit song: ${music.title}", Toast.LENGTH_SHORT).show()
    playerVM.onEvent(PlayerEvent.EditMusic(music.copy(title = "New Title", artist = "New Artist")))
}

fun onDeleteSong(context: Context, playerVM: PlayerViewModel, music: MusicEntity) {
    val alertDialog = android.app.AlertDialog.Builder(context)
        .setTitle("Delete Song")
        .setMessage("Are you sure you want to delete ${music.title}?")
        .setPositiveButton("Yes") { _, _ ->
            playerVM.onEvent(PlayerEvent.DeleteMusic(music))
            Toast.makeText(context, "${music.title} deleted", Toast.LENGTH_SHORT).show()
        }
        .setNegativeButton("No", null)
        .create()

    alertDialog.show()
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
        val userPreferences = UserPreferences(context)
        val currentUserEmail by userPreferences.getUserEmail.collectAsState(initial = "")

        val userSongs = musicUiState.musicList.filter {
            it.owner.equals(currentUserEmail, ignoreCase = true)
        }

        val downloadSongs = musicUiState.musicList.filter {
            it.isDownloaded.equals(true)
        }


        var showAddSongDrawer by remember { mutableStateOf(false) }
        var showEditSongForm by remember { mutableStateOf(false) }
        var currentMusicToEdit by remember { mutableStateOf<MusicEntity?>(null) }

        var showSuccessToast by remember { mutableStateOf(false) }
        var showErrorToast by remember { mutableStateOf(false) }
        var toastMessage by remember { mutableStateOf("") }

        val songsToDisplay = when (selectedTab) {
            "Liked" -> likedSongs
            "Downloaded" -> downloadSongs
            else -> userSongs
        }

        val recyclerViewRef = remember { mutableStateOf<RecyclerView?>(null) }

        val adapter = remember {
            MusicAdapter(
                musicList = songsToDisplay,
                onItemClick = { music -> playerVM.onEvent(PlayerEvent.Play(music)) },
                onEdit = { music ->
                    currentMusicToEdit = music
                    showEditSongForm = true
                },
                onDelete = { music -> onDeleteSong(context, playerVM, music) }
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
                            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
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
                        ToggleTab("Downloaded", selectedTab == "Downloaded") { selectedTab = "Downloaded" }
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

            if (showEditSongForm && currentMusicToEdit != null) {
                EditSongForm(
                    currentMusic = currentMusicToEdit!!,
                    onDismiss = { showEditSongForm = false },
                    onSave = { updatedMusic ->
                        playerVM.onEvent(PlayerEvent.EditMusic(updatedMusic))
                        showEditSongForm = false
                    }
                )
            }

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
                    backgroundColor = Color(0xFF1ED760),
                    shape = RoundedCornerShape(8.dp),
                    elevation = 4.dp
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
                            style = MaterialTheme.typography.body1
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
                    backgroundColor = Color.Red,
                    shape = RoundedCornerShape(8.dp),
                    elevation = 4.dp
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
                            style = MaterialTheme.typography.body1
                        )
                    }
                }
            }

            AddSongDrawer(
                visible = showAddSongDrawer,
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
                playerViewModel = playerVM
            )
        }
    }
}

@Composable
fun EditSongForm(
    currentMusic: MusicEntity,
    onDismiss: () -> Unit,
    onSave: (MusicEntity) -> Unit
) {
    var newTitle by remember { mutableStateOf(currentMusic.title) }
    var newArtist by remember { mutableStateOf(currentMusic.artist) }
    var isTitleValid by remember { mutableStateOf(true) }
    var isArtistValid by remember { mutableStateOf(true) }

    fun validate(): Boolean {
        isTitleValid = newTitle.isNotEmpty()
        isArtistValid = newArtist.isNotEmpty()
        return isTitleValid && isArtistValid
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF121212),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Edit Song",
                    style = MaterialTheme.typography.h6.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Song Title Input
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = {
                        Text(
                            "Song Title",
                            color = Color(0xFFB3B3B3)
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    isError = !isTitleValid,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        cursorColor = Color(0xFF1ED760),
                        focusedBorderColor = Color(0xFF1ED760),
                        unfocusedBorderColor = Color(0xFF535353),
                        focusedLabelColor = Color(0xFF1ED760),
                        unfocusedLabelColor = Color(0xFFB3B3B3)
                    ),
                    singleLine = true
                )
                if (!isTitleValid) {
                    Text(
                        text = "Title cannot be empty",
                        color = Color(0xFFE22134),
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Song Artist Input
                OutlinedTextField(
                    value = newArtist,
                    onValueChange = { newArtist = it },
                    label = {
                        Text(
                            "Artist",
                            color = Color(0xFFB3B3B3)
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    isError = !isArtistValid,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        cursorColor = Color(0xFF1ED760),
                        focusedBorderColor = Color(0xFF1ED760),
                        unfocusedBorderColor = Color(0xFF535353),
                        focusedLabelColor = Color(0xFF1ED760),
                        unfocusedLabelColor = Color(0xFFB3B3B3)
                    ),
                    singleLine = true
                )
                if (!isArtistValid) {
                    Text(
                        text = "Artist cannot be empty",
                        color = Color(0xFFE22134),
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFB3B3B3)
                        )
                    ) {
                        Text("CANCEL")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            if (validate()) {
                                onSave(currentMusic.copy(title = newTitle, artist = newArtist))
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1ED760),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Text(
                            "SAVE",
                            style = MaterialTheme.typography.button.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }
    }
}
