package com.indriver.bot.utils

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferenceManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "indriver_bot_prefs"
        const val KEY_BOT_ENABLED = "bot_enabled"
        private const val KEY_LAST_DATE = "last_stats_date"

        @Volatile
        var isBotEnabled: Boolean = false
            private set

        fun init(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            isBotEnabled = prefs.getBoolean(KEY_BOT_ENABLED, false)
        }
    }

    init {
        Companion.isBotEnabled = sharedPreferences.getBoolean(KEY_BOT_ENABLED, false)
        checkDateReset()
    }

    // Bot enabled - Single source of truth
    fun isBotEnabled(): Boolean = Companion.isBotEnabled

    fun setBotEnabled(enabled: Boolean) {
        Companion.isBotEnabled = enabled
        sharedPreferences.edit().putBoolean(KEY_BOT_ENABLED, enabled).apply()
    }

    // Settings
    fun isAutoBidEnabled(): Boolean = sharedPreferences.getBoolean("auto_bid_enabled", true)
    fun setAutoBidEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean("auto_bid_enabled", enabled).apply()

    fun areNotificationsEnabled(): Boolean = sharedPreferences.getBoolean("notifications_enabled", true)
    fun setNotificationsEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean("notifications_enabled", enabled).apply()

    fun getMinPrice(): Double = sharedPreferences.getString("min_price", "0.0")?.toDoubleOrNull() ?: 0.0
    fun setMinPrice(price: Double) = sharedPreferences.edit().putString("min_price", price.toString()).apply()

    fun getMaxDistance(): Double = sharedPreferences.getString("max_distance", "10.0")?.toDoubleOrNull() ?: 10.0
    fun setMaxDistance(distance: Double) = sharedPreferences.edit().putString("max_distance", distance.toString()).apply()

    // Stats
    fun getAcceptedCount(): Int = sharedPreferences.getInt("accepted_count", 0)
    fun incrementAccepted() = sharedPreferences.edit().putInt("accepted_count", getAcceptedCount() + 1).apply()

    fun getMissedCount(): Int = sharedPreferences.getInt("missed_count", 0)
    fun incrementMissed() = sharedPreferences.edit().putInt("missed_count", getMissedCount() + 1).apply()

    fun getTodayAccepted(): Int {
        checkDateReset()
        return sharedPreferences.getInt("today_accepted", 0)
    }

    fun incrementTodayAccepted() {
        checkDateReset()
        sharedPreferences.edit().putInt("today_accepted", getTodayAccepted() + 1).apply()
    }

    fun getTodayEarnings(): Double {
        checkDateReset()
        return sharedPreferences.getString("today_earnings", "0.0")?.toDoubleOrNull() ?: 0.0
    }

    fun addEarnings(amount: Double) {
        checkDateReset()
        val total = getTotalEarnings() + amount
        val today = getTodayEarnings() + amount
        sharedPreferences.edit()
            .putString("total_earnings", total.toString())
            .putString("today_earnings", today.toString())
            .apply()
    }

    fun getTotalEarnings(): Double = sharedPreferences.getString("total_earnings", "0.0")?.toDoubleOrNull() ?: 0.0

    fun getWinRate(): Double {
        val accepted = getAcceptedCount()
        val missed = getMissedCount()
        val total = accepted + missed
        return if (total > 0) (accepted.toDouble() / total) * 100 else 0.0
    }

    fun resetStats() {
        sharedPreferences.edit().apply {
            putInt("accepted_count", 0)
            putInt("missed_count", 0)
            putString("total_earnings", "0.0")
            putInt("today_accepted", 0)
            putString("today_earnings", "0.0")
            putString(KEY_LAST_DATE, getTodayDateString())
            apply()
        }
    }

    fun checkDateReset() {
        val today = getTodayDateString()
        val lastDate = sharedPreferences.getString(KEY_LAST_DATE, null)
        if (lastDate != today) {
            sharedPreferences.edit()
                .putString(KEY_LAST_DATE, today)
                .putInt("today_accepted", 0)
                .putString("today_earnings", "0.0")
                .apply()
        }
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }
}
