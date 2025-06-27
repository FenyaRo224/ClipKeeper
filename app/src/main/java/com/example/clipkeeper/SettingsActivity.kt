package com.example.clipkeeper

import android.content.Context
import android.os.Bundle
import android.content.res.Resources
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import java.util.Locale
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

        val saveButton = findViewById<Button>(R.id.save_button)
        val resetButton = findViewById<Button>(R.id.reset_button)
        val backButton = findViewById<Button>(R.id.back_button)

        saveButton.setOnClickListener {
            Prefs.theme = themeValues[themeSpinner.selectedItemPosition]
            Prefs.language = langValues[langSpinner.selectedItemPosition]
            recreate()
        }

        resetButton.setOnClickListener {
            Prefs.theme = "default"
            val systemLang = Resources.getSystem().configuration.locales[0].language
            Prefs.language = systemLang
            recreate()
        }

        backButton.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }

        overridePendingTransition(0, 0)

        ThemeUtils.decorateRoyal(saveButton, resetButton, backButton)
    }

    override fun onBackPressed() {
        setResult(RESULT_OK)
        super.onBackPressed()
    }
}