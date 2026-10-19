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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.trinhquyetchien.markdowntray.data.MarkdownNode
import com.trinhquyetchien.markdowntray.data.MarkdownParser
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.data.SyncStatusInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
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
    var activeNoteFile by remember { mutableStateOf<File?>(null) }
    var showNewFileDialog by remember { mutableStateOf(false) }

    fun refreshList() {
        if (!currentFolder.exists()) {
            currentFolder.mkdirs()
        }
        val list = currentFolder.listFiles()?.toList() ?: emptyList()
        // Sort folders first, then files alphabetically
        filesAndDirs = list.sortedWith(
            compareBy<File> { !it.isDirectory }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }

    LaunchedEffect(currentFolder) {
        refreshList()
    }

    // Auto-reload on file system changes (Syncthing syncs)
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

    // Handle back button when inside a subfolder
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
                                text = if (searchQuery.isEmpty()) "Thư mục trống" else "Không tìm thấy file nào",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredList, key = { it.absolutePath }) { file ->
                        if (file.isDirectory) {
                            FolderItem(
                                folder = file,
                                onClick = { currentFolder = file }
                            )
                        } else {
                            MarkdownFileItem(
                                file = file,
                                storageManager = storageManager,
                                onClick = { activeNoteFile = file }
                            )
                        }
                    }
                }
            }
        }
    }

    // New Note Dialog
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
    onClick: () -> Unit
) {
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
        }
    }
}

@Composable
private fun MarkdownFileItem(
    file: File,
    storageManager: StorageManager,
    onClick: () -> Unit
) {
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
