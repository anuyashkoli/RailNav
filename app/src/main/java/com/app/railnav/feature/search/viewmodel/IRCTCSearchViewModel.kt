package com.app.railnav.feature.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.railnav.core.data.repository.IRCTCRepository
import com.app.railnav.core.data.remote.models.TrainSearchResult
import com.app.railnav.core.data.remote.models.StationSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IRCTCSearchUiState(
    val trainResults: List<TrainSearchResult> = emptyList(),
    val stationResults: List<StationSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Utility ViewModel for train/station autocomplete search.
 * Uses debouncing (300ms) to avoid spamming the API on every keystroke. 
 */
@HiltViewModel
class IRCTCSearchViewModel @Inject constructor(
    private val repository: IRCTCRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IRCTCSearchUiState())
    val uiState: StateFlow<IRCTCSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /**
     * Autocomplete: type "12105" → returns "Vidarbha Express"
     * Debounced to 300ms to avoid excessive API calls.
     */
    fun searchTrain(query: String) {
        if (query.length < 3) {
            _uiState.value = _uiState.value.copy(trainResults = emptyList())
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.searchTrain(query)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        trainResults = response.data ?: emptyList()
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.localizedMessage
                    )
                }
        }
    }

    /**
     * Autocomplete: type "CSMT" → returns "Chhatrapati Shivaji Maharaj Terminus"
     * Debounced to 300ms.
     */
    fun searchStation(query: String) {
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(stationResults = emptyList())
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.searchStation(query)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        stationResults = response.data ?: emptyList()
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.localizedMessage
                    )
                }
        }
    }

    fun clearResults() {
        _uiState.value = IRCTCSearchUiState()
    }
}
