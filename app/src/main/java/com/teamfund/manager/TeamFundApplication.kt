package com.teamfund.manager

import android.app.Application
import com.teamfund.manager.core.common.AppContainer

class TeamFundApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
