package com.example.notifier

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AlarmScheduler {

    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    fun scheduleTimer(context: Context, timerId: String, config: TimerConfig) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val time = LocalTime.parse(config.time, formatter)
        val days = config.days.padEnd(7, '0').take(7).map { it == '1' }

        DayOfWeek.entries.forEachIndexed { index, day ->
            if (!days[index]) return@forEachIndexed
            val requestCode = (timerId + index).hashCode()
            val triggerMillis = nextTriggerMillis(day, time)

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("title", config.title)
                putExtra("text", config.text)
                putExtra("image", config.image)
                putExtra("color", Color(config.color).toArgb())
                putExtra("hash", requestCode)
                putExtra("timerId", timerId)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    // Launch intent to ask user to enable exact alarms
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    return
                }
            }


            Log.d("Timer", "Scheduling alarm for $triggerMillis (now: ${System.currentTimeMillis()})")

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }


    fun cancelTimer(context: Context, timerId: String) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (index in 0..6) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val requestCode = (timerId + index).hashCode()

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    private fun nextTriggerMillis(
        day: DayOfWeek,
        time: LocalTime,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val today = LocalDate.now(zoneId)
        val now = LocalDateTime.now(zoneId)

        // Start with today at the target time
        var candidate = today.atTime(time)

        // Calculate days until the target day
        val daysUntil = (day.value - today.dayOfWeek.value + 7) % 7
        candidate = candidate.plusDays(daysUntil.toLong())

        // If it's today and time has already passed, jump to next week
        if (candidate <= now) {
            candidate = candidate.plusWeeks(1)
        }

        return candidate
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
