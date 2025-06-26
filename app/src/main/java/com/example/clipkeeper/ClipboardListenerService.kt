package com.example.clipkeeper

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.clipkeeper.ACTION_HISTORY_UPDATED
import com.example.clipkeeper.CHANNEL_ID

/**
 * Background service that listens for clipboard changes.
 */
class ClipboardListenerService : Service() {
    private lateinit var clipboard: ClipboardManager
    private var lastText: String? = null
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkClipboard()
    }
    private val handler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            checkClipboard()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ClipboardRepository.init(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Clipboard Listener",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.service_notification))
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()
            startForeground(1, notification)
        }

        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        lastText = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
        lastText?.let {
            if (it.isNotEmpty()) {
                ClipboardRepository.add(ClipboardItem(it))
                sendUpdate()
            }
        }
        clipboard.addPrimaryClipChangedListener(clipListener)

        handler.post(pollRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        clipboard.removePrimaryClipChangedListener(clipListener)
        super.onDestroy()
    }

    private fun checkClipboard() {
        val data = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
        if (!data.isNullOrEmpty() && data != lastText) {
            lastText = data
            ClipboardRepository.add(ClipboardItem(data))
            sendUpdate()
        }
    }

    private fun sendUpdate() {
        val intent = Intent(ACTION_HISTORY_UPDATED).setPackage(packageName)
        sendBroadcast(intent)
    }
}