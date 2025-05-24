package com.kolee.composemusicexoplayer.presentation.music_screen

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.data.auth.UserPreferences
import com.kolee.composemusicexoplayer.data.network.NetworkSensing
import com.kolee.composemusicexoplayer.data.profile.ProfileViewModel
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.presentation.MusicPlayerSheet.MusicPlayerSheet
import com.kolee.composemusicexoplayer.presentation.component.BottomMusicPlayerHeight
import com.kolee.composemusicexoplayer.presentation.component.BottomMusicPlayerImpl
import com.kolee.composemusicexoplayer.presentation.component.MusicItem
import com.kolee.composemusicexoplayer.presentation.component.NetworkSensingScreen
import com.kolee.composemusicexoplayer.presentation.online_song.DownloadManager
import com.kolee.composemusicexoplayer.presentation.online_song.OnlineSongsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MusicScreen"

@Composable
fun MusicScreen(
    playerVM: PlayerViewModel = hiltViewModel(),
    onlineSongsVM: OnlineSongsViewModel = hiltViewModel(),
    navController: NavHostController,
    profileViewModel: ProfileViewModel,
    networkSensing: NetworkSensing
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val musicUiState by playerVM.uiState.collectAsState()
    var open = musicUiState.isPlayerExpanded
    val isMusicPlaying = musicUiState.currentPlayedMusic != MusicEntity.default
    val userPreferences = UserPreferences(context)

    var selectedCategory by remember { mutableStateOf("Global") }
    var showFullList by remember { mutableStateOf(false) }
    var currentFullList by remember { mutableStateOf(emptyList<MusicEntity>()) }

    LaunchedEffect(key1 = musicUiState.currentPlayedMusic) {
        val isShowed = (musicUiState.currentPlayedMusic != MusicEntity.default)
        playerVM.onEvent(PlayerEvent.SetShowBottomPlayer(isShowed))
    }

    val currentUserCountry by userPreferences.getUserCountry.collectAsState(initial = "GLOBAL")
    val currentUserEmail by userPreferences.getUserEmail.collectAsState(initial = "")

    val globalSongs = musicUiState.musicList.filter { it.owner == "GLOBAL" }
    val countrySongs = musicUiState.musicList.filter {
        it.country.equals(currentUserCountry, ignoreCase = true)
    }
    val userSongs = musicUiState.musicList.filter {
        it.owner.equals(currentUserEmail, ignoreCase = true)
    }

    NetworkSensingScreen(
        networkSensing = networkSensing,
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            if (isLandscape) {
                LandscapeLayout(
                    userSongs = userSongs,
                    playerVM = playerVM,
                    musicUiState = musicUiState
                )
            } else {
                currentUserCountry?.let {
                    PortraitLayout(
                        showFullList = showFullList,
                        selectedCategory = selectedCategory,
                        currentFullList = currentFullList,
                        userSongs = userSongs,
                        globalSongs = globalSongs,
                        countrySongs = countrySongs,
                        currentUserCountry = it,
                        playerVM = playerVM,
                        musicUiState = musicUiState,
                        onCategorySelected = { category, list ->
                            selectedCategory = category
                            currentFullList = list
                            showFullList = true
                        },
                        onBackPressed = { showFullList = false }
                    )
                }
            }

            // Player UI
            if (isMusicPlaying) {
                if (open) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PortraitLayout(
    showFullList: Boolean,
    selectedCategory: String,
    currentFullList: List<MusicEntity>,
    userSongs: List<MusicEntity>,
    globalSongs: List<MusicEntity>,
    countrySongs: List<MusicEntity>,
    currentUserCountry: String,
    playerVM: PlayerViewModel,
    musicUiState: MusicUiState,
    onCategorySelected: (String, List<MusicEntity>) -> Unit,
    onBackPressed: () -> Unit,
    isMusicPlaying: Boolean = false,
    open: Boolean = false
) {
    val context = LocalContext.current
    val userPreferences = UserPreferences(context)

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    var isDownloadingGlobal by remember { mutableStateOf(false) }
    var isDownloadingCountry by remember { mutableStateOf(false) }
    val recommendationState = playerVM.recommendationState.value

    val downloadManager = remember {
        DownloadManager(
            context = context,
            musicRepository = playerVM.musicRepository,
            userPreferences = userPreferences
        )
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(-10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (isMusicPlaying && !open) BottomMusicPlayerHeight.value else 0.dp)
            ) {
                if (!showFullList) {
                    item {
                        Column {
                            Text(
                                text = "Charts",
                                style = MaterialTheme.typography.h6.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.onBackground
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GradientCategoryCard(
                                    title = "Top Songs Global",
                                    gradientColors = listOf(Color(0xFF6A11CB), Color(0xFF2575FC)),
                                    isSelected = selectedCategory == "Global",
                                    onClick = { onCategorySelected("Global", globalSongs) }
                                )

                                GradientCategoryCard(
                                    title = "Top Songs $currentUserCountry",
                                    gradientColors = listOf(
                                        Color(0xFF11998E),
                                        Color(0xFF38EF7D)
                                    ),
                                    isSelected = selectedCategory == "Country",
                                    onClick = { onCategorySelected("Country", countrySongs) },
                                   )
                            }

                            Column(modifier = Modifier.padding(horizontal = 16.dp,vertical = 8.dp)) {
                                Text(
                                    text = "Recommendations",
                                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Curated just for you",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    GradientCategoryCard(
                                        title = "Daily Mix",
                                        gradientColors = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
                                        isSelected = selectedCategory == "Daily Mix",
                                        onClick = { onCategorySelected("Daily Mix", recommendationState.dailyPlaylist) },
                                    )
                                }

                                items(recommendationState.topMixes.keys.take(10).toList()) { genre ->
                                    GradientCategoryCard(
                                        title = "Top $genre Mix",
                                        gradientColors = listOf(
                                            Color(0xFF4776E6),
                                            Color(0xFF8E54E9)
                                        ),
                                        isSelected = selectedCategory == "Top $genre Mix",
                                        onClick = { onCategorySelected("Top $genre Mix", recommendationState.topMixes[genre] ?: emptyList()) }
                                    )
                                }
                            }

                            HorizontalMusicList(
                                title = "New Songs",
                                musicList = userSongs.reversed(),
                                musicUiState = musicUiState,
                                onSelectedMusic = { playerVM.onEvent(PlayerEvent.Play(it)) }
                            )

                        }
                    }

                    item {
                        VerticalMusicList(
                            title = "Recently Played",
                            musicList = playerVM.getRecentlyPlayed(),
                            musicUiState = musicUiState,
                            onSelectedMusic = { playerVM.onEvent(PlayerEvent.Play(it)) }
                        )
                    }
                } else {
                    stickyHeader {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colors.background,
                            elevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onBackPressed,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }

                                Text(
                                    text = "$selectedCategory Songs",
                                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                                if (selectedCategory == "Global" || selectedCategory == "Country") {
                                IconButton(
                                    onClick = {
                                        when (selectedCategory) {
                                            "Global" -> {
                                                if (globalSongs.isNotEmpty() && !isDownloadingGlobal) {
                                                    isDownloadingGlobal = true
                                                    scope.launch {
                                                        try {
                                                            scaffoldState.snackbarHostState.showSnackbar(
                                                                message = "Starting download...",
                                                                duration = SnackbarDuration.Short
                                                            )
                                                            val success = withContext(Dispatchers.IO) {
                                                                downloadManager.downloadMusic(globalSongs)
                                                            }
                                                            val message = if (success) {
                                                                "Global playlist downloaded successfully"
                                                            } else {
                                                                "Download failed"
                                                            }
                                                            scaffoldState.snackbarHostState.showSnackbar(
                                                                message = message,
                                                                duration = SnackbarDuration.Long
                                                            )
                                                        } catch (e: Exception) {
                                                            scaffoldState.snackbarHostState.showSnackbar(
                                                                message = "Download failed: ${e.localizedMessage}",
                                                                duration = SnackbarDuration.Long
                                                            )
                                                        } finally {
                                                            isDownloadingGlobal = false
                                                        }
                                                    }
                                                }
                                            }
                                            "Country" -> {
                                                if (countrySongs.isNotEmpty() && !isDownloadingCountry) {
                                                    isDownloadingCountry = true
                                                    scope.launch {
                                                        try {
                                                            scaffoldState.snackbarHostState.showSnackbar(
                                                                message = "Starting download...",
                                                                duration = SnackbarDuration.Short
                                                            )
                                                            val success = withContext(Dispatchers.IO) {
                                                                downloadManager.downloadMusic(countrySongs)
                                                            }
                                                            val message = if (success) {
                                                                "$currentUserCountry playlist downloaded successfully"
                                                            } else {
                                                                "Download failed"
                                                            }
                                                            scaffoldState.snackbarHostState.showSnackbar(
                                                                message = message,
                                                                duration = SnackbarDuration.Long
                                                            )
                                                        } catch (e: Exception) {
                                                            scaffoldState.snackbarHostState.showSnackbar(
                                                                message = "Download failed: ${e.localizedMessage}",
                                                                duration = SnackbarDuration.Long
                                                            )
                                                        } finally {
                                                            isDownloadingCountry = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isDownloadingGlobal && !isDownloadingCountry
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (selectedCategory == "Global" && globalSongs.all { it.isDownloaded } ||
                                                selectedCategory == "Country" && countrySongs.all { it.isDownloaded }) {
                                                R.drawable.ic_downloaded
                                            } else {
                                                R.drawable.ic_download
                                            }
                                        ),
                                        contentDescription = "Download",
                                        tint = MaterialTheme.colors.primary
                                    )
                                }
                            }
                            }
                        }
                    }

                    itemsIndexed(currentFullList) { index, music ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    ,
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.subtitle2.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = when (index) {
                                            0, 1, 2 -> Color.White
                                            else -> MaterialTheme.colors.onSurface
                                        }
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(1.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                MusicItem(
                                    music = music,
                                    selected = music.audioId == musicUiState.currentPlayedMusic.audioId,
                                    isMusicPlaying = musicUiState.isPlaying,
                                    isHorizontal = false,
                                    onClick = { playerVM.onEvent(PlayerEvent.Play(music)) },
                                )
                            }
                        }
                        Divider(color = Color.Gray.copy(alpha = 0.1f))
                    }
                }
            }

            // Loading indicator saat download
            if (isDownloadingGlobal || isDownloadingCountry) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Downloading music...",
                                style = MaterialTheme.typography.body1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalMusicList(
    title: String,
    musicList: List<MusicEntity>,
    musicUiState: MusicUiState,
    onSelectedMusic: (MusicEntity) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(musicList) { music ->
                MusicItem(
                    music = music,
                    selected = music.audioId == musicUiState.currentPlayedMusic.audioId,
                    isMusicPlaying = musicUiState.isPlaying,
                    isHorizontal = true,
                    onClick = { onSelectedMusic(music) }
                )
            }
        }
    }
}

@Composable
private fun VerticalMusicList(
    title: String,
    musicList: List<MusicEntity>,
    musicUiState: MusicUiState,
    onSelectedMusic: (MusicEntity) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            musicList.forEach { music ->
                MusicItem(
                    music = music,
                    selected = music.audioId == musicUiState.currentPlayedMusic.audioId,
                    isMusicPlaying = musicUiState.isPlaying,
                    isHorizontal = false,
                    onClick = { onSelectedMusic(music) }
                )
                Divider(color = Color.Gray.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
private fun LandscapeLayout(
    userSongs: List<MusicEntity>,
    playerVM: PlayerViewModel,
    musicUiState: MusicUiState
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            MusicSection(
                title = "New Songs",
                musicList = userSongs.reversed(),
                isHorizontal = false,
                musicUiState = musicUiState,
                onSelectedMusic = { playerVM.onEvent(PlayerEvent.Play(it)) },
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            MusicSection(
                title = "Recently Played",
                musicList = playerVM.getRecentlyPlayed(),
                isHorizontal = false,
                musicUiState = musicUiState,
                onSelectedMusic = { playerVM.onEvent(PlayerEvent.Play(it)) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GradientCategoryCard(
    title: String,
    gradientColors: List<Color>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val brush = Brush.linearGradient(
        colors = gradientColors,
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    Card(
        modifier = Modifier
            .width(120.dp)
            .height(120.dp),
        elevation = if (isSelected) 12.dp else 8.dp,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle2.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MusicSection(
    title: String,
    musicList: List<MusicEntity>,
    isHorizontal: Boolean,
    musicUiState: MusicUiState,
    onSelectedMusic: (MusicEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (isHorizontal) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(musicList) { music ->
                    MusicItem(
                        music = music,
                        selected = music.audioId == musicUiState.currentPlayedMusic.audioId,
                        isMusicPlaying = musicUiState.isPlaying,
                        isHorizontal = true,
                        onClick = { onSelectedMusic(music) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = if (musicUiState.currentPlayedMusic != MusicEntity.default) BottomMusicPlayerHeight.value else 0.dp),
                verticalArrangement = Arrangement.spacedBy(-10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(musicList) { music ->
                    MusicItem(
                        music = music,
                        selected = music.audioId == musicUiState.currentPlayedMusic.audioId,
                        isMusicPlaying = musicUiState.isPlaying,
                        isHorizontal = false,
                        onClick = { onSelectedMusic(music) }
                    )
                    Divider(color = Color.Gray.copy(alpha = 0.1f))
                }
            }
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