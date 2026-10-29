package com.trinhquyetchien.markdowntray.viewmodel

import androidx.lifecycle.ViewModel
import com.trinhquyetchien.markdowntray.data.DirectoryObserver
import com.trinhquyetchien.markdowntray.data.OverdueTask
import com.trinhquyetchien.markdowntray.data.OverdueTaskManager
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.data.SyncStatusInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TodayUiState(
    val selectedCalendar: Calendar = Calendar.getInstance(),
    val selectedDateStr: String = "",
    val todayDateStr: String = "",
    val rawContent: String = "",
    val syncInfo: SyncStatusInfo? = null,
    val overdueTasks: List<OverdueTask> = emptyList(),
    val isOverdueExpanded: Boolean = true
)

class TodayViewModel(
    private val storageManager: StorageManager
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _uiState = MutableStateFlow(
        TodayUiState(
            selectedDateStr = dateFormat.format(Date()),
            todayDateStr = dateFormat.format(Date())
        )
    )
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private var directoryObserver: DirectoryObserver? = null

    init {
        loadContent()
        setupDirectoryObserver()
    }

    private fun setupDirectoryObserver() {
        directoryObserver?.stop()
        val dir = File(storageManager.dailyNotesDir)
        directoryObserver = storageManager.createDirectoryObserver(dir) {
            loadContent()
        }
        directoryObserver?.start()
    }

    fun loadContent() {
        val state = _uiState.value
        var targetFile = File(storageManager.dailyNotesDir, "${state.selectedDateStr}.md")
        if (state.selectedDateStr == state.todayDateStr && (!targetFile.exists() || targetFile.length() == 0L)) {
            targetFile = storageManager.ensureTodayFileExists()
        }

        val content = if (targetFile.exists()) storageManager.readText(targetFile) else ""
        val sync = storageManager.getFileSyncInfo(targetFile)

        // Load overdue tasks từ các ngày trước
        val overdues = if (state.selectedDateStr == state.todayDateStr) {
            OverdueTaskManager.getOverdueTasks(File(storageManager.dailyNotesDir), state.todayDateStr)
        } else {
            emptyList()
        }

        _uiState.value = state.copy(
            rawContent = content,
            syncInfo = sync,
            overdueTasks = overdues
        )
    }

    fun toggleOverdueExpanded() {
        _uiState.value = _uiState.value.copy(
            isOverdueExpanded = !_uiState.value.isOverdueExpanded
        )
    }

    fun selectPreviousDay() {
        val cal = _uiState.value.selectedCalendar.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val dateStr = dateFormat.format(cal.time)
        _uiState.value = _uiState.value.copy(selectedCalendar = cal, selectedDateStr = dateStr)
        loadContent()
    }

    fun selectNextDay() {
        val cal = _uiState.value.selectedCalendar.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dateStr = dateFormat.format(cal.time)
        _uiState.value = _uiState.value.copy(selectedCalendar = cal, selectedDateStr = dateStr)
        loadContent()
    }

    fun selectToday() {
        val cal = Calendar.getInstance()
        val dateStr = dateFormat.format(cal.time)
        _uiState.value = _uiState.value.copy(selectedCalendar = cal, selectedDateStr = dateStr)
        loadContent()
    }

    fun toggleTask(lineIndex: Int, isChecked: Boolean): Boolean {
        val file = File(storageManager.dailyNotesDir, "${_uiState.value.selectedDateStr}.md")
        val success = storageManager.toggleTask(file, lineIndex, isChecked)
        if (success) loadContent()
        return success
    }

    fun toggleOverdueTask(task: OverdueTask, isChecked: Boolean): Boolean {
        val success = storageManager.toggleTask(task.file, task.lineIndex, isChecked)
        if (success) loadContent()
        return success
    }

    fun moveOverdueTaskToToday(task: OverdueTask): Boolean {
        val todayFile = storageManager.getTodayFile()
        val success = OverdueTaskManager.moveTaskToToday(storageManager, task, todayFile)
        if (success) loadContent()
        return success
    }

    fun addTask(taskText: String): Boolean {
        val file = File(storageManager.dailyNotesDir, "${_uiState.value.selectedDateStr}.md")
        val success = storageManager.appendTask(file, taskText)
        if (success) loadContent()
        return success
    }

    fun saveRawContent(newContent: String): Boolean {
        val file = File(storageManager.dailyNotesDir, "${_uiState.value.selectedDateStr}.md")
        val success = storageManager.writeText(file, newContent)
        if (success) loadContent()
        return success
    }

    fun deleteTask(lineIndex: Int): Boolean {
        val file = File(storageManager.dailyNotesDir, "${_uiState.value.selectedDateStr}.md")
        val success = storageManager.deleteTask(file, lineIndex)
        if (success) loadContent()
        return success
    }

    fun updateTaskText(lineIndex: Int, newText: String): Boolean {
        val file = File(storageManager.dailyNotesDir, "${_uiState.value.selectedDateStr}.md")
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
