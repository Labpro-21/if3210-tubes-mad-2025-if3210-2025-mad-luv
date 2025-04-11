package com.kolee.composemusicexoplayer.presentation.component

import androidx.compose.runtime.*
import androidx.compose.material.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kolee.composemusicexoplayer.data.network.NetworkSensing
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun NetworkSensingScreen(
    networkSensing: NetworkSensing,
    showFallbackPage: Boolean = false,
    content: @Composable () -> Unit
) {
    val isConnected by networkSensing.isConnected.collectAsState(initial = true)

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isConnected && showFallbackPage) {
            OfflineFallbackPage()
        } else {

            content()


            if (!isConnected) {
                OfflinePopup()
            }
        }
    }
}

@Composable
fun OfflinePopup() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFE5E5))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Koneksi Anda Terputus",
                color = Color.Red,
                style = MaterialTheme.typography.body1
            )
        }
    }
}


@Composable
fun OfflineFallbackPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = "Offline",
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Tidak dapat memuat data", color = Color.White, fontSize = 18.sp)
        Text("Periksa koneksi internet Anda", color = Color.LightGray, fontSize = 14.sp)
    }
}
