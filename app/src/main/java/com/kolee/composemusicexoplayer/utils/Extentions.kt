package com.kolee.composemusicexoplayer.utils

import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateList

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetScaffoldState.currentFraction(): Float {
    val fraction = remember(this.bottomSheetState.progress, this.bottomSheetState.currentValue, this.bottomSheetState.targetValue) {
        val current = bottomSheetState.currentValue
        val target = bottomSheetState.targetValue
        val progress = bottomSheetState.progress

        when {
            current == BottomSheetValue.Collapsed && target == BottomSheetValue.Collapsed -> 0f
            current == BottomSheetValue.Expanded && target == BottomSheetValue.Expanded -> 1f
            current == BottomSheetValue.Collapsed && target == BottomSheetValue.Expanded -> progress
            current == BottomSheetValue.Expanded && target == BottomSheetValue.Collapsed -> 1f - progress
            else -> progress
        }
    }
    return fraction
}


fun <T> Collection<T>.move(from: Int, to: Int): List<T> {
    if (from == to || from !in indices || to !in indices) return this.toList()
    return ArrayList(this).apply {
        val item = removeAt(from)
        add(to, item)
    }
}

private val <T> Collection<T>.indices: IntRange
    get() = 0 until size

fun <T> SnapshotStateList<T>.swap(newList: List<T>): SnapshotStateList<T> {
    clear()
    addAll(newList)
    return this
}
