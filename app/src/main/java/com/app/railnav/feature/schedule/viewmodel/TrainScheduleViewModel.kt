package com.app.railnav.feature.schedule.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.railnav.core.data.repository.IRCTCRepository
import com.app.railnav.core.data.remote.models.ScheduleStation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrainScheduleUiState(
    val isLoading: Boolean = false,
    val stations: List<ScheduleStation> = emptyList(),
    val error: String? = null,
    val trainNumberQuery: String = ""
)

@HiltViewModel
class TrainScheduleViewModel @Inject constructor(
    private val repository: IRCTCRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainScheduleUiState())
    val uiState: StateFlow<TrainScheduleUiState> = _uiState.asStateFlow()

    fun onTrainNumberChanged(query: String) {
        if (query.length <= 5) {
            _uiState.value = _uiState.value.copy(trainNumberQuery = query)
        }
    }

    fun fetchSchedule() {
        val trainNo = _uiState.value.trainNumberQuery.trim()
        if (trainNo.length != 5) {
            _uiState.value = _uiState.value.copy(error = "Train Number must be 5 digits")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null, stations = emptyList())

        viewModelScope.launch {
            repository.getTrainSchedule(trainNo)
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        // Filter to stopping stations only (skip pass-throughs)
                        val allStations = response.data
                        val stoppingStations = if (allStations.size > 2) {
                            listOf(allStations.first()) +
                            allStations.drop(1).dropLast(1).filter { it.isStopping } +
                            listOf(allStations.last())
                        } else allStations
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            stations = stoppingStations
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.errorMessage ?: "Schedule not found"
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
