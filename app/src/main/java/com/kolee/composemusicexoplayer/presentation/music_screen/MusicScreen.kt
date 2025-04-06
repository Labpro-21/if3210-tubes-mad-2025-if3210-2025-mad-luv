package com.kolee.composemusicexoplayer.presentation.music_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.abs
import com.kolee.composemusicexoplayer.data.auth.AuthViewModel
import com.kolee.composemusicexoplayer.ui.theme.Dimens
import com.kolee.composemusicexoplayer.ui.theme.Green700
import com.kolee.composemusicexoplayer.ui.theme.TextDefaultColor
import com.kolee.composemusicexoplayer.ui.theme.TintDefaultColor
import com.kolee.composemusicexoplayer.ui.theme.getRandomColor
import Song

@Composable
fun MusicScreen(viewModel: AuthViewModel) {
    val userName by viewModel.userName.collectAsState(initial = "")
    var isPlaying by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.Two)
            ) {
                Text(
                    text = "Halo, ${userName.ifBlank { "user" }}!",
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.TextDefaultColor,
                    modifier = Modifier.padding(bottom = Dimens.Two)
                )
                Text(
                    text = "New songs",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.TextDefaultColor,
                    modifier = Modifier.padding(bottom = Dimens.Two)
                )

                NewSongsSection()

                Spacer(modifier = Modifier.height(Dimens.Three))

                Text(
                    text = "Recently played",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.TextDefaultColor,
                    modifier = Modifier.padding(bottom = Dimens.Two)
                )

                RecentlyPlayedSection()

                Spacer(modifier = Modifier.height(Dimens.Two))

                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Green700),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Logout",
                        color = MaterialTheme.colors.TextDefaultColor,
                        style = MaterialTheme.typography.button
                    )
                }
            }

            NowPlayingBar(
                isPlaying = isPlaying,
                onPlayPauseClick = { isPlaying = !isPlaying }
            )
        }
    }
}

@Composable
fun NewSongsSection() {
    val newSongs = listOf(
        Song("Starboy", "The Weeknd"),
        Song("Here Comes The Sun", "The Beatles"),
        Song("Midnight Pretenders", "Tomoko Aran"),
        Song("Violent Crimes", "Kanye West")
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.One)
    ) {
        items(newSongs) { song ->
            Column(
                modifier = Modifier.width(Dimens.Sixteen)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.Sixteen)
                        .clip(MaterialTheme.shapes.medium)
                        .background(getRandomColor(song.title))
                ) {
                    Text(
                        text = song.title.take(1),
                        color = Color.White,
                        style = MaterialTheme.typography.h4,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.One))

                Text(
                    text = song.title,
                    color = MaterialTheme.colors.TextDefaultColor,
                    style = MaterialTheme.typography.subtitle2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = song.artist,
                    color = MaterialTheme.colors.TextDefaultColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RecentlyPlayedSection() {
    val recentSongs = listOf(
        Song("Jazz is for ordinary people", "Berlioz"),
        Song("Loose", "Daniel Caesar"),
        Song("Nights", "Frank Ocean"),
        Song("Kiss of Life", "Sade"),
        Song("BEST INTEREST", "Tyler, The Creator")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.Two)
    ) {
        recentSongs.forEach { song ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Colored box instead of image
                Box(
                    modifier = Modifier
                        .size(Dimens.Six)
                        .clip(MaterialTheme.shapes.small)
                        .background(getRandomColor(song.title))
                ) {
                    // Album initial in the center of the box
                    Text(
                        text = song.title.take(1),
                        color = Color.White,
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.width(Dimens.One))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        color = MaterialTheme.colors.TextDefaultColor,
                        style = MaterialTheme.typography.subtitle2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = song.artist,
                        color = MaterialTheme.colors.TextDefaultColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun NowPlayingBar(
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .padding(Dimens.One)
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.Five)
                .clip(MaterialTheme.shapes.small)
                .background(Green700)
        ) {
            Text(
                text = "S",
                color = Color.White,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.width(Dimens.One))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Starboy",
                color = MaterialTheme.colors.TextDefaultColor,
                style = MaterialTheme.typography.subtitle2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "The Weeknd",
                color = MaterialTheme.colors.TextDefaultColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add to playlist",
                tint = MaterialTheme.colors.TintDefaultColor
            )
        }

        IconButton(onClick = onPlayPauseClick) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Green700
            )
        }
    }
}