package com.trinhquyetchien.markdowntray.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trinhquyetchien.markdowntray.data.StorageManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    storageManager: StorageManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    var selectedCalendar by remember { mutableStateOf(calendar.clone() as Calendar) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
    val todayDateStr = remember { dateFormat.format(Date()) }

    var selectedDateStr by remember { mutableStateOf(dateFormat.format(selectedCalendar.time)) }
    var rawContent by remember { mutableStateOf("") }
    var currentFile by remember { mutableStateOf(File(storageManager.dailyNotesDir, "$selectedDateStr.md")) }

    fun loadContent() {
        currentFile = File(storageManager.dailyNotesDir, "$selectedDateStr.md")
        if (currentFile.exists()) {
            rawContent = storageManager.readText(currentFile)
        } else {
            rawContent = ""
        }
    }

    LaunchedEffect(selectedDateStr, storageManager.dailyNotesDir) {
        loadContent()
    }

    val isViewingToday = selectedDateStr == todayDateStr

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = if (isViewingToday) storageManager.dailyAlias else selectedDateStr,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = displayFormat.format(selectedCalendar.time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                // Previous Day
                IconButton(onClick = {
                    selectedCalendar.add(Calendar.DAY_OF_YEAR, -1)
                    selectedCalendar = selectedCalendar.clone() as Calendar
                    selectedDateStr = dateFormat.format(selectedCalendar.time)
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Day"
                    )
                }

                // Jump to Today button if not on today
                if (!isViewingToday) {
                    IconButton(onClick = {
                        selectedCalendar = Calendar.getInstance()
                        selectedDateStr = dateFormat.format(selectedCalendar.time)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Go to Today",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Next Day
                IconButton(onClick = {
                    selectedCalendar.add(Calendar.DAY_OF_YEAR, 1)
                    selectedCalendar = selectedCalendar.clone() as Calendar
                    selectedDateStr = dateFormat.format(selectedCalendar.time)
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Day"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        MarkdownViewer(
            title = if (isViewingToday) "Today ($selectedDateStr)" else selectedDateStr,
            rawContent = rawContent,
            onToggleTask = { lineIndex, isChecked ->
                val success = storageManager.toggleTask(currentFile, lineIndex, isChecked)
                if (success) {
                    loadContent()
                } else {
                    Toast.makeText(context, "Failed to update task", Toast.LENGTH_SHORT).show()
                }
            },
            onAddTask = { taskText ->
                val success = storageManager.appendTask(currentFile, taskText)
                if (success) {
                    loadContent()
                    Toast.makeText(context, "Task added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to add task", Toast.LENGTH_SHORT).show()
                }
            },
            onSaveRawContent = { newContent ->
                val success = storageManager.writeText(currentFile, newContent)
                if (success) {
                    loadContent()
                    Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save note", Toast.LENGTH_SHORT).show()
                }
            },
            onRefresh = {
                loadContent()
                Toast.makeText(context, "Refreshed", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.weight(1f)
        )
    }
}
