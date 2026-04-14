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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.app.railnav.core.data.local.dao.SearchHistoryDao
import com.app.railnav.core.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class LiveTrainUiState(
    val isLoading: Boolean = false,
    val result: LiveTrainResponse? = null,
    val error: String? = null,
    val trainNumberQuery: String = "",
    val startDay: String = "0",   // 0 = today, 1 = yesterday, 2 = 2 days ago
    val searchHistory: List<SearchHistoryEntity> = emptyList()
)

@HiltViewModel
class LiveTrainViewModel @Inject constructor(
    private val api: IRCTCApi,
    private val searchHistoryDao: SearchHistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveTrainUiState())
    val uiState: StateFlow<LiveTrainUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        searchHistoryDao.getRecentSearches("LIVETRAIN", 3)
            .onEach { history ->
                _uiState.value = _uiState.value.copy(searchHistory = history)
            }
            .launchIn(viewModelScope)
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.trainNumberQuery.length == 5) {
                    fetchLiveStatus()
                }
                delay(30000) // Poll every 30 seconds
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }

    fun onTrainNumberChanged(query: String) {
        if (query.length <= 5) {
            _uiState.value = _uiState.value.copy(trainNumberQuery = query)
        }
    }

    fun onStartDayChanged(day: String) {
        _uiState.value = _uiState.value.copy(startDay = day)
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
                val response = api.getLiveTrainStatus(trainNo, _uiState.value.startDay)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        result = response
                    )
                    searchHistoryDao.insertSearch(SearchHistoryEntity(query = trainNo, searchType = "LIVETRAIN"))
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.errorMessage ?: "API returned error"
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
