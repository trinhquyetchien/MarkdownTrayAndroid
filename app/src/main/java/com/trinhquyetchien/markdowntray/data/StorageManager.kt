package com.trinhquyetchien.markdowntray.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StorageManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("markdown_tray_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DAILY_DIR = "key_daily_dir"
        private const val KEY_DAILY_ALIAS = "key_daily_alias"
        private const val KEY_DEADLINE_FILE = "key_deadline_file"
        private const val KEY_DEADLINE_ALIAS = "key_deadline_alias"
        private const val KEY_CALENDAR_FILE = "key_calendar_file"

        val DEFAULT_STORAGE_ROOT: String = Environment.getExternalStorageDirectory().absolutePath
        val DEFAULT_DAILY_DIR: String = "$DEFAULT_STORAGE_ROOT/Note/Daily"
        val DEFAULT_DEADLINE_FILE: String = "$DEFAULT_STORAGE_ROOT/Note/Daily/Deadline.md"
        val DEFAULT_CALENDAR_FILE: String = "$DEFAULT_STORAGE_ROOT/Note/calendar.json"
    }

    var dailyNotesDir: String
        get() = prefs.getString(KEY_DAILY_DIR, DEFAULT_DAILY_DIR) ?: DEFAULT_DAILY_DIR
        set(value) = prefs.edit().putString(KEY_DAILY_DIR, value).apply()

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
            val initial = "# $dateStr\n\n- [ ] Important task today\n"
            writeText(file, initial)
        }
        return file
    }

    fun ensureDeadlineFileExists(): File {
        val file = getDeadlineFile()
        if (!file.exists() || file.length() == 0L) {
            val initial = "# Deadlines\n\n- [ ] Example deadline task\n"
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
