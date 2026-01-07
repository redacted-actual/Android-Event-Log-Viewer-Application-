package com.example.logviewer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object LogReader {

    private val LOG_PATTERN = Regex("""(\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([A-Z])\s+([^:]+):\s+(.*)""")

    suspend fun readDumpLogs(): List<LogEntry> = withContext(Dispatchers.IO) {
        val logs = mutableListOf<LogEntry>()
        try {
            val process = Runtime.getRuntime().exec("logcat -d -v threadtime")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            parseLogs(reader, logs)
            reader.close()
        } catch (e: Exception) {
            logs.add(LogEntry(0, "", "E", "APP_ERROR", "Failed to read logs: ${e.message}"))
        }
        logs.reversed()
    }

    fun readLiveLogs(): Flow<LogEntry> = flow {
        try {
            val process = Runtime.getRuntime().exec("logcat -v threadtime")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val channel = Channel<LogEntry>(Channel.UNLIMITED)
            var idCounter = 0
            while (true) {
                val line = reader.readLine() ?: break
                val match = LOG_PATTERN.find(line)
                if (match != null) {
                    val (time, _, _, level, tag, msg) = match.destructured
                    val entry = LogEntry(idCounter++, time, level, tag.trim(), msg)
                    emit(entry)
                }
            }
            reader.close()
        } catch (e: Exception) {
            emit(LogEntry(0, "", "E", "APP_ERROR", "Streaming failed: ${e.message}"))
        }
    }

    private fun parseLogs(reader: BufferedReader, logs: MutableList<LogEntry>) {
        var line: String?
        var idCounter = 0
        while (reader.readLine().also { line = it } != null) {
            line?.let { rawLine ->
                val match = LOG_PATTERN.find(rawLine)
                if (match != null) {
                    val (time, _, _, level, tag, msg) = match.destructured
                    logs.add(LogEntry(idCounter++, time, level, tag.trim(), msg))
                }
            }
        }
    }
}
