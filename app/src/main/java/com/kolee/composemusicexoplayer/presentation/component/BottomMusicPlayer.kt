package com.kolee.composemusicexoplayer.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerEvent
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomMusicPlayer(
    currentMusic: MusicEntity,
    currentDuration: Long,
    isPlaying: Boolean,
    onClick: () -> Unit,
    playerVM: PlayerViewModel,
    onPlayPauseClicked: (isPlaying: Boolean) -> Unit
) {

    val musicUiState by playerVM.uiState.collectAsState()

    val progress = remember(currentDuration, currentMusic.duration) {
        if (currentMusic.duration > 0) currentDuration.toFloat() / currentMusic.duration.toFloat()
        else 0f
    }

    Card(
        onClick = onClick,
        backgroundColor = MaterialTheme.colors.primarySurface,
        modifier = Modifier.height(BottomMusicPlayerHeight.value)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            AlbumImage(albumPath = currentMusic.albumPath)

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = currentMusic.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.subtitle1.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Text(
                    text = currentMusic.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.subtitle2
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ShareButton(
                    songId = currentMusic.audioId,
                    modifier = Modifier
                        .zIndex(1f)
                )

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

                Spacer(modifier = Modifier.width(8.dp))

                PlayPauseButton(
                    progress = progress,
                    isPlaying = isPlaying
                ) {
                    onPlayPauseClicked(!isPlaying)
                }

            }
        }
    }
}

@Composable
private fun PlayPauseButton(
    progress: Float,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(100.dp))
            .clickable { onClick() }
    ) {
        Icon(
            painter = painterResource(
                id = if (isPlaying) R.drawable.ic_pause_filled_rounded else R.drawable.ic_play_filled_rounded
            ),
            contentDescription = null
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AlbumImage(albumPath: String) {
    Card(
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(2.dp, MaterialTheme.colors.onSurface),
        elevation = 8.dp,
        modifier = Modifier.size(56.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(albumPath.toUri())
                        .error(R.drawable.ic_music_unknown)
                        .placeholder(R.drawable.ic_music_unknown)
                        .build()
                ),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

object BottomMusicPlayerHeight {
    val value = 96.dp
}
