package com.trinhquyetchien.markdowntray.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.trinhquyetchien.markdowntray.MainActivity
import com.trinhquyetchien.markdowntray.R
import com.trinhquyetchien.markdowntray.data.StorageManager

class CalendarAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            val storageManager = StorageManager(context)
            CalendarAlarmScheduler.scheduleUpcomingAlarms(context, storageManager)
            return
        }

        val notificationId = intent.getIntExtra(EXTRA_ID, (System.currentTimeMillis() % 100000).toInt())
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Báo thức"
        val note = intent.getStringExtra(EXTRA_NOTE) ?: ""

        if (action == ACTION_DISMISS) {
            AlarmSoundService.stop(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
            return
        }

        if (action == ACTION_SNOOZE) {
            AlarmSoundService.stop(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            // Snooze for 5 minutes
            val snoozeMs = System.currentTimeMillis() + 5 * 60 * 1000L
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                CalendarAlarmScheduler.setAlarm(
                    context,
                    alarmManager,
                    snoozeMs,
                    notificationId,
                    title,
                    "$note (Báo lại)"
                )
            }
            return
        }

        // Trigger Alarm: Start Alarm sound & vibration service
        AlarmSoundService.start(context)

        // Make sure the notification channel is ready
        CalendarAlarmScheduler.createNotificationChannel(context)

        // Intent to open app when clicking notification
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for Dismiss button
        val dismissIntent = Intent(context, CalendarAlarmReceiver::class.java).apply {
            this.action = ACTION_DISMISS
            putExtra(EXTRA_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for Snooze 5m button
        val snoozeIntent = Intent(context, CalendarAlarmReceiver::class.java).apply {
            this.action = ACTION_SNOOZE
            putExtra(EXTRA_ID, notificationId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_NOTE, note)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CalendarAlarmScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ $title")
            .setContentText(note.ifEmpty { "Đến giờ hẹn báo thức!" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.ifEmpty { "Đến giờ hẹn báo thức!" }))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true) // Heads-up / Full-screen when screen off
            .addAction(0, "TẮT BÁO THỨC", dismissPendingIntent)
            .addAction(0, "BÁO LẠI (5p)", snoozePendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val ACTION_DISMISS = "com.trinhquyetchien.markdowntray.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE = "com.trinhquyetchien.markdowntray.ACTION_SNOOZE_ALARM"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_ID = "extra_id"
    }
}
