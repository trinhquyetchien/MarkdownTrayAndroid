package com.trinhquyetchien.markdowntray.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    storageManager: StorageManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(storageManager.hasStoragePermission()) }

    var dailyDir by remember { mutableStateOf(storageManager.dailyNotesDir) }
    var dailyAlias by remember { mutableStateOf(storageManager.dailyAlias) }

    var notesRootDir by remember { mutableStateOf(storageManager.notesRootDir) }

    var deadlineFile by remember { mutableStateOf(storageManager.deadlineFilePath) }
    var deadlineAlias by remember { mutableStateOf(storageManager.deadlineAlias) }

    var calendarFile by remember { mutableStateOf(storageManager.calendarFilePath) }

    LaunchedEffect(Unit) {
        hasPermission = storageManager.hasStoragePermission()
    }

    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Cài đặt & Đồng bộ",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Permission Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasPermission) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasPermission) "Quyền truy cập bộ nhớ: Đã cấp" else "Cần cấp quyền truy cập bộ nhớ",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (hasPermission) {
                                    "App có thể đọc và ghi trực tiếp vào thư mục Syncthing theo thời gian thực."
                                } else {
                                    "Vui lòng cấp quyền 'All files access' để app có thể chỉnh sửa ghi chú đồng bộ."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!hasPermission) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(storageManager.getRequestStoragePermissionIntent())
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Không thể mở cài đặt: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cấp quyền truy cập tất cả tệp")
                        }
                    }
                }
            }

            // Syncthing Live Diagnostics
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trạng thái đồng bộ Syncthing",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text = "App tích hợp sẵn DirectoryObserver để tự động cập nhật ngay khi Syncthing tải file mới về từ PC.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val todayInfo = storageManager.getFileSyncInfo(storageManager.getTodayFile())
                    val deadlineInfo = storageManager.getFileSyncInfo(storageManager.getDeadlineFile())
                    val calInfo = storageManager.getFileSyncInfo(storageManager.getCalendarFile())

                    Text(
                        text = "• Hôm nay: ${todayInfo.statusText}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Deadline: ${deadlineInfo.statusText}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Lịch tuần: ${calInfo.statusText}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Notes Root Directory (For 5th Tab "List")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderShared, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thư mục gốc ghi chú (Menu List)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedTextField(
                        value = notesRootDir,
                        onValueChange = { notesRootDir = it },
                        label = { Text("Đường dẫn thư mục ghi chú gốc") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val exists = File(notesRootDir).isDirectory
                    Text(
                        text = if (exists) "✓ Thư mục tồn tại" else "⚠ Chưa có thư mục (sẽ tự tạo khi cần)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (exists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Daily Notes Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cấu hình Daily Notes",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedTextField(
                        value = dailyAlias,
                        onValueChange = { dailyAlias = it },
                        label = { Text("Tên hiển thị (Alias)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dailyDir,
                        onValueChange = { dailyDir = it },
                        label = { Text("Thư mục Daily Notes") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val dirExists = File(dailyDir).isDirectory
                    Text(
                        text = if (dirExists) "✓ Thư mục tồn tại" else "⚠ Chưa có thư mục",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dirExists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Deadline Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cấu hình Deadline",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedTextField(
                        value = deadlineAlias,
                        onValueChange = { deadlineAlias = it },
                        label = { Text("Tên hiển thị (Alias)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = deadlineFile,
                        onValueChange = { deadlineFile = it },
                        label = { Text("Đường dẫn file Deadline (.md)") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val fileExists = File(deadlineFile).isFile
                    Text(
                        text = if (fileExists) "✓ File tồn tại" else "⚠ File chưa tồn tại (sẽ tự tạo)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (fileExists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Calendar Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cấu hình Lịch biểu (Calendar)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedTextField(
                        value = calendarFile,
                        onValueChange = { calendarFile = it },
                        label = { Text("Đường dẫn file calendar.json") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val calExists = File(calendarFile).isFile
                    Text(
                        text = if (calExists) "✓ File calendar.json tồn tại" else "⚠ File chưa tồn tại",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (calExists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Save & Reset Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        dailyDir = StorageManager.DEFAULT_DAILY_DIR
                        dailyAlias = "Daily List"
                        notesRootDir = StorageManager.DEFAULT_NOTES_ROOT_DIR
                        deadlineFile = StorageManager.DEFAULT_DEADLINE_FILE
                        deadlineAlias = "Deadline"
                        calendarFile = StorageManager.DEFAULT_CALENDAR_FILE

                        storageManager.dailyNotesDir = dailyDir
                        storageManager.dailyAlias = dailyAlias
                        storageManager.notesRootDir = notesRootDir
                        storageManager.deadlineFilePath = deadlineFile
                        storageManager.deadlineAlias = deadlineAlias
                        storageManager.calendarFilePath = calendarFile

                        Toast.makeText(context, "Đã khôi phục mặc định", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mặc định")
                }

                Button(
                    onClick = {
                        storageManager.dailyNotesDir = dailyDir.trim()
                        storageManager.dailyAlias = dailyAlias.trim().ifEmpty { "Daily List" }
                        storageManager.notesRootDir = notesRootDir.trim().ifEmpty { StorageManager.DEFAULT_NOTES_ROOT_DIR }
                        storageManager.deadlineFilePath = deadlineFile.trim()
                        storageManager.deadlineAlias = deadlineAlias.trim().ifEmpty { "Deadline" }
                        storageManager.calendarFilePath = calendarFile.trim()

                        Toast.makeText(context, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lưu")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
