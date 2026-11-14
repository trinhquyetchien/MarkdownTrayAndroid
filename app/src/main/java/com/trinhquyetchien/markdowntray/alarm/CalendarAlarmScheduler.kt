package com.trinhquyetchien.markdowntray.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.trinhquyetchien.markdowntray.data.StandaloneAlarm
import com.trinhquyetchien.markdowntray.data.StorageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.json.JSONArray

object CalendarAlarmScheduler {
    const val CHANNEL_ID = "full_alarm_channel"
    private const val CHANNEL_NAME = "Chuông Báo thức"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh phát chuông báo thức toàn màn hình và nhắc nhở công việc"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                setSound(alarmSoundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleUpcomingAlarms(context: Context, storageManager: StorageManager) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        scheduleCalendarAlarms(context, alarmManager, storageManager)
        scheduleStandaloneAlarms(context, alarmManager, storageManager)
    }

    private fun scheduleCalendarAlarms(context: Context, alarmManager: AlarmManager, storageManager: StorageManager) {
        val file = storageManager.getCalendarFile()
        if (!file.exists()) return

        val now = Calendar.getInstance()
        val nowMs = now.timeInMillis
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
        val jsWeekday = (now.get(Calendar.DAY_OF_WEEK) - 1) // 0=Sun, 1=Mon, ..., 6=Sat

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
                    val alarmEnabled = ev.optBoolean("alarmEnabled", false)
                    if (!alarmEnabled) continue

                    val leadMinutes = ev.optInt("alarmLeadMinutes", 0)
                    val targetCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, evHour)
                        set(Calendar.MINUTE, evMin)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.MINUTE, -leadMinutes)
                    }

                    val targetMs = targetCal.timeInMillis
                    if (targetMs > nowMs) {
                        val title = ev.optString("title", "Lịch biểu")
                        val note = ev.optString("note", "")
                        val leadText = if (leadMinutes > 0) " (Trước $leadMinutes phút)" else ""
                        val displayTitle = "[Lịch] $title$leadText"
                        val requestCode = (ev.optString("id", "${i}_$targetMs").hashCode() and 0x7FFFFFFF)

                        setAlarm(context, alarmManager, targetMs, requestCode, displayTitle, note)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleStandaloneAlarms(context: Context, alarmManager: AlarmManager, storageManager: StorageManager) {
        val standaloneAlarms = storageManager.loadStandaloneAlarms()
        val now = Calendar.getInstance()
        val nowMs = now.timeInMillis
        val jsWeekday = (now.get(Calendar.DAY_OF_WEEK) - 1) // 0=Sun..6=Sat

        for (alarm in standaloneAlarms) {
            if (!alarm.isEnabled) continue

            // Determine next trigger time
            val nextCal = getNextAlarmCalendar(alarm, now)
            val targetMs = nextCal.timeInMillis
            if (targetMs > nowMs) {
                val title = if (alarm.title.isNotBlank()) alarm.title else "Báo thức"
                val requestCode = (("standalone_" + alarm.id).hashCode() and 0x7FFFFFFF)
                val note = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)

                setAlarm(context, alarmManager, targetMs, requestCode, title, note)
            }
        }
    }

    private fun getNextAlarmCalendar(alarm: StandaloneAlarm, now: Calendar): Calendar {
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.repeatDays.isEmpty()) {
            // Once alarm: if past today, schedule for tomorrow
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target
        }

        // Repeating days
        for (i in 0..7) {
            val checkCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val weekday = (checkCal.get(Calendar.DAY_OF_WEEK) - 1)
            if (alarm.repeatDays.contains(weekday) && checkCal.timeInMillis > now.timeInMillis) {
                return checkCal
            }
        }

        return target
    }

    fun setAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMs: Long,
        requestCode: Int,
        title: String,
        note: String
    ) {
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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setWindow(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        60_000L,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelAlarm(context: Context, requestCode: Int) {
        val intent = Intent(context, CalendarAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.cancel(pendingIntent)
    }
}
