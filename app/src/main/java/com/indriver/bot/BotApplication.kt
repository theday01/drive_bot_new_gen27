package com.indriver.bot

import android.app.Application
import com.indriver.bot.utils.PreferenceManager

class BotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferenceManager.init(this)
    }
}
