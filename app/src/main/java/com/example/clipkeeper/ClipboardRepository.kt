package com.example.clipkeeper

/**
 * Simple in-memory repository for clipboard items.
 */
object ClipboardRepository {
    val history: MutableList<ClipboardItem> = mutableListOf()

    fun add(item: ClipboardItem) {
        history.add(0, item)
    }

    fun update(index: Int, newContent: String) {
        if (index in history.indices) {
            val current = history[index]
            history[index] = current.copy(content = newContent)
        }
    }
}