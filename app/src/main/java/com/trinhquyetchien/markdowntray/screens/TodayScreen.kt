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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.ui.text.style.TextDecoration
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

    val displayFormat = remember { SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN")) }
    val isViewingToday = uiState.selectedDateStr == uiState.todayDateStr

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = if (isViewingToday) "Hôm nay" else uiState.selectedDateStr,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = displayFormat.format(uiState.selectedCalendar.time).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                // Hôm trước
                IconButton(onClick = { viewModel.selectPreviousDay() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Ngày trước",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Nút quay về Hôm nay nếu đang xem ngày khác
                if (!isViewingToday) {
                    IconButton(onClick = { viewModel.selectToday() }) {
                        Icon(
                            imageVector = Icons.Outlined.Today,
                            contentDescription = "Hôm nay",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Hôm sau
                IconButton(onClick = { viewModel.selectNextDay() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
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
            title = if (isViewingToday) "Ghi chú hôm nay (${uiState.selectedDateStr})" else uiState.selectedDateStr,
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, StatusOrange.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = StatusOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Task trễ từ ngày trước (${tasks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
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
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                        text = "Từ ngày: ${task.dateStr}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusOrange
                                    )
                                }

                                IconButton(
                                    onClick = { onMoveToToday(task) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowForward,
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
