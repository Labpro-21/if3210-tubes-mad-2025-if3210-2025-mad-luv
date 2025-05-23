package com.kolee.composemusicexoplayer.presentation.component

import android.content.res.Configuration
import android.util.Log
import androidx.annotation.Dimension
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import androidx.constraintlayout.compose.layoutId
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.presentation.music_screen.PlaybackMode
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerEvent
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel

@OptIn(ExperimentalMotionApi::class)
@Composable
fun MotionContent(
    playerVM: PlayerViewModel,
    fraction: Float,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val musicUiState by playerVM.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val motionScene = remember {
        context.resources
            .openRawResource(R.raw.motion_scene)
            .readBytes()
            .decodeToString()
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1DB954), Color.Black),
        startY = -6000f,
        endY = Float.POSITIVE_INFINITY
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        IconButton(
            onClick = { onBack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(48.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = "Back",
                modifier = Modifier.size(30.dp)
            )
        }

        if (isLandscape) {
            // LANDSCAPE LAYOUT - Image on left, controls on right
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Image on left
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .padding(end = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AlbumImage(
                        albumPath = musicUiState.currentPlayedMusic.albumPath,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth(0.8f)
                    )
                }

                // Player controls on right
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Title & Artist
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = musicUiState.currentPlayedMusic.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.h5.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = musicUiState.currentPlayedMusic.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.h6.copy(
                                color = Color.LightGray
                            )
                        )
                    }

                    // Progress bar
                    PlayingProgress(
                        playerVM = playerVM,
                        maxDuration = musicUiState.currentPlayedMusic.duration,
                        currentDuration = musicUiState.currentDuration,
                        isPlaying = musicUiState.isPlaying,
                        onChangeFinished = { ratio ->
                            val duration = ratio * musicUiState.currentPlayedMusic.duration
                            playerVM.onEvent(PlayerEvent.SnapTo(duration.toLong()))
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Player controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle Button
                        IconButton(
                            onClick = { playerVM.onEvent(PlayerEvent.ToggleShuffle) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_shuffle),
                                contentDescription = "Shuffle",
                                tint = if (musicUiState.isShuffleEnabled) Color(0xFF1ED760) else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { playerVM.onEvent(PlayerEvent.Previous) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_previous_filled_rounded),
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = { playerVM.onEvent(PlayerEvent.PlayPause(musicUiState.isPlaying)) },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (musicUiState.isPlaying)
                                        R.drawable.ic_pause_filled_rounded
                                    else
                                        R.drawable.ic_play_filled_rounded
                                ),
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(
                            onClick = { playerVM.onEvent(PlayerEvent.Next) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_next_filled_rounded),
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Repeat Button
                        IconButton(
                            onClick = { playerVM.onEvent(PlayerEvent.TogglePlaybackMode) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = when (musicUiState.playbackMode) {
                                        PlaybackMode.REPEAT_ONE -> R.drawable.ic_repeat_one
                                        PlaybackMode.REPEAT_ALL -> R.drawable.ic_repeat
                                        PlaybackMode.REPEAT_OFF -> R.drawable.ic_repeat_off
                                    }
                                ),
                                contentDescription = "Repeat Mode",
                                tint = when (musicUiState.playbackMode) {
                                    PlaybackMode.REPEAT_OFF -> Color.Gray
                                    else -> Color(0xFF1ED760)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // PORTRAIT LAYOUT
            MotionLayout(
                motionScene = MotionScene(content = motionScene),
                progress = fraction,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 30.dp)
                    .padding(top = 190.dp, bottom = 64.dp)
            ) {
                AnimatedVisibility(visible = fraction < 0.8f) {
                    Spacer(
                        modifier = Modifier
                            .layoutId("top_bar")
                            .height(24.dp)
                    )
                }

                // Album Image
                AlbumImage(
                    albumPath = musicUiState.currentPlayedMusic.albumPath,
                    modifier = Modifier
                        .layoutId("album_image")
                        .padding(bottom = 140.dp)
                        .aspectRatio(1f)
                )

                // Title & Artist + Love Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .layoutId("column_title_artist")
                        .fillMaxWidth()
                        .padding(bottom = 15.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = musicUiState.currentPlayedMusic.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.h6.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp,
                                color = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = musicUiState.currentPlayedMusic.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.body2.copy(
                                color = Color.Gray,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    IconButton(
                        onClick = {
                            playerVM.onEvent(PlayerEvent.ToggleLoved(musicUiState.currentPlayedMusic))
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (musicUiState.currentPlayedMusic.loved)
                                    R.drawable.ic_favorite
                                else
                                    R.drawable.ic_favorite_border
                            ),
                            contentDescription = "Love",
                            tint = if (musicUiState.currentPlayedMusic.loved) Color.Red else Color.White
                        )
                    }
                }

                // Main player controls
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .layoutId("main_player_control")
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    PlayingProgress(
                        playerVM = playerVM,
                        maxDuration = musicUiState.currentPlayedMusic.duration,
                        currentDuration = musicUiState.currentDuration,
                        isPlaying = musicUiState.isPlaying,
                        onChangeFinished = { ratio ->
                            val duration = ratio * musicUiState.currentPlayedMusic.duration
                            playerVM.onEvent(PlayerEvent.SnapTo(duration.toLong()))
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Shuffle Button
                        IconButton(
                            onClick = { playerVM.onEvent(PlayerEvent.ToggleShuffle) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_shuffle),
                                contentDescription = "Shuffle",
                                tint = if (musicUiState.isShuffleEnabled) Color(0xFF1ED760) else Color.Gray
                            )
                        }

                        IconButton(
                            onClick = { playerVM.onEvent(PlayerEvent.Previous) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_previous_filled_rounded),
                                contentDescription = "Previous",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { playerVM.onEvent(PlayerEvent.PlayPause(musicUiState.isPlaying)) },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (musicUiState.isPlaying)
                                        R.drawable.ic_pause_filled_rounded
                                    else
                                        R.drawable.ic_play_filled_rounded
                                ),
                                contentDescription = "Play/Pause",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { playerVM.onEvent(PlayerEvent.Next) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_next_filled_rounded),
                                contentDescription = "Next",
                                tint = Color.White
                            )
                        }

                        // Repeat Button
                        IconButton(
                            onClick = { playerVM.onEvent(PlayerEvent.TogglePlaybackMode) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = when (musicUiState.playbackMode) {
                                        PlaybackMode.REPEAT_ONE -> R.drawable.ic_repeat_one
                                        PlaybackMode.REPEAT_ALL -> R.drawable.ic_repeat
                                        PlaybackMode.REPEAT_OFF -> R.drawable.ic_repeat_off
                                    }
                                ),
                                contentDescription = "Repeat Mode",
                                tint = when (musicUiState.playbackMode) {
                                    PlaybackMode.REPEAT_OFF -> Color.Gray
                                    else -> Color(0xFF1ED760)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}