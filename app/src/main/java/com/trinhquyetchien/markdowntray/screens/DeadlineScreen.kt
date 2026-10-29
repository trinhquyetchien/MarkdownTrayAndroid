package com.trinhquyetchien.markdowntray.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.viewmodel.AppViewModelFactory
import com.trinhquyetchien.markdowntray.viewmodel.DeadlineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlineScreen(
    storageManager: StorageManager,
    modifier: Modifier = Modifier,
    viewModel: DeadlineViewModel = viewModel(
        factory = AppViewModelFactory(LocalContext.current, storageManager)
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = uiState.deadlineAlias,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            actions = {
                IconButton(onClick = {
                    viewModel.loadContent()
                    Toast.makeText(context, "Đã làm mới Deadline", Toast.LENGTH_SHORT).show()
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

        MarkdownViewer(
            title = uiState.deadlineAlias,
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
