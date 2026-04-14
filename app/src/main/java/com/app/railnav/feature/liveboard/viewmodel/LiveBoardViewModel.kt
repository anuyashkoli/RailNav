package com.app.railnav.feature.liveboard.viewmodel

import androidx.lifecycle.ViewModel
import com.app.railnav.data.TrainDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class LiveBoardUiState(
    val showLiveBoardSheet: Boolean = false,
    val liveBoardDirection: TrainDirection = TrainDirection.DOWN,
    val showMiniLiveBoard: Boolean = true
)

@HiltViewModel
class LiveBoardViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(LiveBoardUiState())
    val uiState: StateFlow<LiveBoardUiState> = _uiState.asStateFlow()

    fun openLiveBoard() {
        _uiState.value = _uiState.value.copy(showLiveBoardSheet = true, showMiniLiveBoard = true)
    }

    fun closeLiveBoard() {
        _uiState.value = _uiState.value.copy(showLiveBoardSheet = false)
    }

    fun closeMiniLiveBoard() {
        _uiState.value = _uiState.value.copy(showMiniLiveBoard = false)
    }

    fun setLiveBoardDirection(direction: TrainDirection) {
        _uiState.value = _uiState.value.copy(liveBoardDirection = direction)
    }
}
