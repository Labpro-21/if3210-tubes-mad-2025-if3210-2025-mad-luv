package com.kolee.composemusicexoplayer.presentation.music_screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

open class StatefulViewModel<T>(
    initState: T
) : ViewModel() {

    val _uiState = MutableStateFlow(initState)
    val uiState: StateFlow<T> = _uiState.asStateFlow()

    protected fun updateState(newState: T.() -> T){
        _uiState.update(newState)
    }
}
