package com.kolee.composemusicexoplayer.presentation.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity

@Composable
fun ShareButton(
    music: MusicEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    IconButton(
        onClick = { showShareOptions(context, music) },
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_share),
            contentDescription = "Share Song",
            tint = Color.White
        )
    }
}
