package com.kolee.composemusicexoplayer.presentation.music_screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.presentation.MusicPlayerSheet.MusicPlayerSheet
import com.kolee.composemusicexoplayer.presentation.component.BottomMusicPlayerHeight
import com.kolee.composemusicexoplayer.presentation.component.BottomMusicPlayerImpl
import com.kolee.composemusicexoplayer.presentation.component.MusicItem

private const val TAG = "MusicScreen"

@Composable
fun MusicScreen(playerVM: PlayerViewModel = hiltViewModel(), navController: NavHostController) {
    val context = LocalContext.current
    val musicUiState by playerVM.uiState.collectAsState()
    var open by remember { mutableStateOf(true) }
    val isMusicPlaying = musicUiState.currentPlayedMusic != MusicEntity.default

    LaunchedEffect(key1 = musicUiState.currentPlayedMusic) {
        val isShowed = (musicUiState.currentPlayedMusic != MusicEntity.default)
        playerVM.onEvent(PlayerEvent.SetShowBottomPlayer(isShowed))
    }

    Box(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxSize()
    ) {
        Column {
            MusicListContent(musicUiState = musicUiState) { music ->
                playerVM.onEvent(PlayerEvent.Play(music))
            }
        }

        if (isMusicPlaying) {
            if (open) {
                MusicPlayerSheet(
                    playerVM = playerVM,
                    navController = navController,
                    onCollapse = { open = false } // <-- collapse ke mini
                )
            } else {
                BottomMusicPlayerImpl(
                    musicUiState = musicUiState,
                    onPlayPauseClicked = { playerVM.onEvent(PlayerEvent.PlayPause(isMusicPlaying)) },
                    onExpand = { open = true } // <-- expand ke full sheet
                )
            }
        }
    }

    ComposableLifeCycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                Log.d(TAG, "MusicScreen: ON_RESUME")
                playerVM.onEvent(PlayerEvent.RefreshMusicList)
            }
            Lifecycle.Event.ON_PAUSE -> {
                Log.d(TAG, "MusicScreen: ON_PAUSE")
            }
            else -> {}
        }
    }
}


@Composable
fun ComposableLifeCycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onEvent: (source: LifecycleOwner, event: Lifecycle.Event) -> Unit
) {
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { source, event ->
            onEvent(source, event)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun MusicListContent(
    musicUiState: MusicUiState,
    onSelectedMusic: (music: MusicEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        val currentAudioId = musicUiState.currentPlayedMusic.audioId

        itemsIndexed(musicUiState.musicList) { _, music ->
            MusicItem(
                music = music,
                selected = (music.audioId == currentAudioId),
                isMusicPlaying = musicUiState.isPlaying,
                onClick = { onSelectedMusic(music) }
            )
        }

        item {
            Spacer(modifier =Modifier.height(BottomMusicPlayerHeight.value))
        }
    }
}
//
//
//
//@Composable
//fun MusicScreen(viewModel: AuthViewModel) {
//    val userName by viewModel.userName.collectAsState(initial = "")
//    var isPlaying by remember { mutableStateOf(true) }
//
//    Surface(
//        modifier = Modifier.fillMaxSize(),
//        color = Color.Black
//    ) {
//        Column(
//            modifier = Modifier.fillMaxSize()
//        ) {
//            Column(
//                modifier = Modifier
//                    .weight(1f)
//                    .verticalScroll(rememberScrollState())
//                    .padding(Dimens.Two)
//            ) {
//                Text(
//                    text = "Halo, ${userName.ifBlank { "user" }}!",
//                    style = MaterialTheme.typography.subtitle1,
//                    color = MaterialTheme.colors.TextDefaultColor,
//                    modifier = Modifier.padding(bottom = Dimens.Two)
//                )
//                Text(
//                    text = "New songs",
//                    style = MaterialTheme.typography.h6,
//                    color = MaterialTheme.colors.TextDefaultColor,
//                    modifier = Modifier.padding(bottom = Dimens.Two)
//                )
//
//                NewSongsSection()
//
//                Spacer(modifier = Modifier.height(Dimens.Three))
//
//                Text(
//                    text = "Recently played",
//                    style = MaterialTheme.typography.h6,
//                    color = MaterialTheme.colors.TextDefaultColor,
//                    modifier = Modifier.padding(bottom = Dimens.Two)
//                )
//
//                RecentlyPlayedSection()
//
//                Spacer(modifier = Modifier.height(Dimens.Two))
//
//                Button(
//                    onClick = { viewModel.logout() },
//                    modifier = Modifier.align(Alignment.CenterHorizontally),
//                    colors = ButtonDefaults.buttonColors(backgroundColor = Green700),
//                    shape = MaterialTheme.shapes.medium
//                ) {
//                    Text(
//                        text = "Logout",
//                        color = MaterialTheme.colors.TextDefaultColor,
//                        style = MaterialTheme.typography.button
//                    )
//                }
//            }
//
//            NowPlayingBar(
//                isPlaying = isPlaying,
//                onPlayPauseClick = { isPlaying = !isPlaying }
//            )
//        }
//    }
//}
//
//@Composable
//fun NewSongsSection() {
//    val newSongs = listOf(
//        Song("Starboy", "The Weeknd"),
//        Song("Here Comes The Sun", "The Beatles"),
//        Song("Midnight Pretenders", "Tomoko Aran"),
//        Song("Violent Crimes", "Kanye West")
//    )
//
//    LazyRow(
//        horizontalArrangement = Arrangement.spacedBy(Dimens.One)
//    ) {
//        items(newSongs) { song ->
//            Column(
//                modifier = Modifier.width(Dimens.Sixteen)
//            ) {
//                Box(
//                    modifier = Modifier
//                        .size(Dimens.Sixteen)
//                        .clip(MaterialTheme.shapes.medium)
//                        .background(getRandomColor(song.title))
//                ) {
//                    Text(
//                        text = song.title.take(1),
//                        color = Color.White,
//                        style = MaterialTheme.typography.h4,
//                        modifier = Modifier.align(Alignment.Center)
//                    )
//                }
//
//                Spacer(modifier = Modifier.height(Dimens.One))
//
//                Text(
//                    text = song.title,
//                    color = MaterialTheme.colors.TextDefaultColor,
//                    style = MaterialTheme.typography.subtitle2,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//
//                Text(
//                    text = song.artist,
//                    color = MaterialTheme.colors.TextDefaultColor.copy(alpha = 0.7f),
//                    style = MaterialTheme.typography.caption,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun RecentlyPlayedSection() {
//    val recentSongs = listOf(
//        Song("Jazz is for ordinary people", "Berlioz"),
//        Song("Loose", "Daniel Caesar"),
//        Song("Nights", "Frank Ocean"),
//        Song("Kiss of Life", "Sade"),
//        Song("BEST INTEREST", "Tyler, The Creator")
//    )
//
//    Column(
//        verticalArrangement = Arrangement.spacedBy(Dimens.Two)
//    ) {
//        recentSongs.forEach { song ->
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                // Colored box instead of image
//                Box(
//                    modifier = Modifier
//                        .size(Dimens.Six)
//                        .clip(MaterialTheme.shapes.small)
//                        .background(getRandomColor(song.title))
//                ) {
//                    // Album initial in the center of the box
//                    Text(
//                        text = song.title.take(1),
//                        color = Color.White,
//                        style = MaterialTheme.typography.subtitle1,
//                        modifier = Modifier.align(Alignment.Center)
//                    )
//                }
//
//                Spacer(modifier = Modifier.width(Dimens.One))
//
//                Column(
//                    modifier = Modifier.weight(1f)
//                ) {
//                    Text(
//                        text = song.title,
//                        color = MaterialTheme.colors.TextDefaultColor,
//                        style = MaterialTheme.typography.subtitle2,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
//
//                    Text(
//                        text = song.artist,
//                        color = MaterialTheme.colors.TextDefaultColor.copy(alpha = 0.7f),
//                        style = MaterialTheme.typography.caption,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun NowPlayingBar(
//    isPlaying: Boolean,
//    onPlayPauseClick: () -> Unit
//) {
//    Row(
//        verticalAlignment = Alignment.CenterVertically,
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(Color(0xFF1E1E1E))
//            .padding(Dimens.One)
//    ) {
//        Box(
//            modifier = Modifier
//                .size(Dimens.Five)
//                .clip(MaterialTheme.shapes.small)
//                .background(Green700)
//        ) {
//            Text(
//                text = "S",
//                color = Color.White,
//                style = MaterialTheme.typography.subtitle1,
//                modifier = Modifier.align(Alignment.Center)
//            )
//        }
//
//        Spacer(modifier = Modifier.width(Dimens.One))
//
//        Column(
//            modifier = Modifier.weight(1f)
//        ) {
//            Text(
//                text = "Starboy",
//                color = MaterialTheme.colors.TextDefaultColor,
//                style = MaterialTheme.typography.subtitle2,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
//
//            Text(
//                text = "The Weeknd",
//                color = MaterialTheme.colors.TextDefaultColor.copy(alpha = 0.7f),
//                style = MaterialTheme.typography.caption,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
//        }
//
//        IconButton(onClick = {}) {
//            Icon(
//                imageVector = Icons.Default.Add,
//                contentDescription = "Add to playlist",
//                tint = MaterialTheme.colors.TintDefaultColor
//            )
//        }
//
//        IconButton(onClick = onPlayPauseClick) {
//            Icon(
//                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
//                contentDescription = if (isPlaying) "Pause" else "Play",
//                tint = Green700
//            )
//        }
//    }
//}