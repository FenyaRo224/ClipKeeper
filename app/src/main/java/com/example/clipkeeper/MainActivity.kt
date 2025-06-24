package com.example.clipkeeper

import android.app.AlertDialog
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.clipkeeper.ACTION_HISTORY_UPDATED

/**
 * Main screen showing clipboard history.
 */
class MainActivity : AppCompatActivity() {
    private val history = ClipboardRepository.history
    private lateinit var adapter: HistoryAdapter
    private lateinit var emptyView: TextView
    private lateinit var recyclerView: RecyclerView

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            adapter.notifyDataSetChanged()
            updateEmptyView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serviceIntent = Intent(this, ClipboardListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }

        adapter = HistoryAdapter(history) { index -> showEditDialog(index) }
        recyclerView = findViewById(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        emptyView = findViewById(R.id.empty_view)
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
    }

    override fun onPause() {
        unregisterReceiver(receiver)
        super.onPause()
    }

    private fun updateEmptyView() {
        emptyView.isVisible = history.isEmpty()
    }
}