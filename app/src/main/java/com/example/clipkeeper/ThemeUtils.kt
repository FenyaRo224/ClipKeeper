package com.example.clipkeeper

import android.app.Activity
import android.content.Context
import java.util.Locale

object ThemeUtils {
    private const val CROWN = "\uD83D\uDC51"

    fun applyTheme(activity: Activity) {
        when (Prefs.theme) {
            "dark" -> activity.setTheme(R.style.Theme_ClipKeeper_Dark)
            "gray" -> activity.setTheme(R.style.Theme_ClipKeeper_Gray)
            "pastelBlue" -> activity.setTheme(R.style.Theme_ClipKeeper_PastelBlue)
            "pastelPink" -> activity.setTheme(R.style.Theme_ClipKeeper_PastelPink)
            "pastelPurple" -> activity.setTheme(R.style.Theme_ClipKeeper_PastelPurple)
            "pastelYellow" -> activity.setTheme(R.style.Theme_ClipKeeper_PastelYellow)
            "pastelRed" -> activity.setTheme(R.style.Theme_ClipKeeper_PastelRed)
            "pastelOrange" -> activity.setTheme(R.style.Theme_ClipKeeper_PastelOrange)
            "pastelMint" -> activity.setTheme(R.style.Theme_ClipKeeper_PastelMint)
            "pastelTeal" -> activity.setTheme(R.style.Theme_ClipKeeper_PastelTeal)
            "pastelLavender" -> activity.setTheme(R.style.Theme_ClipKeeper_PastelLavender)
            "pastelPeach" -> activity.setTheme(R.style.Theme_ClipKeeper_PastelPeach)
            "pastelGreen" -> activity.setTheme(R.style.Theme_ClipKeeper_PastelGreen)
            "neonCyber" -> activity.setTheme(R.style.Theme_ClipKeeper_NeonCyber)
            "neonSunset" -> activity.setTheme(R.style.Theme_ClipKeeper_NeonSunset)
            "royalGold" -> activity.setTheme(R.style.Theme_ClipKeeper_RoyalGold)
            "darkGold" -> activity.setTheme(R.style.Theme_ClipKeeper_DarkGold)
            "darkRed" -> activity.setTheme(R.style.Theme_ClipKeeper_DarkRed)
            "darkPink" -> activity.setTheme(R.style.Theme_ClipKeeper_DarkPink)
            "darkBlue" -> activity.setTheme(R.style.Theme_ClipKeeper_DarkBlue)
            "darkPurple" -> activity.setTheme(R.style.Theme_ClipKeeper_DarkPurple)
            else -> activity.setTheme(R.style.Theme_ClipKeeper)
        }
    }

    fun decorateRoyal(vararg views: android.widget.TextView) {
        if (Prefs.theme != "royalGold") return
        views.forEach { v ->
            val base = v.text.toString().replace(CROWN, "").trim()
            v.text = "$CROWN $base $CROWN"
        }
    }

    fun withRoyal(text: String): String {
        return if (Prefs.theme == "royalGold") "$CROWN $text $CROWN" else text
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