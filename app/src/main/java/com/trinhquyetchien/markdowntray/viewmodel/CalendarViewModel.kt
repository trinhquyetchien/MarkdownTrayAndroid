package com.trinhquyetchien.markdowntray.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.trinhquyetchien.markdowntray.alarm.CalendarAlarmScheduler
import com.trinhquyetchien.markdowntray.data.CalendarEvent
import com.trinhquyetchien.markdowntray.data.CalendarOccurrence
import com.trinhquyetchien.markdowntray.data.CalendarRepository
import com.trinhquyetchien.markdowntray.data.CalendarUtils
import com.trinhquyetchien.markdowntray.data.CalendarViewMode
import com.trinhquyetchien.markdowntray.data.DirectoryObserver
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.data.SyncStatusInfo
import java.io.File
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CalendarUiState(
    val events: List<CalendarEvent> = emptyList(),
    val syncInfo: SyncStatusInfo? = null,
    val viewMode: CalendarViewMode = CalendarViewMode.DAY,
    val selectedCalendar: Calendar = Calendar.getInstance(),
    val selectedYMD: String = "",
    val todayYMD: String = "",
    val editingOccurrence: CalendarOccurrence? = null,
    val showNewEventDialog: Boolean = false
)

class CalendarViewModel(
    private val context: Context,
    private val storageManager: StorageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CalendarUiState(
            selectedYMD = CalendarUtils.formatYMD(Date()),
            todayYMD = CalendarUtils.formatYMD(Date())
        )
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var directoryObserver: DirectoryObserver? = null

    init {
        loadEvents()
        setupDirectoryObserver()
    }

    private fun setupDirectoryObserver() {
        directoryObserver?.stop()
        val file = storageManager.getCalendarFile()
        val parentDir = file.parentFile ?: File(StorageManager.DEFAULT_STORAGE_ROOT)
        directoryObserver = storageManager.createDirectoryObserver(parentDir) { changedPath ->
            if (changedPath == null || changedPath == file.name) {
                loadEvents()
            }
        }
        directoryObserver?.start()
    }

    fun loadEvents() {
        val file = storageManager.getCalendarFile()
        val loadedEvents = CalendarRepository.loadEvents(file)
        val sync = storageManager.getFileSyncInfo(file)
        CalendarAlarmScheduler.scheduleUpcomingAlarms(context, storageManager)

        _uiState.value = _uiState.value.copy(
            events = loadedEvents,
            syncInfo = sync
        )
    }

    fun setViewMode(mode: CalendarViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun selectDate(ymd: String, cal: Calendar) {
        _uiState.value = _uiState.value.copy(
            selectedCalendar = cal,
            selectedYMD = ymd
        )
    }

    fun navigatePrevious() {
        val cal = _uiState.value.selectedCalendar.clone() as Calendar
        when (_uiState.value.viewMode) {
            CalendarViewMode.DAY -> cal.add(Calendar.DAY_OF_YEAR, -1)
            CalendarViewMode.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, -1)
            CalendarViewMode.MONTH -> cal.add(Calendar.MONTH, -1)
        }
        _uiState.value = _uiState.value.copy(
            selectedCalendar = cal,
            selectedYMD = CalendarUtils.formatYMD(cal.time)
        )
    }

    fun navigateNext() {
        val cal = _uiState.value.selectedCalendar.clone() as Calendar
        when (_uiState.value.viewMode) {
            CalendarViewMode.DAY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            CalendarViewMode.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            CalendarViewMode.MONTH -> cal.add(Calendar.MONTH, 1)
        }
        _uiState.value = _uiState.value.copy(
            selectedCalendar = cal,
            selectedYMD = CalendarUtils.formatYMD(cal.time)
        )
    }

    fun navigateToday() {
        val cal = Calendar.getInstance()
        _uiState.value = _uiState.value.copy(
            selectedCalendar = cal,
            selectedYMD = CalendarUtils.formatYMD(cal.time)
        )
    }

    fun setEditingOccurrence(occurrence: CalendarOccurrence?) {
        _uiState.value = _uiState.value.copy(editingOccurrence = occurrence)
    }

    fun setShowNewEventDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showNewEventDialog = show)
    }

    fun saveAllEvents(newEvents: List<CalendarEvent>): Boolean {
        val file = storageManager.getCalendarFile()
        val success = CalendarRepository.saveEvents(file, newEvents)
        if (success) {
            val sync = storageManager.getFileSyncInfo(file)
            _uiState.value = _uiState.value.copy(
                events = newEvents,
                syncInfo = sync
            )
            CalendarAlarmScheduler.scheduleUpcomingAlarms(context, storageManager)
        }
        return success
    }

    fun addEvent(event: CalendarEvent): Boolean {
        return saveAllEvents(_uiState.value.events + event)
    }

    fun updateEvent(event: CalendarEvent): Boolean {
        val updated = _uiState.value.events.toMutableList()
        val index = updated.indexOfFirst { it.id == event.id }
        if (index != -1) {
            updated[index] = event
        } else {
            updated.add(event)
        }
        return saveAllEvents(updated)
    }

    fun deleteEvent(eventId: String): Boolean {
        val filtered = _uiState.value.events.filter { it.id != eventId }
        return saveAllEvents(filtered)
    }

    fun updateRecurringEvent(
        existingEvent: CalendarEvent,
        updatedEvent: CalendarEvent,
        occurrenceDate: String,
        applyToAll: Boolean
    ): Boolean {
        val currentEvents = _uiState.value.events.toMutableList()
        if (applyToAll) {
            val idx = currentEvents.indexOfFirst { it.id == existingEvent.id }
            if (idx != -1) {
                currentEvents[idx] = updatedEvent
            }
        } else {
            val idx = currentEvents.indexOfFirst { it.id == existingEvent.id }
            if (idx != -1) {
                val old = currentEvents[idx]
                val exc = (old.exceptionDates + occurrenceDate).distinct()
                currentEvents[idx] = old.copy(exceptionDates = exc)
            }
            currentEvents.add(
                updatedEvent.copy(
                    id = System.currentTimeMillis().toString(),
                    repeatDays = emptyList(),
                    exceptionDates = emptyList()
                )
            )
        }
        return saveAllEvents(currentEvents)
    }

    fun deleteRecurringEvent(
        existingEvent: CalendarEvent,
        occurrenceDate: String,
        applyToAll: Boolean
    ): Boolean {
        val currentEvents = _uiState.value.events.toMutableList()
        if (applyToAll) {
            currentEvents.removeAll { it.id == existingEvent.id }
        } else {
            val idx = currentEvents.indexOfFirst { it.id == existingEvent.id }
            if (idx != -1) {
                val old = currentEvents[idx]
                val exc = (old.exceptionDates + occurrenceDate).distinct()
                currentEvents[idx] = old.copy(exceptionDates = exc)
            }
        }
        return saveAllEvents(currentEvents)
    }

    override fun onCleared() {
        super.onCleared()
        directoryObserver?.stop()
        directoryObserver = null
    }
}
