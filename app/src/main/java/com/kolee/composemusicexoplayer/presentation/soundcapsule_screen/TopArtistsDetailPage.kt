package com.kolee.composemusicexoplayer.presentation.soundcapsule_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kolee.composemusicexoplayer.data.model.ArtistStats
import com.kolee.composemusicexoplayer.data.model.SongStats


@Composable
fun TopArtistsDetailPage(
    topArtists: List<ArtistStats>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Top artists",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "April 2025",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = buildAnnotatedString {
                    append("You played ")
                    withStyle(style = SpanStyle(color = Color(0xFFFFD700))) {
                        append("${topArtists.size} different\nartists")
                    }
                    append(" this month.")
                },
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Artists List
            LazyColumn {
                itemsIndexed(topArtists) { index, artist ->
                    Column {
                        ArtistListItem(
                            rank = index + 1,
                            artist = artist
                        )

                        // Add divider line except for the last item
                        if (index < topArtists.size - 1) {
                            Divider(
                                color = Color.Gray.copy(alpha = 0.3f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(
                                    start = 48.dp, // Align with artist info, skipping rank
                                    top = 16.dp,
                                    bottom = 16.dp
                                )
                            )
                        } else {
                            // Add bottom spacing for the last item
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistListItem(
    rank: Int,
    artist: ArtistStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        Text(
            text = String.format("%02d", rank),
            fontSize = 16.sp,
            color = Color(0xFFFFD700),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(32.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Artist info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = artist.artist,
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${artist.playCount} plays",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        // Artist image placeholder (circular)
        AsyncImage(
            model = artist.albumPath,
            contentDescription = "Artist image for ${artist.artist}",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}