package com.igygtimer.repository

import com.igygtimer.audio.BeepPlayer
import com.igygtimer.model.TimerPhase
import com.igygtimer.model.TimerUiState
import com.igygtimer.model.WorkoutConfig
import com.igygtimer.util.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TimerRepository(
    private val timeProvider: TimeProvider
) {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var phaseStartTime: Long = 0
    private var workStartTime: Long = 0
    private var workoutStartTime: Long = 0
    private var pausedElapsedWork: Long = 0

    private var lastBeepSecond: Int = -1

    var beepPlayer: BeepPlayer? = null

    fun startWorkout(config: WorkoutConfig) {
        lastBeepSecond = -1
        _uiState.update {
            TimerUiState(
                phase = TimerPhase.Idle,
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
        pausedElapsedWork = 0
        _uiState.update {
            it.copy(
                phase = TimerPhase.Work(it.currentRound),
                displayTimeMs = 0
            )
        }
    }

    fun onWorkDone() {
        val state = _uiState.value
        if (state.phase !is TimerPhase.Work) return

        val workDuration = if (pausedElapsedWork > 0) {
            pausedElapsedWork + (timeProvider.uptimeMillis() - phaseStartTime)
        } else {
            timeProvider.uptimeMillis() - workStartTime
        }

        val restDurationMs = (workDuration * state.ratio).toLong()
        startRest(restDurationMs)
    }

    private fun startRest(durationMs: Long) {
        phaseStartTime = timeProvider.uptimeMillis()
        lastBeepSecond = -1
        val round = _uiState.value.currentRound
        _uiState.update {
            it.copy(
                phase = TimerPhase.Rest(round, durationMs),
                displayTimeMs = durationMs
            )
        }
    }

    fun tick() {
        val state = _uiState.value
        val now = timeProvider.uptimeMillis()
        val totalElapsed = now - workoutStartTime

        when (val phase = state.phase) {
            is TimerPhase.Work -> {
                val elapsed = if (pausedElapsedWork > 0) {
                    pausedElapsedWork + (now - phaseStartTime)
                } else {
                    now - phaseStartTime
                }
                _uiState.update {
                    it.copy(displayTimeMs = elapsed, totalElapsedMs = totalElapsed)
                }
            }
            is TimerPhase.Rest -> {
                val elapsed = now - phaseStartTime
                val remaining = (phase.durationMs - elapsed).coerceAtLeast(0)
                _uiState.update {
                    it.copy(displayTimeMs = remaining, totalElapsedMs = totalElapsed)
                }

                val remainingSeconds = (remaining / 1000).toInt()
                if (remainingSeconds in 1..3 && remainingSeconds != lastBeepSecond) {
                    lastBeepSecond = remainingSeconds
                    beepPlayer?.playBeep()
                }

                if (remaining <= 0) {
                    onRestComplete()
                }
            }
            else -> { }
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

    fun pause() {
        val state = _uiState.value
        val phase = state.phase

        when (phase) {
            is TimerPhase.Work -> {
                val elapsed = if (pausedElapsedWork > 0) {
                    pausedElapsedWork + (timeProvider.uptimeMillis() - phaseStartTime)
                } else {
                    timeProvider.uptimeMillis() - phaseStartTime
                }
                _uiState.update {
                    it.copy(phase = TimerPhase.Paused(phase, elapsed))
                }
            }
            is TimerPhase.Rest -> {
                val remaining = state.displayTimeMs
                _uiState.update {
                    it.copy(phase = TimerPhase.Paused(phase, remaining))
                }
            }
            else -> { }
        }
    }

    fun resume() {
        val state = _uiState.value
        val paused = state.phase as? TimerPhase.Paused ?: return

        phaseStartTime = timeProvider.uptimeMillis()

        when (val from = paused.from) {
            is TimerPhase.Work -> {
                pausedElapsedWork = paused.remainingMs
                _uiState.update { it.copy(phase = from) }
            }
            is TimerPhase.Rest -> {
                val newRestDuration = paused.remainingMs
                _uiState.update {
                    it.copy(
                        phase = TimerPhase.Rest(from.round, newRestDuration),
                        displayTimeMs = newRestDuration
                    )
                }
            }
            else -> { }
        }
    }

    fun stop() {
        _uiState.update { TimerUiState() }
    }

    fun reset() {
        _uiState.update { TimerUiState() }
    }

    fun isActive(): Boolean {
        return when (_uiState.value.phase) {
            is TimerPhase.Work, is TimerPhase.Rest, is TimerPhase.Paused -> true
            else -> false
        }
    }
}
