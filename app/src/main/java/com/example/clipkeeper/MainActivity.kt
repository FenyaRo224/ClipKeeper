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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.provider.Settings
import android.text.TextUtils
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName

import com.example.clipkeeper.ACTION_HISTORY_UPDATED

/**
 * Main screen showing clipboard history.
 */
class MainActivity : AppCompatActivity() {
    private val history = ClipboardRepository.history
    private lateinit var adapter: HistoryAdapter
    private lateinit var emptyView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var refreshButton: Button

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

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            adapter.notifyDataSetChanged()
            updateEmptyView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load any previously saved clipboard history
        ClipboardRepository.init(applicationContext)

        if (!isAccessibilityEnabled()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.enable_service_title)
                .setMessage(R.string.enable_service_message)
                .setPositiveButton(R.string.open_settings) { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton(android.R.string.cancel, null)
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
            history,
            onItemClick = { index -> copyItem(index) },
            onItemLongClick = { index -> showEditDialog(index) }
        )
        recyclerView = findViewById(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        emptyView = findViewById(R.id.empty_view)
        refreshButton = findViewById(R.id.refresh_button)
        refreshButton.setOnClickListener { refreshHistory() }
        updateEmptyView()
    }

    private fun showEditDialog(index: Int) {
        val item = history[index]
        val input = EditText(this).apply { setText(item.content) }

        AlertDialog.Builder(this)
            .setTitle("Edit item")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                ClipboardRepository.update(index, input.text.toString())
                adapter.notifyItemChanged(index)
            }
            .setNegativeButton("Cancel", null)
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
        adapter.notifyDataSetChanged()
        updateEmptyView()
        startClipboardService()
    }

    override fun onPause() {
        unregisterReceiver(receiver)
        super.onPause()
    }

    private fun updateEmptyView() {
        emptyView.isVisible = history.isEmpty()
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
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("copied", item.content)
        clipboard.setPrimaryClip(clip)
        ClipboardRepository.incrementUsage(index)
        adapter.notifyItemChanged(index)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun refreshHistory() {
        adapter.notifyDataSetChanged()
        updateEmptyView()
    }
}