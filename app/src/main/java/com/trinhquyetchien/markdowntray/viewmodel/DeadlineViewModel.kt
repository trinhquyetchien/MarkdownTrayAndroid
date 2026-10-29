package com.trinhquyetchien.markdowntray.viewmodel

import androidx.lifecycle.ViewModel
import com.trinhquyetchien.markdowntray.data.DirectoryObserver
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.data.SyncStatusInfo
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeadlineUiState(
    val rawContent: String = "",
    val syncInfo: SyncStatusInfo? = null,
    val deadlineAlias: String = "Deadline"
)

class DeadlineViewModel(
    private val storageManager: StorageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DeadlineUiState(deadlineAlias = storageManager.deadlineAlias)
    )
    val uiState: StateFlow<DeadlineUiState> = _uiState.asStateFlow()

    private var directoryObserver: DirectoryObserver? = null

    init {
        loadContent()
        setupDirectoryObserver()
    }

    private fun setupDirectoryObserver() {
        directoryObserver?.stop()
        val currentFile = storageManager.getDeadlineFile()
        val parentDir = currentFile.parentFile ?: File(storageManager.dailyNotesDir)
        directoryObserver = storageManager.createDirectoryObserver(parentDir) { changedPath ->
            if (changedPath == null || changedPath == currentFile.name) {
                loadContent()
            }
        }
        directoryObserver?.start()
    }

    fun loadContent() {
        var currentFile = storageManager.getDeadlineFile()
        if (!currentFile.exists() || currentFile.length() == 0L) {
            currentFile = storageManager.ensureDeadlineFileExists()
        }

        val content = if (currentFile.exists()) storageManager.readText(currentFile) else ""
        val sync = storageManager.getFileSyncInfo(currentFile)

        _uiState.value = _uiState.value.copy(
            rawContent = content,
            syncInfo = sync,
            deadlineAlias = storageManager.deadlineAlias
        )
    }

    fun toggleTask(lineIndex: Int, isChecked: Boolean): Boolean {
        val file = storageManager.getDeadlineFile()
        val success = storageManager.toggleTask(file, lineIndex, isChecked)
        if (success) loadContent()
        return success
    }

    fun addTask(taskText: String): Boolean {
        val file = storageManager.getDeadlineFile()
        val success = storageManager.appendTask(file, taskText)
        if (success) loadContent()
        return success
    }

    fun saveRawContent(newContent: String): Boolean {
        val file = storageManager.getDeadlineFile()
        val success = storageManager.writeText(file, newContent)
        if (success) loadContent()
        return success
    }

    fun deleteTask(lineIndex: Int): Boolean {
        val file = storageManager.getDeadlineFile()
        val success = storageManager.deleteTask(file, lineIndex)
        if (success) loadContent()
        return success
    }

    fun updateTaskText(lineIndex: Int, newText: String): Boolean {
        val file = storageManager.getDeadlineFile()
        val success = storageManager.updateTaskText(file, lineIndex, newText)
        if (success) loadContent()
        return success
    }

    override fun onCleared() {
        super.onCleared()
        directoryObserver?.stop()
        directoryObserver = null
    }
}
