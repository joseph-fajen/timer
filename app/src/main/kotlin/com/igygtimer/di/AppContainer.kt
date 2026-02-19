package com.igygtimer.di

import android.content.Context
import com.igygtimer.repository.SettingsRepository
import com.igygtimer.repository.TimerRepository
import com.igygtimer.util.SystemTimeProvider

class AppContainer(context: Context) {

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val timerRepository: TimerRepository by lazy {
        TimerRepository(timeProvider = SystemTimeProvider())
    }
}
