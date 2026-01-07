package com.example.logviewer

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel : ViewModel() {

    private val _rawLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedLevels = MutableStateFlow(setOf<String>()) // Empty means all
    private val _isStreaming = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private var streamingJob: Job? = null

    val searchQuery: StateFlow<String> = _searchQuery
    val selectedLevels: StateFlow<Set<String>> = _selectedLevels
    val isStreaming: StateFlow<Boolean> = _isStreaming
    val isLoading: StateFlow<Boolean> = _isLoading

    val filteredLogs: StateFlow<List<LogEntry>> = combine(_rawLogs, _searchQuery, _selectedLevels) { logs, query, levels ->
        logs.filter { entry ->
            (levels.isEmpty() || levels.contains(entry.level)) &&
                    (query.isBlank() ||
                            entry.tag.contains(query, ignoreCase = true) ||
                            entry.message.contains(query, ignoreCase = true) ||
                            entry.level.equals(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadDumpLogs()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleLevel(level: String) {
        val current = _selectedLevels.value.toMutableSet()
        if (current.contains(level)) current.remove(level) else current.add(level)
        _selectedLevels.value = current
    }

    fun clearLevels() {
        _selectedLevels.value = emptySet()
    }

    fun toggleStreaming() {
        val newStreaming = !_isStreaming.value
        _isStreaming.value = newStreaming
        if (newStreaming) {
            startStreaming()
        } else {
            stopStreaming()
            loadDumpLogs() // Refresh to snapshot on stop
        }
    }

    private fun startStreaming() {
        stopStreaming() // Ensure no duplicate jobs
        _rawLogs.value = emptyList() // Clear for fresh stream
        streamingJob = viewModelScope.launch {
            LogReader.readLiveLogs().collect { entry ->
                _rawLogs.value = _rawLogs.value + entry
            }
        }
    }

    private fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null
    }

    fun loadDumpLogs() {
        if (_isStreaming.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _rawLogs.value = LogReader.readDumpLogs()
            } catch (e: SecurityException) {
                // Handle permission
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportLogs(context: Context): Intent? {
        val logs = filteredLogs.value
        if (logs.isEmpty()) return null

        val exportText = logs.joinToString(separator = "\n") { "${it.timestamp} ${it.level}/${it.tag}: ${it.message}" }
        val file = File(context.cacheDir, "logs.txt")
        file.writeText(exportText)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Share Logs")
    }
}
