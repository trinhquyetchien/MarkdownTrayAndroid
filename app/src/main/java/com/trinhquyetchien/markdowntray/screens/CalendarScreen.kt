package com.trinhquyetchien.markdowntray.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trinhquyetchien.markdowntray.data.CalendarEvent
import com.trinhquyetchien.markdowntray.data.CalendarOccurrence
import com.trinhquyetchien.markdowntray.data.CalendarUtils
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.viewmodel.AppViewModelFactory
import com.trinhquyetchien.markdowntray.viewmodel.CalendarViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CalendarViewMode {
    DAY, WEEK, MONTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    storageManager: StorageManager,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(
        factory = AppViewModelFactory(LocalContext.current, storageManager)
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val dayOccurrences = remember(uiState.events, uiState.selectedYMD) {
        CalendarUtils.getOccurrencesForDate(uiState.events, uiState.selectedYMD)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Lịch biểu",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        // Sync Status pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.loadEvents()
                                    Toast.makeText(context, "Đã kiểm tra đồng bộ", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.syncInfo?.isSynced == true) Color(0xFF4CAF50)
                                        else Color(0xFFFF9800)
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.syncInfo?.statusText ?: "Đang kiểm tra...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.loadEvents()
                        Toast.makeText(context, "Đã làm mới lịch", Toast.LENGTH_SHORT).show()
                    }) {
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
                onClick = { viewModel.setShowNewEventDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm sự kiện")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Controls: View mode switcher (Day, Week, Month)
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Navigation prev/today/next
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.navigatePrevious() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trước", modifier = Modifier.size(20.dp))
                        }

                        TextButton(onClick = { viewModel.navigateToday() }) {
                            Text(
                                text = if (uiState.selectedYMD == uiState.todayYMD) "Hôm nay" else "Về hôm nay",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { viewModel.navigateNext() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Sau", modifier = Modifier.size(20.dp))
                        }
                    }

                    // Mode switch tabs
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(2.dp)
                    ) {
                        ViewModeButton(
                            label = "Ngày",
                            selected = uiState.viewMode == CalendarViewMode.DAY,
                            onClick = { viewModel.setViewMode(CalendarViewMode.DAY) }
                        )
                        ViewModeButton(
                            label = "Tuần",
                            selected = uiState.viewMode == CalendarViewMode.WEEK,
                            onClick = { viewModel.setViewMode(CalendarViewMode.WEEK) }
                        )
                        ViewModeButton(
                            label = "Tháng",
                            selected = uiState.viewMode == CalendarViewMode.MONTH,
                            onClick = { viewModel.setViewMode(CalendarViewMode.MONTH) }
                        )
                    }
                }
            }

            // Current date title banner
            val headerTitle = when (uiState.viewMode) {
                CalendarViewMode.DAY -> SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.forLanguageTag("vi-VN")).format(uiState.selectedCalendar.time)
                CalendarViewMode.WEEK -> "Tuần ${uiState.selectedCalendar.get(Calendar.WEEK_OF_YEAR)} - Tháng ${uiState.selectedCalendar.get(Calendar.MONTH) + 1}/${uiState.selectedCalendar.get(Calendar.YEAR)}"
                CalendarViewMode.MONTH -> SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("vi-VN")).format(uiState.selectedCalendar.time)
            }
            Text(
                text = headerTitle.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Content body according to view mode
            when (uiState.viewMode) {
                CalendarViewMode.DAY -> {
                    DayScheduleView(
                        selectedDateYMD = uiState.selectedYMD,
                        occurrences = dayOccurrences,
                        onOccurrenceClick = { occ -> viewModel.setEditingOccurrence(occ) },
                        modifier = Modifier.weight(1f)
                    )
                }
                CalendarViewMode.WEEK -> {
                    WeekScheduleView(
                        anchorCalendar = uiState.selectedCalendar,
                        selectedYMD = uiState.selectedYMD,
                        allEvents = uiState.events,
                        onSelectDate = { ymd, cal -> viewModel.selectDate(ymd, cal) },
                        onOccurrenceClick = { occ -> viewModel.setEditingOccurrence(occ) },
                        modifier = Modifier.weight(1f)
                    )
                }
                CalendarViewMode.MONTH -> {
                    MonthScheduleView(
                        anchorCalendar = uiState.selectedCalendar,
                        selectedYMD = uiState.selectedYMD,
                        allEvents = uiState.events,
                        onSelectDate = { ymd, cal ->
                            viewModel.selectDate(ymd, cal)
                            viewModel.setViewMode(CalendarViewMode.DAY)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Dialog Edit / Add Event
    if (uiState.showNewEventDialog) {
        EventEditDialog(
            initialDateYMD = uiState.selectedYMD,
            existingEvent = null,
            occurrenceDate = uiState.selectedYMD,
            onDismiss = { viewModel.setShowNewEventDialog(false) },
            onSave = { updatedEvent ->
                val success = viewModel.addEvent(updatedEvent)
                if (success) {
                    Toast.makeText(context, "Đã thêm sự kiện", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi thêm sự kiện", Toast.LENGTH_SHORT).show()
                }
                viewModel.setShowNewEventDialog(false)
            },
            onDelete = null
        )
    }

    if (uiState.editingOccurrence != null) {
        val occ = uiState.editingOccurrence!!
        var showRecurringChoiceDialog by remember { mutableStateOf(false) }
        var pendingAction by remember { mutableStateOf<((applyToAll: Boolean) -> Unit)?>(null) }

        EventEditDialog(
            initialDateYMD = occ.occurrenceDate,
            existingEvent = occ.event,
            occurrenceDate = occ.occurrenceDate,
            onDismiss = { viewModel.setEditingOccurrence(null) },
            onSave = { updatedEvent ->
                if (occ.isRecurring) {
                    pendingAction = { applyToAll ->
                        val success = viewModel.updateRecurringEvent(
                            existingEvent = occ.event,
                            updatedEvent = updatedEvent,
                            occurrenceDate = occ.occurrenceDate,
                            applyToAll = applyToAll
                        )
                        if (success) {
                            Toast.makeText(context, "Đã cập nhật sự kiện", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showRecurringChoiceDialog = true
                } else {
                    val success = viewModel.updateEvent(updatedEvent)
                    if (success) {
                        Toast.makeText(context, "Đã cập nhật sự kiện", Toast.LENGTH_SHORT).show()
                    }
                    viewModel.setEditingOccurrence(null)
                }
            },
            onDelete = {
                if (occ.isRecurring) {
                    pendingAction = { applyToAll ->
                        val success = viewModel.deleteRecurringEvent(
                            existingEvent = occ.event,
                            occurrenceDate = occ.occurrenceDate,
                            applyToAll = applyToAll
                        )
                        if (success) {
                            Toast.makeText(context, "Đã xóa sự kiện", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showRecurringChoiceDialog = true
                } else {
                    val success = viewModel.deleteEvent(occ.event.id)
                    if (success) {
                        Toast.makeText(context, "Đã xóa sự kiện", Toast.LENGTH_SHORT).show()
                    }
                    viewModel.setEditingOccurrence(null)
                }
            }
        )

        if (showRecurringChoiceDialog && pendingAction != null) {
            AlertDialog(
                onDismissRequest = {
                    showRecurringChoiceDialog = false
                    pendingAction = null
                },
                title = { Text("Lặp lại sự kiện", fontWeight = FontWeight.Bold) },
                text = { Text("Bạn muốn áp dụng thay đổi này chỉ cho ngày này hay toàn bộ chuỗi sự kiện?") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingAction?.invoke(true)
                        showRecurringChoiceDialog = false
                        pendingAction = null
                        viewModel.setEditingOccurrence(null)
                    }) {
                        Text("Tất cả sự kiện", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        pendingAction?.invoke(false)
                        showRecurringChoiceDialog = false
                        pendingAction = null
                        viewModel.setEditingOccurrence(null)
                    }) {
                        Text("Chỉ ngày này")
                    }
                }
            )
        }
    }
}

@Composable
private fun ViewModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -------------------------------------------------------------
// Day View: Timeline 24h
// -------------------------------------------------------------
@Composable
private fun DayScheduleView(
    selectedDateYMD: String,
    occurrences: List<CalendarOccurrence>,
    onOccurrenceClick: (CalendarOccurrence) -> Unit,
    modifier: Modifier = Modifier
) {
    if (occurrences.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Không có lịch hẹn nào trong ngày",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Bấm dấu + góc dưới để tạo sự kiện mới",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(occurrences, key = { "${it.event.id}_${it.startHour}_${it.startMinute}" }) { occ ->
                EventCard(
                    occurrence = occ,
                    onClick = { onOccurrenceClick(occ) }
                )
            }
        }
    }
}

@Composable
private fun EventCard(
    occurrence: CalendarOccurrence,
    onClick: () -> Unit
) {
    val ev = occurrence.event
    val eventColor = try {
        Color(android.graphics.Color.parseColor(ev.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val timeRangeStr = String.format(
        Locale.US,
        "%02d:%02d - %02d:%02d",
        occurrence.startHour,
        occurrence.startMinute,
        occurrence.endHour,
        occurrence.endMinute
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = eventColor.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(eventColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ev.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (occurrence.isRecurring) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Lặp lại",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeRangeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (ev.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ev.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Week View
// -------------------------------------------------------------
@Composable
private fun WeekScheduleView(
    anchorCalendar: Calendar,
    selectedYMD: String,
    allEvents: List<CalendarEvent>,
    onSelectDate: (String, Calendar) -> Unit,
    onOccurrenceClick: (CalendarOccurrence) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekDates = remember(anchorCalendar) {
        CalendarUtils.getWeekDates(anchorCalendar)
    }

    val dayNames = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    Column(modifier = modifier.fillMaxSize()) {
        // Week days header strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekDates.forEachIndexed { idx, date ->
                val dateCal = Calendar.getInstance().apply { time = date }
                val ymd = CalendarUtils.formatYMD(date)
                val isSelected = ymd == selectedYMD
                val isToday = ymd == CalendarUtils.formatYMD(Date())

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else Color.Transparent
                        )
                        .clickable { onSelectDate(ymd, dateCal) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = dayNames[idx],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = SimpleDateFormat("dd", Locale.getDefault()).format(date),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )

                    // Event indicator dot
                    val dayOccs = CalendarUtils.getOccurrencesForDate(allEvents, ymd)
                    if (dayOccs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Schedule of the selected date in week
        val currentDayOccurrences = remember(allEvents, selectedYMD) {
            CalendarUtils.getOccurrencesForDate(allEvents, selectedYMD)
        }

        DayScheduleView(
            selectedDateYMD = selectedYMD,
            occurrences = currentDayOccurrences,
            onOccurrenceClick = onOccurrenceClick,
            modifier = Modifier.weight(1f)
        )
    }
}

// -------------------------------------------------------------
// Month View
// -------------------------------------------------------------
@Composable
private fun MonthScheduleView(
    anchorCalendar: Calendar,
    selectedYMD: String,
    allEvents: List<CalendarEvent>,
    onSelectDate: (String, Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthGridDates = remember(anchorCalendar) {
        CalendarUtils.getMonthGridDates(anchorCalendar)
    }

    val currentMonth = anchorCalendar.get(Calendar.MONTH)
    val dayNames = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // Day names header
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dayNames.forEach { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(42.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        HorizontalDivider()

        // 6 rows x 7 columns grid
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            for (week in 0 until 6) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (day in 0 until 7) {
                        val index = week * 7 + day
                        val date = monthGridDates[index]
                        val cal = Calendar.getInstance().apply { time = date }
                        val ymd = CalendarUtils.formatYMD(date)
                        val isThisMonth = cal.get(Calendar.MONTH) == currentMonth
                        val isSelected = ymd == selectedYMD
                        val isToday = ymd == CalendarUtils.formatYMD(Date())
                        val dayOccs = CalendarUtils.getOccurrencesForDate(allEvents, ymd)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .clickable { onSelectDate(ymd, cal) }
                                .padding(4.dp)
                        ) {
                            Text(
                                text = SimpleDateFormat("d", Locale.getDefault()).format(date),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else if (isThisMonth) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )

                            if (dayOccs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    dayOccs.take(3).forEach { occ ->
                                        val color = try {
                                            Color(android.graphics.Color.parseColor(occ.event.color))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else color)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Dialog: Thêm / Sửa Sự kiện
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventEditDialog(
    initialDateYMD: String,
    existingEvent: CalendarEvent?,
    occurrenceDate: String,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var note by remember { mutableStateOf(existingEvent?.note ?: "") }
    var selectedColor by remember { mutableStateOf(existingEvent?.color ?: "#0a84ff") }

    var dateStr by remember { mutableStateOf(occurrenceDate) }

    val initialStart = if (existingEvent != null && existingEvent.start.length >= 16) {
        existingEvent.start.substring(11, 16)
    } else "09:00"

    val initialEnd = if (existingEvent != null && existingEvent.end.length >= 16) {
        existingEvent.end.substring(11, 16)
    } else "10:00"

    var startTime by remember { mutableStateOf(initialStart) }
    var endTime by remember { mutableStateOf(initialEnd) }

    val repeatDays = remember {
        mutableStateListOf<Int>().apply {
            existingEvent?.repeatDays?.let { addAll(it) }
        }
    }

    val colorOptions = listOf(
        "#0a84ff", // Xanh dương
        "#30d158", // Xanh lá
        "#ff9f0a", // Cam
        "#ff453a", // Đỏ
        "#bf5af2", // Tím
        "#64d2ff", // Lam nhạt
        "#ffd60a", // Vàng
        "#8e8e93"  // Xám
    )

    val weekDays = listOf(
        Pair(1, "T2"),
        Pair(2, "T3"),
        Pair(3, "T4"),
        Pair(4, "T5"),
        Pair(5, "T6"),
        Pair(6, "T7"),
        Pair(0, "CN")
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingEvent == null) "Thêm sự kiện" else "Sửa sự kiện",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề") },
                    placeholder = { Text("Bạn định làm gì?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Note Input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú / Mô tả") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Time Pickers (Start - End)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start Time Button
                    OutlinedButton(
                        onClick = {
                            val (h, m) = CalendarUtils.parseTime(startTime)
                            TimePickerDialog(context, { _, hour, minute ->
                                startTime = String.format(Locale.US, "%02d:%02d", hour, minute)
                            }, h, m, true).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Bắt đầu", style = MaterialTheme.typography.labelSmall)
                            Text(startTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    // End Time Button
                    OutlinedButton(
                        onClick = {
                            val (h, m) = CalendarUtils.parseTime(endTime)
                            TimePickerDialog(context, { _, hour, minute ->
                                endTime = String.format(Locale.US, "%02d:%02d", hour, minute)
                            }, h, m, true).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Kết thúc", style = MaterialTheme.typography.labelSmall)
                            Text(endTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color picker
                Text("Màu sắc sự kiện", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorOptions.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Repeat Days Selector
                Text("Lặp lại hàng tuần", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDays.forEach { (dayInt, dayLabel) ->
                        val isChecked = repeatDays.contains(dayInt)
                        Surface(
                            shape = CircleShape,
                            color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    if (isChecked) repeatDays.remove(dayInt)
                                    else repeatDays.add(dayInt)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChecked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions: Save & Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa sự kiện", tint = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Hủy")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val cleanTitle = title.trim().ifEmpty { "Không tiêu đề" }
                            val startDateTime = "${dateStr}T${startTime}:00"
                            val endDateTime = "${dateStr}T${endTime}:00"

                            val event = CalendarEvent(
                                id = existingEvent?.id ?: System.currentTimeMillis().toString(),
                                title = cleanTitle,
                                note = note.trim(),
                                color = selectedColor,
                                start = startDateTime,
                                end = endDateTime,
                                repeatDays = repeatDays.toList(),
                                exceptionDates = existingEvent?.exceptionDates ?: emptyList(),
                                untilDate = existingEvent?.untilDate
                            )
                            onSave(event)
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Lưu", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
