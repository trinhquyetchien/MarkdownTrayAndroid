package com.trinhquyetchien.markdowntray.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.trinhquyetchien.markdowntray.data.FilePathUtils
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

    var notesRootDir by remember { mutableStateOf(uiState.notesRootDir) }
    var dailyDir by remember { mutableStateOf(uiState.dailyDir) }
    var deadlineFile by remember { mutableStateOf(uiState.deadlineFile) }
    var calendarFile by remember { mutableStateOf(uiState.calendarFile) }

    LaunchedEffect(uiState) {
        notesRootDir = uiState.notesRootDir
        dailyDir = uiState.dailyDir
        deadlineFile = uiState.deadlineFile
        calendarFile = uiState.calendarFile
    }

    // Launchers chọn thư mục
    val notesRootFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = FilePathUtils.getAbsolutePathFromUri(context, uri)
            if (path != null) {
                notesRootDir = path
                viewModel.setNotesRootDir(path)
                Toast.makeText(context, "Đã chọn thư mục Note", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val dailyFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = FilePathUtils.getAbsolutePathFromUri(context, uri)
            if (path != null) {
                dailyDir = path
                viewModel.setDailyDir(path)
                Toast.makeText(context, "Đã chọn thư mục Daily", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launchers chọn tệp (Deadline.md & calendar.json)
    val deadlineFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = FilePathUtils.getAbsolutePathFromUri(context, uri)
            if (path != null) {
                deadlineFile = path
                viewModel.setDeadlineFile(path)
                Toast.makeText(context, "Đã chọn tệp Deadline", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val calendarFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = FilePathUtils.getAbsolutePathFromUri(context, uri)
            if (path != null) {
                calendarFile = path
                viewModel.setCalendarFile(path)
                Toast.makeText(context, "Đã chọn tệp Calendar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Cài đặt",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            actions = {
                IconButton(onClick = {
                    viewModel.refreshPermission()
                    Toast.makeText(context, "Đã làm mới trạng thái", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = "Đồng bộ / Làm mới",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
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
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.hasStoragePermission) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = if (uiState.hasStoragePermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.hasStoragePermission) "Đã cấp quyền bộ nhớ" else "Chưa cấp quyền bộ nhớ",
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
                            shape = RoundedCornerShape(12.dp),
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

            // Path Configuration Card with Folder & File Pickers
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đường dẫn & Syncthing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 1. Thư mục Note chính
                    OutlinedTextField(
                        value = notesRootDir,
                        onValueChange = { notesRootDir = it },
                        label = { Text("Thư mục Note chính") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            IconButton(onClick = { notesRootFolderLauncher.launch(null) }) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = "Duyệt thư mục", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )

                    // 2. Thư mục Daily
                    OutlinedTextField(
                        value = dailyDir,
                        onValueChange = { dailyDir = it },
                        label = { Text("Thư mục Daily") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            IconButton(onClick = { dailyFolderLauncher.launch(null) }) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = "Duyệt thư mục", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )

                    // 3. Tệp Deadline.md
                    OutlinedTextField(
                        value = deadlineFile,
                        onValueChange = { deadlineFile = it },
                        label = { Text("Tệp Deadline") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            IconButton(onClick = { deadlineFileLauncher.launch(arrayOf("text/markdown", "text/plain", "*/*")) }) {
                                Icon(Icons.Rounded.InsertDriveFile, contentDescription = "Chọn tệp", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )

                    // 4. Tệp calendar.json
                    OutlinedTextField(
                        value = calendarFile,
                        onValueChange = { calendarFile = it },
                        label = { Text("Tệp Lịch biểu (calendar.json)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            IconButton(onClick = { calendarFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                                Icon(Icons.Rounded.InsertDriveFile, contentDescription = "Chọn tệp", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )

                    // Action Buttons (Save & Reset)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.resetToDefaults()
                                Toast.makeText(context, "Đã đặt lại đường dẫn mặc định", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mặc định")
                        }

                        Button(
                            onClick = {
                                viewModel.savePaths(
                                    notesRootDir = notesRootDir,
                                    dailyDir = dailyDir,
                                    deadlineFile = deadlineFile,
                                    calendarFile = calendarFile
                                )
                                Toast.makeText(context, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lưu")
                        }
                    }
                }
            }
        }
    }
}
