package com.indriver.bot.utils

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.indriver.bot.service.InDriverAccessibilityService

object PermissionHelper {

    fun isAccessibilityEnabled(context: Context): Boolean {
        var accessibilityEnabled = 0
        val expectedFullName = ComponentName(context, InDriverAccessibilityService::class.java).flattenToString()
        val expectedShortName = ComponentName(context, InDriverAccessibilityService::class.java).flattenToShortString()

        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (_: Settings.SettingNotFoundException) {
            return false
        }

        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(settingValue)

            while (splitter.hasNext()) {
                val service = splitter.next()
                if (service.equals(expectedFullName, ignoreCase = true) ||
                    service.equals(expectedShortName, ignoreCase = true)
                ) {
                    return true
                }
            }
        }

        return false
    }
}
