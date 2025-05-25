package com.kolee.composemusicexoplayer.presentation.profil_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.data.auth.AuthViewModel
import com.kolee.composemusicexoplayer.data.model.ArtistStats
import com.kolee.composemusicexoplayer.data.model.DailyListeningTime
import com.kolee.composemusicexoplayer.data.model.MonthlyAnalytics
import com.kolee.composemusicexoplayer.data.model.SongStats
import com.kolee.composemusicexoplayer.data.model.StreakData
import com.kolee.composemusicexoplayer.data.network.NetworkSensing
import com.kolee.composemusicexoplayer.data.profile.ProfileViewModel
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerEvent
import com.kolee.composemusicexoplayer.presentation.component.NetworkSensingScreen
import com.kolee.composemusicexoplayer.presentation.component.BottomMusicPlayerHeight
import com.kolee.composemusicexoplayer.presentation.component.BottomMusicPlayerImpl
import com.kolee.composemusicexoplayer.presentation.MusicPlayerSheet.MusicPlayerSheet
import com.kolee.composemusicexoplayer.presentation.profile_screen.EditProfileScreen
import com.kolee.composemusicexoplayer.utils.AnalyticsExporter
import com.kolee.composemusicexoplayer.utils.ExportFormat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    playerViewModel: PlayerViewModel,
    authViewModel: AuthViewModel,
    networkSensing: NetworkSensing,
    navController: NavHostController, // Add navController parameter
    onTopArtistsClick: (List<ArtistStats>) -> Unit = {},
    onTopSongsClick: (List<SongStats>) -> Unit = {},
    onTimeDetailClick: (List<DailyListeningTime>) -> Unit = {},
    onEditProfileClick: () -> Unit = {}
) {
    val isConnected by networkSensing.isConnected.collectAsState(initial = true)
    val profileState by viewModel.profile.collectAsState()
    val uiState by playerViewModel.uiState.collectAsState()
    val monthlyAnalytics by playerViewModel.monthlyAnalytics.collectAsState()

    // Player state management
    var open = uiState.isPlayerExpanded
    val isMusicPlaying = uiState.currentPlayedMusic != MusicEntity.default

    // Current date
    val currentDate = Calendar.getInstance()
    val currentMonth = currentDate.get(Calendar.MONTH)
    val currentYear = currentDate.get(Calendar.YEAR)

    // Selected month and year state
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedYear by remember { mutableStateOf(currentYear) }

    // Format selected date
    val selectedDate = Calendar.getInstance().apply {
        set(Calendar.MONTH, selectedMonth)
        set(Calendar.YEAR, selectedYear)
    }
    val selectedMonthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedDate.time)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val analyticsExporter = remember { AnalyticsExporter(context) }

    var showExportDialog by remember { mutableStateOf(false) }

    // Update show bottom player when music changes
    LaunchedEffect(key1 = uiState.currentPlayedMusic) {
        val isShowed = (uiState.currentPlayedMusic != MusicEntity.default)
        playerViewModel.onEvent(PlayerEvent.SetShowBottomPlayer(isShowed))
    }

    LaunchedEffect(Unit) {
        viewModel.resetProfile()
        viewModel.fetchProfile()
        // Load current month data initially
        playerViewModel.getMonthAnalyticsForMonth(selectedMonth, selectedYear)
    }

    LaunchedEffect(selectedMonth, selectedYear) {
        playerViewModel.getMonthAnalyticsForMonth(selectedMonth, selectedYear)
    }

    val currentUserEmail = profileState?.email ?: ""

    val allSongs by remember(uiState.musicList, currentUserEmail) {
        derivedStateOf {
            uiState.musicList
                .filter { music -> music.owner.any { it.equals(currentUserEmail, ignoreCase = true) } }
                .size
        }
    }

    val likedSongs by remember(playerViewModel.getLoved(), currentUserEmail) {
        derivedStateOf {
            playerViewModel.getLoved()
                .filter { music -> music.owner.any { it.equals(currentUserEmail, ignoreCase = true) } }
                .size
        }
    }

    val listenedSongs by remember(playerViewModel.getListenedSongs(), currentUserEmail) {
        derivedStateOf {
            playerViewModel.getListenedSongs()
                .filter { music -> music.owner.any { it.equals(currentUserEmail, ignoreCase = true) } }
                .size
        }
    }

    NetworkSensingScreen(
        networkSensing = networkSensing,
        showFallbackPage = !isConnected && profileState == null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF015871), Color.Black)
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = 48.dp,
                        bottom = if (isMusicPlaying && !open) BottomMusicPlayerHeight.value + 16.dp else 100.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                profileState?.let { profile ->
                    val imageUrl by remember(profile.profilePhoto) {
                        mutableStateOf("http://34.101.226.132:3000/uploads/profile-picture/${profile.profilePhoto}")
                    }

                    Box(contentAlignment = Alignment.BottomEnd) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current)
                                    .data(imageUrl)
                                    .placeholder(R.drawable.profile)
                                    .error(R.drawable.profile)
                                    .build()
                            ),
                            contentDescription = "Profile Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                        )

                        IconButton(
                            onClick = { /* TODO: Edit profile picture */ },
                            modifier = Modifier
                                .offset(x = (-8).dp, y = (-8).dp)
                                .background(Color.White, CircleShape)
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile Picture",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    profile.username?.let { Text(text = it, fontSize = 20.sp, color = Color.White) }
                    profile.location?.let { Text(text = it, color = Color.LightGray) }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onEditProfileClick,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)
                    ) {
                        Text(text = "Edit Profile", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStat(count = allSongs.toString(), label = "SONGS")
                        ProfileStat(count = likedSongs.toString(), label = "LIKED")
                        ProfileStat(count = listenedSongs.toString(), label = "LISTENED")
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Sound Capsule Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Sound Capsule",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    monthlyAnalytics?.let {
                                        showExportDialog = true
                                    }
                                },
                                modifier = Modifier.size(24.dp),
                                enabled = monthlyAnalytics != null && monthlyAnalytics!!.totalMinutes > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Export Analytics",
                                    tint = if (monthlyAnalytics != null && monthlyAnalytics!!.totalMinutes > 0)
                                        Color.White else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Month/Year Selector
                    MonthYearSelector(
                        selectedMonth = selectedMonth,
                        selectedYear = selectedYear,
                        onMonthSelected = { newMonth ->
                            selectedMonth = newMonth
                        },
                        onYearSelected = { newYear ->
                            selectedYear = newYear
                        },
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sound Capsule Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    ) {
                        if (monthlyAnalytics != null && monthlyAnalytics!!.totalMinutes > 0) {
                            MonthHeader(currentMonth = selectedMonthYear)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Time listened card
                            TimeListenedCard(
                                totalMinutes = monthlyAnalytics!!.totalMinutes,
                                dailyStats = monthlyAnalytics!!.dailyStats,
                                onTimeDetailClick = { onTimeDetailClick(monthlyAnalytics!!.dailyStats) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Top Artist and Song Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TopArtistCard(
                                    topArtists = monthlyAnalytics!!.topArtists,
                                    onArrowClick = { onTopArtistsClick(monthlyAnalytics!!.topArtists) },
                                    modifier = Modifier.weight(1f)
                                )
                                TopSongCard(
                                    topSongs = monthlyAnalytics!!.topSongs,
                                    onArrowClick = { onTopSongsClick(monthlyAnalytics!!.topSongs) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Achievement/Streak Card
                            if (monthlyAnalytics!!.longestStreak != null) {
                                StreakAchievementCard(streakData = monthlyAnalytics!!.longestStreak!!)
                            } else {
                                NoStreakCard()
                            }
                        } else {
                            NoDataAvailableCard(currentMonth = selectedMonthYear)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { authViewModel.logout() },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(text = "Logout")
                    }
                } ?: run {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { authViewModel.logout() }) {
                            Text(text = "Logout")
                        }
                    }
                }
            }

            // Player UI - positioned at the bottom
            if (isMusicPlaying) {
                if (open) {
                    MusicPlayerSheet(
                        playerVM = playerViewModel,
                        navController = navController,
                        onCollapse = { playerViewModel.setPlayerExpanded(false) }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        BottomMusicPlayerImpl(
                            playerVM = playerViewModel,
                            musicUiState = uiState,
                            onPlayPauseClicked = {
                                playerViewModel.onEvent(PlayerEvent.PlayPause(uiState.isPlaying))
                            },
                            onExpand = { playerViewModel.setPlayerExpanded(true) }
                        )
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        ExportFormatDialog(
            onFormatSelected = { format ->
                showExportDialog = false
                monthlyAnalytics?.let { analytics ->
                    profileState?.let { profile ->
                        coroutineScope.launch {
                            profile.username?.let {
                                analyticsExporter.exportAnalytics(
                                    analytics = analytics,
                                    username = it,
                                    format = format
                                )
                            }
                        }
                    }
                }
            },
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun MonthYearSelector(
    selectedMonth: Int,
    selectedYear: Int,
    onMonthSelected: (Int) -> Unit,
    onYearSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear - 5..currentYear).toList().reversed()

    var expandedMonth by remember { mutableStateOf(false) }
    var expandedYear by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Month Dropdown
        Box(modifier = Modifier.weight(1f)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                backgroundColor = Color(0xFF2A2A2A)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = months[selectedMonth],
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = { expandedMonth = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Month",
                            tint = Color.White
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = expandedMonth,
                onDismissRequest = { expandedMonth = false },
                modifier = Modifier.background(Color(0xFF2A2A2A))
            ) {
                months.forEachIndexed { index, month ->
                    DropdownMenuItem(
                        onClick = {
                            onMonthSelected(index)
                            expandedMonth = false
                        }
                    ) {
                        Text(
                            text = month,
                            color = if (index == selectedMonth) Color(0xFF1DB954) else Color.White
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                backgroundColor = Color(0xFF2A2A2A)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedYear.toString(),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = { expandedYear = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Year",
                            tint = Color.White
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = expandedYear,
                onDismissRequest = { expandedYear = false },
                modifier = Modifier.background(Color(0xFF2A2A2A))
            ) {
                years.forEach { year ->
                    DropdownMenuItem(
                        onClick = {
                            onYearSelected(year)
                            expandedYear = false
                        }
                    ) {
                        Text(
                            text = year.toString(),
                            color = if (year == selectedYear) Color(0xFF1DB954) else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportFormatDialog(
    onFormatSelected: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Export Analytics",
                color = Color.White
            )
        },
        text = {
            Text(
                text = "Choose export format:",
                color = Color.Gray
            )
        },
        buttons = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // CSV Option
                Button(
                    onClick = { onFormatSelected(ExportFormat.CSV) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1DB954))
                ) {
                    Text("CSV Format", color = Color.White)
                }

                // PDF Option
                Button(
                    onClick = { onFormatSelected(ExportFormat.PDF) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4A9EFF))
                ) {
                    Text("PDF Format", color = Color.White)
                }

                // Cancel Option
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        },
        backgroundColor = Color(0xFF1E1E1E),
        contentColor = Color.White
    )
}

@Composable
fun MonthHeader(currentMonth: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentMonth,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun TimeListenedCard(
    totalMinutes: Int,
    dailyStats: List<DailyListeningTime>,
    onTimeDetailClick: () -> Unit = {} // Add this parameter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Time listened",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onTimeDetailClick,
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "View time details",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$totalMinutes minutes",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1DB954)
            )

            if (dailyStats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Today breakdown:", // Changed from "Daily breakdown:"
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val todayData = dailyStats.take(1)
                    todayData.forEach { dailyStat ->
                        TodayStatChip(dailyStat = dailyStat)
                    }
                }
            }
        }
    }
}


@Composable
fun TodayStatChip(dailyStat: DailyListeningTime) {
    val minutes = (dailyStat.dailyDuration / 1000 / 60).toInt()
    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    Card(
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color(0xFF2A2A2A)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Today",
                fontSize = 10.sp,
                color = Color.Gray
            )
            Text(
                text = if (hours > 0) "${hours}h ${remainingMinutes}m" else "${minutes}m",
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TopArtistCard(
    topArtists: List<ArtistStats>,
    onArrowClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(180.dp),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top artist",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                IconButton(
                    onClick = onArrowClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "View all artists",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (topArtists.isNotEmpty()) {
                val topArtist = topArtists.first()


                AsyncImage(
                    model = topArtist.albumPath,
                    contentDescription = "Album cover for ${topArtist.artist}",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = topArtist.artist,
                    fontSize = 16.sp,
                    color = Color(0xFF4A9EFF),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "No data",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TopSongCard(
    topSongs: List<SongStats>,
    onArrowClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(180.dp),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top song",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                IconButton(
                    onClick = onArrowClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "View all songs",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (topSongs.isNotEmpty()) {
                val topSong = topSongs.first()

                AsyncImage(
                    model = topSong.albumPath,
                    contentDescription = "Album cover for ${topSong.title}",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = topSong.title,
                    fontSize = 16.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "by ${topSong.artist}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "No data",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StreakAchievementCard(streakData: StreakData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                // Album art background
                if (!streakData.albumPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = streakData.albumPath,
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Overlay gradient for better text visibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF87CEEB), Color(0xFF4682B4))
                                )
                            )
                    )
                }

                // Content overlay
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "${streakData.streakDays} DAYS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "You had a ${streakData.streakDays}-day streak",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "You played \"${streakData.songTitle}\" by ${streakData.artist} day after day. You were on fire!",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${streakData.startDate} - ${streakData.endDate}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun NoStreakCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = "No streak",
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "No streak yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Listen to the same song for 2+ consecutive days to create a streak!",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun NoDataAvailableCard(currentMonth: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "No data",
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No data available",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "for $currentMonth",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Start listening to music to see your analytics here!",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun ProfileStat(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, fontSize = 18.sp, color = Color.White)
        Text(text = label, fontSize = 12.sp, letterSpacing = 1.5.sp, color = Color.Gray)
    }
}