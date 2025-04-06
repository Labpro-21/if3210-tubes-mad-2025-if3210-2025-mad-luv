package com.kolee.composemusicexoplayer.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.R

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MusicItem(
    music: MusicEntity,
    selected: Boolean,
    isMusicPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        backgroundColor = Color.Transparent,
        elevation = 0.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .height(72.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(music.albumPath.toUri())
                        .error(R.drawable.ic_music_unknown)
                        .placeholder(R.drawable.ic_music_unknown)
                        .build()
                ),
                contentDescription = null,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
            )

            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f)
            ) {
                Text(
                    text = music.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.body2.copy(
                        color = if (selected) MaterialTheme.colors.primary
                        else LocalContentColor.current,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = music.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.body2.copy(
                        color = if (selected) MaterialTheme.colors.primary
                        else LocalContentColor.current.copy(alpha = 0.7f)
                    )
                )
            }

            AnimatedVisibility(
                modifier = Modifier.padding(8.dp),
                visible = selected,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                AudioWave(isMusicPlaying = isMusicPlaying)
            }
        }
    }
}
