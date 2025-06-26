package com.example.clipkeeper

/** Types of data that can appear in the clipboard. */
enum class ClipboardType { TEXT, IMAGE, FILE }

/**
 * Data class representing a single clipboard entry.
 */
data class ClipboardItem(
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    var usageCount: Int = 0,
    val type: ClipboardType = ClipboardType.TEXT
)