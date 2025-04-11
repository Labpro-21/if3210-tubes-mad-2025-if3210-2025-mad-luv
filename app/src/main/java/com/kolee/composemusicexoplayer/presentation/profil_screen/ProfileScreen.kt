package com.kolee.composemusicexoplayer.presentation.profil_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.data.auth.AuthViewModel
import com.kolee.composemusicexoplayer.data.profile.ProfileViewModel
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    playerViewModel: PlayerViewModel,
    authViewModel: AuthViewModel,
) {
    val profileState by viewModel.profile.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchProfile()
    }

    val allSongs by remember { derivedStateOf { playerViewModel.uiState.value.musicList.size } }
    val likedSongs by remember { derivedStateOf { playerViewModel.getLoved().size } }
    val listenedSongs by remember { derivedStateOf { playerViewModel.getListenedSongs().size } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF015871), Color.Black)
                )
            )
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        profileState?.let { profile ->
            val imageUrl = "http://34.101.226.132:3000/uploads/profile-picture/${profile.profilePhoto}"

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
                    onClick = { /* TODO:  */ },
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

            Text(text = profile.username, fontSize = 20.sp, color = Color.White)
            Text(text = profile.location, color = Color.LightGray)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* TODO:  */ },
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { authViewModel.logout() }) {
                Text(text = "Logout")
            }

        } ?: run {
            CircularProgressIndicator(color = Color.White)

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { authViewModel.logout() }) {
                Text(text = "Logout")
            }
        }
    }
}

@Composable
fun ProfileStat(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, fontSize = 18.sp, color = Color.White)
        Text(text = label, fontSize = 12.sp, letterSpacing = 1.5.sp, color = Color.Gray)
    }
}
