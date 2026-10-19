package com.trinhquyetchien.markdowntray.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SyncStatusInfo(
    val exists: Boolean,
    val lastModified: Long,
    val sizeBytes: Long,
    val statusText: String,
    val isSynced: Boolean,
    val relativeTime: String
)

class DirectoryObserver(
    private val directory: File,
    private val onFileChanged: (path: String?) -> Unit
) {
    private var observer: FileObserver? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun start() {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        stop()
        val mask = FileObserver.CLOSE_WRITE or FileObserver.CREATE or FileObserver.DELETE or FileObserver.MOVED_TO
        observer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(directory, mask) {
                override fun onEvent(event: Int, path: String?) {
                    mainHandler.post { onFileChanged(path) }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            object : FileObserver(directory.absolutePath, mask) {
                override fun onEvent(event: Int, path: String?) {
                    mainHandler.post { onFileChanged(path) }
                }
            }
        }
        try {
            observer?.startWatching()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            observer?.stopWatching()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        observer = null
    }
}

class StorageManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("markdown_tray_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DAILY_DIR = "key_daily_dir"
        private const val KEY_DAILY_ALIAS = "key_daily_alias"
        private const val KEY_DEADLINE_FILE = "key_deadline_file"
        private const val KEY_DEADLINE_ALIAS = "key_deadline_alias"
        private const val KEY_CALENDAR_FILE = "key_calendar_file"
        private const val KEY_NOTES_ROOT_DIR = "key_notes_root_dir"

        val DEFAULT_STORAGE_ROOT: String = Environment.getExternalStorageDirectory().absolutePath
        val DEFAULT_DAILY_DIR: String = "$DEFAULT_STORAGE_ROOT/Note/Daily"
        val DEFAULT_NOTES_ROOT_DIR: String = "$DEFAULT_STORAGE_ROOT/Note"
        val DEFAULT_DEADLINE_FILE: String = "$DEFAULT_STORAGE_ROOT/Note/Daily/Deadline.md"
        val DEFAULT_CALENDAR_FILE: String = "$DEFAULT_STORAGE_ROOT/Note/calendar.json"
    }

    var dailyNotesDir: String
        get() = prefs.getString(KEY_DAILY_DIR, DEFAULT_DAILY_DIR) ?: DEFAULT_DAILY_DIR
        set(value) = prefs.edit().putString(KEY_DAILY_DIR, value).apply()

    var notesRootDir: String
        get() {
            val saved = prefs.getString(KEY_NOTES_ROOT_DIR, null)
            if (!saved.isNullOrEmpty()) return saved
            val dailyParent = File(dailyNotesDir).parentFile?.absolutePath
            return dailyParent ?: DEFAULT_NOTES_ROOT_DIR
        }
        set(value) = prefs.edit().putString(KEY_NOTES_ROOT_DIR, value).apply()

    var dailyAlias: String
        get() = prefs.getString(KEY_DAILY_ALIAS, "Daily List") ?: "Daily List"
        set(value) = prefs.edit().putString(KEY_DAILY_ALIAS, value).apply()

    var deadlineFilePath: String
        get() = prefs.getString(KEY_DEADLINE_FILE, DEFAULT_DEADLINE_FILE) ?: DEFAULT_DEADLINE_FILE
        set(value) = prefs.edit().putString(KEY_DEADLINE_FILE, value).apply()

    var deadlineAlias: String
        get() = prefs.getString(KEY_DEADLINE_ALIAS, "Deadline") ?: "Deadline"
        set(value) = prefs.edit().putString(KEY_DEADLINE_ALIAS, value).apply()

    var calendarFilePath: String
        get() = prefs.getString(KEY_CALENDAR_FILE, DEFAULT_CALENDAR_FILE) ?: DEFAULT_CALENDAR_FILE
        set(value) = prefs.edit().putString(KEY_CALENDAR_FILE, value).apply()

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getTodayFile(): File {
        val todayStr = "${getTodayDateString()}.md"
        return File(dailyNotesDir, todayStr)
    }

    fun getDeadlineFile(): File {
        return File(deadlineFilePath)
    }

    fun getCalendarFile(): File {
        return File(calendarFilePath)
    }

    fun getNotesRootFolder(): File {
        val dir = File(notesRootDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun readText(file: File): String {
        return try {
            if (file.exists()) file.readText(Charsets.UTF_8) else ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun writeText(file: File, content: String): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun ensureTodayFileExists(): File {
        val file = getTodayFile()
        if (!file.exists() || file.length() == 0L) {
            val dateStr = getTodayDateString()
            val initial = "# $dateStr\n\n- [ ] Ghi chú công việc hôm nay\n"
            writeText(file, initial)
        }
        return file
    }

    fun ensureDeadlineFileExists(): File {
        val file = getDeadlineFile()
        if (!file.exists() || file.length() == 0L) {
            val initial = "# Deadlines\n\n- [ ] Nhiệm vụ quan trọng\n"
            writeText(file, initial)
        }
        return file
    }

    fun toggleTask(file: File, lineIndex: Int, isChecked: Boolean): Boolean {
        return try {
            if (!file.exists()) return false
            val lines = file.readLines(Charsets.UTF_8).toMutableList()
            if (lineIndex in lines.indices) {
                var line = lines[lineIndex]
                val uncheckRegex = Regex("^(\\s*[-*]\\s*)\\[ \\]\\s+")
                val checkRegex = Regex("^(\\s*[-*]\\s*)\\[[xX]\\]\\s+")

                line = if (isChecked) {
                    uncheckRegex.replace(line) { m -> "${m.groupValues[1]}[x] " }
                } else {
                    checkRegex.replace(line) { m -> "${m.groupValues[1]}[ ] " }
                }
                lines[lineIndex] = line
                file.writeText(lines.joinToString("\n"), Charsets.UTF_8)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun appendTask(file: File, taskText: String): Boolean {
        return try {
            val cleanTask = taskText.trim()
            if (cleanTask.isEmpty()) return false

            val currentText = if (file.exists()) file.readText(Charsets.UTF_8) else ""
            val newText = if (currentText.isEmpty()) {
                val header = if (file.name.endsWith(".md")) {
                    "# ${file.nameWithoutExtension}\n\n"
                } else ""
                "$header- [ ] $cleanTask\n"
            } else {
                val separator = if (currentText.endsWith("\n")) "" else "\n"
                "$currentText$separator- [ ] $cleanTask\n"
            }
            writeText(file, newText)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getFileSyncInfo(file: File): SyncStatusInfo {
        if (!file.exists()) {
            return SyncStatusInfo(
                exists = false,
                lastModified = 0L,
                sizeBytes = 0L,
                statusText = "Chưa có file (chờ Syncthing)",
                isSynced = false,
                relativeTime = "Chưa tạo"
            )
        }

        val lastMod = file.lastModified()
        val now = System.currentTimeMillis()
        val diffMs = now - lastMod

        val relative = when {
            diffMs < 30_000L -> "Vừa xong"
            diffMs < 60_000L -> "Khoảng 1 phút trước"
            diffMs < 3600_000L -> "${diffMs / 60_000L} phút trước"
            diffMs < 86400_000L -> "${diffMs / 3600_000L} giờ trước"
            else -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(lastMod))
        }

        val sizeStr = formatFileSize(file.length())
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastMod))

        return SyncStatusInfo(
            exists = true,
            lastModified = lastMod,
            sizeBytes = file.length(),
            statusText = "Đã đồng bộ ($relative lúc $timeStr • $sizeStr)",
            isSynced = true,
            relativeTime = relative
        )
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    fun createDirectoryObserver(dir: File, onFileChanged: (path: String?) -> Unit): DirectoryObserver {
        return DirectoryObserver(dir, onFileChanged)
    }

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun getRequestStoragePermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }
}
