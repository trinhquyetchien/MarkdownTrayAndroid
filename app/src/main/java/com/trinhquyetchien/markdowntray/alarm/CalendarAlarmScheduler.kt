package com.trinhquyetchien.markdowntray.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.trinhquyetchien.markdowntray.data.StorageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

object CalendarAlarmScheduler {
    const val CHANNEL_ID = "calendar_reminders_channel"
    private const val CHANNEL_NAME = "Nhắc nhở lịch biểu"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nhắc nhở khi đến giờ các sự kiện trong lịch"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleUpcomingAlarms(context: Context, storageManager: StorageManager) {
        val file = storageManager.getCalendarFile()
        if (!file.exists()) return

        val now = Calendar.getInstance()
        val nowMs = now.timeInMillis
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
        val jsWeekday = (now.get(Calendar.DAY_OF_WEEK) - 1) // 0=Sun, 1=Mon, ..., 6=Sat

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        try {
            val content = file.readText(Charsets.UTF_8).trim()
            if (content.isEmpty()) return
            val array = JSONArray(content)

            for (i in 0 until array.length()) {
                val ev = array.optJSONObject(i) ?: continue
                val evStart = ev.optString("start")
                if (evStart.length < 16) continue

                val evDate = evStart.substring(0, 10)
                val evTime = evStart.substring(11, 16)
                val parts = evTime.split(":")
                if (parts.size < 2) continue
                val evHour = parts[0].toIntOrNull() ?: continue
                val evMin = parts[1].toIntOrNull() ?: continue

                var isToday = false
                val repeatDays = ev.optJSONArray("repeatDays")
                if (repeatDays != null && repeatDays.length() > 0) {
                    val repeatList = mutableListOf<Int>()
                    for (j in 0 until repeatDays.length()) {
                        repeatList.add(repeatDays.getInt(j))
                    }
                    val anchorDateStr = evDate
                    if (anchorDateStr <= todayStr && repeatList.contains(jsWeekday)) {
                        val excDates = ev.optJSONArray("exceptionDates")
                        var isException = false
                        if (excDates != null) {
                            for (k in 0 until excDates.length()) {
                                if (excDates.getString(k) == todayStr) {
                                    isException = true
                                    break
                                }
                            }
                        }
                        val untilDate = ev.optString("untilDate")
                        if (!isException && (untilDate.isEmpty() || todayStr < untilDate)) {
                            isToday = true
                        }
                    }
                } else {
                    if (evDate == todayStr) {
                        isToday = true
                    }
                }

                if (isToday) {
                    val targetCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, evHour)
                        set(Calendar.MINUTE, evMin)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val targetMs = targetCal.timeInMillis
                    if (targetMs > nowMs) {
                        val title = ev.optString("title", "Lịch biểu")
                        val note = ev.optString("note", "")
                        val requestCode = (ev.optString("id", "${i}_$targetMs").hashCode() and 0x7FFFFFFF)

                        val intent = Intent(context, CalendarAlarmReceiver::class.java).apply {
                            putExtra(CalendarAlarmReceiver.EXTRA_TITLE, title)
                            putExtra(CalendarAlarmReceiver.EXTRA_NOTE, note)
                            putExtra(CalendarAlarmReceiver.EXTRA_ID, requestCode)
                        }

                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    targetMs,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.setWindow(
                                    AlarmManager.RTC_WAKEUP,
                                    targetMs,
                                    60_000L,
                                    pendingIntent
                                )
                            }
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                targetMs,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                targetMs,
                                pendingIntent
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
