package com.kolee.composemusicexoplayer.presentation.permission

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.ui.theme.Dimens
import com.kolee.composemusicexoplayer.ui.theme.TextDefaultColor

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CheckAndRequestPermissions(
    appContent: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalContext.current as Activity

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(android.Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionState = rememberMultiplePermissionsState(permissions = permissions)

    when {
        permissionState.allPermissionsGranted -> {
            appContent()
        }

        permissionState.shouldShowRationale -> {
            PermissionRationaleUI {
                permissionState.launchMultiplePermissionRequest()
            }
        }

        else -> {
            PermissionDeniedUI {
                activity.openAppSettings()
            }
        }
    }
}

@Composable
fun PermissionRationaleUI(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.Six),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.music_player_icon),
            contentDescription = null,
            modifier = Modifier.size(200.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(Dimens.Six))
        Text(
            text = stringResource(R.string.permission_prompt),
            fontSize = 20.sp,
            color = MaterialTheme.colors.TextDefaultColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.Six))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.Three),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
            )
        ) {
            Text(text = stringResource(R.string.enable_permissions))
        }
    }
}

@Composable
fun PermissionDeniedUI(onGoToSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.Six),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.permissions_rationale),
            fontSize = 20.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.Sixteen))
        Button(
            onClick = onGoToSettings,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.Three),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
            )
        ) {
            Text(text = stringResource(R.string.goto_settings))
        }
    }
}

private fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:$packageName")
    ).apply {
        addCategory(Intent.CATEGORY_DEFAULT)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }
}
