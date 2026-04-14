package com.app.railnav.feature.livetrain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.railnav.core.data.remote.IRCTCApi
import com.app.railnav.core.data.remote.models.LiveTrainResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class LiveTrainUiState(
    val isLoading: Boolean = false,
    val result: LiveTrainResponse? = null,
    val error: String? = null,
    val trainNumberQuery: String = ""
)

@HiltViewModel
class LiveTrainViewModel @Inject constructor(
    private val api: IRCTCApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveTrainUiState())
    val uiState: StateFlow<LiveTrainUiState> = _uiState.asStateFlow()

    fun onTrainNumberChanged(query: String) {
        if (query.length <= 5) {
            _uiState.value = _uiState.value.copy(trainNumberQuery = query)
        }
    }

    fun fetchLiveStatus() {
        val trainNo = _uiState.value.trainNumberQuery
        if (trainNo.length != 5) {
            _uiState.value = _uiState.value.copy(error = "Train Number must be 5 digits (e.g. 12321)")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null, result = null)

        viewModelScope.launch {
            try {
                // startDay: 0 = today, 1 = yesterday, etc.
                val response = api.getLiveTrainStatus(trainNo, "0")
                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        result = response
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.error ?: "API returned Error"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Network Error: ${e.localizedMessage}"
                )
            }
        }
    }
}
