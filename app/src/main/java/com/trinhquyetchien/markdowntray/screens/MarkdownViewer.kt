package com.trinhquyetchien.markdowntray.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trinhquyetchien.markdowntray.data.MarkdownAnnotator
import com.trinhquyetchien.markdowntray.data.MarkdownNode
import com.trinhquyetchien.markdowntray.data.MarkdownParser
import com.trinhquyetchien.markdowntray.data.SyncStatusInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MarkdownViewer(
    title: String,
    rawContent: String,
    syncInfo: SyncStatusInfo?,
    onToggleTask: (lineIndex: Int, isChecked: Boolean) -> Unit,
    onAddTask: (taskText: String) -> Unit,
    onSaveRawContent: (newContent: String) -> Unit,
    onRefresh: () -> Unit,
    onDeleteTask: ((lineIndex: Int) -> Unit)? = null,
    onUpdateTaskText: ((lineIndex: Int, newText: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var newTaskText by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }

    val nodes = remember(rawContent) { MarkdownParser.parse(rawContent) }
    val taskNodes = nodes.filterIsInstance<MarkdownNode.Task>()
    val completedCount = taskNodes.count { it.isChecked }
    val totalCount = taskNodes.size
    val pendingCount = totalCount - completedCount
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    Column(modifier = modifier.fillMaxSize()) {
        // Modern Dashboard Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Sync status line with pulsing indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onRefresh() }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (syncInfo?.isSynced == true) Color(0xFF4CAF50)
                                    else Color(0xFFFF9800)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = syncInfo?.statusText ?: "Đang kiểm tra đồng bộ...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Làm mới",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Sửa markdown",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title & Progress percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (totalCount > 0) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (totalCount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stat Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBadge(
                            label = "Xong",
                            count = completedCount,
                            color = Color(0xFF4CAF50),
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                        StatBadge(
                            label = "Chờ",
                            count = pendingCount,
                            color = Color(0xFFFF9800),
                            icon = Icons.Default.PendingActions,
                            modifier = Modifier.weight(1f)
                        )
                        StatBadge(
                            label = "Tổng",
                            count = totalCount,
                            color = MaterialTheme.colorScheme.primary,
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Markdown Nodes List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (nodes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Chưa có nội dung ghi chú nào",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Nhập task bên dưới để bắt đầu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(nodes, key = { index, node -> "${index}_${node.hashCode()}" }) { _, node ->
                    when (node) {
                        is MarkdownNode.Task -> {
                            TaskCardItem(
                                task = node,
                                onCheckedChange = { isChecked ->
                                    onToggleTask(node.lineIndex, isChecked)
                                },
                                onDelete = onDeleteTask?.let { { it(node.lineIndex) } },
                                onUpdateText = onUpdateTaskText?.let { { newText -> it(node.lineIndex, newText) } }
                            )
                        }
                        is MarkdownNode.Header -> {
                            HeaderItem(header = node)
                        }
                        is MarkdownNode.BulletList -> {
                            BulletListItem(item = node)
                        }
                        is MarkdownNode.NumberedList -> {
                            NumberedListItem(item = node)
                        }
                        is MarkdownNode.BlockQuote -> {
                            BlockQuoteItem(text = node.text)
                        }
                        is MarkdownNode.NormalText -> {
                            NormalTextItem(item = node)
                        }
                        is MarkdownNode.Divider -> {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // Docked Quick-Add Task Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    placeholder = {
                        Text(
                            text = "Thêm nhiệm vụ mới...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newTaskText.isNotBlank()) {
                            onAddTask(newTaskText)
                            newTaskText = ""
                        }
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                IconButton(
                    onClick = {
                        if (newTaskText.isNotBlank()) {
                            onAddTask(newTaskText)
                            newTaskText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm task",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

    // Markdown Editor Dialog with Syntax Toolbar & Live Preview
    if (showEditDialog) {
        ModernMarkdownEditorDialog(
            initialContent = rawContent,
            onDismiss = { showEditDialog = false },
            onSave = { newContent ->
                onSaveRawContent(newContent)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun StatBadge(
    label: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$count $label",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun TaskCardItem(
    task: MarkdownNode.Task,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
    onUpdateText: ((String) -> Unit)? = null
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val cardBg by animateColorAsState(
        if (task.isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        label = "taskBg"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant

    val annotatedText = remember(task.text, primaryColor, secondaryColor, codeBgColor) {
        MarkdownAnnotator.annotate(task.text, primaryColor, secondaryColor, codeBgColor)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (task.indentLevel * 16).dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!task.isChecked) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (task.isChecked) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (task.isChecked) FontWeight.Normal else FontWeight.Medium
                )
            }

            if (onUpdateText != null) {
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Sửa task",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            if (onDelete != null) {
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa task",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // Edit Task Dialog
    if (showEditDialog && onUpdateText != null) {
        var editFieldValue by remember { mutableStateOf(task.text) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Chỉnh sửa nhiệm vụ", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editFieldValue,
                    onValueChange = { editFieldValue = it },
                    label = { Text("Nội dung task") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editFieldValue.isNotBlank()) {
                            onUpdateText(editFieldValue)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Lưu", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Delete Task Dialog
    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Xóa nhiệm vụ?", fontWeight = FontWeight.Bold) },
            text = { Text("\"${task.text}\" sẽ bị xóa khỏi file.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun BulletListItem(item: MarkdownNode.BulletList) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant
    val annotated = remember(item.text, primaryColor, secondaryColor, codeBgColor) {
        MarkdownAnnotator.annotate(item.text, primaryColor, secondaryColor, codeBgColor)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (item.indentLevel * 16 + 8).dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.FiberManualRecord,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 7.dp, end = 8.dp)
                .size(6.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NumberedListItem(item: MarkdownNode.NumberedList) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant
    val annotated = remember(item.text, primaryColor, secondaryColor, codeBgColor) {
        MarkdownAnnotator.annotate(item.text, primaryColor, secondaryColor, codeBgColor)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (item.indentLevel * 16 + 8).dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${item.number}.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BlockQuoteItem(text: String) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant
    val annotated = remember(text, primaryColor, secondaryColor, codeBgColor) {
        MarkdownAnnotator.annotate(text, primaryColor, secondaryColor, codeBgColor)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = annotated,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeaderItem(header: MarkdownNode.Header) {
    val (style, topPadding, bottomPadding, accentColor) = when (header.level) {
        1 -> Quadruple(MaterialTheme.typography.headlineSmall, 14.dp, 6.dp, MaterialTheme.colorScheme.primary)
        2 -> Quadruple(MaterialTheme.typography.titleLarge, 10.dp, 4.dp, MaterialTheme.colorScheme.secondary)
        else -> Quadruple(MaterialTheme.typography.titleMedium, 8.dp, 2.dp, MaterialTheme.colorScheme.tertiary)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = header.text,
            style = style,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun NormalTextItem(item: MarkdownNode.NormalText) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant
    val annotated = remember(item.text, primaryColor, secondaryColor, codeBgColor) {
        MarkdownAnnotator.annotate(item.text, primaryColor, secondaryColor, codeBgColor)
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernMarkdownEditorDialog(
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(initialContent, TextRange(initialContent.length)))
    }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Edit, 1 = Preview

    fun insertSyntax(prefix: String, suffix: String = "") {
        val sel = textFieldValue.selection
        val text = textFieldValue.text
        val selectedText = text.substring(sel.start, sel.end)
        val replacement = "$prefix$selectedText$suffix"
        val newText = text.replaceRange(sel.start, sel.end, replacement)
        val newCursorPos = if (selectedText.isEmpty()) sel.start + prefix.length else sel.start + replacement.length
        textFieldValue = TextFieldValue(newText, TextRange(newCursorPos))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Chỉnh sửa Markdown",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng")
                        }
                    },
                    actions = {
                        TextButton(onClick = { onSave(textFieldValue.text) }) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lưu", fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (selectedTab == 0) {
                    Surface(
                        tonalElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            EditorToolButton(text = "[ ] Task") { insertSyntax("\n- [ ] ") }
                            EditorToolButton(text = "# H1") { insertSyntax("\n# ") }
                            EditorToolButton(text = "## H2") { insertSyntax("\n## ") }
                            EditorToolButton(text = "**B**") { insertSyntax("**", "**") }
                            EditorToolButton(text = "*I*") { insertSyntax("*", "*") }
                            EditorToolButton(text = "`Code`") { insertSyntax("`", "`") }
                            EditorToolButton(text = "- List") { insertSyntax("\n- ") }
                            EditorToolButton(text = "1. List") { insertSyntax("\n1. ") }
                            EditorToolButton(text = "> Quote") { insertSyntax("\n> ") }
                            EditorToolButton(text = "📅 Ngày") {
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                insertSyntax(" $today ")
                            }
                            EditorToolButton(text = "---") { insertSyntax("\n---\n") }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Soạn thảo")
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Xem trước")
                            }
                        }
                    )
                }

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        placeholder = { Text("Nội dung markdown...") },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 22.sp
                        )
                    )
                } else {
                    val previewNodes = remember(textFieldValue.text) { MarkdownParser.parse(textFieldValue.text) }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(previewNodes) { node ->
                            when (node) {
                                is MarkdownNode.Task -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (node.isChecked) Icons.Default.CheckCircle else Icons.Default.PendingActions,
                                            contentDescription = null,
                                            tint = if (node.isChecked) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = MarkdownAnnotator.annotate(
                                                node.text,
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary,
                                                MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                textDecoration = if (node.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                            ),
                                            color = if (node.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                is MarkdownNode.Header -> HeaderItem(header = node)
                                is MarkdownNode.BulletList -> BulletListItem(item = node)
                                is MarkdownNode.NumberedList -> NumberedListItem(item = node)
                                is MarkdownNode.BlockQuote -> BlockQuoteItem(text = node.text)
                                is MarkdownNode.NormalText -> NormalTextItem(item = node)
                                is MarkdownNode.Divider -> HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorToolButton(text: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = text, style = MaterialTheme.typography.labelSmall) },
        shape = RoundedCornerShape(8.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}
