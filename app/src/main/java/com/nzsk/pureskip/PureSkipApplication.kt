package com.nzsk.pureskip

import android.app.Application
import android.content.Context
import com.nzsk.pureskip.settings.SettingsManager

/**
 * PureSkip Application class.
 * Initializes global settings and managers on app startup.
 */
class PureSkipApplication : Application() {

    lateinit var settingsManager: SettingsManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsManager = SettingsManager.getInstance(this)
    }

    companion object {
        @Volatile
        private lateinit var instance: PureSkipApplication

        fun getInstance(): PureSkipApplication = instance

        fun getAppContext(): Context = instance.applicationContext
    }
}
