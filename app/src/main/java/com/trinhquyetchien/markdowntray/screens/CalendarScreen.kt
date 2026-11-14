package com.trinhquyetchien.markdowntray.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
import com.trinhquyetchien.markdowntray.viewmodel.AppViewModelFactory
import com.trinhquyetchien.markdowntray.viewmodel.CalendarViewModel
import kotlinx.coroutines.delay
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

    var showRecurringChoiceDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    // Date Picker Dialog để chọn nhanh bất kỳ ngày nào
    val openDatePicker = {
        val cal = uiState.selectedCalendar
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                viewModel.selectDate(CalendarUtils.formatYMD(newCal.time), newCal)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Lịch biểu",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    // Nút chọn ngày tháng bất kỳ (DatePicker)
                    IconButton(onClick = openDatePicker) {
                        Icon(
                            imageVector = Icons.Rounded.DateRange,
                            contentDescription = "Chọn ngày",
                            tint = MaterialTheme.colorScheme.primary
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
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Thêm sự kiện")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. THANH CHUYỂN ĐỔI CHẾ ĐỘ XEM ("Danh sách" & "Lịch") VÀ THANH ĐIỀU HƯỚNG THỜI GIAN
            TopTimeNavBar(
                selectedCalendar = uiState.selectedCalendar,
                selectedYMD = uiState.selectedYMD,
                todayYMD = uiState.todayYMD,
                viewMode = uiState.viewMode,
                allEvents = uiState.events,
                onSelectDate = { ymd, cal -> viewModel.selectDate(ymd, cal) },
                onNavigatePrevWeek = { viewModel.navigateWeek(-1) },
                onNavigateNextWeek = { viewModel.navigateWeek(1) },
                onNavigateToday = { viewModel.navigateToday() },
                onOpenDatePicker = openDatePicker,
                onViewModeChange = { mode -> viewModel.setViewMode(mode) }
            )

            // Tiêu đề ngày tháng hiện tại
            val currentDayDisplay = remember(uiState.selectedCalendar.time) {
                val cal = uiState.selectedCalendar
                val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "Thứ Hai"
                    Calendar.TUESDAY -> "Thứ Ba"
                    Calendar.WEDNESDAY -> "Thứ Tư"
                    Calendar.THURSDAY -> "Thứ Năm"
                    Calendar.FRIDAY -> "Thứ Sáu"
                    Calendar.SATURDAY -> "Thứ Bảy"
                    Calendar.SUNDAY -> "Chủ Nhật"
                    else -> ""
                }
                "$dayOfWeek, ngày ${cal.get(Calendar.DAY_OF_MONTH)} tháng ${cal.get(Calendar.MONTH) + 1} năm ${cal.get(Calendar.YEAR)}"
            }

            Text(
                text = currentDayDisplay,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // 2. NỘI DUNG THEO 2 CHẾ ĐỘ XEM: DANH SÁCH HOẶC LỊCH
            when (uiState.viewMode) {
                CalendarViewMode.DAY -> {
                    // Chế độ "Danh sách"
                    DayScheduleView(
                        selectedDateYMD = uiState.selectedYMD,
                        occurrences = dayOccurrences,
                        onOccurrenceClick = { occ -> viewModel.setEditingOccurrence(occ) },
                        modifier = Modifier.weight(1f)
                    )
                }
                CalendarViewMode.TIMELINE -> {
                    // Chế độ "Lịch" (Timeline 24h hỗ trợ kéo thả mượt mà, tránh va chạm, tap/drag tạo mới)
                    DraggableTimelineView(
                        selectedDateYMD = uiState.selectedYMD,
                        todayYMD = uiState.todayYMD,
                        occurrences = dayOccurrences,
                        onOccurrenceClick = { occ -> viewModel.setEditingOccurrence(occ) },
                        onOccurrenceTimeChanged = { occ, newStartH, newStartM, newEndH, newEndM ->
                            val sStr = String.format(Locale.US, "%sT%02d:%02d:00", occ.occurrenceDate, newStartH, newStartM)
                            val eStr = String.format(Locale.US, "%sT%02d:%02d:00", occ.occurrenceDate, newEndH, newEndM)
                            val updated = occ.event.copy(start = sStr, end = eStr)
                            if (occ.isRecurring) {
                                pendingAction = { applyToAll ->
                                    viewModel.updateRecurringEvent(occ.event, updated, occ.occurrenceDate, applyToAll = applyToAll)
                                    Toast.makeText(context, "Đã cập nhật giờ sự kiện", Toast.LENGTH_SHORT).show()
                                }
                                showRecurringChoiceDialog = true
                            } else {
                                viewModel.updateEvent(updated)
                                Toast.makeText(context, "Đã cập nhật giờ sự kiện", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCreateEventAtTime = { hour, minute ->
                            // Tap hoặc drag để tạo task mới tại mốc giờ
                            val sH = hour.coerceIn(0, 23)
                            val sM = (minute / 15) * 15
                            val eH = (sH + (sM + 60) / 60).coerceAtMost(23)
                            val eM = (sM + 60) % 60
                            val startTimeStr = String.format(Locale.US, "%02d:%02d", sH, sM)
                            val endTimeStr = String.format(Locale.US, "%02d:%02d", eH, eM)
                            val tempEvent = CalendarEvent(
                                id = System.currentTimeMillis().toString(),
                                title = "",
                                start = "${uiState.selectedYMD}T${startTimeStr}:00",
                                end = "${uiState.selectedYMD}T${endTimeStr}:00"
                            )
                            viewModel.setEditingOccurrence(
                                CalendarOccurrence(
                                    event = tempEvent,
                                    occurrenceDate = uiState.selectedYMD,
                                    startHour = sH,
                                    startMinute = sM,
                                    endHour = eH,
                                    endMinute = eM,
                                    isRecurring = false
                                )
                            )
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
            onSave = { newEvent ->
                val success = viewModel.addEvent(newEvent)
                if (success) {
                    Toast.makeText(context, "Đã thêm sự kiện", Toast.LENGTH_SHORT).show()
                }
                viewModel.setShowNewEventDialog(false)
            },
            onDelete = null
        )
    }

    // Modal sửa Occurrence hiện tại
    if (uiState.editingOccurrence != null) {
        val occ = uiState.editingOccurrence!!
        val isNewFromTimeline = occ.event.title.isEmpty()

        EventEditDialog(
            initialDateYMD = occ.occurrenceDate,
            existingEvent = if (isNewFromTimeline) null else occ.event,
            occurrenceDate = occ.occurrenceDate,
            defaultStartTime = String.format(Locale.US, "%02d:%02d", occ.startHour, occ.startMinute),
            defaultEndTime = String.format(Locale.US, "%02d:%02d", occ.endHour, occ.endMinute),
            onDismiss = { viewModel.setEditingOccurrence(null) },
            onSave = { updatedEvent ->
                if (isNewFromTimeline) {
                    val success = viewModel.addEvent(updatedEvent)
                    if (success) {
                        Toast.makeText(context, "Đã tạo sự kiện", Toast.LENGTH_SHORT).show()
                    }
                    viewModel.setEditingOccurrence(null)
                } else if (occ.isRecurring) {
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
            onDelete = if (isNewFromTimeline) null else {
                {
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
            }
        )

        if (showRecurringChoiceDialog && pendingAction != null) {
            AlertDialog(
                onDismissRequest = {
                    showRecurringChoiceDialog = false
                    pendingAction = null
                },
                title = { Text("Lặp lại công việc", fontWeight = FontWeight.SemiBold) },
                text = { Text("Công việc này được lặp lại hàng tuần. Bạn muốn áp dụng thay đổi như thế nào?") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingAction?.invoke(true)
                        showRecurringChoiceDialog = false
                        pendingAction = null
                        viewModel.setEditingOccurrence(null)
                    }) {
                        Text("Áp dụng cho toàn bộ các lần lặp lại sau này", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        pendingAction?.invoke(false)
                        showRecurringChoiceDialog = false
                        pendingAction = null
                        viewModel.setEditingOccurrence(null)
                    }) {
                        Text("Chỉ áp dụng cho công việc này")
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------
// Thanh điều hướng thời gian trên cùng (Tuần, Các nút ngày, DatePicker, Chuyển chế độ)
// -------------------------------------------------------------
@Composable
private fun TopTimeNavBar(
    selectedCalendar: Calendar,
    selectedYMD: String,
    todayYMD: String,
    viewMode: CalendarViewMode,
    allEvents: List<CalendarEvent>,
    onSelectDate: (String, Calendar) -> Unit,
    onNavigatePrevWeek: () -> Unit,
    onNavigateNextWeek: () -> Unit,
    onNavigateToday: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onViewModeChange: (CalendarViewMode) -> Unit
) {
    val weekDates = remember(selectedCalendar) {
        CalendarUtils.getWeekDates(selectedCalendar)
    }
    val dayNames = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Hàng 1: Nút tuần trước, Hôm nay, Tuần sau + Tab chuyển chế độ (Danh sách / Lịch)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Điều hướng tuần
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigatePrevWeek,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Tuần trước", modifier = Modifier.size(18.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { onNavigateToday() }
                    ) {
                        Text(
                            text = "Hôm nay",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onNavigateNextWeek,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Tuần sau", modifier = Modifier.size(18.dp))
                    }
                }

                // 2 Chế độ xem duy nhất: "Danh sách" & "Lịch"
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .padding(3.dp)
                ) {
                    ViewModeButton(
                        label = "Danh sách",
                        selected = viewMode == CalendarViewMode.DAY,
                        onClick = { onViewModeChange(CalendarViewMode.DAY) }
                    )
                    ViewModeButton(
                        label = "Lịch",
                        selected = viewMode == CalendarViewMode.TIMELINE,
                        onClick = { onViewModeChange(CalendarViewMode.TIMELINE) }
                    )
                }
            }

            // Hàng 2: Dải 7 nút ngày trong tuần hiện tại
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDates.forEachIndexed { idx, date ->
                    val dateCal = Calendar.getInstance().apply { time = date }
                    val ymd = CalendarUtils.formatYMD(date)
                    val isSelected = ymd == selectedYMD
                    val isToday = ymd == todayYMD

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else Color.Transparent
                            )
                            .clickable { onSelectDate(ymd, dateCal) }
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = dayNames[idx],
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = SimpleDateFormat("dd", Locale.getDefault()).format(date),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )

                        // Chấm chỉ báo sự kiện có trong ngày
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

            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
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
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -------------------------------------------------------------
// Interactive 24H Draggable Timeline View (Smooth animation + Collision Avoidance + Tap/Drag to Add + Current Time Indicator)
// -------------------------------------------------------------
private data class OccurrenceLayout(
    val occurrence: CalendarOccurrence,
    val columnIndex: Int,
    val totalColumns: Int
)

private fun computeCollisionLayout(occurrences: List<CalendarOccurrence>): List<OccurrenceLayout> {
    if (occurrences.isEmpty()) return emptyList()

    // Chuyển sang danh sách sự kiện có phút bắt đầu/kết thúc
    class TempItem(val occ: CalendarOccurrence, val startMin: Int, val endMin: Int)
    val items = occurrences.map { occ ->
        val s = occ.startHour * 60 + occ.startMinute
        val e = (occ.endHour * 60 + occ.endMinute).let { if (it <= s) s + 30 else it }
        TempItem(occ, s, e)
    }.sortedWith(compareBy({ it.startMin }, { it.endMin }))

    // Thuật toán gán cột (interval coloring)
    val columns = mutableListOf<MutableList<TempItem>>()
    val assignedColumn = mutableMapOf<String, Int>()

    for (item in items) {
        var placed = false
        for (colIdx in columns.indices) {
            val col = columns[colIdx]
            if (col.last().endMin <= item.startMin) {
                col.add(item)
                assignedColumn[item.occ.event.id] = colIdx
                placed = true
                break
            }
        }
        if (!placed) {
            assignedColumn[item.occ.event.id] = columns.size
            columns.add(mutableListOf(item))
        }
    }

    // Nhóm các cụm va chạm (overlapping clusters) để tính tổng số cột
    val clusters = mutableListOf<MutableList<TempItem>>()
    var currentCluster = mutableListOf<TempItem>()
    var clusterEnd = -1

    for (item in items) {
        if (currentCluster.isEmpty()) {
            currentCluster.add(item)
            clusterEnd = item.endMin
        } else if (item.startMin < clusterEnd) {
            currentCluster.add(item)
            clusterEnd = maxOf(clusterEnd, item.endMin)
        } else {
            clusters.add(currentCluster)
            currentCluster = mutableListOf(item)
            clusterEnd = item.endMin
        }
    }
    if (currentCluster.isNotEmpty()) {
        clusters.add(currentCluster)
    }

    val totalColumnsMap = mutableMapOf<String, Int>()
    for (cluster in clusters) {
        val maxCol = cluster.maxOf { assignedColumn[it.occ.event.id] ?: 0 } + 1
        for (it in cluster) {
            totalColumnsMap[it.occ.event.id] = maxCol
        }
    }

    return items.map { item ->
        OccurrenceLayout(
            occurrence = item.occ,
            columnIndex = assignedColumn[item.occ.event.id] ?: 0,
            totalColumns = totalColumnsMap[item.occ.event.id] ?: 1
        )
    }
}

@Composable
private fun DraggableTimelineView(
    selectedDateYMD: String,
    todayYMD: String,
    occurrences: List<CalendarOccurrence>,
    onOccurrenceClick: (CalendarOccurrence) -> Unit,
    onOccurrenceTimeChanged: (CalendarOccurrence, startH: Int, startM: Int, endH: Int, endM: Int) -> Unit,
    onCreateEventAtTime: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hourHeightDp = 64.dp
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeightDp.toPx() }

    val scrollState = rememberScrollState()

    // Tự động scroll đến 7h sáng
    LaunchedEffect(Unit) {
        scrollState.scrollTo((7 * hourHeightPx).toInt())
    }

    // Tính toán bố cục đa cột né va chạm
    val layouts = remember(occurrences) {
        computeCollisionLayout(occurrences)
    }

    // 1. Đường chỉ thời gian thực hiện tại (Current Time Indicator - Chỉ hiển thị khi đang xem ngày HÔM NAY)
    val isToday = selectedDateYMD == todayYMD
    var currentMinuteOfDay by remember {
        val now = Calendar.getInstance()
        mutableIntStateOf(now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE))
    }

    LaunchedEffect(isToday) {
        if (isToday) {
            while (true) {
                val now = Calendar.getInstance()
                currentMinuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                delay(30_000L) // Cập nhật mỗi 30 giây
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Grid 24 giờ & Cho phép click/tap vào chỗ trống để tạo sự kiện mới
        Column(modifier = Modifier.fillMaxWidth()) {
            for (hour in 0..23) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(hourHeightDp)
                        .pointerInput(hour) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val minute = ((offset.y / hourHeightPx) * 60).toInt()
                                    onCreateEventAtTime(hour, minute)
                                }
                            )
                        },
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
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Overlay các sự kiện hỗ trợ kéo thả mượt mà & né va chạm thông minh
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 54.dp, end = 12.dp)
        ) {
            val totalAvailableWidth = maxWidth

            layouts.forEach { layout ->
                val totalCols = layout.totalColumns
                val colIdx = layout.columnIndex
                val colWidth = totalAvailableWidth / totalCols

                DraggableEventBlock(
                    occurrence = layout.occurrence,
                    hourHeightPx = hourHeightPx,
                    offsetX = colWidth * colIdx,
                    itemWidth = colWidth - 4.dp,
                    onClick = { onOccurrenceClick(layout.occurrence) },
                    onDragCommit = { startH, startM, endH, endM ->
                        onOccurrenceTimeChanged(layout.occurrence, startH, startM, endH, endM)
                    }
                )
            }
        }

        // ĐƯỜNG KẺ ĐỎ HIỆN TẠI (Current Time Line) - CHỈ HIỂN THỊ KHI LÀ NGÀY HÔM NAY
        if (isToday) {
            val currentTimeTopPx = (currentMinuteOfDay / 60f) * hourHeightPx
            val currentTimeTopDp = with(density) { currentTimeTopPx.toDp() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = currentTimeTopDp - 6.dp)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mốc chấm đỏ + nhãn giờ
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .padding(start = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEF4444)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", currentMinuteOfDay / 60, currentMinuteOfDay % 60),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // Chấm tròn đỏ
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                )

                // Đường kẻ ngang đỏ đậm xuyên suốt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(Color(0xFFEF4444))
                )
            }
        }
    }
}

@Composable
private fun DraggableEventBlock(
    occurrence: CalendarOccurrence,
    hourHeightPx: Float,
    offsetX: androidx.compose.ui.unit.Dp,
    itemWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    onDragCommit: (startH: Int, startM: Int, endH: Int, endM: Int) -> Unit
) {
    val durationMin = occurrence.durationMinutes
    val durationHours = durationMin / 60f
    val baseTopPx = (occurrence.startHour + occurrence.startMinute / 60f) * hourHeightPx
    val heightPx = (durationHours * hourHeightPx).coerceAtLeast(36f)

    val density = LocalDensity.current

    // TỐI ƯU HIỆU NĂNG: Sử dụng graphicsLayer / fast translationY và IntOffset
    // Tránh recomposition toàn bộ màn hình khi kéo
    var dragDeltaY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val evColor = try {
        Color(android.graphics.Color.parseColor(occurrence.event.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val baseTopDp = with(density) { baseTopPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }

    Surface(
        modifier = Modifier
            .offset(x = offsetX, y = baseTopDp)
            .graphicsLayer {
                translationY = dragDeltaY
                shadowElevation = if (isDragging) 8.dp.toPx() else 2.dp.toPx()
                alpha = if (isDragging) 0.95f else 0.90f
                scaleX = if (isDragging) 1.02f else 1.0f
                scaleY = if (isDragging) 1.02f else 1.0f
            }
            .width(itemWidth)
            .height(heightDp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(occurrence.event.id) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDeltaY += dragAmount.y
                    },
                    onDragEnd = {
                        val currentY = baseTopPx + dragDeltaY
                        val newY = currentY.coerceIn(0f, 23.5f * hourHeightPx)
                        val totalMinutes = (newY / hourHeightPx * 60f).roundToInt()
                        // Snap chính xác mốc 15 phút
                        val snappedMin = ((totalMinutes + 7) / 15) * 15
                        val newStartH = snappedMin / 60
                        val newStartM = snappedMin % 60

                        val endTotalMin = (snappedMin + durationMin).coerceAtMost(24 * 60 - 1)
                        val newEndH = endTotalMin / 60
                        val newEndM = endTotalMin % 60

                        dragDeltaY = 0f
                        isDragging = false
                        onDragCommit(newStartH, newStartM, newEndH, newEndM)
                    },
                    onDragCancel = {
                        dragDeltaY = 0f
                        isDragging = false
                    }
                )
            }
            .clickable { onClick() },
        color = evColor.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = occurrence.event.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (occurrence.event.alarmEnabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "Báo thức",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Text(
                    text = String.format(Locale.US, "%02d:%02d - %02d:%02d", occurrence.startHour, occurrence.startMinute, occurrence.endHour, occurrence.endMinute),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Icon(
                imageVector = Icons.Rounded.TouchApp,
                contentDescription = "Kéo để đổi giờ",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// Chế độ "Danh sách": Hiển thị các công việc/sự kiện trong ngày
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
                    imageVector = Icons.Rounded.Event,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Không có công việc nào trong ngày",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Bấm dấu + bên dưới để thêm công việc mới",
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
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
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

                    if (ev.alarmEnabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "Báo thức",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (occurrence.isRecurring) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "Lặp lại",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
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

                    if (ev.alarmEnabled) {
                        val leadText = if (ev.alarmLeadMinutes == 0) "Đúng giờ" else "Trước ${ev.alarmLeadMinutes}p"
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "⏰ $leadText",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
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
// Dialog: Thêm / Sửa Sự kiện (Tích hợp tính năng Báo thức / Nhắc nhở)
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventEditDialog(
    initialDateYMD: String,
    existingEvent: CalendarEvent?,
    occurrenceDate: String,
    defaultStartTime: String? = null,
    defaultEndTime: String? = null,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var note by remember { mutableStateOf(existingEvent?.note ?: "") }
    var selectedColor by remember { mutableStateOf(existingEvent?.color ?: "#0F766E") }

    var dateStr by remember { mutableStateOf(occurrenceDate) }

    val initialStart = defaultStartTime ?: if (existingEvent != null && existingEvent.start.length >= 16) {
        existingEvent.start.substring(11, 16)
    } else "09:00"

    val initialEnd = defaultEndTime ?: if (existingEvent != null && existingEvent.end.length >= 16) {
        existingEvent.end.substring(11, 16)
    } else "10:00"

    var startTime by remember { mutableStateOf(initialStart) }
    var endTime by remember { mutableStateOf(initialEnd) }

    // Tính năng Báo thức / Nhắc nhở
    var alarmEnabled by remember { mutableStateOf(existingEvent?.alarmEnabled ?: false) }
    var alarmLeadMinutes by remember { mutableIntStateOf(existingEvent?.alarmLeadMinutes ?: 0) }

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

    val alarmLeadOptions = listOf(
        Pair(0, "Đúng giờ"),
        Pair(5, "Trước 5p"),
        Pair(15, "Trước 15p"),
        Pair(30, "Trước 30p"),
        Pair(60, "Trước 1h")
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
                        text = if (existingEvent == null) "Thêm công việc" else "Chi tiết công việc",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

                // TÍNH NĂNG BÁO THỨC / NHẮC NHỞ HẸN GIỜ
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Alarm,
                                    contentDescription = null,
                                    tint = if (alarmEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Báo thức & Nhắc nhở",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (alarmEnabled) "Sẽ phát chuông/rung khi đến giờ" else "Tắt nhắc nhở",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = alarmEnabled,
                                onCheckedChange = { alarmEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        if (alarmEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Thời gian nhắc trước:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                alarmLeadOptions.forEach { (mins, label) ->
                                    val isSelected = alarmLeadMinutes == mins
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.clickable { alarmLeadMinutes = mins }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color picker
                Text("Màu sắc", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
                            Icon(Icons.Rounded.Delete, contentDescription = "Xóa công việc", tint = MaterialTheme.colorScheme.error)
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
                                untilDate = existingEvent?.untilDate,
                                alarmEnabled = alarmEnabled,
                                alarmLeadMinutes = alarmLeadMinutes
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
