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

class DeadlineTasksWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val storageManager = StorageManager(context)
        val file = storageManager.getDeadlineFile()

        val textContent = if (file.exists()) storageManager.readText(file) else ""
        val nodes = MarkdownParser.parse(textContent)
        val tasks = nodes.filterIsInstance<MarkdownNode.Task>()
        val pendingTasks = tasks.filter { !it.isChecked }

        val displayContent = if (pendingTasks.isEmpty()) {
            if (tasks.isNotEmpty()) "Tất cả hạn chót đã hoàn tất! ✨"
            else "Chưa có deadline nào được ghi nhận"
        } else {
            pendingTasks.take(5).joinToString("\n") { "• ${it.text}" }
        }

        val badgeText = "${pendingTasks.size} việc"

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_deadline_tasks).apply {
                setTextViewText(R.id.widget_title, "Deadline quan trọng")
                setTextViewText(R.id.widget_badge, badgeText)
                setTextViewText(R.id.widget_content, displayContent)

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    1,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
