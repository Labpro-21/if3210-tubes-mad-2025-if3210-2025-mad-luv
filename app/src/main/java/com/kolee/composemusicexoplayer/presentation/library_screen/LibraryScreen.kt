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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kolee.composemusicexoplayer.data.network.NetworkSensing
import com.kolee.composemusicexoplayer.presentation.component.AddSongDrawer
import com.kolee.composemusicexoplayer.presentation.component.NetworkSensingScreen
import kotlinx.coroutines.delay

class MusicAdapter(
    private var musicList: List<MusicEntity>,
    private val onItemClick: (MusicEntity) -> Unit
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    private var currentlyPlayingId: Long? = null

    inner class MusicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.itemCard)
        val imageCover: ImageView = view.findViewById(R.id.imageCover)
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textArtist: TextView = view.findViewById(R.id.textArtist)
        val imageWave: ImageView = view.findViewById(R.id.imageWave)
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

        var showSuccessToast by remember { mutableStateOf(false) }
        var showErrorToast by remember { mutableStateOf(false) }
        var toastMessage by remember { mutableStateOf("") }

        val songsToDisplay = when (selectedTab) {
            "Liked" -> likedSongs
            else -> allSongs
        }

        val recyclerViewRef = remember { mutableStateOf<RecyclerView?>(null) }

        val adapter = remember {
            MusicAdapter(songsToDisplay) { music ->
                playerVM.onEvent(PlayerEvent.Play(music))
            }
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
