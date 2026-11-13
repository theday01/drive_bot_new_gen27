# ProGuard / R8 rules for inDriver Bot

# Keep Application class
-keep class com.indriver.bot.BotApplication { *; }

# Keep Accessibility Service and Services
-keep class com.indriver.bot.service.InDriverAccessibilityService { *; }
-keep class com.indriver.bot.service.BotService { *; }

# Keep UI Activities
-keep class com.indriver.bot.ui.main.MainActivity { *; }

# Keep ViewBinding
-keep class com.indriver.bot.databinding.** { *; }

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
