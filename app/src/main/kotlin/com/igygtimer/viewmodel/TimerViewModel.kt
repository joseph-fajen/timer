package com.igygtimer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.igygtimer.IGYGApplication
import com.igygtimer.model.TimerUiState
import com.igygtimer.model.WorkoutConfig
import com.igygtimer.repository.TimerRepository
import kotlinx.coroutines.flow.StateFlow

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TimerRepository =
        (application as IGYGApplication).container.timerRepository

    val uiState: StateFlow<TimerUiState> = repository.uiState

    fun startWorkout(config: WorkoutConfig) {
        repository.startWorkout(config)
    }

    fun onWorkDone() {
        repository.onWorkDone()
    }

    fun pause() {
        repository.pause()
    }

    fun resume() {
        repository.resume()
    }

    fun stop() {
        repository.stop()
    }

    fun reset() {
        repository.reset()
    }
}
