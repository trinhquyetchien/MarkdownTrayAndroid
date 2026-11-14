package com.trinhquyetchien.markdowntray.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.trinhquyetchien.markdowntray.MainActivity
import com.trinhquyetchien.markdowntray.R
import com.trinhquyetchien.markdowntray.data.StorageManager

class CalendarAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val storageManager = StorageManager(context)
            CalendarAlarmScheduler.scheduleUpcomingAlarms(context, storageManager)
            return
        }

        if (intent.action == ACTION_DISMISS) {
            val notificationId = intent.getIntExtra(EXTRA_ID, 0)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Nhắc nhở công việc"
        val note = intent.getStringExtra(EXTRA_NOTE) ?: ""
        val notificationId = intent.getIntExtra(EXTRA_ID, (System.currentTimeMillis() % 100000).toInt())

        CalendarAlarmScheduler.createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(context, CalendarAlarmReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CalendarAlarmScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ $title")
            .setContentText(note.ifEmpty { "Đến giờ thực hiện công việc" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.ifEmpty { "Đến giờ thực hiện công việc" }))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Mở ứng dụng", pendingIntent)
            .addAction(0, "Bỏ qua", dismissPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val ACTION_DISMISS = "com.trinhquyetchien.markdowntray.ACTION_DISMISS_ALARM"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_ID = "extra_id"
    }
}

