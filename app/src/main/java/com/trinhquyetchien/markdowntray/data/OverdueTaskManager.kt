package com.trinhquyetchien.markdowntray.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class OverdueTask(
    val file: File,
    val dateStr: String,
    val lineIndex: Int,
    val text: String,
    val indentLevel: Int
)

object OverdueTaskManager {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Tìm tất cả các task chưa hoàn thành trong thư mục Daily trước ngày targetDateStr
     */
    fun getOverdueTasks(dailyDir: File, beforeDateStr: String): List<OverdueTask> {
        if (!dailyDir.exists() || !dailyDir.isDirectory) return emptyList()

        val files = dailyDir.listFiles { file ->
            file.isFile && file.name.endsWith(".md", ignoreCase = true)
        } ?: return emptyList()

        val overdueList = mutableListOf<OverdueTask>()

        // Lọc các file có tên yyyy-MM-dd.md nhỏ hơn beforeDateStr
        for (file in files) {
            val datePart = file.nameWithoutExtension
            if (datePart.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) && datePart < beforeDateStr) {
                try {
                    val content = file.readText(Charsets.UTF_8)
                    val nodes = MarkdownParser.parse(content)
                    for (node in nodes) {
                        if (node is MarkdownNode.Task && !node.isChecked) {
                            overdueList.add(
                                OverdueTask(
                                    file = file,
                                    dateStr = datePart,
                                    lineIndex = node.lineIndex,
                                    text = node.text,
                                    indentLevel = node.indentLevel
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Sắp xếp ngày gần nhất lên trước
        return overdueList.sortedWith(compareByDescending<OverdueTask> { it.dateStr }.thenBy { it.lineIndex })
    }

    /**
     * Chuyển một task trễ sang ngày hôm nay (append vào todayFile và đánh dấu/xóa ở file cũ)
     */
    fun moveTaskToToday(
        storageManager: StorageManager,
        overdueTask: OverdueTask,
        todayFile: File
    ): Boolean {
        // 1. Thêm vào file hôm nay
        val added = storageManager.appendTask(todayFile, overdueTask.text)
        if (!added) return false

        // 2. Xóa khỏi file cũ
        return storageManager.deleteTask(overdueTask.file, overdueTask.lineIndex)
    }
}
