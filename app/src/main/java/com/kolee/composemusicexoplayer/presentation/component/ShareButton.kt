package com.kolee.composemusicexoplayer.presentation.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.utils.DeepLinkHandler

@Composable
fun ShareButton(
    songId: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    IconButton(
        onClick = { DeepLinkHandler.shareSong(context, songId) },
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_share),
            contentDescription = "Share Song",
            tint = Color.White
        )
    }
}
