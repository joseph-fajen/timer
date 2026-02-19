package com.igygtimer

import android.app.Application
import com.igygtimer.di.AppContainer

class IGYGApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
