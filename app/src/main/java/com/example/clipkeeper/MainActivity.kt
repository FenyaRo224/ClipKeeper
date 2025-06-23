package com.example.clipkeeper

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Main screen showing clipboard history.
 */
class MainActivity : AppCompatActivity() {
    private val history = ClipboardRepository.history
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startService(Intent(this, ClipboardListenerService::class.java))

        adapter = HistoryAdapter(history) { index -> showEditDialog(index) }
        val recyclerView = findViewById<RecyclerView>(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
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
}