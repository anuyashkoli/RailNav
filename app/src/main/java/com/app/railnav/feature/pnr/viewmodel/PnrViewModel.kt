package com.app.railnav.feature.pnr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.railnav.core.data.remote.IRCTCApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.app.railnav.core.data.remote.models.PnrResponse

data class PnrUiState(
    val isLoading: Boolean = false,
    val result: PnrResponse? = null,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class PnrViewModel @Inject constructor(
    private val api: IRCTCApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(PnrUiState())
    val uiState: StateFlow<PnrUiState> = _uiState.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun fetchPnrStatus() {
        val pnr = _uiState.value.searchQuery
        if (pnr.length != 10) {
            _uiState.value = _uiState.value.copy(error = "PNR must be 10 digits")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null, result = null)

        viewModelScope.launch {
            try {
                // Hitting the IRCTC API!
                val response = api.getPnrStatus(pnr)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        result = response
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.error ?: "API returned error: No success"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed: ${e.localizedMessage}"
                )
            }
        }
    }
}
