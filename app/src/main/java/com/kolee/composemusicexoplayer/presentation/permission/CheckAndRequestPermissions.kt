package com.kolee.composemusicexoplayer.presentation.permission

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.ui.theme.Dimens
import com.kolee.composemusicexoplayer.ui.theme.TextDefaultColor

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CheckAndRequestPermissions(
    permissions: List<String>,
    appContent: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalContext.current as Activity

    val permissionState = rememberMultiplePermissionsState(permissions = permissions)

    when {
        permissionState.allPermissionsGranted -> {
            appContent()
        }

        permissionState.shouldShowRationale -> {
            // Case: User denied permission but didn't select "Don't ask again"
            PermissionRationaleUI(onRequestPermission = {
                permissionState.launchMultiplePermissionRequest()
            })
        }

        else -> {
            // Case: User permanently denied permission (selected "Don't ask again")
            PermissionDeniedUI(onGoToSettings = {
                activity.openAppSettings()
            })
        }
    }
}

@Composable
fun PermissionRationaleUI(onRequestPermission: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(Dimens.Six)
    ) {
        Image(
            painter = rememberAsyncImagePainter(R.drawable.music_player_icon),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(Dimens.Six))
        Text(
            text = stringResource(id = R.string.permission_prompt),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.TextDefaultColor,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(Dimens.Six))
        TextButton(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Blue,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(Dimens.Three)
        ) {
            Text(text = stringResource(id = R.string.enable_permissions))
        }
    }
}

@Composable
fun PermissionDeniedUI(onGoToSettings: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(Dimens.Six)
    ) {
        Text(
            text = stringResource(R.string.permissions_rationale),
            textAlign = TextAlign.Center,
            color = Color.Red,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(Dimens.Sixteen))
        TextButton(
            onClick = onGoToSettings,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Blue,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(Dimens.Three)
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
