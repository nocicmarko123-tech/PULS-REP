package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.NextTask
import com.example.domain.SpeedLevel

class WorkoutNotificationHelper(private val context: Context) {

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val timerChannel = NotificationChannel(
                CHANNEL_TIMER,
                context.getString(R.string.notif_channel_timer_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_timer_desc)
                enableVibration(true)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                context.getString(R.string.notif_channel_reminder_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_reminder_desc)
                enableVibration(true)
            }

            manager.createNotificationChannel(timerChannel)
            manager.createNotificationChannel(reminderChannel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showRestCompletedNotification(nextTask: NextTask, speedLevel: SpeedLevel) {
        if (!hasNotificationPermission()) return
        val contentIntent = createOpenAppIntent()
        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏱️ Pauza je gotova! Vreme je za seriju")
            .setContentText("🔔 Sledeće: ${nextTask.formatHeadline(speedLevel)}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🔔 Sledeće: ${nextTask.formatHeadline(speedLevel)}\n${speedLevel.tempoDescriptionSr}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_TIMER, notification)
        }
    }

    fun showNextTaskReminderNotification(nextTask: NextTask, speedLevel: SpeedLevel) {
        if (!hasNotificationPermission()) return
        val contentIntent = createOpenAppIntent()
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🔔 Podsetnik za trening (${speedLevel.bolts})")
            .setContentText("Sledeće: ${nextTask.formatHeadline(speedLevel)}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Sledeće: ${nextTask.formatHeadline(speedLevel)}\nOtvori RepPuls i klikni \"Urađeno — sledeći korak\".")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_REMINDER, notification)
        }
    }

    fun scheduleReminderAlarm(intervalMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = createReminderPendingIntent()
        alarmManager.cancel(pendingIntent)

        if (intervalMinutes <= 0) return

        val triggerAtMillis = System.currentTimeMillis() + intervalMinutes * 60_000L
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminderAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = createReminderPendingIntent()
        alarmManager.cancel(pendingIntent)
    }

    private fun createOpenAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createReminderPendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_TIMER = "reppuls_timer_channel"
        const val CHANNEL_REMINDER = "reppuls_reminder_channel"
        private const val NOTIF_ID_TIMER = 1001
        private const val NOTIF_ID_REMINDER = 1002
        private const val REQUEST_CODE_ALARM = 2001
    }
}
