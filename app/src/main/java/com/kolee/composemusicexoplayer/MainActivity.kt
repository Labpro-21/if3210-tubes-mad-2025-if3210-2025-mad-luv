package com.kolee.composemusicexoplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import com.kolee.composemusicexoplayer.presentation.music_screen.MusicScreen
import com.kolee.composemusicexoplayer.presentation.permission.CheckAndRequestPermissions
import com.kolee.composemusicexoplayer.ui.theme.ComposeMusicExoPlayerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val listOfPermissions = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        setContent {
            ComposeMusicExoPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    CheckAndRequestPermissions(
                        permissions = listOfPermissions
                    ) {
                        MusicScreen()
                    }
                }
            }
        }
    }
}
