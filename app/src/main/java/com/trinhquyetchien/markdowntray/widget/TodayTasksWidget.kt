package com.trinhquyetchien.markdowntray.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.trinhquyetchien.markdowntray.MainActivity
import com.trinhquyetchien.markdowntray.R
import com.trinhquyetchien.markdowntray.data.MarkdownNode
import com.trinhquyetchien.markdowntray.data.MarkdownParser
import com.trinhquyetchien.markdowntray.data.StorageManager

class TodayTasksWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val storageManager = StorageManager(context)
        val todayFile = storageManager.getTodayFile()

        val textContent = if (todayFile.exists()) storageManager.readText(todayFile) else ""
        val nodes = MarkdownParser.parse(textContent)
        val tasks = nodes.filterIsInstance<MarkdownNode.Task>()
        val pendingTasks = tasks.filter { !it.isChecked }

        val displayContent = if (pendingTasks.isEmpty()) {
            if (tasks.isNotEmpty()) "Tất cả ${tasks.size} công việc đã xong! 🎉"
            else "Chưa có công việc nào trong ngày"
        } else {
            pendingTasks.take(5).joinToString("\n") { "• ${it.text}" }
        }

        val badgeText = "${pendingTasks.size} việc"

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_today_tasks).apply {
                setTextViewText(R.id.widget_title, "Hôm nay (${storageManager.getTodayDateString()})")
                setTextViewText(R.id.widget_badge, badgeText)
                setTextViewText(R.id.widget_content, displayContent)

                // Nhấn vào widget để mở trực tiếp App
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
