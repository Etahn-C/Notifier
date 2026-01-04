package com.example.notifier

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import kotlin.collections.get

class AlarmReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {

        val title = intent.getStringExtra("title") ?: return
        val text = intent.getStringExtra("text") ?: return
        val image = intent.getStringExtra("image")
        val color = intent.getIntExtra("color", 0xFF000000.toInt())
        val hash = intent.getIntExtra("hash", 1001)
        val timerId = intent.getStringExtra("timerId")

        NotificationHelper.showNotification(
            context,
            title,
            text,
            image,
            color,
            hash
        )
        val timers = loadTimers(context)
        val timer = timers[timerId] ?: return
        if (timer.on) {
            AlarmScheduler.scheduleTimer(context, timerId!!, timer)
        }
    }
}

