package com.example.clipkeeper

/**
 * Data class representing a single clipboard entry.
 */
data class ClipboardItem(
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    var usageCount: Int = 0
)

