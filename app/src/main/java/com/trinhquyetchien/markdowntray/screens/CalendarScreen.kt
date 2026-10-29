package com.trinhquyetchien.markdowntray.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.TouchApp
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trinhquyetchien.markdowntray.data.CalendarEvent
import com.trinhquyetchien.markdowntray.data.CalendarOccurrence
import com.trinhquyetchien.markdowntray.data.CalendarUtils
import com.trinhquyetchien.markdowntray.data.CalendarViewMode
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.data.SyncStatusInfo
import com.trinhquyetchien.markdowntray.ui.theme.StatusGreen
import com.trinhquyetchien.markdowntray.ui.theme.StatusOrange
import com.trinhquyetchien.markdowntray.viewmodel.AppViewModelFactory
import com.trinhquyetchien.markdowntray.viewmodel.CalendarViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
                                        if (uiState.syncInfo?.isSynced == true) StatusGreen
                                        else StatusOrange
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
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Làm mới",
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
                onClick = { viewModel.setShowNewEventDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Thêm sự kiện")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Controls: 4 View Modes (Danh sách ngày, Kéo thả Timeline, Tuần, Tháng)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Navigation prev/today/next
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.navigatePrevious() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Trước", modifier = Modifier.size(18.dp))
                        }

                        TextButton(
                            onClick = { viewModel.navigateToday() },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = if (uiState.selectedYMD == uiState.todayYMD) "Hôm nay" else "Hôm nay",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.navigateNext() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Sau", modifier = Modifier.size(18.dp))
                        }
                    }

                    // Mode switch tabs
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        ViewModeButton(
                            label = "DS",
                            selected = uiState.viewMode == CalendarViewMode.DAY,
                            onClick = { viewModel.setViewMode(CalendarViewMode.DAY) }
                        )
                        ViewModeButton(
                            label = "Giờ",
                            selected = uiState.viewMode == CalendarViewMode.TIMELINE,
                            onClick = { viewModel.setViewMode(CalendarViewMode.TIMELINE) }
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
                CalendarViewMode.DAY, CalendarViewMode.TIMELINE -> SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.forLanguageTag("vi-VN")).format(uiState.selectedCalendar.time)
                CalendarViewMode.WEEK -> "Tuần ${uiState.selectedCalendar.get(Calendar.WEEK_OF_YEAR)} - Tháng ${uiState.selectedCalendar.get(Calendar.MONTH) + 1}/${uiState.selectedCalendar.get(Calendar.YEAR)}"
                CalendarViewMode.MONTH -> SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("vi-VN")).format(uiState.selectedCalendar.time)
            }
            Text(
                text = headerTitle.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
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
                CalendarViewMode.TIMELINE -> {
                    // Kéo thả và hiển thị trực quan 24 giờ như trên Web
                    DraggableTimelineView(
                        selectedDateYMD = uiState.selectedYMD,
                        occurrences = dayOccurrences,
                        onOccurrenceClick = { occ -> viewModel.setEditingOccurrence(occ) },
                        onOccurrenceTimeChanged = { occ, newStartH, newStartM, newEndH, newEndM ->
                            val sStr = String.format(Locale.US, "%sT%02d:%02d:00", occ.occurrenceDate, newStartH, newStartM)
                            val eStr = String.format(Locale.US, "%sT%02d:%02d:00", occ.occurrenceDate, newEndH, newEndM)
                            val updated = occ.event.copy(start = sStr, end = eStr)
                            if (occ.isRecurring) {
                                viewModel.updateRecurringEvent(occ.event, updated, occ.occurrenceDate, applyToAll = true)
                            } else {
                                viewModel.updateEvent(updated)
                            }
                            Toast.makeText(context, "Đã cập nhật giờ sự kiện", Toast.LENGTH_SHORT).show()
                        },
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
                            viewModel.setViewMode(CalendarViewMode.TIMELINE)
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
                title = { Text("Lặp lại sự kiện", fontWeight = FontWeight.SemiBold) },
                text = { Text("Bạn muốn áp dụng thay đổi này chỉ cho ngày này hay toàn bộ chuỗi sự kiện?") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingAction?.invoke(true)
                        showRecurringChoiceDialog = false
                        pendingAction = null
                        viewModel.setEditingOccurrence(null)
                    }) {
                        Text("Tất cả sự kiện", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -------------------------------------------------------------
// Interactive 24H Draggable Timeline View (Tương tự Calendar Web)
// -------------------------------------------------------------
@Composable
private fun DraggableTimelineView(
    selectedDateYMD: String,
    occurrences: List<CalendarOccurrence>,
    onOccurrenceClick: (CalendarOccurrence) -> Unit,
    onOccurrenceTimeChanged: (CalendarOccurrence, startH: Int, startM: Int, endH: Int, endM: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hourHeightDp = 60.dp
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeightDp.toPx() }

    val scrollState = rememberScrollState()

    // Tự động scroll đến 8h sáng lúc ban đầu
    LaunchedEffect(Unit) {
        scrollState.scrollTo((8 * hourHeightPx).toInt())
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Grid 24 giờ
        Column(modifier = Modifier.fillMaxWidth()) {
            for (hour in 0..23) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(hourHeightDp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = String.format(Locale.US, "%02d:00", hour),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .width(52.dp)
                            .padding(start = 8.dp, top = 2.dp)
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Overlay các sự kiện có thể kéo thả thay đổi giờ
        occurrences.forEach { occ ->
            DraggableEventBlock(
                occurrence = occ,
                hourHeightPx = hourHeightPx,
                onClick = { onOccurrenceClick(occ) },
                onDragCommit = { startH, startM, endH, endM ->
                    onOccurrenceTimeChanged(occ, startH, startM, endH, endM)
                }
            )
        }
    }
}

@Composable
private fun DraggableEventBlock(
    occurrence: CalendarOccurrence,
    hourHeightPx: Float,
    onClick: () -> Unit,
    onDragCommit: (startH: Int, startM: Int, endH: Int, endM: Int) -> Unit
) {
    val durationMin = occurrence.durationMinutes
    val durationHours = durationMin / 60f
    val baseTopPx = (occurrence.startHour + occurrence.startMinute / 60f) * hourHeightPx
    val heightPx = (durationHours * hourHeightPx).coerceAtLeast(36f)

    var dragOffsetY by remember(baseTopPx) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val evColor = try {
        Color(android.graphics.Color.parseColor(occurrence.event.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val topPx = baseTopPx + dragOffsetY
    val topDp = with(density) { topPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }

    Surface(
        modifier = Modifier
            .padding(start = 60.dp, end = 16.dp)
            .offset(y = topDp)
            .fillMaxWidth()
            .height(heightDp)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(occurrence.event.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                    },
                    onDragEnd = {
                        val newY = (baseTopPx + dragOffsetY).coerceIn(0f, 23.5f * hourHeightPx)
                        val totalMinutes = (newY / hourHeightPx * 60f).roundToInt()
                        // Làm tròn về mỗi 15 phút
                        val snappedMin = (totalMinutes / 15) * 15
                        val newStartH = snappedMin / 60
                        val newStartM = snappedMin % 60

                        val endTotalMin = (snappedMin + durationMin).coerceAtMost(24 * 60 - 1)
                        val newEndH = endTotalMin / 60
                        val newEndM = endTotalMin % 60

                        dragOffsetY = 0f
                        onDragCommit(newStartH, newStartM, newEndH, newEndM)
                    },
                    onDragCancel = {
                        dragOffsetY = 0f
                    }
                )
            }
            .clickable { onClick() },
        color = evColor.copy(alpha = 0.88f),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = occurrence.event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = String.format(Locale.US, "%02d:%02d - %02d:%02d", occurrence.startHour, occurrence.startMinute, occurrence.endHour, occurrence.endMinute),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Icon(
                imageVector = Icons.Outlined.TouchApp,
                contentDescription = "Kéo để đổi giờ",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
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
                    imageVector = Icons.Outlined.Event,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Không có lịch hẹn nào trong ngày",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Bấm dấu + để thêm sự kiện mới",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(eventColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ev.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (occurrence.isRecurring) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Repeat,
                            contentDescription = "Lặp lại",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
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
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = ev.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                            else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
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
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(7.dp))
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

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

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
    var selectedColor by remember { mutableStateOf(existingEvent?.color ?: "#0F766E") }

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
        "#0F766E", // Emerald (Mặc định)
        "#2DD4BF", // Mint
        "#3B82F6", // Blue
        "#10B981", // Green
        "#F59E0B", // Amber
        "#EF4444", // Red
        "#8B5CF6", // Purple
        "#64748B"  // Slate
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
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        Icon(Icons.Outlined.Close, contentDescription = "Đóng", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Note Input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú / Mô tả") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
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
                            Text("Bắt đầu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(startTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                            Text("Kết thúc", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(endTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                            Icon(Icons.Outlined.Delete, contentDescription = "Xóa sự kiện", tint = MaterialTheme.colorScheme.error)
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Lưu", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
