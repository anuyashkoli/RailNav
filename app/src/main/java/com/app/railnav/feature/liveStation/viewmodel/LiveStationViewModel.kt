package com.app.railnav.feature.liveStation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.railnav.core.data.repository.IRCTCRepository
import com.app.railnav.core.data.remote.models.LiveStationTrain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.app.railnav.core.data.local.dao.SearchHistoryDao
import com.app.railnav.core.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class LiveStationUiState(
    val isLoading: Boolean = false,
    val trains: List<LiveStationTrain> = emptyList(),
    val error: String? = null,
    val stationCodeQuery: String = "",
    val stationName: String = "",
    val searchHistory: List<SearchHistoryEntity> = emptyList()
)

@HiltViewModel
class LiveStationViewModel @Inject constructor(
    private val repository: IRCTCRepository,
    private val searchHistoryDao: SearchHistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveStationUiState())
    val uiState: StateFlow<LiveStationUiState> = _uiState.asStateFlow()

    init {
        searchHistoryDao.getRecentSearches("STATION", 3)
            .onEach { history ->
                _uiState.value = _uiState.value.copy(searchHistory = history)
            }
            .launchIn(viewModelScope)
    }

    fun onStationCodeChanged(query: String) {
        _uiState.value = _uiState.value.copy(stationCodeQuery = query.uppercase())
    }

    fun fetchDepartures(hours: String = "4") {
        val code = _uiState.value.stationCodeQuery.trim()
        if (code.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Enter a station code (e.g. NDLS, CSMT)")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null, trains = emptyList())

        viewModelScope.launch {
            repository.getLiveStation(code, hours)
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            trains = response.data.trains
                        )
                        searchHistoryDao.insertSearch(SearchHistoryEntity(query = code, searchType = "STATION"))
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.errorMessage ?: "No departures found"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Network Error: ${e.localizedMessage}"
                    )
                }
        }
    }
}
