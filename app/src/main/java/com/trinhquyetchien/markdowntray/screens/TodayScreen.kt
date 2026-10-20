package com.trinhquyetchien.markdowntray.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.data.SyncStatusInfo
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
    val displayFormat = remember { SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN")) }
    val todayDateStr = remember { dateFormat.format(Date()) }

    var selectedDateStr by remember { mutableStateOf(dateFormat.format(selectedCalendar.time)) }
    var rawContent by remember { mutableStateOf("") }
    var currentFile by remember { mutableStateOf(File(storageManager.dailyNotesDir, "$selectedDateStr.md")) }
    var syncInfo by remember { mutableStateOf<SyncStatusInfo?>(null) }

    fun loadContent() {
        currentFile = File(storageManager.dailyNotesDir, "$selectedDateStr.md")
        if (selectedDateStr == todayDateStr && (!currentFile.exists() || currentFile.length() == 0L)) {
            // Tự động khởi tạo file ngày hôm nay nếu chưa có
            currentFile = storageManager.ensureTodayFileExists()
        }

        rawContent = if (currentFile.exists()) {
            storageManager.readText(currentFile)
        } else {
            ""
        }
        syncInfo = storageManager.getFileSyncInfo(currentFile)
    }

    LaunchedEffect(selectedDateStr, storageManager.dailyNotesDir) {
        loadContent()
    }

    // Auto-reload via DirectoryObserver when Syncthing syncs files into the daily folder
    DisposableEffect(storageManager.dailyNotesDir) {
        val dir = File(storageManager.dailyNotesDir)
        val observer = storageManager.createDirectoryObserver(dir) { changedPath ->
            if (changedPath == null || changedPath == "$selectedDateStr.md") {
                loadContent()
            }
        }
        observer.start()
        onDispose {
            observer.stop()
        }
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
                        text = displayFormat.format(selectedCalendar.time).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                // Hôm trước
                IconButton(onClick = {
                    selectedCalendar.add(Calendar.DAY_OF_YEAR, -1)
                    selectedCalendar = selectedCalendar.clone() as Calendar
                    selectedDateStr = dateFormat.format(selectedCalendar.time)
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Ngày trước"
                    )
                }

                // Nút quay về Hôm nay nếu đang xem ngày khác
                if (!isViewingToday) {
                    IconButton(onClick = {
                        selectedCalendar = Calendar.getInstance()
                        selectedDateStr = dateFormat.format(selectedCalendar.time)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Hôm nay",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Hôm sau
                IconButton(onClick = {
                    selectedCalendar.add(Calendar.DAY_OF_YEAR, 1)
                    selectedCalendar = selectedCalendar.clone() as Calendar
                    selectedDateStr = dateFormat.format(selectedCalendar.time)
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Ngày sau"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        MarkdownViewer(
            title = if (isViewingToday) "Hôm nay ($selectedDateStr)" else selectedDateStr,
            rawContent = rawContent,
            syncInfo = syncInfo,
            onToggleTask = { lineIndex, isChecked ->
                val success = storageManager.toggleTask(currentFile, lineIndex, isChecked)
                if (success) {
                    loadContent()
                } else {
                    Toast.makeText(context, "Không thể cập nhật task", Toast.LENGTH_SHORT).show()
                }
            },
            onAddTask = { taskText ->
                val success = storageManager.appendTask(currentFile, taskText)
                if (success) {
                    loadContent()
                    Toast.makeText(context, "Đã thêm công việc", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi thêm task", Toast.LENGTH_SHORT).show()
                }
            },
            onSaveRawContent = { newContent ->
                val success = storageManager.writeText(currentFile, newContent)
                if (success) {
                    loadContent()
                    Toast.makeText(context, "Đã lưu ghi chú", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi lưu ghi chú", Toast.LENGTH_SHORT).show()
                }
            },
            onRefresh = {
                loadContent()
                Toast.makeText(context, "Đã kiểm tra đồng bộ", Toast.LENGTH_SHORT).show()
            },
            onDeleteTask = { lineIndex ->
                val success = storageManager.deleteTask(currentFile, lineIndex)
                if (success) {
                    loadContent()
                    Toast.makeText(context, "Đã xóa nhiệm vụ", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Không thể xóa nhiệm vụ", Toast.LENGTH_SHORT).show()
                }
            },
            onUpdateTaskText = { lineIndex, newText ->
                val success = storageManager.updateTaskText(currentFile, lineIndex, newText)
                if (success) {
                    loadContent()
                    Toast.makeText(context, "Đã cập nhật nhiệm vụ", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Không thể cập nhật nhiệm vụ", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}
