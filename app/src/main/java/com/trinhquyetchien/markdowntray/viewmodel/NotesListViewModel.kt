package com.trinhquyetchien.markdowntray.viewmodel

import androidx.lifecycle.ViewModel
import com.trinhquyetchien.markdowntray.data.CustomMenuEntry
import com.trinhquyetchien.markdowntray.data.DirectoryObserver
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.data.SyncStatusInfo
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotesListUiState(
    val rootFolder: File,
    val currentFolder: File,
    val filesAndDirs: List<File> = emptyList(),
    val customMenus: List<CustomMenuEntry> = emptyList(),
    val searchQuery: String = "",
    val activeNoteFile: File? = null,
    val activeNoteContent: String = "",
    val activeNoteSyncInfo: SyncStatusInfo? = null,
    val showNewFileDialog: Boolean = false,
    val showNewFolderDialog: Boolean = false,
    val targetItemForRename: File? = null,
    val targetItemForDelete: File? = null
)

class NotesListViewModel(
    private val storageManager: StorageManager
) : ViewModel() {

    private val _uiState: MutableStateFlow<NotesListUiState>
    val uiState: StateFlow<NotesListUiState>

    private var directoryObserver: DirectoryObserver? = null

    init {
        val root = storageManager.getNotesRootFolder()
        _uiState = MutableStateFlow(
            NotesListUiState(
                rootFolder = root,
                currentFolder = root,
                customMenus = storageManager.getCustomMenus()
            )
        )
        uiState = _uiState.asStateFlow()

        refreshList()
        setupDirectoryObserver()
    }

    private fun setupDirectoryObserver() {
        directoryObserver?.stop()
        val cur = _uiState.value.currentFolder
        if (cur.exists()) {
            directoryObserver = storageManager.createDirectoryObserver(cur) {
                refreshList()
                refreshActiveNote()
            }
            directoryObserver?.start()
        }
    }

    fun refreshList() {
        val state = _uiState.value
        val folder = state.currentFolder
        if (!folder.exists()) {
            folder.mkdirs()
        }

        val allItems = folder.listFiles()?.toList() ?: emptyList()
        val sorted = allItems.sortedWith(
            compareBy<File> { !it.isDirectory }
                .thenBy { it.name.lowercase(Locale.getDefault()) }
        )

        val filtered = if (state.searchQuery.isBlank()) {
            sorted
        } else {
            val q = state.searchQuery.trim().lowercase(Locale.getDefault())
            sorted.filter { it.name.lowercase(Locale.getDefault()).contains(q) }
        }

        _uiState.value = state.copy(
            filesAndDirs = filtered,
            customMenus = storageManager.getCustomMenus()
        )
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        refreshList()
    }

    fun navigateToFolder(folder: File) {
        _uiState.value = _uiState.value.copy(currentFolder = folder, searchQuery = "")
        refreshList()
        setupDirectoryObserver()
    }

    fun navigateUp(): Boolean {
        val state = _uiState.value
        if (state.currentFolder.absolutePath != state.rootFolder.absolutePath) {
            val parent = state.currentFolder.parentFile
            if (parent != null && parent.exists()) {
                navigateToFolder(parent)
                return true
            }
        }
        return false
    }

    fun openNote(file: File) {
        val content = storageManager.readText(file)
        val sync = storageManager.getFileSyncInfo(file)
        _uiState.value = _uiState.value.copy(
            activeNoteFile = file,
            activeNoteContent = content,
            activeNoteSyncInfo = sync
        )
    }

    fun closeActiveNote() {
        _uiState.value = _uiState.value.copy(
            activeNoteFile = null,
            activeNoteContent = "",
            activeNoteSyncInfo = null
        )
    }

    fun refreshActiveNote() {
        val active = _uiState.value.activeNoteFile ?: return
        if (active.exists()) {
            val content = storageManager.readText(active)
            val sync = storageManager.getFileSyncInfo(active)
            _uiState.value = _uiState.value.copy(
                activeNoteContent = content,
                activeNoteSyncInfo = sync
            )
        }
    }

    fun toggleTask(lineIndex: Int, isChecked: Boolean): Boolean {
        val file = _uiState.value.activeNoteFile ?: return false
        val success = storageManager.toggleTask(file, lineIndex, isChecked)
        if (success) refreshActiveNote()
        return success
    }

    fun addTask(taskText: String): Boolean {
        val file = _uiState.value.activeNoteFile ?: return false
        val success = storageManager.appendTask(file, taskText)
        if (success) refreshActiveNote()
        return success
    }

    fun saveRawContent(newContent: String): Boolean {
        val file = _uiState.value.activeNoteFile ?: return false
        val success = storageManager.writeText(file, newContent)
        if (success) refreshActiveNote()
        return success
    }

    fun deleteTask(lineIndex: Int): Boolean {
        val file = _uiState.value.activeNoteFile ?: return false
        val success = storageManager.deleteTask(file, lineIndex)
        if (success) refreshActiveNote()
        return success
    }

    fun updateTaskText(lineIndex: Int, newText: String): Boolean {
        val file = _uiState.value.activeNoteFile ?: return false
        val success = storageManager.updateTaskText(file, lineIndex, newText)
        if (success) refreshActiveNote()
        return success
    }

    fun createNote(name: String): Boolean {
        val clean = if (name.endsWith(".md", ignoreCase = true)) name else "$name.md"
        val target = File(_uiState.value.currentFolder, clean)
        val success = storageManager.writeText(target, "# ${target.nameWithoutExtension}\n\n- [ ] Nhiệm vụ đầu tiên\n")
        if (success) {
            refreshList()
            openNote(target)
        }
        return success
    }

    fun createFolder(name: String): Boolean {
        val success = storageManager.createFolder(_uiState.value.currentFolder, name)
        if (success) refreshList()
        return success
    }

    fun deleteItem(file: File): Boolean {
        val success = storageManager.deleteFileOrFolder(file)
        if (success) {
            if (_uiState.value.activeNoteFile?.absolutePath == file.absolutePath) {
                closeActiveNote()
            }
            refreshList()
        }
        return success
    }

    fun renameItem(file: File, newName: String): Boolean {
        val renamed = storageManager.renameFileOrFolder(file, newName)
        if (renamed != null) {
            if (_uiState.value.activeNoteFile?.absolutePath == file.absolutePath) {
                openNote(renamed)
            }
            refreshList()
            return true
        }
        return false
    }

    fun toggleCustomMenu(folder: File): Boolean {
        val isPinned = _uiState.value.customMenus.any { it.path == folder.absolutePath }
        if (isPinned) {
            storageManager.removeCustomMenu(folder.absolutePath)
        } else {
            storageManager.addCustomMenu(folder.name, folder.absolutePath)
        }
        _uiState.value = _uiState.value.copy(customMenus = storageManager.getCustomMenus())
        return !isPinned
    }

    fun setShowNewFileDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showNewFileDialog = show)
    }

    fun setShowNewFolderDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showNewFolderDialog = show)
    }

    fun setTargetItemForRename(file: File?) {
        _uiState.value = _uiState.value.copy(targetItemForRename = file)
    }

    fun setTargetItemForDelete(file: File?) {
        _uiState.value = _uiState.value.copy(targetItemForDelete = file)
    }

    override fun onCleared() {
        super.onCleared()
        directoryObserver?.stop()
        directoryObserver = null
    }
}
