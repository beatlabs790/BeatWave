package com.music.echo.utils.cipher

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File

object CipherHistoryStore {
    private const val TAG = "EchoMusic_CipherHistory"
    private const val HISTORY_FILE = "cipher_history.txt"
    
    private val _history = MutableStateFlow<List<Long>>(emptyList())
    val history: StateFlow<List<Long>> = _history.asStateFlow()
    
    @Volatile
    private var appContext: Context? = null
    
    fun initialize(context: Context) {
        appContext = context.applicationContext
        loadHistory()
    }
    
    private fun loadHistory() {
        val file = historyFile() ?: return
        try {
            if (file.exists()) {
                val lines = file.readLines()
                val timestamps = lines.mapNotNull { it.toLongOrNull() }.sortedDescending()
                _history.value = timestamps
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to load cipher history")
        }
    }
    
    fun recordUpdate(timestamp: Long) {
        val file = historyFile() ?: return
        try {
            // Append the timestamp
            file.appendText("$timestamp\n")
            // Keep up to 50 entries
            val lines = file.readLines()
            if (lines.size > 50) {
                file.writeText(lines.takeLast(50).joinToString("\n") + "\n")
            }
            
            // Reload in memory
            loadHistory()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to record cipher update")
        }
    }
    
    private fun historyFile(): File? {
        val context = appContext ?: return null
        return File(context.filesDir, HISTORY_FILE)
    }
}
