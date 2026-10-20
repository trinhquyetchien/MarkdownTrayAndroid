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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.trinhquyetchien.markdowntray.data.CustomMenuEntry
import com.trinhquyetchien.markdowntray.data.StorageManager
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    storageManager: StorageManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rootFolder = remember(storageManager.notesRootDir) { storageManager.getNotesRootFolder() }
    var currentFolder by remember { mutableStateOf(rootFolder) }
    var searchQuery by remember { mutableStateOf("") }

    var filesAndDirs by remember { mutableStateOf<List<File>>(emptyList()) }
    var customMenus by remember { mutableStateOf(storageManager.getCustomMenus()) }
    var activeNoteFile by remember { mutableStateOf<File?>(null) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }

    var targetItemForRename by remember { mutableStateOf<File?>(null) }
    var targetItemForDelete by remember { mutableStateOf<File?>(null) }

    fun refreshList() {
        if (!currentFolder.exists()) {
            currentFolder.mkdirs()
        }
        val list = currentFolder.listFiles()?.toList() ?: emptyList()
        filesAndDirs = list.sortedWith(
            compareBy<File> { !it.isDirectory }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
        customMenus = storageManager.getCustomMenus()
    }

    LaunchedEffect(currentFolder) {
        refreshList()
    }

    DisposableEffect(currentFolder) {
        val observer = storageManager.createDirectoryObserver(currentFolder) {
            refreshList()
        }
        observer.start()
        onDispose {
            observer.stop()
        }
    }

    val isAtRoot = currentFolder.absolutePath == rootFolder.absolutePath

    BackHandler(enabled = !isAtRoot) {
        currentFolder.parentFile?.let { parent ->
            currentFolder = parent
        }
    }

    val filteredList = remember(filesAndDirs, searchQuery) {
        if (searchQuery.isBlank()) {
            filesAndDirs
        } else {
            filesAndDirs.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isAtRoot) "Danh sách ghi chú" else currentFolder.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = currentFolder.absolutePath.replace(StorageManager.DEFAULT_STORAGE_ROOT, "~"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (!isAtRoot) {
                        IconButton(onClick = {
                            currentFolder.parentFile?.let { currentFolder = it }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Thư mục cha"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showNewFolderDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "Thư mục mới",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { refreshList() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới",
                            tint = MaterialTheme.colorScheme.onSurface
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
                onClick = { showNewFileDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tạo ghi chú mới")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Pinned Custom Folders (Shortcuts)
            if (customMenus.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    customMenus.forEach { menu ->
                        val targetDir = File(menu.path)
                        val isSelected = targetDir.absolutePath == currentFolder.absolutePath
                        AssistChip(
                            onClick = {
                                if (targetDir.exists() && targetDir.isDirectory) {
                                    currentFolder = targetDir
                                } else {
                                    Toast.makeText(context, "Thư mục không tồn tại: ${menu.name}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            label = { Text(menu.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm kiếm thư mục, file ghi chú...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Xóa")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // Items List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isEmpty()) "Thư mục trống" else "Không tìm thấy mục nào",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredList, key = { it.absolutePath }) { file ->
                        val isPinned = customMenus.any { it.path == file.absolutePath }
                        if (file.isDirectory) {
                            FolderItem(
                                folder = file,
                                isPinned = isPinned,
                                onClick = { currentFolder = file },
                                onRename = { targetItemForRename = file },
                                onDelete = { targetItemForDelete = file },
                                onTogglePin = {
                                    if (isPinned) {
                                        storageManager.removeCustomMenu(file.absolutePath)
                                        customMenus = storageManager.getCustomMenus()
                                        Toast.makeText(context, "Đã bỏ ghim thư mục", Toast.LENGTH_SHORT).show()
                                    } else {
                                        storageManager.addCustomMenu(file.name, file.absolutePath)
                                        customMenus = storageManager.getCustomMenus()
                                        Toast.makeText(context, "Đã ghim thư mục", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        } else {
                            MarkdownFileItem(
                                file = file,
                                storageManager = storageManager,
                                onClick = { activeNoteFile = file },
                                onRename = { targetItemForRename = file },
                                onDelete = { targetItemForDelete = file }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog Create New Note
    if (showNewFileDialog) {
        var noteName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Tạo ghi chú mới", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Vị trí: ${currentFolder.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteName,
                        onValueChange = { noteName = it },
                        label = { Text("Tên ghi chú (vd: MyNotes.md)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = noteName.trim()
                        if (clean.isNotEmpty()) {
                            val fileName = if (clean.endsWith(".md", ignoreCase = true)) clean else "$clean.md"
                            val newFile = File(currentFolder, fileName)
                            if (newFile.exists()) {
                                Toast.makeText(context, "File đã tồn tại!", Toast.LENGTH_SHORT).show()
                            } else {
                                val initial = "# ${fileName.removeSuffix(".md")}\n\n- [ ] Task đầu tiên\n"
                                storageManager.writeText(newFile, initial)
                                refreshList()
                                activeNoteFile = newFile
                                showNewFileDialog = false
                            }
                        }
                    }
                ) {
                    Text("Tạo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog Create New Folder
    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Tạo thư mục mới", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Tên thư mục") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = folderName.trim()
                        if (clean.isNotEmpty()) {
                            val ok = storageManager.createFolder(currentFolder, clean)
                            if (ok) {
                                refreshList()
                                showNewFolderDialog = false
                                Toast.makeText(context, "Đã tạo thư mục", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Không thể tạo thư mục hoặc thư mục đã tồn tại", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Tạo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog Rename File/Folder
    targetItemForRename?.let { item ->
        var newName by remember { mutableStateOf(item.name) }
        AlertDialog(
            onDismissRequest = { targetItemForRename = null },
            title = { Text("Đổi tên", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Tên mới") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = newName.trim()
                        if (clean.isNotEmpty() && clean != item.name) {
                            val renamed = storageManager.renameFileOrFolder(item, clean)
                            if (renamed != null) {
                                refreshList()
                                targetItemForRename = null
                                Toast.makeText(context, "Đã đổi tên thành công", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Lỗi đổi tên hoặc tên đã tồn tại", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Lưu", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { targetItemForRename = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog Delete File/Folder Confirmation
    targetItemForDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { targetItemForDelete = null },
            title = { Text("Xác nhận xóa?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa \"${item.name}\"? Thao tác này không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = storageManager.deleteFileOrFolder(item)
                        if (ok) {
                            refreshList()
                            targetItemForDelete = null
                            Toast.makeText(context, "Đã xóa", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Lỗi khi xóa", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { targetItemForDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Active Note Viewer & Editor Modal
    activeNoteFile?.let { noteFile ->
        NoteDetailDialog(
            file = noteFile,
            storageManager = storageManager,
            onDismiss = {
                activeNoteFile = null
                refreshList()
            }
        )
    }
}

@Composable
private fun FolderItem(
    folder: File,
    isPinned: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val childCount = folder.listFiles()?.size ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$childCount mục",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Tùy chọn",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isPinned) "Bỏ ghim lối tắt" else "Ghim làm lối tắt") },
                        onClick = {
                            showMenu = false
                            onTogglePin()
                        },
                        leadingIcon = {
                            Icon(
                                if (isPinned) Icons.Default.BookmarkBorder else Icons.Default.Bookmark,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Đổi tên") },
                        onClick = {
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa thư mục", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownFileItem(
    file: File,
    storageManager: StorageManager,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val syncInfo = remember(file.lastModified()) { storageManager.getFileSyncInfo(file) }
    val isMarkdown = file.name.endsWith(".md", ignoreCase = true) || file.name.endsWith(".markdown", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = if (isMarkdown) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${storageManager.formatFileSize(file.length())} • ${syncInfo.relativeTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Tùy chọn",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa file", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteDetailDialog(
    file: File,
    storageManager: StorageManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var rawContent by remember { mutableStateOf(storageManager.readText(file)) }
    var syncInfo by remember { mutableStateOf(storageManager.getFileSyncInfo(file)) }

    fun reload() {
        rawContent = storageManager.readText(file)
        syncInfo = storageManager.getFileSyncInfo(file)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = file.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = syncInfo.statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            MarkdownViewer(
                title = file.nameWithoutExtension,
                rawContent = rawContent,
                syncInfo = syncInfo,
                onToggleTask = { lineIndex, isChecked ->
                    val success = storageManager.toggleTask(file, lineIndex, isChecked)
                    if (success) {
                        reload()
                    } else {
                        Toast.makeText(context, "Không thể cập nhật task", Toast.LENGTH_SHORT).show()
                    }
                },
                onAddTask = { taskText ->
                    val success = storageManager.appendTask(file, taskText)
                    if (success) {
                        reload()
                        Toast.makeText(context, "Đã thêm task", Toast.LENGTH_SHORT).show()
                    }
                },
                onSaveRawContent = { newContent ->
                    val success = storageManager.writeText(file, newContent)
                    if (success) {
                        reload()
                        Toast.makeText(context, "Đã lưu ghi chú", Toast.LENGTH_SHORT).show()
                    }
                },
                onDeleteTask = { lineIndex ->
                    val success = storageManager.deleteTask(file, lineIndex)
                    if (success) {
                        reload()
                        Toast.makeText(context, "Đã xóa task", Toast.LENGTH_SHORT).show()
                    }
                },
                onUpdateTaskText = { lineIndex, newText ->
                    val success = storageManager.updateTaskText(file, lineIndex, newText)
                    if (success) {
                        reload()
                        Toast.makeText(context, "Đã cập nhật task", Toast.LENGTH_SHORT).show()
                    }
                },
                onRefresh = {
                    reload()
                    Toast.makeText(context, "Đã làm mới", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}
