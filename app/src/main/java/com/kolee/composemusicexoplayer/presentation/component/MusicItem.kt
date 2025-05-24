package com.kolee.composemusicexoplayer.presentation.component

import android.app.AlertDialog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
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
import com.kolee.composemusicexoplayer.utils.DeepLinkHandler

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MusicItem(
    music: MusicEntity,
    selected: Boolean,
    isMusicPlaying: Boolean,
    isHorizontal: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Only show share button for server songs (not local songs)
    val isServerSong = music.owner != "LOCAL" && music.audioPath.startsWith("http")

    if (isHorizontal) {
        Card(
            onClick = onClick,
            backgroundColor = Color.Transparent,
            elevation = 0.dp,
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
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
                            .size(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    
                    // Share button for horizontal layout (only for server songs)
                    if (isServerSong) {
                        IconButton(
                            onClick = { showShareOptions(context, music) },
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Song",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .width(110.dp)
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
            }
        }
    }else {
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

                if (isServerSong) {
                    IconButton(
                        onClick = { showShareOptions(context, music) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Song"
                        )
                    }
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
}
private fun showShareOptions(context: Context, music: MusicEntity) {
    val builder = AlertDialog.Builder(context, R.style.CustomAlertDialog)
    builder.setTitle("Share Song")

    val options = arrayOf("Share Link", "Share QR Code")

    builder.setItems(options) { dialog, which ->
        when (which) {
            0 -> DeepLinkHandler.shareSong(context, music.audioId, music.title, music.artist)
            1 -> DeepLinkHandler.shareQRCode(context, music.audioId, music.title, music.artist)
        }
        dialog.dismiss()
    }

    builder.setNegativeButton("Cancel") { dialog, _ ->
        dialog.dismiss()
    }

    builder.show()
}
