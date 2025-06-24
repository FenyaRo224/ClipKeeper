package com.example.clipkeeper

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.clipkeeper.ACTION_HISTORY_UPDATED

/**
 * Background service that listens for clipboard changes.
 */
class ClipboardListenerService : Service() {
    private lateinit var clipboard: ClipboardManager
    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboard.primaryClip
        val data = clip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (data.isNotEmpty()) {
            ClipboardRepository.add(ClipboardItem(data))
            sendBroadcast(Intent(ACTION_HISTORY_UPDATED))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener(listener)
        // If the user already has text copied before the service starts, store it
        val clip = clipboard.primaryClip
        val data = clip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (data.isNotEmpty()) {
            ClipboardRepository.add(ClipboardItem(data))
            sendBroadcast(Intent(ACTION_HISTORY_UPDATED))
        }
    }

    override fun onDestroy() {
        clipboard.removePrimaryClipChangedListener(listener)
        super.onDestroy()
    }
}