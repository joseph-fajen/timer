package com.igygtimer.audio

import android.util.Log
import com.igygtimer.model.TimerPhase
import com.igygtimer.model.TimerUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BeepScheduler(
    private val uiState: StateFlow<TimerUiState>,
    private val beepPlayer: BeepPlayer,
    scope: CoroutineScope
) {
    companion object {
        private const val TAG = "BeepScheduler"
    }

    private var lastBeepSecond: Int = -1

    init {
        scope.launch {
            uiState.collect { state ->
                when (state.phase) {
                    is TimerPhase.Rest -> {
                        val remainingSeconds = (state.displayTimeMs / 1000).toInt()
                        if (remainingSeconds in 1..3 && remainingSeconds != lastBeepSecond) {
                            lastBeepSecond = remainingSeconds
                            Log.d(TAG, "Triggering beep at $remainingSeconds seconds")
                            beepPlayer.playBeep()
                        }
                    }
                    is TimerPhase.Work -> {
                        // Reset so beeps fire fresh for next rest phase
                        lastBeepSecond = -1
                    }
                    else -> { }
                }
            }
        }
    }
}
