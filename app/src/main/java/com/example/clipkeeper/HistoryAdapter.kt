package com.example.clipkeeper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(
    private var items: List<ClipboardItem>,
    private val onItemClick: (Int) -> Unit,
    private val onItemLongClick: (Int) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.item_text)
        val count: TextView = view.findViewById(R.id.item_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clipboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.text.text = when (item.type) {
            ClipboardType.TEXT -> item.content
            ClipboardType.IMAGE -> "Image: ${item.content}"
            ClipboardType.FILE -> "File: ${item.content}"
        }
        holder.count.text = "Used ${item.usageCount}"
        holder.itemView.setOnClickListener { onItemClick(position) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(position)
            true
        }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<ClipboardItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}