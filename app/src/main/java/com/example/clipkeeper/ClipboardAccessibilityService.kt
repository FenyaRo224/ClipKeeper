package com.example.clipkeeper

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

/**
 * Accessibility service to monitor clipboard changes even when the app is not in the foreground.
 */
class ClipboardAccessibilityService : AccessibilityService() {
    private lateinit var clipboard: ClipboardManager

    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboard.primaryClip
        val data = clip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (data.isNotEmpty()) {
            ClipboardRepository.add(ClipboardItem(data))
            sendUpdate()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ClipboardRepository.init(applicationContext)
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener(listener)

        // Save any existing clipboard content
        val clip = clipboard.primaryClip
        val data = clip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (data.isNotEmpty()) {
            ClipboardRepository.add(ClipboardItem(data))
            sendUpdate()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used
    }

    override fun onInterrupt() {
        // Nothing to do
    }

    override fun onDestroy() {
        clipboard.removePrimaryClipChangedListener(listener)
        super.onDestroy()
    }

    private fun sendUpdate() {
        val intent = Intent(ACTION_HISTORY_UPDATED).setPackage(packageName)
        sendBroadcast(intent)
    }
}
