package com.example.logviewer

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LogScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(viewModel: MainViewModel) {
    val logs by viewModel.filteredLogs.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val selectedLevels by viewModel.selectedLevels.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Auto-scroll in streaming mode
    LaunchedEffect(logs.size) {
        if (isStreaming) listState.animateScrollToItem(logs.size - 1)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Event Log Viewer") },
                    actions = {
                        IconButton(onClick = { viewModel.toggleStreaming() }) {
                            Text(if (isStreaming) "Pause" else "Live")
                        }
                        IconButton(onClick = {
                            val intent = viewModel.exportLogs(context)
                            if (intent != null) context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Share, "Share")
                        }
                        if (!isStreaming) {
                            Button(onClick = { viewModel.loadDumpLogs() }) {
                                Text("Refresh")
                            }
                        }
                    }
                )
                SearchBar(query, viewModel::onSearchQueryChanged)
                LevelFilterChips(selectedLevels, viewModel::toggleLevel, viewModel::clearLevels)
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (logs.isEmpty() && !isLoading) {
                if (query.isNotEmpty() || selectedLevels.isNotEmpty()) {
                    Text("No matching logs", Modifier.align(Alignment.Center))
                } else if (ContextCompat.checkSelfPermission(LocalContext.current, "android.permission.READ_LOGS") != PackageManager.PERMISSION_GRANTED) {
                    PermissionInstruction()
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    items(logs) { log ->
                        LogItem(log, query)
                        Divider()
                    }
                }
            }
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChanged: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        placeholder = { Text("Search tag, message, level...") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onQueryChanged("") }) { Icon(Icons.Default.Close, null) }
        },
        colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
    )
}

@Composable
fun LevelFilterChips(selected: Set<String>, onToggle: (String) -> Unit, onClear: () -> Unit) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(8.dp)) {
        FilterChip(selected = selected.isEmpty(), onClick = onClear, label = { Text("All") })
        listOf("V", "D", "I", "W", "E").forEach { level ->
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = selected.contains(level), onClick = { onToggle(level) }, label = { Text(level) })
        }
    }
}

@Composable
fun LogItem(log: LogEntry, query: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(Modifier.width(4.dp).height(40.dp).background(log.getLevelColor()))
        Spacer(Modifier.width(8.dp))
        Column {
            Row {
                Text(log.timestamp, fontSize = 10.sp, color = Color.Gray)
                Spacer(Modifier.width(8.dp))
                Text(log.level, fontWeight = FontWeight.Bold, color = log.getLevelColor())
                Spacer(Modifier.width(8.dp))
                Text(text = log.getHighlightedTag(query), fontWeight = FontWeight.Bold)
            }
            Text(text = log.getHighlightedMessage(query), fontFamily = FontFamily.Monospace, maxLines = 10)
        }
    }
}

@Composable
fun PermissionInstruction() {
    Column(Modifier.padding(16.dp)) {
        Text("Permission Required", style = MaterialTheme.typography.headlineSmall)
        Text("Run: adb shell pm grant com.example.logviewer android.permission.READ_LOGS")
    }
}
