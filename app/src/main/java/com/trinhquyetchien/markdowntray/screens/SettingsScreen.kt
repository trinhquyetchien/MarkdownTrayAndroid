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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.viewmodel.AppViewModelFactory
import com.trinhquyetchien.markdowntray.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    storageManager: StorageManager,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(
        factory = AppViewModelFactory(LocalContext.current, storageManager)
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var dailyDir by remember { mutableStateOf(uiState.dailyDir) }
    var dailyAlias by remember { mutableStateOf(uiState.dailyAlias) }
    var notesRootDir by remember { mutableStateOf(uiState.notesRootDir) }
    var deadlineFile by remember { mutableStateOf(uiState.deadlineFile) }
    var deadlineAlias by remember { mutableStateOf(uiState.deadlineAlias) }
    var calendarFile by remember { mutableStateOf(uiState.calendarFile) }

    var showAddMenuDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        dailyDir = uiState.dailyDir
        dailyAlias = uiState.dailyAlias
        notesRootDir = uiState.notesRootDir
        deadlineFile = uiState.deadlineFile
        deadlineAlias = uiState.deadlineAlias
        calendarFile = uiState.calendarFile
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
            // Permission Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.hasStoragePermission) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = if (uiState.hasStoragePermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.hasStoragePermission) "Đã cấp quyền truy cập bộ nhớ" else "Chưa cấp quyền bộ nhớ ngoài",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (uiState.hasStoragePermission)
                                "Markdown Tray có toàn quyền đọc/ghi dữ liệu Syncthing."
                            else
                                "Cần quyền All Files Access để đồng bộ file ngoài ứng dụng.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!uiState.hasStoragePermission) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(storageManager.getRequestStoragePermissionIntent())
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Không mở được cài đặt quyền", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Cấp quyền")
                        }
                    }
                }
            }

            // Path Configuration Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đường dẫn thư mục & tệp (Syncthing)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Đảm bảo đường dẫn trùng khớp với cấu hình MarkdownTray trên máy tính của bạn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = dailyDir,
                        onValueChange = { dailyDir = it },
                        label = { Text("Thư mục Daily Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dailyAlias,
                        onValueChange = { dailyAlias = it },
                        label = { Text("Tên hiển thị tab Daily") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notesRootDir,
                        onValueChange = { notesRootDir = it },
                        label = { Text("Thư mục gốc ghi chú (Notes Root)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = deadlineFile,
                        onValueChange = { deadlineFile = it },
                        label = { Text("Đường dẫn tệp Deadline") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = deadlineAlias,
                        onValueChange = { deadlineAlias = it },
                        label = { Text("Tên hiển thị tab Deadline") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = calendarFile,
                        onValueChange = { calendarFile = it },
                        label = { Text("Đường dẫn tệp Calendar (calendar.json)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.resetToDefaults()
                                Toast.makeText(context, "Đã khôi phục mặc định", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mặc định")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                viewModel.savePaths(
                                    dailyDir = dailyDir,
                                    dailyAlias = dailyAlias,
                                    notesRootDir = notesRootDir,
                                    deadlineFile = deadlineFile,
                                    deadlineAlias = deadlineAlias,
                                    calendarFile = calendarFile
                                )
                                Toast.makeText(context, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lưu cấu hình", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Custom Menus Management Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lối tắt Custom Menus",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(onClick = { showAddMenuDialog = true }) {
                            Icon(Icons.Outlined.Add, contentDescription = "Thêm Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text(
                        text = "Các thư mục được ghim nhanh trên thanh công cụ duyệt ghi chú.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (uiState.customMenus.isEmpty()) {
                        Text(
                            text = "Chưa có menu nào được ghim. Bạn có thể ghim nhanh trong tab 'List' hoặc thêm tại đây.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    } else {
                        uiState.customMenus.forEach { menu ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(menu.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(menu.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.removeCustomMenu(menu.path)
                                            Toast.makeText(context, "Đã xóa ${menu.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Xóa menu", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Add Menu Shortcut
    if (showAddMenuDialog) {
        var menuName by remember { mutableStateOf("") }
        var menuPath by remember { mutableStateOf(uiState.notesRootDir) }

        AlertDialog(
            onDismissRequest = { showAddMenuDialog = false },
            title = { Text("Thêm Custom Menu", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = menuName,
                        onValueChange = { menuName = it },
                        label = { Text("Tên hiển thị menu") },
                        placeholder = { Text("Ví dụ: Dự án A") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = menuPath,
                        onValueChange = { menuPath = it },
                        label = { Text("Đường dẫn thư mục") },
                        placeholder = { Text("/storage/emulated/0/Note/DuAnA") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (menuName.isNotBlank() && menuPath.isNotBlank()) {
                            viewModel.addCustomMenu(menuName.trim(), menuPath.trim())
                            Toast.makeText(context, "Đã thêm lối tắt", Toast.LENGTH_SHORT).show()
                            showAddMenuDialog = false
                        }
                    }
                ) {
                    Text("Thêm", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMenuDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
