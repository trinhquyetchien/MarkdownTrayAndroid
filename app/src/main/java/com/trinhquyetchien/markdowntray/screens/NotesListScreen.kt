package com.trinhquyetchien.markdowntray.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.viewmodel.AppViewModelFactory
import com.trinhquyetchien.markdowntray.viewmodel.NotesListViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    storageManager: StorageManager,
    modifier: Modifier = Modifier,
    viewModel: NotesListViewModel = viewModel(
        factory = AppViewModelFactory(LocalContext.current, storageManager)
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Handle Back Press to navigate up directories if inside subfolder
    BackHandler(enabled = uiState.currentFolder.absolutePath != uiState.rootFolder.absolutePath) {
        viewModel.navigateUp()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (uiState.currentFolder.absolutePath == uiState.rootFolder.absolutePath) "Thư mục Note chính"
                            else uiState.currentFolder.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = uiState.currentFolder.absolutePath.replace(StorageManager.DEFAULT_STORAGE_ROOT, "~"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (uiState.currentFolder.absolutePath != uiState.rootFolder.absolutePath) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // New Folder Button
                    IconButton(onClick = { viewModel.setShowNewFolderDialog(true) }) {
                        Icon(
                            imageVector = Icons.Rounded.CreateNewFolder,
                            contentDescription = "Tạo thư mục",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowNewFileDialog(true) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Tạo ghi chú mới")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Tìm kiếm ghi chú...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Xóa tìm kiếm", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // Thanh duyệt nhanh các thư mục có trong thư mục chính (Note)
            if (uiState.subfolders.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chip thư mục gốc
                    val isRoot = uiState.currentFolder.absolutePath == uiState.rootFolder.absolutePath
                    AssistChip(
                        onClick = { viewModel.navigateToFolder(uiState.rootFolder) },
                        label = { Text("Tất cả", fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.FolderSpecial,
                                contentDescription = null,
                                tint = if (isRoot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isRoot) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = if (isRoot) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    )

                    // Các thư mục con có trong thư mục chính Note (Daily, Deadline, Projects,...)
                    uiState.subfolders.forEach { dir ->
                        val isCurrent = uiState.currentFolder.absolutePath == dir.absolutePath
                        AssistChip(
                            onClick = { viewModel.navigateToFolder(dir) },
                            label = { Text(dir.name, fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal) },
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            // File & Folder List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (uiState.filesAndDirs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (uiState.searchQuery.isEmpty()) "Thư mục trống" else "Không tìm thấy kết quả phù hợp",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.filesAndDirs, key = { it.absolutePath }) { file ->
                        FileOrFolderItem(
                            file = file,
                            storageManager = storageManager,
                            onClick = {
                                if (file.isDirectory) {
                                    viewModel.navigateToFolder(file)
                                } else {
                                    viewModel.openNote(file)
                                }
                            },
                            onRename = { viewModel.setTargetItemForRename(file) },
                            onDelete = { viewModel.setTargetItemForDelete(file) }
                        )
                    }
                }
            }
        }
    }

    // Modal Viewer Dialog for Active Note
    if (uiState.activeNoteFile != null) {
        val file = uiState.activeNoteFile!!
        Dialog(
            onDismissRequest = { viewModel.closeActiveNote() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(file.nameWithoutExtension, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.closeActiveNote() }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { dialogPadding ->
                MarkdownViewer(
                    title = file.nameWithoutExtension,
                    rawContent = uiState.activeNoteContent,
                    syncInfo = uiState.activeNoteSyncInfo,
                    onToggleTask = { lineIndex, isChecked ->
                        viewModel.toggleTask(lineIndex, isChecked)
                    },
                    onAddTask = { taskText ->
                        viewModel.addTask(taskText)
                    },
                    onSaveRawContent = { newContent ->
                        val success = viewModel.saveRawContent(newContent)
                        if (success) {
                            Toast.makeText(context, "Đã lưu ghi chú", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRefresh = {
                        viewModel.refreshActiveNote()
                        Toast.makeText(context, "Đã làm mới ghi chú", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteTask = { lineIndex ->
                        viewModel.deleteTask(lineIndex)
                    },
                    onUpdateTaskText = { lineIndex, newText ->
                        viewModel.updateTaskText(lineIndex, newText)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dialogPadding)
                )
            }
        }
    }

    // Dialog Create New Note
    if (uiState.showNewFileDialog) {
        var noteTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.setShowNewFileDialog(false) },
            title = { Text("Tạo ghi chú mới", fontWeight = FontWeight.SemiBold) },
            shape = RoundedCornerShape(20.dp),
            text = {
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    label = { Text("Tên ghi chú") },
                    placeholder = { Text("Ví dụ: KeHoachTuan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (noteTitle.isNotBlank()) {
                            val success = viewModel.createNote(noteTitle.trim())
                            if (!success) {
                                Toast.makeText(context, "Lỗi tạo file", Toast.LENGTH_SHORT).show()
                            }
                            viewModel.setShowNewFileDialog(false)
                        }
                    }
                ) {
                    Text("Tạo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowNewFileDialog(false) }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog Create New Folder
    if (uiState.showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.setShowNewFolderDialog(false) },
            title = { Text("Tạo thư mục mới", fontWeight = FontWeight.SemiBold) },
            shape = RoundedCornerShape(20.dp),
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Tên thư mục") },
                    placeholder = { Text("Ví dụ: ProjectA") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            val success = viewModel.createFolder(folderName.trim())
                            if (success) {
                                Toast.makeText(context, "Đã tạo thư mục", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Không thể tạo thư mục", Toast.LENGTH_SHORT).show()
                            }
                            viewModel.setShowNewFolderDialog(false)
                        }
                    }
                ) {
                    Text("Tạo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowNewFolderDialog(false) }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog Rename
    if (uiState.targetItemForRename != null) {
        val target = uiState.targetItemForRename!!
        var newName by remember { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { viewModel.setTargetItemForRename(null) },
            title = { Text("Đổi tên", fontWeight = FontWeight.SemiBold) },
            shape = RoundedCornerShape(20.dp),
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Tên mới") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank() && newName != target.name) {
                            val success = viewModel.renameItem(target, newName)
                            if (success) {
                                Toast.makeText(context, "Đã đổi tên", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Lỗi đổi tên", Toast.LENGTH_SHORT).show()
                            }
                            viewModel.setTargetItemForRename(null)
                        }
                    }
                ) {
                    Text("Lưu", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setTargetItemForRename(null) }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog Delete Confirm
    if (uiState.targetItemForDelete != null) {
        val target = uiState.targetItemForDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.setTargetItemForDelete(null) },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Xác nhận xóa?", fontWeight = FontWeight.SemiBold) },
            text = {
                Text("Bạn có chắc muốn xóa \"${target.name}\"? Toàn bộ dữ liệu sẽ bị xóa khỏi bộ nhớ thiết bị.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val success = viewModel.deleteItem(target)
                        if (success) {
                            Toast.makeText(context, "Đã xóa", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Lỗi xóa", Toast.LENGTH_SHORT).show()
                        }
                        viewModel.setTargetItemForDelete(null)
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setTargetItemForDelete(null) }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun FileOrFolderItem(
    file: File,
    storageManager: StorageManager,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (file.isDirectory) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (file.isDirectory) Icons.Rounded.Folder else Icons.Rounded.Description,
                        contentDescription = null,
                        tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name & Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (file.isDirectory) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val subInfo = if (file.isDirectory) {
                    val count = file.list()?.size ?: 0
                    "$count mục"
                } else {
                    storageManager.formatFileSize(file.length())
                }
                Text(
                    text = subInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // More Options Menu
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Tùy chọn",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Đổi tên") },
                        onClick = {
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    )

                    DropdownMenuItem(
                        text = { Text("Xóa", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}
