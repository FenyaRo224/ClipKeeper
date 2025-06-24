package com.example.clipkeeper

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.clipkeeper.ACTION_HISTORY_UPDATED
import com.example.clipkeeper.CHANNEL_ID

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
        // Prepare a foreground notification so the service can access the clipboard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Clipboard Listener",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            // Display a small message so the user knows the service is active
            .setContentText(getString(R.string.service_notification))
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
        startForeground(1, notification)

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Keep the service alive so clipboard updates continue to be received
        return START_STICKY
    }

    override fun onDestroy() {
        clipboard.removePrimaryClipChangedListener(listener)
        super.onDestroy()
    }
}