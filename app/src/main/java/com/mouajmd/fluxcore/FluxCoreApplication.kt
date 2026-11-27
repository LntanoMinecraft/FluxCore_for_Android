package com.mouajmd.fluxcore

import android.app.Application
import com.mouajmd.fluxcore.data.AppLogger
import com.mouajmd.fluxcore.data.RuleStore
import com.mouajmd.fluxcore.data.SettingsStore

class FluxCoreApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        SettingsStore.init(this)
        RuleStore.init(this)
    }
}
