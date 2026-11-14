package com.trinhquyetchien.markdowntray.screens

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trinhquyetchien.markdowntray.alarm.CalendarAlarmScheduler
import com.trinhquyetchien.markdowntray.data.CalendarEvent
import com.trinhquyetchien.markdowntray.data.StandaloneAlarm
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.viewmodel.AppViewModelFactory
import com.trinhquyetchien.markdowntray.viewmodel.CalendarViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    storageManager: StorageManager,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(
        factory = AppViewModelFactory(LocalContext.current, storageManager)
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Standalone Alarms state
    var standaloneAlarms by remember {
        mutableStateOf(storageManager.loadStandaloneAlarms())
    }

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Cần cấp quyền thông báo để chuông báo thức hoạt động", Toast.LENGTH_LONG).show()
        }
    }

    // Check & request permissions on launch
    LaunchedEffect(Unit) {
        CalendarAlarmScheduler.createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Helper to persist standalone alarms and reschedule
    fun updateStandaloneAlarmsList(newList: List<StandaloneAlarm>) {
        standaloneAlarms = newList
        storageManager.saveStandaloneAlarms(newList)
        CalendarAlarmScheduler.scheduleUpcomingAlarms(context, storageManager)
    }

    // Calendar events with alarms enabled
    val calendarAlarmEvents = remember(uiState.events) {
        uiState.events.filter { it.alarmEnabled }
            .sortedWith(compareBy({ it.start }, { it.title }))
    }

    val totalAlarmCount = standaloneAlarms.size + calendarAlarmEvents.size

    var editingStandaloneAlarm by remember { mutableStateOf<StandaloneAlarm?>(null) }
    var showNewStandaloneAlarmDialog by remember { mutableStateOf(false) }
    var editingCalendarEvent by remember { mutableStateOf<CalendarEvent?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Báo thức",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (totalAlarmCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "$totalAlarmCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewStandaloneAlarmDialog = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Thêm báo thức mới")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (standaloneAlarms.isEmpty() && calendarAlarmEvents.isEmpty()) {
                // Empty state đẹp mắt
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AlarmOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chưa có báo thức nào",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Nhấn nút + bên dưới để tạo báo thức độc lập, hoặc bật báo thức cho các sự kiện trong Lịch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        OutlinedButton(
                            onClick = { showNewStandaloneAlarmDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Thêm báo thức mới")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Mục Báo thức Độc Lập
                    if (standaloneAlarms.isNotEmpty()) {
                        item {
                            Text(
                                text = "Báo thức cố định",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        items(standaloneAlarms, key = { "standalone_${it.id}" }) { alarm ->
                            StandaloneAlarmItemCard(
                                alarm = alarm,
                                onToggleAlarm = { isChecked ->
                                    val updated = standaloneAlarms.map {
                                        if (it.id == alarm.id) it.copy(isEnabled = isChecked) else it
                                    }
                                    updateStandaloneAlarmsList(updated)
                                    val msg = if (isChecked) "Đã bật báo thức" else "Đã tắt báo thức"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onClick = { editingStandaloneAlarm = alarm }
                            )
                        }
                    }

                    // 2. Mục Báo thức từ Sự kiện Lịch
                    if (calendarAlarmEvents.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Báo thức từ Lịch công việc",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        items(calendarAlarmEvents, key = { "calendar_${it.id}" }) { event ->
                            CalendarAlarmItemCard(
                                event = event,
                                onToggleAlarm = { isEnabled ->
                                    val updated = event.copy(alarmEnabled = isEnabled)
                                    val success = viewModel.updateEvent(updated)
                                    if (success) {
                                        val msg = if (isEnabled) "Đã bật báo thức lịch" else "Đã tắt báo thức lịch"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        CalendarAlarmScheduler.scheduleUpcomingAlarms(context, storageManager)
                                    }
                                },
                                onClick = { editingCalendarEvent = event }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Edit Standalone Alarm
    if (editingStandaloneAlarm != null) {
        val al = editingStandaloneAlarm!!
        StandaloneAlarmDialog(
            existingAlarm = al,
            onDismiss = { editingStandaloneAlarm = null },
            onSave = { updated ->
                val updatedList = standaloneAlarms.map { if (it.id == updated.id) updated else it }
                updateStandaloneAlarmsList(updatedList)
                Toast.makeText(context, "Đã cập nhật báo thức", Toast.LENGTH_SHORT).show()
                editingStandaloneAlarm = null
            },
            onDelete = {
                val updatedList = standaloneAlarms.filter { it.id != al.id }
                updateStandaloneAlarmsList(updatedList)
                Toast.makeText(context, "Đã xóa báo thức", Toast.LENGTH_SHORT).show()
                editingStandaloneAlarm = null
            }
        )
    }

    // Modal Create New Standalone Alarm
    if (showNewStandaloneAlarmDialog) {
        StandaloneAlarmDialog(
            existingAlarm = null,
            onDismiss = { showNewStandaloneAlarmDialog = false },
            onSave = { newAlarm ->
                val updatedList = standaloneAlarms + newAlarm
                updateStandaloneAlarmsList(updatedList)
                Toast.makeText(context, "Đã tạo báo thức mới", Toast.LENGTH_SHORT).show()
                showNewStandaloneAlarmDialog = false
            },
            onDelete = null
        )
    }

    // Modal Edit Calendar Event
    if (editingCalendarEvent != null) {
        val ev = editingCalendarEvent!!
        CalendarAlarmDetailDialog(
            event = ev,
            onDismiss = { editingCalendarEvent = null },
            onSave = { updated ->
                viewModel.updateEvent(updated)
                CalendarAlarmScheduler.scheduleUpcomingAlarms(context, storageManager)
                Toast.makeText(context, "Đã cập nhật công việc", Toast.LENGTH_SHORT).show()
                editingCalendarEvent = null
            },
            onDelete = {
                viewModel.deleteEvent(ev.id)
                CalendarAlarmScheduler.scheduleUpcomingAlarms(context, storageManager)
                Toast.makeText(context, "Đã xóa công việc", Toast.LENGTH_SHORT).show()
                editingCalendarEvent = null
            }
        )
    }
}

// -------------------------------------------------------------
// Component: Card Báo thức Độc Lập (Standalone Alarm)
// -------------------------------------------------------------
@Composable
private fun StandaloneAlarmItemCard(
    alarm: StandaloneAlarm,
    onToggleAlarm: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)

    // Chuỗi hiển thị các ngày lặp lại
    val repeatDisplay = remember(alarm.repeatDays) {
        if (alarm.repeatDays.isEmpty()) {
            "Một lần"
        } else if (alarm.repeatDays.size == 7) {
            "Mỗi ngày"
        } else if (alarm.repeatDays.containsAll(listOf(1, 2, 3, 4, 5)) && alarm.repeatDays.size == 5) {
            "Thứ Hai - Thứ Sáu"
        } else {
            val dayNames = mapOf(
                1 to "T2", 2 to "T3", 3 to "T4", 4 to "T5", 5 to "T6", 6 to "T7", 0 to "CN"
            )
            // Sắp xếp T2..CN
            val sorted = alarm.repeatDays.sortedWith(compareBy { if (it == 0) 7 else it })
            sorted.mapNotNull { dayNames[it] }.joinToString(", ")
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon chuông nổi bật
            Surface(
                shape = CircleShape,
                color = if (alarm.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Giờ và phút
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Nhãn và ngày lặp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (alarm.title.isNotBlank()) {
                        Text(
                            text = alarm.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "  •  ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = repeatDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggleAlarm,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

// -------------------------------------------------------------
// Component: Card Báo thức từ Task trong Lịch (Calendar Event Alarm)
// -------------------------------------------------------------
@Composable
private fun CalendarAlarmItemCard(
    event: CalendarEvent,
    onToggleAlarm: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val evColor = try {
        Color(android.graphics.Color.parseColor(event.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val dateDisplay = remember(event.start) {
        if (event.start.length >= 10) {
            val ymd = event.start.substring(0, 10)
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(ymd)
                if (parsed != null) {
                    SimpleDateFormat("EEEE, dd/MM", Locale.forLanguageTag("vi-VN")).format(parsed).replaceFirstChar { it.uppercase() }
                } else ymd
            } catch (e: Exception) {
                ymd
            }
        } else "Chưa rõ"
    }

    val timeDisplay = remember(event.start, event.end) {
        val s = if (event.start.length >= 16) event.start.substring(11, 16) else "09:00"
        val e = if (event.end.length >= 16) event.end.substring(11, 16) else "10:00"
        "$s - $e"
    }

    val leadText = when (event.alarmLeadMinutes) {
        0 -> "Đúng giờ"
        5 -> "Trước 5 phút"
        15 -> "Trước 15 phút"
        30 -> "Trước 30 phút"
        60 -> "Trước 1 giờ"
        else -> "Trước ${event.alarmLeadMinutes} phút"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thanh màu bên trái
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(evColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Nhãn [Lịch] + Tiêu đề task
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "[Lịch]",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Ngày và Giờ
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$timeDisplay  •  $dateDisplay",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Thời gian nhắc trước
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "⏰ $leadText",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (event.repeatDays.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Lặp tuần",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = event.alarmEnabled,
                onCheckedChange = onToggleAlarm,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

// -------------------------------------------------------------
// Dialog: Thêm / Sửa Báo Thức Độc Lập (Standalone Alarm)
// -------------------------------------------------------------
@Composable
private fun StandaloneAlarmDialog(
    existingAlarm: StandaloneAlarm?,
    onDismiss: () -> Unit,
    onSave: (StandaloneAlarm) -> Unit,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(existingAlarm?.title ?: "") }
    var hour by remember { mutableIntStateOf(existingAlarm?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(existingAlarm?.minute ?: 0) }
    val repeatDays = remember {
        mutableStateListOf<Int>().apply {
            if (existingAlarm != null) addAll(existingAlarm.repeatDays)
        }
    }
    var isEnabled by remember { mutableStateOf(existingAlarm?.isEnabled ?: true) }

    val daysOfWeek = listOf(
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
                        text = if (existingAlarm == null) "Thêm báo thức mới" else "Chỉnh sửa báo thức",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time display & TimePicker trigger
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            TimePickerDialog(context, { _, h, m ->
                                hour = h
                                minute = m
                            }, hour, minute, true).show()
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Chạm để đổi giờ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên báo thức / Ghi chú") },
                    placeholder = { Text("Ví dụ: Dậy tập thể dục, Uống thuốc...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Repeat Days Chips
                Text(
                    text = "Lặp lại các ngày:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysOfWeek.forEach { (dayInt, label) ->
                        val isSelected = repeatDays.contains(dayInt)
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (isSelected) repeatDays.remove(dayInt) else repeatDays.add(dayInt)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Enabled switch
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bật báo thức",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Hủy")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val alarm = StandaloneAlarm(
                                id = existingAlarm?.id ?: java.util.UUID.randomUUID().toString(),
                                title = title.trim(),
                                hour = hour,
                                minute = minute,
                                repeatDays = repeatDays.toList(),
                                isEnabled = isEnabled
                            )
                            onSave(alarm)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Lưu báo thức", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Dialog: Chi tiết & Bật tắt Báo thức của sự kiện Lịch
// -------------------------------------------------------------
@Composable
private fun CalendarAlarmDetailDialog(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit,
    onDelete: () -> Unit
) {
    var alarmEnabled by remember { mutableStateOf(event.alarmEnabled) }
    var alarmLeadMinutes by remember { mutableIntStateOf(event.alarmLeadMinutes) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Báo thức Lịch công việc",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (event.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            Text(
                                text = "Bật chuông báo thức",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
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
                                text = "Nhắc trước khi bắt đầu:",
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

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Xóa công việc", tint = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = onDismiss) {
                        Text("Đóng")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val updated = event.copy(
                                alarmEnabled = alarmEnabled,
                                alarmLeadMinutes = alarmLeadMinutes
                            )
                            onSave(updated)
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
