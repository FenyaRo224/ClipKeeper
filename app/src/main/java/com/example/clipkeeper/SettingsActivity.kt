package com.example.clipkeeper

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        Prefs.init(newBase)
        val ctx = LanguageUtils.applyBaseContext(newBase)
        super.attachBaseContext(ctx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val themeSpinner = findViewById<Spinner>(R.id.theme_spinner)
        val langSpinner = findViewById<Spinner>(R.id.language_spinner)

        themeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.themes)
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val themeValues = resources.getStringArray(R.array.theme_values)
        themeSpinner.setSelection(themeValues.indexOf(Prefs.theme))

        langSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.languages)
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val langValues = resources.getStringArray(R.array.language_values)
        langSpinner.setSelection(langValues.indexOf(Prefs.language))

        findViewById<Button>(R.id.save_button).setOnClickListener {
            Prefs.theme = themeValues[themeSpinner.selectedItemPosition]
            Prefs.language = langValues[langSpinner.selectedItemPosition]
            recreate()
        }
    }
}