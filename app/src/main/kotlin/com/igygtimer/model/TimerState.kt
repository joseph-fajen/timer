package com.igygtimer.model

sealed class TimerPhase {
    object Idle : TimerPhase()
    data class Work(val round: Int) : TimerPhase()
    data class Rest(val round: Int, val durationMs: Long) : TimerPhase()
    data class Paused(val from: TimerPhase, val remainingMs: Long) : TimerPhase()
    object Complete : TimerPhase()
}

data class TimerUiState(
    val phase: TimerPhase = TimerPhase.Idle,
    val displayTimeMs: Long = 0,
    val currentRound: Int = 1,
    val totalRounds: Int = 10,
    val ratio: Float = 1.0f,
    val totalElapsedMs: Long = 0
)
