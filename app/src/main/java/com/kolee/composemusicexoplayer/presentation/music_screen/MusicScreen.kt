package com.kolee.composemusicexoplayer.presentation.music_screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.composemusicexoplayer.data.auth.AuthViewModel

@Composable
fun MusicScreen(viewModel: AuthViewModel) {
    val userName by viewModel.userName.collectAsState(initial = "")

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Halo, ${userName.ifBlank { "user" }}!",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.logout() }) {
                Text(text = "Logout")
            }
        }
    }
}
