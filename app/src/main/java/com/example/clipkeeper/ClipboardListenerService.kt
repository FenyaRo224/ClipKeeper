package com.example.clipkeeper

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.clipkeeper.ACTION_HISTORY_UPDATED
import com.example.clipkeeper.CHANNEL_ID

/**
 * Background service that listens for clipboard changes.
 */
class ClipboardListenerService : Service() {
    private lateinit var clipboard: ClipboardManager
    private var lastContent: String? = null
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
        clipboard.primaryClip?.let { clip ->
            if (clip.itemCount > 0) {
                parseClip(clip.getItemAt(0), clip.description)?.let { item ->
                    lastContent = item.content
                    ClipboardRepository.add(item)
                    sendUpdate()
                }
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
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val item = clip.getItemAt(0)
            val description = clip.description
            val newItem = parseClip(item, description)
            if (newItem != null && newItem.content != lastContent) {
                lastContent = newItem.content
                ClipboardRepository.add(newItem)
                sendUpdate()
                showNotification(newItem)
            }
        }
    }

    private fun sendUpdate() {
        val intent = Intent(ACTION_HISTORY_UPDATED).setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun parseClip(item: ClipData.Item, desc: ClipDescription): ClipboardItem? {
        item.uri?.let { uri ->
            val mime = desc.getMimeType(0) ?: contentResolver.getType(uri)
            val type = if (mime != null && mime.startsWith("image/")) {
                ClipboardType.IMAGE
            } else {
                ClipboardType.FILE
            }
            return ClipboardItem(uri.toString(), type = type)
        }
        val text = item.coerceToText(this)?.toString()
        return if (!text.isNullOrEmpty()) ClipboardItem(text) else null
    }

    private fun showNotification(item: ClipboardItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Clipboard Listener",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val text = when (item.type) {
            ClipboardType.TEXT -> item.content.take(40)
            ClipboardType.IMAGE -> getString(R.string.image_copied)
            ClipboardType.FILE -> getString(R.string.file_copied)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.new_clipboard_item))
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        manager.notify(2, notification)
    }
}