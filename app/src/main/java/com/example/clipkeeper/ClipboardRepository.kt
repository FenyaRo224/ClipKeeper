package com.example.clipkeeper

/**
 * Simple in-memory repository for clipboard items.
 */
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Needed for storing clipboard item types */
import com.example.clipkeeper.ClipboardType

/**
 * Simple in-memory repository for clipboard items.
 * Data is persisted to SharedPreferences so history survives process restarts.
 */
object ClipboardRepository {
    private lateinit var prefs: SharedPreferences
    private const val PREFS_NAME = "clipboard_history"
    private const val KEY_HISTORY = "items"
    private var initialized = false

    val history: MutableList<ClipboardItem> = mutableListOf()

    /** Content that was copied from within the app and should not create a new
     * history entry when detected by the service. */
    private var ignoreNext: String? = null

    /** Initialize the repository. Safe to call multiple times. */
    fun init(context: Context) {
        if (initialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        load()
        initialized = true
    }

    private fun load() {
        val json = prefs.getString(KEY_HISTORY, null) ?: return
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val type = try {
                ClipboardType.valueOf(obj.optString("type"))
            } catch (_: Exception) {
                ClipboardType.TEXT
            }
            history.add(
                ClipboardItem(
                    obj.getString("content"),
                    obj.optLong("timestamp"),
                    obj.optInt("usageCount"),
                    type
                )
            )
        }
    }

    private fun persist() {
        val array = JSONArray()
        history.forEach { item ->
            val obj = JSONObject()
            obj.put("content", item.content)
            obj.put("timestamp", item.timestamp)
            obj.put("usageCount", item.usageCount)
            obj.put("type", item.type.name)
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun add(item: ClipboardItem) {
        if (history.firstOrNull()?.content == item.content) return
        if (item.content == ignoreNext) {
            // Skip adding duplicates triggered by copying from the app
            ignoreNext = null
            return
        }
        history.add(0, item)
        persist()
    }

    fun update(index: Int, newContent: String) {
        if (index in history.indices) {
            val current = history[index]
            history[index] = current.copy(content = newContent)
            persist()
        }
    }

    fun incrementUsage(index: Int) {
        if (index in history.indices) {
            val item = history[index]
            history[index] = item.copy(usageCount = item.usageCount + 1)
            persist()
        }
    }

    fun markCopied(content: String) {
        ignoreNext = content
    }

    fun clearIgnore() {
        ignoreNext = null
    }

    fun clear() {
        history.clear()
        persist()
    }
}