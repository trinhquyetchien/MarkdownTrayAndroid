package com.trinhquyetchien.markdowntray.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.trinhquyetchien.markdowntray.MainActivity
import com.trinhquyetchien.markdowntray.R
import com.trinhquyetchien.markdowntray.data.CalendarRepository
import com.trinhquyetchien.markdowntray.data.CalendarUtils
import com.trinhquyetchien.markdowntray.data.StorageManager
import java.util.Locale

class CalendarEventsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val storageManager = StorageManager(context)
        val file = storageManager.getCalendarFile()
        val events = CalendarRepository.loadEvents(file)

        val todayDateStr = storageManager.getTodayDateString()
        val occurrences = CalendarUtils.getOccurrencesForDate(events, todayDateStr)

        val displayContent = if (occurrences.isEmpty()) {
            "Không có lịch hẹn nào hôm nay"
        } else {
            occurrences.take(4).joinToString("\n") { occ ->
                val timeStr = String.format(Locale.US, "%02d:%02d-%02d:%02d", occ.startHour, occ.startMinute, occ.endHour, occ.endMinute)
                "⏰ $timeStr: ${occ.event.title}"
            }
        }

        val badgeText = "${occurrences.size} lịch"

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_calendar_events).apply {
                setTextViewText(R.id.widget_title, "Lịch hôm nay ($todayDateStr)")
                setTextViewText(R.id.widget_badge, badgeText)
                setTextViewText(R.id.widget_content, displayContent)

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    2,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
