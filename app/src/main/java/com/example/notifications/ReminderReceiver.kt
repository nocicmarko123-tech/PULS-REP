package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.RepPulsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as? RepPulsApp ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = app.repository.getSettingsOnce()
                val nextTask = app.repository.getNextTaskOnce()
                if (settings.steadyReminderActive &&
                    settings.notificationsEnabled &&
                    settings.reminderIntervalMinutes > 0
                ) {
                    app.notificationHelper.showNextTaskReminderNotification(
                        nextTask = nextTask,
                        speedLevel = settings.speedLevel
                    )
                    val nextAt = System.currentTimeMillis() + settings.reminderIntervalMinutes * 60_000L
                    app.repository.setSteadyReminderState(
                        active = true,
                        intervalMinutes = settings.reminderIntervalMinutes,
                        nextAtMillis = nextAt
                    )
                    app.notificationHelper.scheduleReminderAlarm(settings.reminderIntervalMinutes)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
