package com.trinhquyetchien.markdowntray.viewmodel

import androidx.lifecycle.ViewModel
import com.trinhquyetchien.markdowntray.data.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val hasStoragePermission: Boolean = false,
    val notesRootDir: String = "",
    val dailyDir: String = "",
    val deadlineFile: String = "",
    val calendarFile: String = ""
)

class SettingsViewModel(
    private val storageManager: StorageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadCurrentSettings())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun loadCurrentSettings(): SettingsUiState {
        return SettingsUiState(
            hasStoragePermission = storageManager.hasStoragePermission(),
            notesRootDir = storageManager.notesRootDir,
            dailyDir = storageManager.dailyNotesDir,
            deadlineFile = storageManager.deadlineFilePath,
            calendarFile = storageManager.calendarFilePath
        )
    }

    fun refreshPermission() {
        _uiState.value = _uiState.value.copy(
            hasStoragePermission = storageManager.hasStoragePermission()
        )
    }

    fun setNotesRootDir(path: String) {
        storageManager.notesRootDir = path.trim()
        _uiState.value = loadCurrentSettings()
    }

    fun setDailyDir(path: String) {
        storageManager.dailyNotesDir = path.trim()
        _uiState.value = loadCurrentSettings()
    }

    fun setDeadlineFile(path: String) {
        storageManager.deadlineFilePath = path.trim()
        _uiState.value = loadCurrentSettings()
    }

    fun setCalendarFile(path: String) {
        storageManager.calendarFilePath = path.trim()
        _uiState.value = loadCurrentSettings()
    }

    fun savePaths(
        notesRootDir: String,
        dailyDir: String,
        deadlineFile: String,
        calendarFile: String
    ) {
        storageManager.notesRootDir = notesRootDir.trim()
        storageManager.dailyNotesDir = dailyDir.trim()
        storageManager.deadlineFilePath = deadlineFile.trim()
        storageManager.calendarFilePath = calendarFile.trim()

        _uiState.value = loadCurrentSettings()
    }

    fun resetToDefaults() {
        storageManager.notesRootDir = StorageManager.DEFAULT_NOTES_ROOT_DIR
        storageManager.dailyNotesDir = StorageManager.DEFAULT_DAILY_DIR
        storageManager.deadlineFilePath = StorageManager.DEFAULT_DEADLINE_FILE
        storageManager.calendarFilePath = StorageManager.DEFAULT_CALENDAR_FILE

        _uiState.value = loadCurrentSettings()
    }
}
