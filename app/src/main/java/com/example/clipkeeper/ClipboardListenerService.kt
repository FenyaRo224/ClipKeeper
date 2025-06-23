package com.example.clipkeeper

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * Background service that listens for clipboard changes.
 */
class ClipboardListenerService : Service() {
    private lateinit var clipboard: ClipboardManager
    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        val data = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this).toString()
        ClipboardRepository.add(ClipboardItem(data))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener(listener)
    }

    override fun onDestroy() {
        clipboard.removePrimaryClipChangedListener(listener)
        super.onDestroy()
    }
}