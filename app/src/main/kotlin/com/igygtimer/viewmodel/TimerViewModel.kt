package com.igygtimer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.igygtimer.model.TimerPhase
import com.igygtimer.model.TimerUiState
import com.igygtimer.model.WorkoutConfig
import com.igygtimer.util.SystemTimeProvider
import com.igygtimer.util.TimeProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerViewModel(
    private val timeProvider: TimeProvider = SystemTimeProvider()
) : ViewModel() {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var phaseStartTime: Long = 0
    private var workStartTime: Long = 0
    private var lastWorkDurationMs: Long = 0
    private var workoutStartTime: Long = 0

    fun startWorkout(config: WorkoutConfig) {
        _uiState.update {
            it.copy(
                totalRounds = config.totalRounds,
                ratio = config.ratio,
                currentRound = 1,
                totalElapsedMs = 0
            )
        }
        workoutStartTime = timeProvider.uptimeMillis()
        startWork()
    }

    private fun startWork() {
        phaseStartTime = timeProvider.uptimeMillis()
        workStartTime = phaseStartTime
        _uiState.update { it.copy(phase = TimerPhase.Work(it.currentRound), displayTimeMs = 0) }
        startTickLoop()
    }

    fun onWorkDone() {
        val now = timeProvider.uptimeMillis()
        lastWorkDurationMs = now - workStartTime
        val restDurationMs = (lastWorkDurationMs * _uiState.value.ratio).toLong()
        startRest(restDurationMs)
    }

    private fun startRest(durationMs: Long) {
        timerJob?.cancel()
        phaseStartTime = timeProvider.uptimeMillis()
        val round = _uiState.value.currentRound
        _uiState.update { it.copy(phase = TimerPhase.Rest(round, durationMs), displayTimeMs = durationMs) }
        startRestCountdown(durationMs)
    }

    private fun startRestCountdown(durationMs: Long) {
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = timeProvider.uptimeMillis() - phaseStartTime
                val remaining = (durationMs - elapsed).coerceAtLeast(0)
                val totalElapsed = timeProvider.uptimeMillis() - workoutStartTime
                _uiState.update { it.copy(displayTimeMs = remaining, totalElapsedMs = totalElapsed) }

                if (remaining <= 0) {
                    onRestComplete()
                    break
                }
                delay(50)
            }
        }
    }

    private fun onRestComplete() {
        val state = _uiState.value
        if (state.currentRound >= state.totalRounds) {
            _uiState.update { it.copy(phase = TimerPhase.Complete) }
        } else {
            _uiState.update { it.copy(currentRound = it.currentRound + 1) }
            startWork()
        }
    }

    private fun startTickLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = timeProvider.uptimeMillis() - phaseStartTime
                val totalElapsed = timeProvider.uptimeMillis() - workoutStartTime
                _uiState.update { it.copy(displayTimeMs = elapsed, totalElapsedMs = totalElapsed) }
                delay(50)
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        val state = _uiState.value
        val remaining = when (val phase = state.phase) {
            is TimerPhase.Work -> state.displayTimeMs
            is TimerPhase.Rest -> state.displayTimeMs
            else -> 0
        }
        _uiState.update { it.copy(phase = TimerPhase.Paused(state.phase, remaining)) }
    }

    fun resume() {
        val state = _uiState.value
        val paused = state.phase as? TimerPhase.Paused ?: return
        phaseStartTime = timeProvider.uptimeMillis()

        when (val from = paused.from) {
            is TimerPhase.Work -> {
                val alreadyElapsed = paused.remainingMs
                _uiState.update { it.copy(phase = from) }
                timerJob = viewModelScope.launch {
                    while (isActive) {
                        val elapsed = alreadyElapsed + (timeProvider.uptimeMillis() - phaseStartTime)
                        val totalElapsed = timeProvider.uptimeMillis() - workoutStartTime
                        _uiState.update { it.copy(displayTimeMs = elapsed, totalElapsedMs = totalElapsed) }
                        delay(50)
                    }
                }
            }
            is TimerPhase.Rest -> {
                val remaining = paused.remainingMs
                _uiState.update { it.copy(phase = from, displayTimeMs = remaining) }
                startRestCountdown(remaining)
            }
            else -> {}
        }
    }

    fun stop() {
        timerJob?.cancel()
        _uiState.update { TimerUiState() }
    }

    fun reset() {
        timerJob?.cancel()
        _uiState.update { TimerUiState() }
    }
}
