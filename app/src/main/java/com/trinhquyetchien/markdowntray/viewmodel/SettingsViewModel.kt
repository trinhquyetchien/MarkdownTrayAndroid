package com.trinhquyetchien.markdowntray.viewmodel

import androidx.lifecycle.ViewModel
import com.trinhquyetchien.markdowntray.data.CustomMenuEntry
import com.trinhquyetchien.markdowntray.data.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val hasStoragePermission: Boolean = false,
    val dailyDir: String = "",
    val dailyAlias: String = "",
    val notesRootDir: String = "",
    val deadlineFile: String = "",
    val deadlineAlias: String = "",
    val calendarFile: String = "",
    val customMenus: List<CustomMenuEntry> = emptyList()
)

class SettingsViewModel(
    private val storageManager: StorageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadCurrentSettings())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun loadCurrentSettings(): SettingsUiState {
        return SettingsUiState(
            hasStoragePermission = storageManager.hasStoragePermission(),
            dailyDir = storageManager.dailyNotesDir,
            dailyAlias = storageManager.dailyAlias,
            notesRootDir = storageManager.notesRootDir,
            deadlineFile = storageManager.deadlineFilePath,
            deadlineAlias = storageManager.deadlineAlias,
            calendarFile = storageManager.calendarFilePath,
            customMenus = storageManager.getCustomMenus()
        )
    }

    fun refreshPermission() {
        _uiState.value = _uiState.value.copy(
            hasStoragePermission = storageManager.hasStoragePermission()
        )
    }

    fun savePaths(
        dailyDir: String,
        dailyAlias: String,
        notesRootDir: String,
        deadlineFile: String,
        deadlineAlias: String,
        calendarFile: String
    ) {
        storageManager.dailyNotesDir = dailyDir.trim()
        storageManager.dailyAlias = dailyAlias.trim()
        storageManager.notesRootDir = notesRootDir.trim()
        storageManager.deadlineFilePath = deadlineFile.trim()
        storageManager.deadlineAlias = deadlineAlias.trim()
        storageManager.calendarFilePath = calendarFile.trim()

        _uiState.value = loadCurrentSettings()
    }

    fun resetToDefaults() {
        storageManager.dailyNotesDir = StorageManager.DEFAULT_DAILY_DIR
        storageManager.dailyAlias = "Daily List"
        storageManager.notesRootDir = StorageManager.DEFAULT_NOTES_ROOT_DIR
        storageManager.deadlineFilePath = StorageManager.DEFAULT_DEADLINE_FILE
        storageManager.deadlineAlias = "Deadline"
        storageManager.calendarFilePath = StorageManager.DEFAULT_CALENDAR_FILE

        _uiState.value = loadCurrentSettings()
    }

    fun removeCustomMenu(path: String) {
        storageManager.removeCustomMenu(path)
        _uiState.value = _uiState.value.copy(
            customMenus = storageManager.getCustomMenus()
        )
    }

    fun addCustomMenu(name: String, path: String) {
        storageManager.addCustomMenu(name, path)
        _uiState.value = _uiState.value.copy(
            customMenus = storageManager.getCustomMenus()
        )
    }
}
