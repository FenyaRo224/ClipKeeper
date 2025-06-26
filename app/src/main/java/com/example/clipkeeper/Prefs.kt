package com.example.clipkeeper

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

object Prefs {
    private lateinit var prefs: SharedPreferences
    private const val NAME = "settings"

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        }
    }

    var theme: String
        get() = prefs.getString("theme", "default") ?: "default"
        set(value) { prefs.edit().putString("theme", value).apply() }

    var language: String
        get() = prefs.getString("language", Locale.getDefault().language) ?: Locale.getDefault().language
        set(value) { prefs.edit().putString("language", value).apply() }
}