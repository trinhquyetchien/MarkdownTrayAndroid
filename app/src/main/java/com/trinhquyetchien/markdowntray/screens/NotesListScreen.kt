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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
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
import com.trinhquyetchien.markdowntray.data.CustomMenuEntry
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
                            text = if (uiState.currentFolder.absolutePath == uiState.rootFolder.absolutePath) "Ghi chú & Thư mục"
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
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Refresh Button
                    IconButton(onClick = {
                        viewModel.refreshList()
                        Toast.makeText(context, "Đã làm mới danh sách", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Làm mới",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // New Folder Button
                    IconButton(onClick = { viewModel.setShowNewFolderDialog(true) }) {
                        Icon(
                            imageVector = Icons.Outlined.CreateNewFolder,
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Tạo ghi chú mới")
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
                placeholder = { Text("Tìm kiếm ghi chú hoặc thư mục...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Xóa tìm kiếm", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // Custom Pinned Shortcuts (Custom Menus row)
            if (uiState.customMenus.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.customMenus.forEach { menu ->
                        val targetDir = File(menu.path)
                        val isCurrent = uiState.currentFolder.absolutePath == targetDir.absolutePath
                        AssistChip(
                            onClick = {
                                if (targetDir.exists()) {
                                    viewModel.navigateToFolder(targetDir)
                                } else {
                                    Toast.makeText(context, "Thư mục không tồn tại: ${menu.name}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            label = { Text(menu.name, fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Bookmark,
                                    contentDescription = null,
                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
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
                                    imageVector = Icons.Outlined.Folder,
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
                        val isPinned = uiState.customMenus.any { it.path == file.absolutePath }
                        FileOrFolderItem(
                            file = file,
                            storageManager = storageManager,
                            isPinned = isPinned,
                            onClick = {
                                if (file.isDirectory) {
                                    viewModel.navigateToFolder(file)
                                } else {
                                    viewModel.openNote(file)
                                }
                            },
                            onRename = { viewModel.setTargetItemForRename(file) },
                            onDelete = { viewModel.setTargetItemForDelete(file) },
                            onTogglePin = if (file.isDirectory) {
                                {
                                    val pinnedNow = viewModel.toggleCustomMenu(file)
                                    val msg = if (pinnedNow) "Đã ghim menu: ${file.name}" else "Đã bỏ ghim menu: ${file.name}"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            } else null
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
                            Column {
                                Text(file.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    file.parentFile?.name ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.closeActiveNote() }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Đóng", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            text = {
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    label = { Text("Tên ghi chú") },
                    placeholder = { Text("Ví dụ: KeHoachTuan") },
                    modifier = Modifier.fillMaxWidth(),
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
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Tên thư mục") },
                    placeholder = { Text("Ví dụ: ProjectA") },
                    modifier = Modifier.fillMaxWidth(),
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
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Tên mới") },
                    modifier = Modifier.fillMaxWidth(),
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
    isPinned: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: (() -> Unit)?
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Monochromatic outline icon
            Icon(
                imageVector = if (file.isDirectory) Icons.Outlined.Folder else Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Name & Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (file.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Pinned indicator
            if (isPinned) {
                Icon(
                    imageVector = Icons.Outlined.Bookmark,
                    contentDescription = "Đã ghim",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 4.dp)
                )
            }

            // More Options Menu
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Tùy chọn",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onTogglePin != null) {
                        DropdownMenuItem(
                            text = { Text(if (isPinned) "Bỏ ghim Menu" else "Ghim làm Menu") },
                            onClick = {
                                showMenu = false
                                onTogglePin()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isPinned) Icons.Outlined.BookmarkBorder else Icons.Outlined.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Đổi tên") },
                        onClick = {
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    )

                    DropdownMenuItem(
                        text = { Text("Xóa", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}
