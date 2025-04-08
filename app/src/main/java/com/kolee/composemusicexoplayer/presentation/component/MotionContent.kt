package com.kolee.composemusicexoplayer.presentation.component

import android.util.Log
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
            onClick = {
                Log.d("MotionContent", "Back button pressed")
                onBack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 36.dp)
                .size(48.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = "Back",
                modifier = Modifier.size(30.dp)
            )
        }


        MotionLayout(
            motionScene = MotionScene(content = motionScene),
            progress = fraction,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp)
                .padding(top = 260.dp, bottom = 64.dp)
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
                    .padding(bottom = 180.dp)
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


            // Top player buttons
            AnimatedVisibility(visible = fraction > 0.8f) {
                Row(
                    modifier = Modifier
                        .layoutId("top_player_buttons")
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = {
                        playerVM.onEvent(PlayerEvent.PlayPause(musicUiState.isPlaying))
                    }) {
                        Icon(
                            painter = painterResource(
                                id = if (!musicUiState.isPlaying)
                                    R.drawable.ic_play_filled_rounded
                                else
                                    R.drawable.ic_pause_filled_rounded
                            ),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = {
                        playerVM.onEvent(PlayerEvent.Next)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_next_filled_rounded),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
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
                    IconButton(onClick = {
                        playerVM.onEvent(PlayerEvent.Previous)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_previous_filled_rounded),
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(onClick = {
                        playerVM.onEvent(PlayerEvent.PlayPause(musicUiState.isPlaying))
                    }) {
                        Icon(
                            painter = painterResource(
                                id = if (musicUiState.isPlaying)
                                    R.drawable.ic_pause_filled_rounded
                                else
                                    R.drawable.ic_play_filled_rounded
                            ),
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    IconButton(onClick = {
                        playerVM.onEvent(PlayerEvent.Next)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_next_filled_rounded),
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
