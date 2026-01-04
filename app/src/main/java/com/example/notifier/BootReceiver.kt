package com.example.notifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        Thread {
            try {
                val timers = loadTimers(context)
                timers.forEach { (id, timer) ->
                    if (timer.on) {
                        AlarmScheduler.scheduleTimer(context, id, timer)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}
