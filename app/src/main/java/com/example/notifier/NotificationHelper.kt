package com.example.notifier

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.File
import androidx.core.graphics.ColorUtils


object NotificationHelper {

    const val CHANNEL_ID = "notifier_custom_notification_channel"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Custom Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Custom image notifications"
            enableVibration(true)
            enableLights(true)
        }

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNotification(
        context: Context,
        title: String = "Title",
        text: String = "Text",
        imgPath: String?,
        backgroundColor: Int,
        hash: Int = 1001
    ) {

        val remoteViews = RemoteViews(
            context.packageName,
            R.layout.custom_notification
        ).apply {
            setTextViewText(R.id.title, title)
            setTextViewText(R.id.message, text)
            val textColor =
                if (ColorUtils.calculateLuminance(backgroundColor) < 0.5)
                    Color.WHITE
                else
                    Color.BLACK
            setTextColor(R.id.title, textColor)
            setTextColor(R.id.message, textColor)
            if (imgPath != null) {
                val file = File(imgPath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    setImageViewBitmap(R.id.rightImage, bitmap)
                }
            }

            setInt(R.id.root, "setBackgroundColor", backgroundColor)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notifier_icon_fg)
            .setCustomContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(hash, notification)
    }

}
