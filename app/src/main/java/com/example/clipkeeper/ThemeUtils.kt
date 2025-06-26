package com.example.clipkeeper

import android.app.Activity
import android.content.Context
import java.util.Locale

object ThemeUtils {
    fun applyTheme(activity: Activity) {
        when (Prefs.theme) {
            "green" -> activity.setTheme(R.style.Theme_ClipKeeper_Green)
            "dark" -> activity.setTheme(R.style.Theme_ClipKeeper_Dark)
            else -> activity.setTheme(R.style.Theme_ClipKeeper)
        }
    }
}

object LanguageUtils {
    fun applyBaseContext(context: Context): Context {
        val locale = Locale(Prefs.language)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}