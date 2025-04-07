package com.kolee.composemusicexoplayer.presentation.MusicPlayerSheet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.kolee.composemusicexoplayer.presentation.component.MotionContent
import com.kolee.composemusicexoplayer.presentation.component.SheetContent
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel
import com.kolee.composemusicexoplayer.utils.currentFraction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MusicPlayerSheet(
    playerVM: PlayerViewModel,
     navController: NavHostController,
    onCollapse: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(initialValue = BottomSheetValue.Collapsed)
    )

    val fraction = scaffoldState.currentFraction()

    Box(modifier = Modifier.fillMaxSize()) {


        MotionContent(
            playerVM = playerVM,
            fraction = fraction,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .zIndex(0f),
            onBack = {
                scope.launch {
                    scaffoldState.bottomSheetState.collapse()
                    onCollapse()
                }

            }
        )


    }
}
