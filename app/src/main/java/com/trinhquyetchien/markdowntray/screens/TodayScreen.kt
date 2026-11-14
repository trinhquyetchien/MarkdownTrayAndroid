package com.trinhquyetchien.markdowntray.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trinhquyetchien.markdowntray.data.OverdueTask
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.ui.theme.StatusOrange
import com.trinhquyetchien.markdowntray.viewmodel.AppViewModelFactory
import com.trinhquyetchien.markdowntray.viewmodel.TodayViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    storageManager: StorageManager,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = viewModel(
        factory = AppViewModelFactory(androidx.compose.ui.platform.LocalContext.current, storageManager)
    )
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val isViewingToday = uiState.selectedDateStr == uiState.todayDateStr

    // Định dạng đầy đủ: "Thứ ..., ngày dd tháng MM năm yyyy"
    val fullDateTitle = remember(uiState.selectedCalendar.time) {
        val cal = uiState.selectedCalendar
        val dayOfWeek = when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> "Thứ Hai"
            java.util.Calendar.TUESDAY -> "Thứ Ba"
            java.util.Calendar.WEDNESDAY -> "Thứ Tư"
            java.util.Calendar.THURSDAY -> "Thứ Năm"
            java.util.Calendar.FRIDAY -> "Thứ Sáu"
            java.util.Calendar.SATURDAY -> "Thứ Bảy"
            java.util.Calendar.SUNDAY -> "Chủ Nhật"
            else -> ""
        }
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val year = cal.get(java.util.Calendar.YEAR)
        "$dayOfWeek, ngày $day tháng $month năm $year"
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = fullDateTitle,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isViewingToday) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Hôm nay",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            actions = {
                // Hôm trước
                IconButton(onClick = { viewModel.selectPreviousDay() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Ngày trước",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Nút quay về Hôm nay nếu đang xem ngày khác
                if (!isViewingToday) {
                    IconButton(onClick = { viewModel.selectToday() }) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Hôm nay",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Hôm sau
                IconButton(onClick = { viewModel.selectNextDay() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Ngày sau",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Overdue Tasks Section (Chỉ hiển thị khi đang xem ngày hôm nay và có task chưa hoàn thành)
        if (isViewingToday && uiState.overdueTasks.isNotEmpty()) {
            OverdueTasksHeader(
                tasks = uiState.overdueTasks,
                isExpanded = uiState.isOverdueExpanded,
                onToggleExpand = { viewModel.toggleOverdueExpanded() },
                onToggleTask = { task, isChecked ->
                    viewModel.toggleOverdueTask(task, isChecked)
                },
                onMoveToToday = { task ->
                    val success = viewModel.moveOverdueTaskToToday(task)
                    if (success) {
                        Toast.makeText(context, "Đã chuyển task sang hôm nay", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        MarkdownViewer(
            title = "Nội dung Markdown",
            rawContent = uiState.rawContent,
            syncInfo = uiState.syncInfo,
            onToggleTask = { lineIndex, isChecked ->
                val success = viewModel.toggleTask(lineIndex, isChecked)
                if (!success) {
                    Toast.makeText(context, "Không thể cập nhật task", Toast.LENGTH_SHORT).show()
                }
            },
            onAddTask = { taskText ->
                val success = viewModel.addTask(taskText)
                if (success) {
                    Toast.makeText(context, "Đã thêm công việc", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi thêm task", Toast.LENGTH_SHORT).show()
                }
            },
            onSaveRawContent = { newContent ->
                val success = viewModel.saveRawContent(newContent)
                if (success) {
                    Toast.makeText(context, "Đã lưu ghi chú", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi lưu ghi chú", Toast.LENGTH_SHORT).show()
                }
            },
            onRefresh = {
                viewModel.loadContent()
                Toast.makeText(context, "Đã kiểm tra đồng bộ", Toast.LENGTH_SHORT).show()
            },
            onDeleteTask = { lineIndex ->
                val success = viewModel.deleteTask(lineIndex)
                if (success) {
                    Toast.makeText(context, "Đã xóa nhiệm vụ", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Không thể xóa nhiệm vụ", Toast.LENGTH_SHORT).show()
                }
            },
            onUpdateTaskText = { lineIndex, newText ->
                val success = viewModel.updateTaskText(lineIndex, newText)
                if (success) {
                    Toast.makeText(context, "Đã cập nhật nhiệm vụ", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Không thể cập nhật nhiệm vụ", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OverdueTasksHeader(
    tasks: List<OverdueTask>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleTask: (OverdueTask, Boolean) -> Unit,
    onMoveToToday: (OverdueTask) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, StatusOrange.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = StatusOrange.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = StatusOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tồn đọng (${tasks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tasks.forEach { task ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = false,
                                    onCheckedChange = { checked ->
                                        onToggleTask(task, checked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        uncheckedColor = StatusOrange
                                    )
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = task.dateStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusOrange
                                    )
                                }

                                IconButton(
                                    onClick = { onMoveToToday(task) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = "Chuyển sang hôm nay",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
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

