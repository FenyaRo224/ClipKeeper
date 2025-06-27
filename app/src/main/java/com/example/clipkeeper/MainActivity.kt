package com.example.clipkeeper

import android.app.AlertDialog
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.provider.Settings
import android.text.TextUtils
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.view.View
import com.example.clipkeeper.Prefs
import com.example.clipkeeper.ThemeUtils
import com.example.clipkeeper.LanguageUtils

import com.example.clipkeeper.ACTION_HISTORY_UPDATED

/**
 * Main screen showing clipboard history.
 */
class MainActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        Prefs.init(newBase)
        val ctx = LanguageUtils.applyBaseContext(newBase)
        super.attachBaseContext(ctx)
    }
    private val history = ClipboardRepository.history
    private lateinit var adapter: HistoryAdapter
    private lateinit var emptyView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var clearButton: Button
    private lateinit var reportButton: Button
    private lateinit var settingsButton: Button
    private lateinit var searchView: SearchView
    private var filtered: List<ClipboardItem> = history
    private lateinit var clipboard: ClipboardManager
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        handleClipboard()
    }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    R.string.notification_permission_required,
                    Toast.LENGTH_LONG
                ).show()
            }
            // Start service regardless so clipboard history still works
            startClipboardService()
        }

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Recreate activity to apply new language and theme
            recreate()
        }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            filter(searchView.query?.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load any previously saved clipboard history
        ClipboardRepository.init(applicationContext)

        if (!isAccessibilityEnabled()) {
            AlertDialog.Builder(this)
                .setTitle(ThemeUtils.withRoyal(getString(R.string.enable_service_title)))
                .setMessage(ThemeUtils.withRoyal(getString(R.string.enable_service_message)))
                .setPositiveButton(ThemeUtils.withRoyal(getString(R.string.open_settings))) { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton(ThemeUtils.withRoyal(getString(android.R.string.cancel)), null)
                .show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startClipboardService()
        }

        adapter = HistoryAdapter(
            filtered,
            onItemClick = { index -> copyItem(index) },
            onItemLongClick = { index -> showEditDialog(index) }
        )
        recyclerView = findViewById(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        emptyView = findViewById(R.id.empty_view)
        searchView = findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filter(newText)
                return true
            }
        })
        clearButton = findViewById(R.id.clear_button)
        clearButton.setOnClickListener {
            ClipboardRepository.clear()
            filter(searchView.query?.toString())
            updateEmptyView()
        }
        reportButton = findViewById(R.id.report_button)
        reportButton.setOnClickListener { showUsageReport() }
        settingsButton = findViewById(R.id.settings_button)
        settingsButton.setOnClickListener {
            settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        ThemeUtils.decorateRoyal(clearButton, reportButton, settingsButton)
        updateEmptyView()
    }

    private fun showEditDialog(index: Int) {
        val item = history[index]
        val input = EditText(this).apply { setText(item.content) }

        AlertDialog.Builder(this)
            .setTitle(ThemeUtils.withRoyal("Edit item"))
            .setView(input)
            .setPositiveButton(ThemeUtils.withRoyal("Save")) { _, _ ->
                ClipboardRepository.update(index, input.text.toString())
                adapter.notifyItemChanged(index)
            }
            .setNegativeButton(ThemeUtils.withRoyal("Cancel"), null)
            .show()
    }

    private fun startClipboardService() {
        val serviceIntent = Intent(this, ClipboardListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.RECEIVER_NOT_EXPORTED
        } else {
            0
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(ACTION_HISTORY_UPDATED),
            flags
        )
        clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener(clipboardListener)
        handleClipboard()
        filter(searchView.query?.toString())
        updateEmptyView()
        startClipboardService()
    }

    override fun onPause() {
        clipboard.removePrimaryClipChangedListener(clipboardListener)
        unregisterReceiver(receiver)
        super.onPause()
    }

    private fun updateEmptyView() {
        emptyView.isVisible = filtered.isEmpty()
    }

    private fun isAccessibilityEnabled(): Boolean {
        val component = ComponentName(this, ClipboardAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any { TextUtils.equals(it, component.flattenToString()) }
    }

    private fun copyItem(index: Int) {
        val item = history[index]
        ClipboardRepository.markCopied(item.content)
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("copied", item.content)
        clipboard.setPrimaryClip(clip)
        ClipboardRepository.incrementUsage(index)
        adapter.notifyItemChanged(index)
        val text = ThemeUtils.withRoyal(getString(R.string.copied_to_clipboard))
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun filter(query: String?) {
        filtered = if (query.isNullOrEmpty()) {
            history
        } else {
            history.filter { it.content.contains(query, true) }
        }
        adapter.update(filtered)
        updateEmptyView()
    }

    private fun handleClipboard() {
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(this)?.toString()
            if (!text.isNullOrBlank()) {
                ClipboardRepository.add(ClipboardItem(text))
                filter(searchView.query?.toString())
            }
        }
    }

    private fun showUsageReport() {
        if (history.isEmpty()) {
            val msg = ThemeUtils.withRoyal(getString(R.string.no_usage_data))
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            return
        }
        val usage = mutableMapOf<String, Int>()
        history.forEach { item ->
            val key = item.content
            usage[key] = (usage[key] ?: 0) + item.usageCount
        }
        val report = usage.entries.joinToString("\n") {
            "${it.key.take(20)}: ${it.value}"
        }
        val title = ThemeUtils.withRoyal(getString(R.string.usage_report))
        val decoratedReport = ThemeUtils.withRoyal(report)
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(decoratedReport)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

}