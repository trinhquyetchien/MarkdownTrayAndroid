package com.trinhquyetchien.markdowntray.alarm

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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val storageManager = StorageManager(context)
            CalendarAlarmScheduler.scheduleUpcomingAlarms(context, storageManager)
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Nhắc nhở lịch"
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

        val notification = NotificationCompat.Builder(context, CalendarAlarmScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔔 $title")
            .setContentText(note.ifEmpty { "Đã đến giờ theo lịch biểu" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.ifEmpty { "Đã đến giờ theo lịch biểu" }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_ID = "extra_id"
    }
}
